package uy.eleven.canasta.dto.category;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CategoryProductsRequest(
        @NotNull(message = "Category ID is required") Integer categoryId,
        @Min(value = 0, message = "Page must be >= 0") int page,
        @Min(value = 1, message = "Size must be >= 1")
                @Max(value = 100, message = "Size must be <= 100")
                int size) {
    public CategoryProductsRequest {
        if (page < 0) {
            page = 0;
        }
        if (size < 1) {
            size = 20;
        }
    }
}
