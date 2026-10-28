package nl.mockstock.order.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import nl.mockstock.order.model.LimitOrder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface LimitOrderRepository extends JpaRepository<LimitOrder, UUID> {

  @Query("SELECT o FROM LimitOrder o WHERE o.status = 'PENDING'")
  List<LimitOrder> findPendingOrders();

  @Query(
      """
        SELECT o FROM LimitOrder o
        WHERE o.status = 'PENDING' AND o.expiresAt <= :now
      """)
  List<LimitOrder> findExpiredOrders(@Param("now") Instant now, Pageable pageable);
}
