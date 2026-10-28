package nl.mockstock.order.model.state;

import nl.mockstock.order.model.Order;
import nl.mockstock.order.model.OrderStatus;

public class CreatedOrderState implements OrderState {
  @Override
  public void pend(Order order) {
    order.transitionTo(new PendingOrderState(), OrderStatus.PENDING);
  }
}
