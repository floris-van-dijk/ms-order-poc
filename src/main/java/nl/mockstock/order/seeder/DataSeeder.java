package nl.mockstock.order.seeder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import nl.mockstock.order.model.LimitOrder;
import nl.mockstock.order.model.MarketOrder;
import nl.mockstock.order.model.OrderFactory;
import nl.mockstock.order.model.OrderSide;
import nl.mockstock.order.model.ScheduledOrder;
import nl.mockstock.order.repository.OrderRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.data-seeder.enabled", havingValue = "true")
public class DataSeeder implements CommandLineRunner {

  private final OrderRepository orderRepository;
  private final OrderFactory orderFactory;

  public DataSeeder(OrderRepository orderRepository, OrderFactory orderFactory) {
    this.orderRepository = orderRepository;
    this.orderFactory = orderFactory;
  }

  @Override
  public void run(String @NonNull ... args) {
    // Prevent reseeding if data already exists
    if (orderRepository.count() > 0) {
      log.info("Database already seeded. Skipping seeding.");
      return;
    }

    log.info("Seeding mock data...");

    UUID mockUserId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    UUID mockReservationId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    // 1. Standard Market Order
    MarketOrder marketOrder =
        orderFactory.createMarketOrder(
            mockUserId,
            "seed-market-1",
            OrderSide.BUY,
            "AAPL",
            BigDecimal.valueOf(10),
            mockReservationId);
    marketOrder.markPending();
    orderRepository.save(marketOrder);

    // 2. Limit Order
    LimitOrder limitOrder =
        orderFactory.createLimitOrder(
            mockUserId,
            "seed-limit-1",
            OrderSide.SELL,
            "MSFT",
            BigDecimal.valueOf(5),
            new BigDecimal("400.00"),
            Instant.now().plus(30, ChronoUnit.DAYS),
            mockReservationId);
    limitOrder.markPending();
    orderRepository.save(limitOrder);

    // 3. Scheduled Order (Due in the past - should be executed immediately by scheduler)
    // Note: We use createMarketOrder + scheduleOrder manually here because createScheduledOrder
    // throws an exception if the date is in the past.
    ScheduledOrder pastScheduledOrder =
        orderFactory.createScheduledOrder(
            mockUserId,
            "seed-scheduled-past",
            OrderSide.BUY,
            "GOOGL",
            BigDecimal.TWO,
            Instant.now().plus(1, ChronoUnit.MINUTES),
            mockReservationId);
    pastScheduledOrder.scheduleOrder(Instant.now().minus(5, ChronoUnit.MINUTES));
    pastScheduledOrder.markPending();
    orderRepository.save(pastScheduledOrder);

    // 4. Scheduled Order (Future - should wait)
    ScheduledOrder futureScheduledOrder =
        orderFactory.createScheduledOrder(
            mockUserId,
            "seed-scheduled-future",
            OrderSide.BUY,
            "TSLA",
            BigDecimal.valueOf(3),
            Instant.now().plus(1, ChronoUnit.MINUTES),
            mockReservationId);
    futureScheduledOrder.markPending();
    orderRepository.save(futureScheduledOrder);

    // 5. Expired Limit Order (should be marked EXPIRED by scheduler)
    LimitOrder expiredLimitOrder =
        orderFactory.createLimitOrder(
            mockUserId,
            "seed-limit-expired",
            OrderSide.SELL,
            "NVDA",
            BigDecimal.valueOf(20),
            new BigDecimal("900.00"),
            Instant.now().plus(1, ChronoUnit.MINUTES),
            mockReservationId);
    expiredLimitOrder.markPending();
    orderRepository.save(expiredLimitOrder);

    log.info("Seeding completed. Mock User ID: {}", mockUserId);
  }
}
