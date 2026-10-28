package nl.mockstock.order.client.base;

import java.time.Duration;
import java.util.function.Function;
import nl.mockstock.order.exception.ExternalServiceException;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

public abstract class BaseApiClient {

  protected final WebClient webClient;

  protected BaseApiClient(WebClient.Builder builder, String baseUrl) {
    this.webClient = builder.baseUrl(baseUrl).build();
  }

  protected <T> Mono<T> get(
      String uri,
      Class<T> responseType,
      Function<ClientResponse, Mono<? extends Throwable>> clientErrorHandler,
      Object... uriVariables) {
    return executeRequest(webClient.get().uri(uri, uriVariables), responseType, clientErrorHandler);
  }

  protected <T, R> Mono<R> post(
      String uri,
      T body,
      Class<R> responseType,
      Function<ClientResponse, Mono<? extends Throwable>> clientErrorHandler) {
    return executeRequest(
        webClient.post().uri(uri).bodyValue(body), responseType, clientErrorHandler);
  }

  protected <T> Mono<T> delete(
      String uri,
      Class<T> responseType,
      Function<ClientResponse, Mono<? extends Throwable>> clientErrorHandler,
      Object... uriVariables) {
    return executeRequest(
        webClient.delete().uri(uri, uriVariables), responseType, clientErrorHandler);
  }

  private <T> Mono<T> executeRequest(
      WebClient.RequestHeadersSpec<?> requestSpec,
      Class<T> responseType,
      Function<ClientResponse, Mono<? extends Throwable>> clientErrorHandler) {
    return requestSpec
        .retrieve()
        .onStatus(HttpStatusCode::is5xxServerError, this::handle5xx)
        .onStatus(HttpStatusCode::is4xxClientError, clientErrorHandler)
        .bodyToMono(responseType)
        .onErrorMap(
            e ->
                e instanceof java.net.ConnectException
                    || e.getCause() instanceof java.net.ConnectException,
            e -> new ExternalServiceException("Connection failed: External service is down"))
        .timeout(Duration.ofSeconds(5))
        .retryWhen(getRetrySpec());
  }

  private Mono<? extends Throwable> handle5xx(ClientResponse response) {
    return Mono.error(new ExternalServiceException("External service server error"));
  }

  private Mono<? extends Throwable> default4xxHandler(ClientResponse response) {
    return response
        .bodyToMono(String.class)
        .flatMap(
            body ->
                Mono.<Throwable>error(new ExternalServiceException("External API error: " + body)))
        .switchIfEmpty(Mono.error(new ExternalServiceException("External API error")));
  }

  private Retry getRetrySpec() {
    return Retry.backoff(3, Duration.ofSeconds(2))
        .filter(throwable -> throwable instanceof ExternalServiceException)
        .onRetryExhaustedThrow(
            (retryBackoffSpec, retrySignal) ->
                new ExternalServiceException(
                    "Failed to reach external service after multiple attempts."));
  }
}
