package nl.mockstock.order.model.state;

import nl.mockstock.order.model.Order;

public class CancelledOrderState implements OrderState {
  @Override
  public void cancel(Order order) {
    // Order is already cancelled, do nothing to support idempotency.
  }
}
