package nl.mockstock.order.dto.portfolio.response;

import jakarta.validation.constraints.NotBlank;
import java.util.UUID;

public record ReservationResponseDTO(
    @NotBlank(message = "reservationId is required") UUID reservationId) {}
