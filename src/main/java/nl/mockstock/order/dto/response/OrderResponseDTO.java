package nl.mockstock.order.dto.response;

import java.util.UUID;
import nl.mockstock.order.model.OrderStatus;

public record OrderResponseDTO(UUID orderId, OrderStatus status) {}
