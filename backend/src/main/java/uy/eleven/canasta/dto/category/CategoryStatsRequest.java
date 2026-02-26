package uy.eleven.canasta.dto.category;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CategoryStatsRequest(
        @NotNull(message = "Category ID is required")
        Integer categoryId,
        
        LocalDate from,
        LocalDate to
) {
    public LocalDate getEffectiveFrom() {
        return from != null ? from : LocalDate.now().minusDays(365);
    }
    
    public LocalDate getEffectiveTo() {
        return to != null ? to : LocalDate.now();
    }
}
