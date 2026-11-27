package com.smartpantry.domain;

import java.time.LocalDate;

public enum PantryStatus {
    AVAILABLE,
    LOW_STOCK,
    EXPIRED,
    USED_UP;

    public static PantryStatus resolve(double quantity, LocalDate expiryDate, LocalDate today) {
        if (quantity <= 0) {
            return USED_UP;
        }
        if (expiryDate != null && expiryDate.isBefore(today)) {
            return EXPIRED;
        }
        if (quantity <= 1) {
            return LOW_STOCK;
        }
        return AVAILABLE;
    }
}
