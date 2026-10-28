package nl.mockstock.order.client.portfolio;

import java.util.UUID;
import nl.mockstock.order.client.base.BaseApiClient;
import nl.mockstock.order.dto.portfolio.request.ReservationRequestDTO;
import nl.mockstock.order.dto.portfolio.response.ReservationResponseDTO;
import nl.mockstock.order.dto.request.FillRequestDTO;
import nl.mockstock.order.exception.OrderValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class PortfolioClient extends BaseApiClient {

  public PortfolioClient(
      WebClient.Builder builder,
      @Value("${app.services.portfolio.url:http://localhost:8084}") String baseUrl) {
    super(builder, baseUrl);
  }

  public Mono<Void> notifyFillOrder(FillRequestDTO request) {
    return post(
        "/api/v1/portfolios/internal/events/fill", request, Void.class, this::handlePortfolioError);
  }

  public Mono<ReservationResponseDTO> reserveBalance(ReservationRequestDTO request) {
    return post(
        "/api/v1/portfolios/internal/reservations/buy",
        request,
        ReservationResponseDTO.class,
        this::handlePortfolioError);
  }

  public Mono<ReservationResponseDTO> reserveStock(ReservationRequestDTO request) {
    return post(
        "/api/v1/portfolios/internal/reservations/sell",
        request,
        ReservationResponseDTO.class,
        this::handlePortfolioError);
  }

  public Mono<Void> cancelReservation(UUID reservationId) {
    return delete(
        "/api/v1/portfolios/internal/reservations/{reservationId}",
        Void.class,
        this::handlePortfolioError,
        reservationId);
  }

  private Mono<? extends Throwable> handlePortfolioError(ClientResponse response) {
    return response
        .bodyToMono(String.class)
        .defaultIfEmpty("Portfolio service rejected the request")
        .flatMap(msg -> Mono.error(new OrderValidationException(msg)));
  }
}
