package com.smartpantry.repository;

import com.smartpantry.domain.PantryUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PantryUserRepository extends JpaRepository<PantryUser, Long> {

    Optional<PantryUser> findByEmail(String email);

    boolean existsByEmail(String email);
}
