package nl.mockstock.order.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import nl.mockstock.order.model.OrderSide;
import nl.mockstock.order.model.OrderType;

public record OrderRequestDTO(
    @NotNull(message = "User ID is required") @JsonAlias("user_id") UUID userId,
    @NotBlank(message = "Idempotency key is required") @JsonAlias("idempotency_key")
        String idempotencyKey,
    @NotBlank(message = "Stock symbol is required")
        @Size(min = 1, max = 5, message = "Symbol must be between 1 and 5 characters")
        String symbol,
    @NotNull(message = "Order side is required") OrderSide side,
    @NotNull(message = "Order type is required") OrderType type,
    @Positive(message = "Quantity must be greater than zero") BigDecimal quantity,
    @JsonAlias("limit_price") BigDecimal limitPrice,
    @Future(message = "Scheduled date must be in the future") @JsonAlias("scheduled_for")
        Instant scheduledFor,
    @Future(message = "Expiration date must be in the future") @JsonAlias("expires_at")
        Instant expiresAt) {}
