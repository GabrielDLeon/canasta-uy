package uy.eleven.canasta.dto.price;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public record PriceSearchRequest(
        String productIds,
        Integer categoryId,
        LocalDate from,
        LocalDate to,
        @Pattern(regexp = "daily|monthly", message = "Granularity must be 'daily' or 'monthly'")
                String granularity,
        @Min(value = 0, message = "Page must be >= 0") int page,
        @Min(value = 1, message = "Size must be >= 1")
                @Max(value = 100, message = "Size must be <= 100")
                int size) {
    public PriceSearchRequest {
        if (granularity == null) {
            granularity = "daily";
        }
        if (page < 0) {
            page = 0;
        }
        if (size < 1) {
            size = 20;
        }
    }

    public LocalDate getEffectiveFrom() {
        return from != null ? from : LocalDate.now().minusDays(365);
    }

    public LocalDate getEffectiveTo() {
        return to != null ? to : LocalDate.now();
    }
}
