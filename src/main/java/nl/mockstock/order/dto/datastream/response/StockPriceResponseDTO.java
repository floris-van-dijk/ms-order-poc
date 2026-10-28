package nl.mockstock.order.dto.datastream.response;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record StockPriceResponseDTO(
    @NotBlank(message = "Symbol is required") String symbol,
    @NotNull(message = "Price is required")
        @DecimalMin(value = "0.0001", message = "Price must be positive")
        BigDecimal price,
    double change,
    double changePercent,
    String name) {}
