package nl.mockstock.order.service;

import jakarta.persistence.EntityNotFoundException;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import nl.mockstock.order.dto.idempotency.IdempotencyDTO;
import nl.mockstock.order.model.IdempotencyStatus;
import nl.mockstock.order.model.Order;
import nl.mockstock.order.repository.OrderRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Component
@RequiredArgsConstructor
public class IdempotencyService {

  private final StringRedisTemplate redisTemplate;
  private final ObjectMapper objectMapper;
  private final OrderRepository orderRepository;

  private static final Duration LOCK_TTL = Duration.ofMinutes(2);
  private static final Duration RESULT_TTL = Duration.ofHours(2);

  private static final String LOCK_KEY_PREFIX = "idem:lock:";
  private static final String RESULT_KEY_PREFIX = "idem:result:";

  public Order execute(String idempotencyKey, Supplier<Order> action) {
    String lockKey = lockKey(idempotencyKey);
    String resultKey = resultKey(idempotencyKey);

    String cached = redisTemplate.opsForValue().get(resultKey);
    if (cached != null) {
      IdempotencyDTO record = deserialize(cached);

      return orderRepository
          .findById(UUID.fromString(record.orderId()))
          .orElseThrow(() -> new EntityNotFoundException("Order not found"));
    }

    Boolean acquired = redisTemplate.opsForValue().setIfAbsent(lockKey, "PROCESSING", LOCK_TTL);

    if (!Boolean.TRUE.equals(acquired)) {

      cached = redisTemplate.opsForValue().get(resultKey);
      if (cached != null) {
        IdempotencyDTO record = deserialize(cached);

        return orderRepository
            .findById(UUID.fromString(record.orderId()))
            .orElseThrow(() -> new EntityNotFoundException("Order not found"));
      }

      throw new ResponseStatusException(HttpStatus.CONFLICT, "Order already being processed");
    }

    try {
      Order order = action.get();

      registerAfterCommit(idempotencyKey, order.getId());

      return order;

    } catch (Exception e) {
      redisTemplate.delete(lockKey);
      throw e;
    }
  }

  private void registerAfterCommit(String idempotencyKey, UUID orderId) {
    String lockKey = lockKey(idempotencyKey);
    String resultKey = resultKey(idempotencyKey);

    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
          @Override
          public void afterCommit() {
            try {
              IdempotencyDTO record =
                  new IdempotencyDTO(IdempotencyStatus.COMPLETED, orderId.toString());

              redisTemplate.opsForValue().set(resultKey, serialize(record), RESULT_TTL);
            } finally {
              redisTemplate.delete(lockKey);
            }
          }

          @Override
          public void afterCompletion(int status) {
            if (status == STATUS_ROLLED_BACK) {
              redisTemplate.delete(lockKey);
            }
          }
        });
  }

  private String lockKey(String idempotencyKey) {
    return LOCK_KEY_PREFIX + idempotencyKey;
  }

  private String resultKey(String idempotencyKey) {
    return RESULT_KEY_PREFIX + idempotencyKey;
  }

  private String serialize(IdempotencyDTO record) {
    try {
      return objectMapper.writeValueAsString(record);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private IdempotencyDTO deserialize(String json) {
    try {
      return objectMapper.readValue(json, IdempotencyDTO.class);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
