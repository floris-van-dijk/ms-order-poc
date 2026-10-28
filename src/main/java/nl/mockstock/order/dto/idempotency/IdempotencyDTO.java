package nl.mockstock.order.dto.idempotency;

import nl.mockstock.order.model.IdempotencyStatus;

public record IdempotencyDTO(IdempotencyStatus status, String orderId) {}
