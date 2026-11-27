package com.smartpantry.service;

import com.smartpantry.domain.PantryItem;
import com.smartpantry.domain.PantryStatus;
import com.smartpantry.dto.ConsumeRequest;
import com.smartpantry.dto.PantryItemRequest;
import com.smartpantry.dto.PantryItemResponse;
import com.smartpantry.dto.PantrySummaryResponse;
import com.smartpantry.exception.PantryItemNotFoundException;
import com.smartpantry.repository.PantryItemRepository;
import com.smartpantry.repository.PantryUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class PantryItemService {

    private static final int EXPIRING_SOON_DAYS = 3;

    private final PantryItemRepository repository;
    private final PantryUserRepository userRepository;

    public PantryItemService(PantryItemRepository repository, PantryUserRepository userRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<PantryItemResponse> list(String category, PantryStatus status,
                                         Integer expiringWithinDays, String search, Long ownerId) {
        if (expiringWithinDays != null && expiringWithinDays < 0) {
            throw new IllegalArgumentException("expiringWithinDays must be zero or greater.");
        }
        LocalDate today = LocalDate.now();
        LocalDate expiryLimit = expiringWithinDays == null ? null : today.plusDays(expiringWithinDays);
        String normalizedCategory = normalize(category);
        String normalizedSearch = normalize(search);

        return repository.findAllByOwnerId(ownerId).stream()
                .filter(item -> normalizedCategory == null
                        || item.getCategory().equalsIgnoreCase(normalizedCategory))
                .filter(item -> status == null
                        || resolveStatus(item, today) == status)
                .filter(item -> expiryLimit == null
                        || isExpiringBetween(item, today, expiryLimit, today))
                .filter(item -> normalizedSearch == null
                        || item.getName().toLowerCase().contains(normalizedSearch))
                .map(item -> toResponse(item, today))
                .toList();
    }

    @Transactional(readOnly = true)
    public PantryItemResponse get(Long id, Long ownerId) {
        return toResponse(findItem(id, ownerId), LocalDate.now());
    }

    @Transactional
    public PantryItemResponse create(PantryItemRequest request, Long ownerId) {
        PantryItem item = new PantryItem();
        item.setOwner(userRepository.getReferenceById(ownerId));
        applyRequest(item, request);
        return toResponse(repository.save(item), LocalDate.now());
    }

    @Transactional
    public PantryItemResponse update(Long id, PantryItemRequest request, Long ownerId) {
        PantryItem item = findItem(id, ownerId);
        applyRequest(item, request);
        return toResponse(repository.save(item), LocalDate.now());
    }

    @Transactional
    public PantryItemResponse consume(Long id, ConsumeRequest request, Long ownerId) {
        PantryItem item = findItem(id, ownerId);
        double quantityToConsume = request == null || request.quantity() == null
                ? 1.0
                : request.quantity();
        if (quantityToConsume > item.getQuantity()) {
            throw new IllegalArgumentException("Quantity to consume cannot exceed the available quantity.");
        }
        item.setQuantity(quantityToConsume == item.getQuantity()
                ? 0
                : Math.max(0, item.getQuantity() - quantityToConsume));
        item.setStatus(PantryStatus.resolve(item.getQuantity(), item.getExpiryDate(), LocalDate.now()));
        return toResponse(repository.save(item), LocalDate.now());
    }

    @Transactional
    public void delete(Long id, Long ownerId) {
        repository.delete(findItem(id, ownerId));
    }

    @Transactional(readOnly = true)
    public PantrySummaryResponse summary(Long ownerId) {
        LocalDate today = LocalDate.now();
        LocalDate expiryLimit = today.plusDays(EXPIRING_SOON_DAYS);
        List<PantryItem> items = repository.findAllByOwnerId(ownerId);
        long lowStock = items.stream()
                .filter(item -> resolveStatus(item, today) == PantryStatus.LOW_STOCK)
                .count();
        long expiringSoon = items.stream()
                .filter(item -> isExpiringBetween(item, today, expiryLimit, today))
                .count();
        return new PantrySummaryResponse(items.size(), lowStock, expiringSoon);
    }

    private PantryItem findItem(Long id, Long ownerId) {
        return repository.findByIdAndOwnerId(id, ownerId)
                .orElseThrow(() -> new PantryItemNotFoundException(id));
    }

    private void applyRequest(PantryItem item, PantryItemRequest request) {
        item.setName(request.name().trim());
        item.setCategory(request.category().trim());
        item.setQuantity(request.quantity());
        item.setUnit(request.unit().trim());
        item.setExpiryDate(request.expiryDate());
        item.setStatus(PantryStatus.resolve(item.getQuantity(), item.getExpiryDate(), LocalDate.now()));
    }

    private PantryItemResponse toResponse(PantryItem item, LocalDate today) {
        return new PantryItemResponse(
                item.getId(),
                item.getName(),
                item.getCategory(),
                item.getQuantity(),
                item.getUnit(),
                item.getExpiryDate(),
                resolveStatus(item, today),
                item.getUpdatedAt()
        );
    }

    private PantryStatus resolveStatus(PantryItem item, LocalDate today) {
        return PantryStatus.resolve(item.getQuantity(), item.getExpiryDate(), today);
    }

    private boolean isExpiringBetween(PantryItem item, LocalDate start, LocalDate end, LocalDate today) {
        return resolveStatus(item, today) != PantryStatus.USED_UP
                && item.getExpiryDate() != null
                && !item.getExpiryDate().isBefore(start)
                && !item.getExpiryDate().isAfter(end);
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
