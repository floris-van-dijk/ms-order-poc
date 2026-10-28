package nl.mockstock.order.exception;

import java.time.Instant;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiError {
  private final Instant timestamp;
  private final int status;
  private final String error;
  private final String message;
  private final String path;

  public ApiError(HttpStatus httpStatus, String message, String path) {
    this.timestamp = Instant.now();
    this.status = httpStatus.value();
    this.error = httpStatus.getReasonPhrase();
    this.message = message;
    this.path = path;
  }
}
