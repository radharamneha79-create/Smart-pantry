package com.smartpantry.repository;

import com.smartpantry.domain.PantryItem;
import com.smartpantry.domain.PantryUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PantryItemRepository extends JpaRepository<PantryItem, Long> {

    List<PantryItem> findAllByOwnerId(Long ownerId);

    Optional<PantryItem> findByIdAndOwnerId(Long id, Long ownerId);

    @Modifying
    @Query("update PantryItem item set item.owner = :owner where item.owner is null")
    int assignUnclaimedItemsTo(@Param("owner") PantryUser owner);
}
