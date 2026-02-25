package uy.eleven.canasta.dto.common;

public record PaginationInfo(
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious) {}
