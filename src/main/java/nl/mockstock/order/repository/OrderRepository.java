package nl.mockstock.order.repository;

import java.util.Optional;
import java.util.UUID;
import nl.mockstock.order.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

  Page<Order> findByUserId(UUID userId, Pageable pageable);

  Optional<Order> findByIdempotencyKey(String idempotencyKey);
}
