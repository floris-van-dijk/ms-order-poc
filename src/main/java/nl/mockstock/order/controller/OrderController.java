package nl.mockstock.order.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.StreamSupport;
import lombok.RequiredArgsConstructor;
import nl.mockstock.order.dto.request.CancelOrderRequestDTO;
import nl.mockstock.order.dto.request.OrderRequestDTO;
import nl.mockstock.order.dto.response.OrderDetailsDTO;
import nl.mockstock.order.dto.response.OrderResponseDTO;
import nl.mockstock.order.model.Order;
import nl.mockstock.order.service.OrderService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/orders")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class OrderController {

  private final OrderService orderService;

  @PostMapping
  public ResponseEntity<OrderResponseDTO> placeOrder(
      @Valid @RequestBody OrderRequestDTO orderRequestDTO) {
    Order createdOrder = orderService.placeOrder(orderRequestDTO);
    OrderResponseDTO response =
        new OrderResponseDTO(createdOrder.getId(), createdOrder.getStatus());
    return new ResponseEntity<>(response, HttpStatus.CREATED);
  }

  @PostMapping("/{id}/cancel")
  public ResponseEntity<OrderResponseDTO> cancelOrder(
      @PathVariable("id") UUID id, @RequestBody CancelOrderRequestDTO request) {
    // In a real app, userId would come from the Security Context (JWT), not the body.
    // For now, we accept it in the body to simulate the user.
    Order cancelledOrder = orderService.cancelOrder(id, request.userId());
    OrderResponseDTO response =
        new OrderResponseDTO(cancelledOrder.getId(), cancelledOrder.getStatus());
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{userId}")
  public ResponseEntity<Page<OrderDetailsDTO>> getOrdersByUserId(
      @PathVariable("userId") UUID userId, @PageableDefault(size = 25) Pageable pageable) {
    Page<Order> orders = orderService.getOrdersByUserId(userId, sanitizeSort(pageable));
    Page<OrderDetailsDTO> response = orders.map(OrderDetailsDTO::new);
    return ResponseEntity.ok(response);
  }

  private Pageable sanitizeSort(Pageable pageable) {
    Set<String> childOnlyFields = Set.of("limitPrice", "scheduledFor", "expiresAt", "expiredAt");

    List<Sort.Order> validOrders =
        StreamSupport.stream(pageable.getSort().spliterator(), false)
            .filter(order -> !childOnlyFields.contains(order.getProperty()))
            .toList();

    Sort sanitizedSort = validOrders.isEmpty() ? Sort.unsorted() : Sort.by(validOrders);
    return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sanitizedSort);
  }
}
