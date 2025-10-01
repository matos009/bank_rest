package com.example.bankcards.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
public class JwtServiceImpl implements JwtService {

    private final JwtProperties props;
    private final SecretKey key;

    public JwtServiceImpl(JwtProperties props) {
        Assert.hasText(props.getSecret(), "security.jwt.secret must be set");
        this.props = props;

        byte[] raw = Base64.getDecoder().decode(props.getSecret());
        if (raw.length < 32) { // для HS256 нужно >= 256 бит
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes (256-bit) after base64 decode");
        }
        this.key = Keys.hmacShaKeyFor(raw);
    }

    @Override
    public String generateToken(long userId, String email, List<String> roles) {
        Instant now = Instant.now();
        Instant exp = now.plus(props.getExpiryMinutes(), ChronoUnit.MINUTES);


        return Jwts.builder()
                .setHeaderParam(Header.TYPE, Header.JWT_TYPE) // тип заголовка "JWT"
                .setIssuer(props.getIssuer())
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .setSubject(Long.toString(userId))
                .claim("email", email)
                .claim("roles", roles == null ? Collections.emptyList() : roles)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public boolean validate(String token) {
        try {
            Jwts.parserBuilder()
                    .requireIssuer(props.getIssuer())
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims claims(String token) {
        Jws<Claims> jws = Jwts.parserBuilder()
                .requireIssuer(props.getIssuer())
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
        return jws.getBody();
    }

    @Override
    public Optional<Long> getUserId(String token) {
        try {
            String sub = claims(token).getSubject();
            return Optional.ofNullable(sub).map(Long::parseLong);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> getEmail(String token) {
        try {
            return Optional.ofNullable(claims(token).get("email", String.class));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getRoles(String token) {
        try {
            // тип стирается, поэтому получаем как raw List и приводим элементы к String
            Object v = claims(token).get("roles");
            if (v instanceof List<?> list) {
                List<String> out = new ArrayList<>(list.size());
                for (Object o : list) out.add(String.valueOf(o));
                return out;
            }
            return Collections.emptyList();
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Override
    public Instant getExpiry(String token) {
        Date d = claims(token).getExpiration();
        return d.toInstant();
    }
}