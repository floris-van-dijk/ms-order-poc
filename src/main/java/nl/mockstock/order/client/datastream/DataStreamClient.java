package nl.mockstock.order.client.datastream;

import nl.mockstock.order.client.base.BaseApiClient;
import nl.mockstock.order.dto.datastream.response.StockPriceResponseDTO;
import nl.mockstock.order.exception.ExternalServiceException;
import nl.mockstock.order.exception.OrderValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class DataStreamClient extends BaseApiClient {

  public DataStreamClient(
      WebClient.Builder webClientBuilder,
      @Value("${app.services.data-stream.url:http://localhost:8082}") String baseUrl) {
    super(webClientBuilder, baseUrl);
  }

  public Mono<StockPriceResponseDTO> getStock(String symbol) {
    return get(
        "/api/stocks/{symbol}",
        StockPriceResponseDTO.class,
        response -> {
          if (response.statusCode().value() == 404) {
            return Mono.error(new OrderValidationException("Stock symbol not found: " + symbol));
          }
          return Mono.error(
              new ExternalServiceException("Client error calling DataStream service"));
        },
        symbol);
  }
}
