package com.example.bankcards.security.crypto;

import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
@Service
public class CryptoServiceImpl implements CryptoService{
    private static final int GCM_TAG_BITS = 128;
    private static final int GCM_IV_LEN   = 12;
    private static final String AES_ALG   = "AES";
    private static final String AES_TRANS = "AES/GCM/NoPadding";
    private static final String HMAC_ALG  = "HmacSHA256";

    private final SecretKey aesKey;
    private final SecretKey hmacKey;
    private final SecureRandom rnd;

    @org.springframework.beans.factory.annotation.Autowired
    public CryptoServiceImpl(CryptoProperties props) {
        this(props, new SecureRandom());
    }



    /** Отдельный конструктор чтобы использовать в тестах */
    public CryptoServiceImpl(CryptoProperties props, SecureRandom rnd) {
        Assert.hasText(props.getAesKey(), "crypto.aes-key must be set");
        Assert.hasText(props.getHmacKey(), "crypto.hmac-key must be set");

        byte[] aes = Base64.getDecoder().decode(props.getAesKey());
        byte[] hmac = Base64.getDecoder().decode(props.getHmacKey());

        if (aes.length != 32) throw new IllegalArgumentException("AES key must be 32 bytes (256-bit)");
        if (hmac.length != 32) throw new IllegalArgumentException("HMAC key must be 32 bytes (256-bit)");

        this.aesKey  = new SecretKeySpec(aes, AES_ALG);
        this.hmacKey = new SecretKeySpec(hmac, HMAC_ALG);
        this.rnd = rnd;
    }

    @Override
    public byte[] encryptPan(String pan) {
        validatePan(pan);
        try {
            byte[] iv = new byte[GCM_IV_LEN];
            rnd.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_TRANS);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ct = cipher.doFinal(pan.getBytes(StandardCharsets.UTF_8));

            // Склеиваем: IV || CIPHERTEXT||TAG
            ByteBuffer buf = ByteBuffer.allocate(iv.length + ct.length);
            buf.put(iv);
            buf.put(ct);
            return buf.array();
        } catch (Exception e) {
            throw new IllegalStateException("PAN encryption failed", e);
        }
    }

    @Override
    public String decryptPan(byte[] panEnc) {
        try {
            if (panEnc == null || panEnc.length <= GCM_IV_LEN + 16)
                throw new IllegalArgumentException("Invalid encrypted PAN format");

            byte[] iv = new byte[GCM_IV_LEN];
            byte[] ct = new byte[panEnc.length - GCM_IV_LEN];

            System.arraycopy(panEnc, 0, iv, 0, GCM_IV_LEN);
            System.arraycopy(panEnc, GCM_IV_LEN, ct, 0, ct.length);

            Cipher cipher = Cipher.getInstance(AES_TRANS);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] pt = cipher.doFinal(ct);

            return new String(pt, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("PAN decryption failed", e);
        }
    }


    @Override
    public byte[] fingerprintPan(String pan) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            mac.init(hmacKey);
           return mac.doFinal(pan.getBytes(StandardCharsets.UTF_8));
        }catch (Exception e) {
            throw new IllegalStateException("PAN HMAC failed", e);
        }

    }

    @Override
    public String last4(String pan) {
        validatePan(pan);
        return pan.substring(pan.length() - 4);
    }

    public static void validatePan(String pan){
        if (pan == null || !pan.matches("\\d{12,19}")){
            throw new IllegalArgumentException("PAN must be 12-19 digits");
        }
    }
}
