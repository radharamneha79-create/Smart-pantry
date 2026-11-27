package com.smartpantry.security;

import com.smartpantry.domain.PantryUser;
import com.smartpantry.repository.PantryUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class PantryUserDetailsService implements UserDetailsService {

    private final PantryUserRepository repository;

    public PantryUserDetailsService(PantryUserRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        PantryUser user = repository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new UsernameNotFoundException("Account was not found."));
        return new PantryUserDetails(user.getId(), user.getEmail(), user.getPasswordHash());
    }
}
