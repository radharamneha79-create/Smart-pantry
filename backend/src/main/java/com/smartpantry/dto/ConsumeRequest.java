package com.smartpantry.dto;

import jakarta.validation.constraints.Positive;

public record ConsumeRequest(
        @Positive(message = "Quantity to consume must be greater than zero.") Double quantity
) {
}
