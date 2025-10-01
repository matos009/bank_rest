package com.example.bankcards.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("security.jwt")
public class JwtProperties {
    private String secret;
    private String issuer = "bankcards";
    private int expiryMinutes = 60;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }
    public int getExpiryMinutes() { return expiryMinutes; }
    public void setExpiryMinutes(int expiryMinutes) { this.expiryMinutes = expiryMinutes; }
}