package com.example.bankcards.jwt;



import com.example.bankcards.security.jwt.JwtProperties;
import com.example.bankcards.security.jwt.JwtServiceImpl;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceImplTest {

    private static JwtServiceImpl newSvc(String issuer, int minutes) {
        String b64 = Base64.getEncoder().encodeToString(new byte[32]);

        JwtProperties p = new JwtProperties();
        p.setSecret(b64);
        p.setIssuer(issuer);
        p.setExpiryMinutes(minutes);

        return new JwtServiceImpl(p);
    }

    @Test
    void generate_and_parse_ok() {
        JwtServiceImpl svc = newSvc("bankcards", 60);

        String token = svc.generateToken(
                42L,
                "john@example.com",
                List.of("USER", "ADMIN")
        );

        assertNotNull(token);
        assertTrue(svc.validate(token), "token must validate");

        Optional<Long> userId = svc.getUserId(token);
        Optional<String> email = svc.getEmail(token);
        var roles = svc.getRoles(token);
        Instant exp = svc.getExpiry(token);

        assertTrue(userId.isPresent());
        assertEquals(42L, userId.get());
        assertTrue(email.isPresent());
        assertEquals("john@example.com", email.get());
        assertTrue(roles.containsAll(List.of("USER", "ADMIN")));


        Instant now = Instant.now();
        assertTrue(exp.isAfter(now), "expiry should be in future");
        assertTrue(Duration.between(now.plusSeconds(3600), exp).abs().getSeconds() < 10,
                "expiry should be ~60 minutes from now");
    }

    @Test
    void validate_false_when_token_tampered() {
        JwtServiceImpl svc = newSvc("bankcards", 60);
        String token = svc.generateToken(1L, "a@b.c", List.of("USER"));

        // Подпорим последний символ (сегмент подписи после последней '.')
        String[] parts = token.split("\\.");
        assertEquals(3, parts.length, "JWT must have 3 parts");
        String sig = parts[2];


        char last = sig.charAt(sig.length() - 1);
        char replacement = (last == 'A') ? 'B' : 'A';
        String tamperedSig = sig.substring(0, sig.length() - 1) + replacement;

        String tampered = parts[0] + "." + parts[1] + "." + tamperedSig;

        assertFalse(svc.validate(tampered), "tampered token must not validate");
    }

    @Test
    void validate_false_when_wrong_issuer() {
        JwtServiceImpl issuerA = newSvc("issuer-A", 60);
        JwtServiceImpl issuerB = newSvc("issuer-B", 60);

        String tokenFromA = issuerA.generateToken(7L, "x@y.z", List.of());

        // Парсим токен «другим» сервисом (другой issuer) — должен быть невалиден
        assertFalse(issuerB.validate(tokenFromA));
    }

    @Test
    void constructor_rejects_short_secret() {
        JwtProperties p = new JwtProperties();


        String shortB64 = Base64.getEncoder().encodeToString(new byte[16]);
        p.setSecret(shortB64);
        p.setIssuer("bankcards");
        p.setExpiryMinutes(60);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new JwtServiceImpl(p));
        assertTrue(ex.getMessage().contains("at least 32 bytes"));
    }
}