package com.example.bankcards.jwt;

import com.example.bankcards.security.jwt.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Base64;

@TestConfiguration
@EnableWebSecurity
class JwtFilterTestConfig {

    @Bean @Primary
    JwtProperties jwtProperties() {
        JwtProperties p = new JwtProperties();
        p.setIssuer("test-issuer");
        p.setSecret(Base64.getEncoder().encodeToString(new byte[32])); // 32 байта
        p.setExpiryMinutes(60);
        return p;
    }

    @Bean @Primary
    JwtService jwtService(JwtProperties props) {
        return new JwtServiceImpl(props);
    }

    @Bean
    JwtAuthFilter jwtAuthFilter(JwtService jwt) {
        return new JwtAuthFilter(jwt);
    }

    @Bean
    SecurityFilterChain testChain(HttpSecurity http, JwtAuthFilter jwtAuthFilter) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(fl -> fl.disable())
                .httpBasic(b -> b.disable())
                .exceptionHandling(e -> e

                                .authenticationEntryPoint((req, res, ex) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED))

                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/open/**").permitAll()
                        .requestMatchers("/secure/admin").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}