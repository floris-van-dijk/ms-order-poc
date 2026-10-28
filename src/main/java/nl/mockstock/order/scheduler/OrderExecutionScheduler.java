package nl.mockstock.order.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.mockstock.order.service.SchedulerService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExecutionScheduler {

  private final SchedulerService schedulerService;

  @Scheduled(fixedRateString = "${order.schedule.ms:60000}", initialDelayString = "15000")
  public void executeScheduledOrders() {
    log.info("Running scheduled order execution job...");

    // TODO: This loop is intentionally designed to process records in batches.
    //  The `executeScheduledOrdersBatch` method processes up to 100 records and returns the count.
    //  The loop continues as long as batches are being found, ensuring all due orders are
    // processed.
    // We loop to process all due orders in batches, not just the first one.
    // Each batch is handled in its own transaction.
    while (schedulerService.executeScheduledOrdersBatch() > 0) {
      // continue processing batches as long as we are finding due orders
    }

    log.info("Scheduled order execution job finished.");
  }
}
