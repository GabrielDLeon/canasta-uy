package uy.eleven.canasta.dto.common;

import jakarta.validation.ValidationException;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public record DateRangeRequest(LocalDate from, LocalDate to) {
    private static final int MAX_DATE_RANGE_DAYS = 365;

    public DateRangeRequest {
        LocalDate effectiveFrom = from != null ? from : LocalDate.now().minusDays(365);
        LocalDate effectiveTo = to != null ? to : LocalDate.now();

        if (effectiveFrom.isAfter(effectiveTo)) {
            throw new ValidationException("'from' date must be before 'to' date");
        }

        long days = ChronoUnit.DAYS.between(effectiveFrom, effectiveTo);
        if (days > MAX_DATE_RANGE_DAYS) {
            throw new ValidationException("Date range cannot exceed 365 days");
        }
    }

    public LocalDate effectiveFrom() {
        return from != null ? from : LocalDate.now().minusDays(365);
    }

    public LocalDate effectiveTo() {
        return to != null ? to : LocalDate.now();
    }
}
