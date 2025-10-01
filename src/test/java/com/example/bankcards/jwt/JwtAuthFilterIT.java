package com.example.bankcards.jwt;

import com.example.bankcards.security.jwt.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import com.example.bankcards.jwt.JwtFilterTestControllers;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import({JwtFilterTestConfig.class, JwtFilterTestControllers.class})
class JwtAuthFilterIT {

    @Autowired MockMvc mvc;
    @Autowired
    JwtService jwt;

    @Test
    void openEndpoint_isAccessibleWithoutToken() throws Exception {
        mvc.perform(get("/open/ping"))
                .andExpect(status().isOk())
                .andExpect(content().string("pong"));
    }

    @Test
    void secureEndpoint_withoutToken_returns401() throws Exception {
        mvc.perform(get("/secure/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminEndpoint_withUserRole_returns403() throws Exception {
        String token = jwt.generateToken(7L, "u@test", List.of("USER"));

        mvc.perform(get("/secure/admin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpoint_withAdminRole_returns200() throws Exception {
        String token = jwt.generateToken(7L, "u@test", List.of("USER","ADMIN"));

        mvc.perform(get("/secure/admin")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void secureMe_withValidToken_returnsUserIdFromPrincipal() throws Exception {
        String token = jwt.generateToken(42L, "john@example.com", List.of("USER"));

        mvc.perform(get("/secure/me")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.TEXT_PLAIN))
                .andExpect(status().isOk())
                .andExpect(content().string("userId=42"));
    }

    @Test
    void secureEndpoint_withBrokenToken_returns401() throws Exception {
        String token = jwt.generateToken(100L, "x@y", List.of("USER"));
        String broken = token.substring(0, token.length() - 2) + "zz";

        mvc.perform(get("/secure/me")
                        .header("Authorization", "Bearer " + broken))
                .andExpect(status().isUnauthorized());
    }
}