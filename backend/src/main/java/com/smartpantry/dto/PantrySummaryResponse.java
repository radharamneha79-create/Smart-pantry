package com.smartpantry.dto;

public record PantrySummaryResponse(
        long totalItems,
        long lowStockItems,
        long expiringSoonItems
) {
}
