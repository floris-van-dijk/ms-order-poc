package nl.mockstock.order.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.mockstock.order.model.LimitOrder;
import nl.mockstock.order.model.OrderSide;
import nl.mockstock.order.model.ScheduledOrder;
import nl.mockstock.order.repository.LimitOrderRepository;
import nl.mockstock.order.repository.ScheduledOrderRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchedulerService {

  private final ScheduledOrderRepository scheduledOrderRepository;
  private final LimitOrderRepository limitOrderRepository;
  private final OrderService orderService;

  /** Processes a single batch of up to 100 due orders. */
  public int executeScheduledOrdersBatch() {
    List<ScheduledOrder> dueOrders =
        scheduledOrderRepository.findDueOrders(Instant.now(), PageRequest.of(0, 100));

    if (dueOrders.isEmpty()) {
      return 0;
    }

    dueOrders.forEach(orderService::executeScheduledOrder);
    return dueOrders.size();
  }

  /** Processes a single batch of up to 100 expired limit orders. */
  public int expireLimitOrdersBatch() {
    List<LimitOrder> expiredOrders =
        limitOrderRepository.findExpiredOrders(Instant.now(), PageRequest.of(0, 100));

    if (expiredOrders.isEmpty()) {
      return 0;
    }

    expiredOrders.forEach(orderService::expireOrder);
    return expiredOrders.size();
  }

  /** Processes a single batch for every symbol of limit orders. */
  public void executeLimitOrdersExecution() {
    List<LimitOrder> pendingLimitOrders = limitOrderRepository.findPendingOrders();

    if (pendingLimitOrders.isEmpty()) {
      return;
    }

    // Group orders by symbol to fetch the price only once per stock
    Map<String, List<LimitOrder>> ordersBySymbol =
        pendingLimitOrders.stream().collect(Collectors.groupingBy(LimitOrder::getSymbol));

    ordersBySymbol.forEach(
        (symbol, orders) -> {
          try {
            BigDecimal currentPrice = orderService.getCurrentStockPrice(symbol);
            orders.forEach(order -> processLimitOrder(order, currentPrice));
          } catch (Exception e) {
            log.error("Failed to process limit orders for symbol: {}", symbol, e);
          }
        });
  }

  private void processLimitOrder(LimitOrder order, BigDecimal currentPrice) {
    boolean shouldExecute = false;

    if (order.getSide() == OrderSide.BUY) {
      shouldExecute = currentPrice.compareTo(order.getLimitPrice()) <= 0;
    } else if (order.getSide() == OrderSide.SELL) {
      shouldExecute = currentPrice.compareTo(order.getLimitPrice()) >= 0;
    }

    if (shouldExecute) {
      orderService.executeLimitOrder(order, currentPrice);
    }
  }
}
