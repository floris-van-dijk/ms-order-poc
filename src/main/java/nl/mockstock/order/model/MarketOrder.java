package nl.mockstock.order.model;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "market_orders")
@DiscriminatorValue("MARKET")
public class MarketOrder extends Order {

  protected MarketOrder() {}

  MarketOrder(
      UUID userId, String idempotencyKey, OrderSide side, String symbol, BigDecimal quantity) {
    super(userId, idempotencyKey, side, symbol, quantity);
  }

  @Override
  public OrderType getType() {
    return OrderType.MARKET;
  }
}
