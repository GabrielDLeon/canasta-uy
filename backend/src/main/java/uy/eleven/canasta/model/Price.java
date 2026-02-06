package uy.eleven.canasta.model;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "prices")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Price {
    @EmbeddedId private PriceId id;

    @Column(name = "price_min", nullable = false)
    private BigDecimal priceMinimum;

    @Column(name = "price_max", nullable = false)
    private BigDecimal priceMaximum;

    @Column(name = "price_avg", nullable = false)
    private BigDecimal priceAverage;

    @Column(name = "price_median", nullable = false)
    private BigDecimal priceMedian;

    @Column(name = "price_std")
    private BigDecimal priceStandardDeviation;

    @Column(name = "store_count")
    private Integer storeCount;

    @Column(name = "offer_count")
    private Integer offerCount;

    @Column(name = "offer_percentage")
    private BigDecimal offerPercentage;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", insertable = false, updatable = false)
    private Product product;
}
