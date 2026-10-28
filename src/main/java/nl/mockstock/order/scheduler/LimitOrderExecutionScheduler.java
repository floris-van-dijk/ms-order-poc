package nl.mockstock.order.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.mockstock.order.service.SchedulerService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LimitOrderExecutionScheduler {

  private final SchedulerService schedulerService;

  @Scheduled(fixedRateString = "${order.limit.execution.ms:300000}", initialDelay = 45000)
  public void executeLimitOrders() {
    log.info("Starting limit order execution check...");
    schedulerService.executeLimitOrdersExecution();
    log.info("Limit order execution check finished.");
  }
}
