package nl.mockstock.order.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;
import nl.mockstock.order.model.OrderSide;

public record FillRequestDTO(
    @NotNull(message = "userId is required") @JsonAlias("user_id") UUID userId,
    @NotBlank(message = "symbol is required") String symbol,
    @NotNull(message = "side is required") OrderSide side,
    @NotNull(message = "quantity is required")
        @DecimalMin(value = "0.00000001", message = "quantity must be positive")
        BigDecimal quantity,
    @NotNull(message = "price is required")
        @DecimalMin(value = "0.0001", message = "price must be positive")
        BigDecimal price,
    @NotNull(message = "reservationId is required") UUID reservationId) {}
