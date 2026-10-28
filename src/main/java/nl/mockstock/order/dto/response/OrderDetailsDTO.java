package nl.mockstock.order.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import nl.mockstock.order.model.LimitOrder;
import nl.mockstock.order.model.Order;
import nl.mockstock.order.model.OrderSide;
import nl.mockstock.order.model.OrderStatus;
import nl.mockstock.order.model.OrderType;
import nl.mockstock.order.model.ScheduledOrder;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderDetailsDTO(
    UUID id,
    UUID userId,
    String symbol,
    OrderSide side,
    OrderType type,
    OrderStatus status,
    BigDecimal quantity,
    BigDecimal limitPrice,
    BigDecimal fulfilledPrice,
    Instant createdAt,
    Instant fulfilledAt,
    Instant cancelledAt,
    Instant rejectedAt,
    Instant scheduledFor,
    Instant expiresAt,
    Instant expiredAt) {
  public OrderDetailsDTO(Order order) {
    this(
        order.getId(),
        order.getUserId(),
        order.getSymbol(),
        order.getSide(),
        order.getType(),
        order.getStatus(),
        order.getQuantity(),
        order instanceof LimitOrder lo ? lo.getLimitPrice() : null,
        order.getFulfilledPrice(),
        order.getCreatedAt(),
        order.getFulfilledAt(),
        order.getCancelledAt(),
        order.getRejectedAt(),
        order instanceof ScheduledOrder so ? so.getScheduledFor() : null,
        order instanceof LimitOrder lo ? lo.getExpiresAt() : null,
        order instanceof LimitOrder lo ? lo.getExpiredAt() : null);
  }
}
