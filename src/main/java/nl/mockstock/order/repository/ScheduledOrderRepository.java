package nl.mockstock.order.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import nl.mockstock.order.model.ScheduledOrder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduledOrderRepository extends JpaRepository<ScheduledOrder, UUID> {

  @Query(
      """
        SELECT o FROM ScheduledOrder o
        WHERE o.status = 'PENDING' AND o.scheduledFor <= :now
      """)
  List<ScheduledOrder> findDueOrders(@Param("now") Instant now, Pageable pageable);
}
