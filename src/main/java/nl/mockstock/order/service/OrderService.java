package nl.mockstock.order.service;

import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.mockstock.order.client.datastream.DataStreamClient;
import nl.mockstock.order.client.portfolio.PortfolioClient;
import nl.mockstock.order.dto.datastream.response.StockPriceResponseDTO;
import nl.mockstock.order.dto.portfolio.request.ReservationRequestDTO;
import nl.mockstock.order.dto.portfolio.response.ReservationResponseDTO;
import nl.mockstock.order.dto.request.FillRequestDTO;
import nl.mockstock.order.dto.request.OrderRequestDTO;
import nl.mockstock.order.exception.OrderValidationException;
import nl.mockstock.order.model.Order;
import nl.mockstock.order.model.OrderFactory;
import nl.mockstock.order.model.OrderSide;
import nl.mockstock.order.model.OrderStatus;
import nl.mockstock.order.model.OrderType;
import nl.mockstock.order.repository.OrderRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepository orderRepository;
  private final OrderFactory orderFactory;
  private final IdempotencyService idempotencyService;

  private final DataStreamClient dataStreamClient;
  private final PortfolioClient portfolioClient;

  @Transactional
  public Order placeOrder(OrderRequestDTO orderRequest) {
    return idempotencyService.execute(
        orderRequest.idempotencyKey(), () -> doPlaceOrder(orderRequest));
  }

  @Transactional
  public Order cancelOrder(UUID orderId, UUID userId) {
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new EntityNotFoundException("Order not found"));

    if (!order.getUserId().equals(userId)) {
      throw new SecurityException("User is not authorized to cancel this order");
    }

    if (order.getStatus() == OrderStatus.CANCELLED) {
      return order;
    }

    if (order.getReservationId() != null) {
      portfolioClient.cancelReservation(order.getReservationId()).block();
    }

    order.markCancelled();
    return order;
  }

  @Transactional(readOnly = true)
  public Page<Order> getOrdersByUserId(UUID userId, Pageable pageable) {
    return orderRepository.findByUserId(userId, pageable);
  }

  @Transactional
  public void executeScheduledOrder(Order order) {
    Order managedOrder =
        orderRepository
            .findById(order.getId())
            .orElseThrow(() -> new EntityNotFoundException("Order not found: " + order.getId()));

    BigDecimal currentPrice = getCurrentStockPrice(managedOrder.getSymbol());
    executeOrder(managedOrder, currentPrice);
  }

  @Transactional
  public void executeLimitOrder(Order order, BigDecimal currentPrice) {
    Order managedOrder =
        orderRepository
            .findById(order.getId())
            .orElseThrow(() -> new EntityNotFoundException("Order not found: " + order.getId()));

    executeOrder(managedOrder, currentPrice);
  }

  @Transactional
  public void expireOrder(Order order) {
    Order managedOrder =
        orderRepository
            .findById(order.getId())
            .orElseThrow(() -> new EntityNotFoundException("Order not found: " + order.getId()));

    if (managedOrder.getReservationId() != null) {
      portfolioClient.cancelReservation(managedOrder.getReservationId()).block();
    }
    managedOrder.markExpired();
  }

  public BigDecimal getCurrentStockPrice(String symbol) {
    StockPriceResponseDTO stock = dataStreamClient.getStock(symbol).block();

    if (stock == null) {
      throw new OrderValidationException("Stock not found: " + symbol);
    }

    return stock.price();
  }

  private Order doPlaceOrder(OrderRequestDTO orderRequest) {
    BigDecimal currentStockPrice = getCurrentStockPrice(orderRequest.symbol());
    UUID reservationId = reserveResources(orderRequest, currentStockPrice);

    registerReservationCleanup(reservationId);

    Order order = createAndPersistOrder(orderRequest, reservationId);

    if (isImmediateMarketOrder(orderRequest)) {
      executeOrder(order, currentStockPrice);
    }

    return order;
  }

  private void executeOrder(Order order, BigDecimal price) {
    FillRequestDTO fillRequest =
        new FillRequestDTO(
            order.getUserId(),
            order.getSymbol(),
            order.getSide(),
            order.getQuantity(),
            price,
            order.getReservationId());

    portfolioClient.notifyFillOrder(fillRequest).block();
    order.markFulfilled(price);
  }

  private Order createAndPersistOrder(OrderRequestDTO orderRequest, UUID reservationId) {
    Order order;

    if (orderRequest.scheduledFor() != null) {
      order =
          orderFactory.createScheduledOrder(
              orderRequest.userId(),
              orderRequest.idempotencyKey(),
              orderRequest.side(),
              orderRequest.symbol(),
              orderRequest.quantity(),
              orderRequest.scheduledFor(),
              reservationId);
    } else if (orderRequest.type() == OrderType.LIMIT) {
      order =
          orderFactory.createLimitOrder(
              orderRequest.userId(),
              orderRequest.idempotencyKey(),
              orderRequest.side(),
              orderRequest.symbol(),
              orderRequest.quantity(),
              orderRequest.limitPrice(),
              orderRequest.expiresAt(),
              reservationId);
    } else {
      order =
          orderFactory.createMarketOrder(
              orderRequest.userId(),
              orderRequest.idempotencyKey(),
              orderRequest.side(),
              orderRequest.symbol(),
              orderRequest.quantity(),
              reservationId);
    }

    order.markPending();
    return orderRepository.save(order);
  }

  private UUID reserveResources(OrderRequestDTO orderRequest, BigDecimal currentPrice) {
    BigDecimal priceToUse =
        (orderRequest.type() == OrderType.LIMIT) ? orderRequest.limitPrice() : currentPrice;

    ReservationRequestDTO request =
        new ReservationRequestDTO(
            orderRequest.userId(), orderRequest.symbol(), orderRequest.quantity(), priceToUse);

    if (orderRequest.side() == OrderSide.BUY) {
      return portfolioClient
          .reserveBalance(request)
          .map(ReservationResponseDTO::reservationId)
          .block();
    } else if (orderRequest.side() == OrderSide.SELL) {
      return portfolioClient
          .reserveStock(request)
          .map(ReservationResponseDTO::reservationId)
          .block();
    } else {
      throw new OrderValidationException("Something went wrong reserving resources.");
    }
  }

  private boolean isImmediateMarketOrder(OrderRequestDTO orderRequest) {
    return orderRequest.type() == OrderType.MARKET && orderRequest.scheduledFor() == null;
  }

  private void registerReservationCleanup(UUID reservationId) {
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCompletion(int status) {
            if (status == STATUS_ROLLED_BACK) {
              log.warn(
                  "Order transaction rolled back. Triggering compensating cleanup for reservation: {}",
                  reservationId);
              portfolioClient
                  .cancelReservation(reservationId)
                  .doOnError(
                      ex ->
                          log.error(
                              "Compensating cleanup failed for reservation {}", reservationId, ex))
                  .subscribe();
            }
          }
        });
  }
}
