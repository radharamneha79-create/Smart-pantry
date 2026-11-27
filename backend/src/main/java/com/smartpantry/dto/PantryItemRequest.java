package com.smartpantry.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;

public record PantryItemRequest(
        @NotBlank(message = "Name is required.") String name,
        @NotBlank(message = "Category is required.") String category,
        @NotNull(message = "Quantity is required.")
        @Positive(message = "Quantity must be greater than zero.") Double quantity,
        @NotBlank(message = "Unit is required.") String unit,
        LocalDate expiryDate
) {
}
