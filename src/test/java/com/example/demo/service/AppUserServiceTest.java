package com.example.demo.service;

import com.example.demo.config.PasswordConfig;
import com.example.demo.model.AppUser;
import com.example.demo.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import({AppUserService.class, PasswordConfig.class})
class AppUserServiceTest {

    @Autowired
    private AppUserService appUserService;

    @Autowired
    private AppUserRepository repository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void registersAndLoadsUserWithEncodedPassword() {
        appUserService.registerUser("bob", "s3cret!");

        AppUser saved = repository.findByUsername("bob").orElseThrow();
        assertTrue(passwordEncoder.matches("s3cret!", saved.getPasswordHash()));

        UserDetails loaded = appUserService.loadUserByUsername("bob");
        assertEquals("bob", loaded.getUsername());
        assertTrue(passwordEncoder.matches("s3cret!", loaded.getPassword()));
        assertFalse(loaded.getAuthorities().isEmpty());
    }

    @Test
    void duplicateUsernameThrows() {
        appUserService.registerUser("dupe", "pass1");
        assertThrows(IllegalArgumentException.class, () -> appUserService.registerUser("dupe", "pass2"));
    }

    @Test
    void missingUserThrows() {
        assertThrows(UsernameNotFoundException.class, () -> appUserService.loadUserByUsername("absent"));
    }
}
