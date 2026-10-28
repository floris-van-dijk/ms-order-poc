package nl.mockstock.order.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import nl.mockstock.order.exception.OrderValidationException;
import org.springframework.stereotype.Component;

@Component
public class OrderFactory {

  public MarketOrder createMarketOrder(
      UUID userId,
      String idempotencyKey,
      OrderSide side,
      String symbol,
      BigDecimal quantity,
      UUID reservationId) {
    MarketOrder order = new MarketOrder(userId, idempotencyKey, side, symbol, quantity);
    order.setReservationId(reservationId);
    return order;
  }

  public LimitOrder createLimitOrder(
      UUID userId,
      String idempotencyKey,
      OrderSide side,
      String symbol,
      BigDecimal quantity,
      BigDecimal price,
      Instant expiresAt,
      UUID reservationId) {
    if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
      throw new OrderValidationException("Price must be greater than zero for Limit orders");
    }
    if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
      throw new OrderValidationException("Expiration date must be in the future for limit orders.");
    }

    LimitOrder order = new LimitOrder(userId, idempotencyKey, side, symbol, quantity, price);
    order.setExpiresAt(expiresAt);
    order.setReservationId(reservationId);
    return order;
  }

  public ScheduledOrder createScheduledOrder(
      UUID userId,
      String idempotencyKey,
      OrderSide side,
      String symbol,
      BigDecimal quantity,
      Instant scheduledFor,
      UUID reservationId) {
    if (scheduledFor == null || scheduledFor.isBefore(Instant.now())) {
      throw new OrderValidationException("Scheduled date must be in the future");
    }

    ScheduledOrder order = new ScheduledOrder(userId, idempotencyKey, side, symbol, quantity);
    order.scheduleOrder(scheduledFor);
    order.setReservationId(reservationId);
    return order;
  }
}
