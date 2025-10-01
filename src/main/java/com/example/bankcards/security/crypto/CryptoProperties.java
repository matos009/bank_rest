package com.example.bankcards.security.crypto;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "crypto")
public class CryptoProperties {
    private String aesKey;
    private String hmacKey;

    public String getAesKey() { return aesKey; }
    public void setAesKey(String aesKey) { this.aesKey = aesKey; }
    public String getHmacKey() { return hmacKey; }
    public void setHmacKey(String hmacKey) { this.hmacKey = hmacKey; }
}
