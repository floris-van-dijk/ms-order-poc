package nl.mockstock.order.exception;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  // Handle validation errors (e.g. missing fields in DTO)
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, String>> handleValidationExceptions(
      MethodArgumentNotValidException ex) {
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult()
        .getAllErrors()
        .forEach(
            (error) -> {
              String fieldName = ((FieldError) error).getField();
              String errorMessage = error.getDefaultMessage();
              errors.put(fieldName, errorMessage);
            });
    return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
  }

  // Handle our custom business validation errors
  @ExceptionHandler(OrderValidationException.class)
  public ResponseEntity<ApiError> handleOrderValidationException(
      OrderValidationException ex, HttpServletRequest request) {
    ApiError apiError =
        new ApiError(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI());
    return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
  }

  // Handle database constraints (e.g. duplicate idempotency key)
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiError> handleDataIntegrityViolation(
      DataIntegrityViolationException ex, HttpServletRequest request) {
    // In a real scenario, we might parse the message to be more specific,
    // but usually this means a duplicate key or constraint violation.
    String message =
        "Data integrity violation. This usually means a duplicate order (Idempotency Key) or invalid reference.";
    ApiError apiError = new ApiError(HttpStatus.CONFLICT, message, request.getRequestURI());
    return new ResponseEntity<>(apiError, HttpStatus.CONFLICT);
  }

  @ExceptionHandler(EntityNotFoundException.class)
  public ResponseEntity<ApiError> handleEntityNotFound(
      EntityNotFoundException ex, HttpServletRequest request) {
    ApiError apiError =
        new ApiError(HttpStatus.NOT_FOUND, ex.getMessage(), request.getRequestURI());
    return new ResponseEntity<>(apiError, HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(SecurityException.class)
  public ResponseEntity<ApiError> handleSecurityException(
      SecurityException ex, HttpServletRequest request) {
    ApiError apiError =
        new ApiError(HttpStatus.FORBIDDEN, ex.getMessage(), request.getRequestURI());
    return new ResponseEntity<>(apiError, HttpStatus.FORBIDDEN);
  }

  // Handle invalid state transitions (e.g. cancelling a fulfilled order)
  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ApiError> handleIllegalStateException(
      IllegalStateException ex, HttpServletRequest request) {
    ApiError apiError = new ApiError(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI());
    return new ResponseEntity<>(apiError, HttpStatus.CONFLICT);
  }

  // Handle JSON parsing errors (e.g. invalid enum value, wrong data type)
  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiError> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex, HttpServletRequest request) {
    // This exception is thrown for malformed JSON or invalid enum values.
    // We return a generic message to avoid exposing internal implementation details like enum
    // names.
    String message = "The request body is malformed or contains invalid values.";
    ApiError apiError = new ApiError(HttpStatus.BAD_REQUEST, message, request.getRequestURI());
    return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
  }

  // Handle errors when calling other microservices
  @ExceptionHandler(ExternalServiceException.class)
  public ResponseEntity<ApiError> handleExternalServiceException(
      ExternalServiceException ex, HttpServletRequest request) {
    log.error("External service call failed: {}", ex.getMessage());

    ApiError apiError =
        new ApiError(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), request.getRequestURI());

    return new ResponseEntity<>(apiError, HttpStatus.SERVICE_UNAVAILABLE);
  }
}
