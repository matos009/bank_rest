package com.example.bankcards.controller;


import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.BusinessException;
import com.example.bankcards.security.jwt.JwtService;
import com.example.bankcards.service.user.UserService;
import com.example.bankcards.web.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Set;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@Import({GlobalExceptionHandler.class})
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockBean UserService users;
    @MockBean PasswordEncoder encoder;
    @MockBean JwtService jwt;

    @Test
    void login_ok() throws Exception {
        var entity = new User();
        entity.setId(42L);
        entity.setEmail("john@example.com");
        entity.setEnabled(true);
        entity.setPasswordHash("$2a...");
        var role = new Role();
        role.setName("USER");
        entity.setRoles(Set.of(role));

        given(users.getEntityByEmailOrThrow("john@example.com")).willReturn(entity);
        given(encoder.matches("qwerty", "$2a...")).willReturn(true);
        given(jwt.generateToken(42L, "john@example.com", List.of("USER"))).willReturn("tok");
        given(jwt.getExpiry("tok")).willReturn(Instant.parse("2030-01-01T00:00:00Z"));

        var body = om.writeValueAsString(new AuthController.LoginRequest("john@example.com", "qwerty"));

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("tok"));
    }

    @Test
    void login_bad_credentials() throws Exception {
        var entity = new User();
        entity.setEnabled(true);
        entity.setPasswordHash("$2a...");
        var role = new Role();
        role.setName("USER");
        entity.setRoles(Set.of(role));

        given(users.getEntityByEmailOrThrow("a@b.c")).willReturn(entity);
        given(encoder.matches("wrong", "$2a...")).willReturn(false);

        var body = om.writeValueAsString(new AuthController.LoginRequest("a@b.c", "wrong"));

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }
}