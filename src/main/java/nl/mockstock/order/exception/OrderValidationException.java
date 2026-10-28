package nl.mockstock.order.exception;

public class OrderValidationException extends RuntimeException {
  public OrderValidationException(String message) {
    super(message);
  }
}
