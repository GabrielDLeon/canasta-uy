package uy.eleven.canasta.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Embeddable
@Data
public class PriceId implements Serializable {
    @Column(name = "product_id")
    private Integer productId;

    @Column(name = "date")
    private LocalDate date;
}
