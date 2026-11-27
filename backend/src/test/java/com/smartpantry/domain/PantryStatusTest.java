package com.smartpantry.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PantryStatusTest {

    private final LocalDate today = LocalDate.of(2026, 1, 15);

    @Test
    void zeroQuantityIsUsedUpEvenWhenExpired() {
        assertEquals(PantryStatus.USED_UP,
                PantryStatus.resolve(0, today.minusDays(1), today));
    }

    @Test
    void expiryTakesPrecedenceOverLowStock() {
        assertEquals(PantryStatus.EXPIRED,
                PantryStatus.resolve(0.5, today.minusDays(1), today));
    }

    @Test
    void quantityAtOrBelowOneIsLowStock() {
        assertEquals(PantryStatus.LOW_STOCK,
                PantryStatus.resolve(1, today.plusDays(1), today));
    }

    @Test
    void largerQuantityIsAvailable() {
        assertEquals(PantryStatus.AVAILABLE,
                PantryStatus.resolve(2, null, today));
    }
}
