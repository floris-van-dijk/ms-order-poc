package nl.mockstock.order.model.state;

import nl.mockstock.order.model.Order;
import nl.mockstock.order.model.OrderStatus;

public class PendingOrderState implements OrderState {
  @Override
  public void fulfill(Order order) {
    order.transitionTo(new FulfilledOrderState(), OrderStatus.FULFILLED);
  }

  @Override
  public void cancel(Order order) {
    order.transitionTo(new CancelledOrderState(), OrderStatus.CANCELLED);
  }

  @Override
  public void expire(Order order) {
    order.transitionTo(new ExpiredOrderState(), OrderStatus.EXPIRED);
  }

  @Override
  public void reject(Order order) {
    order.transitionTo(new RejectedOrderState(), OrderStatus.REJECTED);
  }
}
