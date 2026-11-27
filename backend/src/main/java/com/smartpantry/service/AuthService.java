package com.smartpantry.service;

import com.smartpantry.domain.PantryUser;
import com.smartpantry.exception.AccountAlreadyExistsException;
import com.smartpantry.repository.PantryItemRepository;
import com.smartpantry.repository.PantryUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.Locale;

@Service
public class AuthService {

    private final PantryUserRepository userRepository;
    private final PantryItemRepository itemRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(PantryUserRepository userRepository,
                       PantryItemRepository itemRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.itemRepository = itemRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public PantryUser register(String email, String password) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new AccountAlreadyExistsException(normalizedEmail);
        }

        PantryUser user;
        try {
            user = userRepository.saveAndFlush(
                    new PantryUser(normalizedEmail, passwordEncoder.encode(password)));
        } catch (DataIntegrityViolationException exception) {
            if (exception.getMostSpecificCause() instanceof SQLException sqlException
                    && "23505".equals(sqlException.getSQLState())) {
                throw new AccountAlreadyExistsException(normalizedEmail);
            }
            throw exception;
        }
        itemRepository.assignUnclaimedItemsTo(user);
        return user;
    }
}
