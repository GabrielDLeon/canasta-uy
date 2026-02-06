package uy.eleven.canasta.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import uy.eleven.canasta.model.Price;
import uy.eleven.canasta.model.PriceId;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface PriceRepository extends JpaRepository<Price, PriceId> {

    List<Price> findByIdProductId(Integer productId);

    List<Price> findByIdProductIdOrderByIdDateDesc(Integer productId);

    List<Price> findByIdDateBetween(LocalDate startDate, LocalDate endDate);

    List<Price> findByIdProductIdAndIdDateBetween(
            Integer productId, LocalDate startDate, LocalDate endDate);
}
