package uy.eleven.canasta.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import uy.eleven.canasta.model.Price;
import uy.eleven.canasta.model.PriceId;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface PriceRepository extends JpaRepository<Price, PriceId> {

    List<Price> findByIdProductId(Integer productId);

    List<Price> findByIdProductIdOrderByIdDateDesc(Integer productId);

    List<Price> findByIdDateBetween(LocalDate startDate, LocalDate endDate);

    List<Price> findByIdProductIdAndIdDateBetween(
            Integer productId, LocalDate startDate, LocalDate endDate);

    @Query(
            "SELECT p FROM Price p WHERE p.id.productId IN :productIds AND p.id.date BETWEEN :from"
                    + " AND :to")
    List<Price> findByProductIdsAndDateRange(
            @Param("productIds") List<Integer> productIds,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // TODO: Fix JPQL projection - types mismatch
    // @Query(
    //         "SELECT new uy.eleven.canasta.dto.projection.PriceAggregation("
    //                 + " AVG(p.priceAverage), MIN(p.priceAverage), MAX(p.priceAverage),"
    //                 + " AVG(p.priceMedian), MIN(p.priceMinimum), MAX(p.priceMaximum))"
    //                 + " FROM Price p WHERE p.id.productId = :productId AND p.id.date BETWEEN
    // :from"
    //                 + " AND :to")
    // PriceAggregation aggregateByProductAndDateRange(
    //         @Param("productId") Integer productId,
    //         @Param("from") LocalDate from,
    //         @Param("to") LocalDate to);

    // TODO: Fix JPQL projection
    // @Query(
    //         "SELECT new uy.eleven.canasta.dto.projection.PriceChange("
    //                 + " p.id.productId, (MAX(p.priceAverage) - MIN(p.priceAverage)) /"
    //                 + " MIN(p.priceAverage) * 100) FROM Price p WHERE p.id.date BETWEEN"
    //                 + " :from AND :to GROUP BY p.id.productId ORDER BY 2 DESC")
    // List<PriceChange> findTopPriceChanges(
    //         @Param("from") LocalDate from, @Param("to") LocalDate to, Pageable pageable);

    @Query("SELECT p FROM Price p WHERE p.id.productId = :productId ORDER BY p.id.date ASC")
    List<Price> findFirstByProductIdOrderByDateAsc(@Param("productId") Integer productId);

    @Query("SELECT p FROM Price p WHERE p.id.productId = :productId ORDER BY p.id.date DESC")
    List<Price> findFirstByProductIdOrderByDateDesc(@Param("productId") Integer productId);

    @Query(
            "SELECT AVG(p.priceAverage) FROM Price p WHERE p.id.productId = :productId AND"
                    + " p.id.date BETWEEN :from AND :to")
    BigDecimal calculateAveragePriceForProduct(
            @Param("productId") Integer productId,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to);

    // TODO: Fix JPQL projection
    // @Query(
    //         "SELECT new uy.eleven.canasta.dto.projection.DailyPriceAverage( AVG(p.priceAverage),"
    //             + " p.id.date) FROM Price p WHERE p.id.productId = :productId AND p.id.date
    // BETWEEN"
    //             + " :from AND :to GROUP BY p.id.date ORDER BY p.id.date")
    // List<DailyPriceAverage> findDailyAveragesByProductAndDateRange(
    //         @Param("productId") Integer productId,
    //         @Param("from") LocalDate from,
    //         @Param("to") LocalDate to);
}
