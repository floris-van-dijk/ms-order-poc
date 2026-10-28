package nl.mockstock.order.dto.portfolio.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record ReservationRequestDTO(
    @NotNull(message = "userId is required") UUID userId,
    @NotBlank(message = "symbol is required") String symbol,
    @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be positive")
        BigDecimal quantity,
    @NotNull(message = "price is required")
        @DecimalMin(value = "0.0001", message = "price must be positive")
        BigDecimal price) {}
