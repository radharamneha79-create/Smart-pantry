package com.smartpantry.dto;

import com.smartpantry.domain.PantryStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PantryItemResponse(
        Long id,
        String name,
        String category,
        Double quantity,
        String unit,
        LocalDate expiryDate,
        PantryStatus status,
        LocalDateTime updatedAt
) {
}
