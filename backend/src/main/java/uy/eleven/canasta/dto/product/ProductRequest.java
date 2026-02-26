package uy.eleven.canasta.dto.product;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ProductRequest(
        @Min(value = 0, message = "Page must be >= 0") int page,
        @Min(value = 1, message = "Size must be >= 1")
        @Max(value = 100, message = "Size must be <= 100")
        int size) {
    
    public ProductRequest {
        if (page < 0) {
            page = 0;
        }
        if (size < 1) {
            size = 20;
        }
    }
}
