package nl.mockstock.order.model.state;

import nl.mockstock.order.model.Order;

public interface OrderState {

  default void pend(Order order) {
    throw new IllegalStateException("Cannot pend order in current state: " + order.getStatus());
  }

  default void fulfill(Order order) {
    throw new IllegalStateException("Cannot fulfill order in current state: " + order.getStatus());
  }

  default void cancel(Order order) {
    throw new IllegalStateException("Cannot cancel order in current state: " + order.getStatus());
  }

  default void expire(Order order) {
    throw new IllegalStateException("Cannot expire order in current state: " + order.getStatus());
  }

  default void reject(Order order) {
    throw new IllegalStateException("Cannot reject order in current state: " + order.getStatus());
  }
}
