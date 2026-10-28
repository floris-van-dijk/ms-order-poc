package nl.mockstock.order.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import nl.mockstock.order.model.state.*;
import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Formula;

@Entity
@Table(
    name = "orders",
    indexes = {
      @Index(name = "idx_user", columnList = "user_id"),
      @Index(name = "idx_status", columnList = "status"),
    },
    uniqueConstraints = {@UniqueConstraint(columnNames = {"idempotency_key", "user_id"})})
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "dtype", discriminatorType = DiscriminatorType.STRING)
@DynamicUpdate
@Getter
public abstract class Order {
  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(nullable = false)
  private UUID userId;

  @Setter
  @Column(nullable = false)
  private UUID reservationId;

  @Column(nullable = false)
  private String idempotencyKey;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderSide side;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status;

  @Column(nullable = false)
  private String symbol;

  @Getter(AccessLevel.NONE)
  @Formula("CASE dtype WHEN 'LIMIT' THEN 'LIMIT' ELSE 'MARKET' END")
  private String type;

  @Column(nullable = false)
  private BigDecimal quantity;

  // Price per stock the order is fulfilled with (retrieved from data stream)
  @Column(precision = 19, scale = 4)
  private BigDecimal fulfilledPrice;

  @Transient private OrderState currentState;

  private Instant fulfilledAt;
  private Instant cancelledAt;
  private Instant rejectedAt;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  protected Order() {}

  Order(UUID userId, String idempotencyKey, OrderSide side, String symbol, BigDecimal quantity) {
    this.userId = userId;
    this.idempotencyKey = idempotencyKey;
    this.side = side;
    this.symbol = symbol.toUpperCase();
    this.quantity = quantity;
    this.status = OrderStatus.CREATED;
    this.currentState = new CreatedOrderState();
  }

  public abstract OrderType getType();

  // Handling state

  @PostLoad
  void initializeState() {
    this.currentState =
        switch (this.status) {
          case CREATED -> new CreatedOrderState();
          case PENDING -> new PendingOrderState();
          case FULFILLED -> new FulfilledOrderState();
          case CANCELLED -> new CancelledOrderState();
          case EXPIRED -> new ExpiredOrderState();
          case REJECTED -> new RejectedOrderState();
        };
  }

  public void transitionTo(OrderState newState, OrderStatus newStatus) {
    this.currentState = newState;
    this.status = newStatus;

    // Centralize the side-effects of a state transition
    switch (newStatus) {
      case FULFILLED -> setFulfilledAt();
      case CANCELLED -> setCancelledAt();
      case REJECTED -> setRejectedAt();
      default -> {} // No timestamp needed for CREATED, PENDING, or EXPIRED (handled in subclass)
    }
  }

  public void markPending() {
    this.currentState.pend(this);
  }

  public void markFulfilled(BigDecimal price) {
    this.fulfilledPrice = price;
    this.currentState.fulfill(this);
  }

  public void markCancelled() {
    this.currentState.cancel(this);
  }

  public void markRejected() {
    this.currentState.reject(this);
  }

  public void markExpired() {
    this.currentState.expire(this);
  }

  // Handle timestamps

  private void setFulfilledAt() {
    this.fulfilledAt = Instant.now();
  }

  private void setCancelledAt() {
    this.cancelledAt = Instant.now();
  }

  private void setRejectedAt() {
    this.rejectedAt = Instant.now();
  }

  // Keep track when record is created/updated

  @PrePersist
  protected void onCreate() {
    createdAt = Instant.now();
    updatedAt = Instant.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = Instant.now();
  }
}
