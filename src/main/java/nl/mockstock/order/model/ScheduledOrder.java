package nl.mockstock.order.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;

@Entity
@Table(
    name = "scheduled_orders",
    indexes = @Index(name = "idx_scheduled_orders_scheduled_for", columnList = "scheduled_for"))
@DiscriminatorValue("SCHEDULED")
@Getter
public class ScheduledOrder extends Order {

  @Column(nullable = false)
  private Instant scheduledFor;

  protected ScheduledOrder() {}

  ScheduledOrder(
      UUID userId, String idempotencyKey, OrderSide side, String symbol, BigDecimal quantity) {
    super(userId, idempotencyKey, side, symbol, quantity);
  }

  @Override
  public OrderType getType() {
    return OrderType.MARKET;
  }

  public void scheduleOrder(Instant scheduledFor) {
    this.scheduledFor = scheduledFor;
  }
}
