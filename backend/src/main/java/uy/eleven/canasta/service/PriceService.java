package uy.eleven.canasta.service;

import org.springframework.stereotype.Service;
import uy.eleven.canasta.model.Price;
import uy.eleven.canasta.repository.PriceRepository;
import java.time.LocalDate;
import java.util.List;

@Service
public class PriceService {

    private final PriceRepository priceRepository;

    public PriceService(PriceRepository priceRepository) {
        this.priceRepository = priceRepository;
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

    public List<Price> getPricesByProductAndDateRange(Integer productId, LocalDate startDate, LocalDate endDate) {
        return priceRepository.findByIdProductIdAndIdDateBetween(productId, startDate, endDate);
    }
}
