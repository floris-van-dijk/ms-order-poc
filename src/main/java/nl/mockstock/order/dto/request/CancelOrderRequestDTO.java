package nl.mockstock.order.dto.request;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CancelOrderRequestDTO(
    @NotNull(message = "userId is required") @JsonAlias("user_id") UUID userId) {}
