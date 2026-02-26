package uy.eleven.canasta.service;

import lombok.AllArgsConstructor;

import org.springframework.stereotype.Service;

import uy.eleven.canasta.dto.price.PriceResponse;
import uy.eleven.canasta.model.Price;
import uy.eleven.canasta.repository.PriceRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@AllArgsConstructor
public class PriceService {

    private static final int MAX_DATE_RANGE_DAYS = 365;

    private final PriceRepository priceRepository;

    public void validateDateRange(LocalDate from, LocalDate to) {
        if (from != null && to != null) {
            if (from.isAfter(to)) {
                throw new IllegalArgumentException("'from' date must be before 'to' date");
            }
            long days = ChronoUnit.DAYS.between(from, to);
            if (days > MAX_DATE_RANGE_DAYS) {
                throw new IllegalArgumentException(
                        "Date range cannot exceed " + MAX_DATE_RANGE_DAYS + " days");
            }
        }
    }

    public List<Price> getPricesByProductId(Integer productId) {
        return priceRepository.findByIdProductId(productId);
    }

    public List<Price> getPricesByProductIdOrderedByDate(Integer productId) {
        return priceRepository.findByIdProductIdOrderByIdDateDesc(productId);
    }

    public List<Price> getPricesByDateRange(LocalDate startDate, LocalDate endDate) {
        return priceRepository.findByIdDateBetween(startDate, endDate);
    }

    public List<Price> getPricesByProductAndDateRange(
            Integer productId, LocalDate startDate, LocalDate endDate) {
        return priceRepository.findByIdProductIdAndIdDateBetween(productId, startDate, endDate);
    }

    public List<PriceResponse> getPriceResponsesByProductAndDateRange(
            Integer productId, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return priceRepository
                .findByIdProductIdAndIdDateBetween(productId, startDate, endDate)
                .stream()
                .map(this::mapToPriceResponse)
                .toList();
    }

    public List<PriceResponse> getPriceResponsesByDateRange(
            LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return priceRepository.findByIdDateBetween(startDate, endDate).stream()
                .map(this::mapToPriceResponse)
                .toList();
    }

    public List<PriceResponse> getPriceResponsesByProductIdsAndDateRange(
            List<Integer> productIds, LocalDate startDate, LocalDate endDate) {
        validateDateRange(startDate, endDate);
        return priceRepository.findByIdDateBetween(startDate, endDate).stream()
                .filter(p -> productIds.contains(p.getId().getProductId()))
                .map(this::mapToPriceResponse)
                .toList();
    }

    private PriceResponse mapToPriceResponse(Price price) {
        return new PriceResponse(
                price.getId().getDate(),
                price.getPriceMinimum(),
                price.getPriceMaximum(),
                price.getPriceAverage(),
                price.getPriceMedian(),
                price.getStoreCount(),
                price.getOfferPercentage());
    }
}
