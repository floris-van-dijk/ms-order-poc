package nl.mockstock.order.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.mockstock.order.service.SchedulerService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LimitOrderExpirationScheduler {

  private final SchedulerService schedulerService;

  // This job runs every minute, with an initial delay to avoid running at the same time as other
  // schedulers.
  @Scheduled(fixedRateString = "${order.schedule.ms:60000}", initialDelayString = "30000")
  public void expireLimitOrders() {
    log.info("Running limit order expiration job...");

    // TODO: This loop is intentionally designed to process records in batches.
    //  The `expireLimitOrdersBatch` method processes up to 100 records and returns the count.
    //  The loop continues as long as batches are being found, ensuring all expired orders are
    // processed.
    while (schedulerService.expireLimitOrdersBatch() > 0) {
      // continue processing batches until no expired orders are found
    }

    log.info("Limit order expiration job finished.");
  }
}
