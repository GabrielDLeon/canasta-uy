package uy.eleven.canasta.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;

import uy.eleven.canasta.dto.common.PaginationInfo;
import uy.eleven.canasta.model.Category;

import java.util.List;

@Schema(description = "Lista paginada de categorías")
public record CategoryListResponse(
        @Schema(description = "Lista de categorías") List<Category> categories,
        @Schema(description = "Información de paginación") PaginationInfo pagination) {}
