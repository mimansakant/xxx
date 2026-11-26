package com.collective404.bankclient.auth;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.collective404.bankclient.model.AppUser;
import com.collective404.bankclient.repository.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:bankclient-auth;DB_CLOSE_DELAY=-1"
})
class AuthServiceTest {

    @Autowired
    private AuthService auth;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private SessionRegistry sessions;

    @BeforeEach
    void clearUsers() {
        users.deleteAll();
    }

    @Test
    void signupStoresPasswordHashAndReturnsSessionToken() {
        final AuthService.AuthResult result =
                auth.signUp("Samara", "long-enough-password");
        final AppUser saved = users.findByUsernameIgnoreCase("samara")
                .orElseThrow();

        assertTrue(sessions.getUserId(result.token()).equals(saved.getId()));
        assertNotEquals("long-enough-password", saved.getPasswordHash());
        assertTrue(passwordEncoder.matches(
                "long-enough-password", saved.getPasswordHash()));
    }

    @Test
    void loginComparesPasswordAgainstStoredHash() {
        auth.signUp("Samara", "correct-password");

        final AuthService.AuthResult result =
                auth.signIn("SAMARA", "correct-password");

        assertTrue(sessions.getUserId(result.token()) != null);
    }
}
