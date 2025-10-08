package com.example.bankcards.service;


import com.example.bankcards.dto.login.LoginRequest;
import com.example.bankcards.dto.login.LoginResponse;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.security.jwt.JwtService;
import com.example.bankcards.service.auth.AuthServiceImpl;
import com.example.bankcards.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock UserService users;
    @Mock PasswordEncoder encoder;
    @Mock JwtService jwt;

    @InjectMocks
    AuthServiceImpl service;

    private static User enabledUser() {
        User u = new User();
        u.setId(42L);
        u.setEmail("john@example.com");
        u.setEnabled(true);
        u.setPasswordHash("$2a$dummy");

        Role role = new Role();
        role.setName("USER");
        u.setRoles(Set.of(role));

        return u;
    }

    @Test
    void login_ok() {
        var req = new LoginRequest("john@example.com", "qwerty");
        var user = enabledUser();

        given(users.getEntityByEmailOrThrow("john@example.com")).willReturn(user);
        given(encoder.matches("qwerty", "$2a$dummy")).willReturn(true);
        given(jwt.generateToken(42L, "john@example.com", List.of("USER"))).willReturn("tok");
        Instant exp = Instant.parse("2030-01-01T00:00:00Z");
        given(jwt.getExpiry("tok")).willReturn(exp);

        LoginResponse resp = service.login(req);

        assertThat(resp.token()).isEqualTo("tok");
        assertThat(resp.expiresAt()).isEqualTo(exp);
    }

    @Test
    void login_user_disabled() {
        var req = new LoginRequest("john@example.com", "qwerty");
        var user = enabledUser();
        user.setEnabled(false);

        given(users.getEntityByEmailOrThrow("john@example.com")).willReturn(user);

        assertThatThrownBy(() -> service.login(req))
                .isInstanceOf(BusinessException.class)
                .hasMessage("User disabled");
    }

    @Test
    void login_bad_credentials() {
        var req = new LoginRequest("john@example.com", "wrong");
        var user = enabledUser();

        given(users.getEntityByEmailOrThrow("john@example.com")).willReturn(user);
        given(encoder.matches("wrong", "$2a$dummy")).willReturn(false);

        assertThatThrownBy(() -> service.login(req))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Invalid credentials");
    }
}