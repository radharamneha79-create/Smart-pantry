package com.smartpantry;

import com.smartpantry.domain.PantryItem;
import com.smartpantry.domain.PantryStatus;
import com.smartpantry.repository.PantryItemRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.LocalDate;
import java.util.List;

@SpringBootApplication
public class SmartPantryApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartPantryApplication.class, args);
    }

    @Bean
    CommandLineRunner seedPantry(PantryItemRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                LocalDate today = LocalDate.now();
                repository.saveAll(List.of(
                        new PantryItem("Oat milk", "Beverages", 2.0, "L", today.plusDays(2),
                                PantryStatus.AVAILABLE),
                        new PantryItem("Brown rice", "Grains", 1.0, "kg", today.plusMonths(8),
                                PantryStatus.LOW_STOCK),
                        new PantryItem("Cherry tomatoes", "Vegetables", 6.0, "pcs", today.minusDays(1),
                                PantryStatus.EXPIRED),
                        new PantryItem("Cinnamon", "Spices", 1.0, "packs", today.plusMonths(12),
                                PantryStatus.LOW_STOCK)
                ));
            }
        };
    }
}
