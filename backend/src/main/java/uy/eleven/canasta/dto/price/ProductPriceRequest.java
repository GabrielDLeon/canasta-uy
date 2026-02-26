package uy.eleven.canasta.dto.price;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record ProductPriceRequest(
        @NotNull(message = "Product ID is required") Integer productId,
        LocalDate from,
        LocalDate to,
        @Pattern(regexp = "daily|monthly", message = "Granularity must be 'daily' or 'monthly'")
                String granularity) {
    public ProductPriceRequest {
        if (granularity == null) {
            granularity = "daily";
        }
    }

    public LocalDate getEffectiveFrom() {
        return from != null ? from : LocalDate.now().minusDays(365);
    }

    public LocalDate getEffectiveTo() {
        return to != null ? to : LocalDate.now();
    }
}
