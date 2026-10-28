package nl.mockstock.order.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import nl.mockstock.order.model.state.OrderState;

@Entity
@Table(
    name = "limit_orders",
    indexes = @Index(name = "idx_limit_orders_expires_at", columnList = "expires_at"))
@DiscriminatorValue("LIMIT")
@Getter
public class LimitOrder extends Order {

  @Column(nullable = false, precision = 19, scale = 4)
  private BigDecimal limitPrice;

  @Column private Instant expiresAt;

  @Column private Instant expiredAt;

  protected LimitOrder() {}

  LimitOrder(
      UUID userId,
      String idempotencyKey,
      OrderSide side,
      String symbol,
      BigDecimal quantity,
      BigDecimal limitPrice) {
    super(userId, idempotencyKey, side, symbol, quantity);
    this.limitPrice = limitPrice;
  }

  @Override
  public OrderType getType() {
    return OrderType.LIMIT;
  }

  void setExpiresAt(Instant expiresAt) {
    this.expiresAt = expiresAt;
  }

  @Override
  public void transitionTo(OrderState newState, OrderStatus newStatus) {
    super.transitionTo(newState, newStatus);
    if (newStatus == OrderStatus.EXPIRED) {
      this.expiredAt = Instant.now();
    }
  }
}
