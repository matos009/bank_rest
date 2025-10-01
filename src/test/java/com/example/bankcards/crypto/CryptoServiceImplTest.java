package com.example.bankcards.crypto;

import com.example.bankcards.security.crypto.CryptoProperties;
import com.example.bankcards.security.crypto.CryptoService;
import com.example.bankcards.security.crypto.CryptoServiceImpl;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class CryptoServiceImplTest {

    private static CryptoService newServiceDeterministic() {

        String aesB64  = Base64.getEncoder().encodeToString(new byte[32]);
        String hmacB64 = Base64.getEncoder().encodeToString(new byte[32]);

        CryptoProperties props = new CryptoProperties();
        props.setAesKey(aesB64);
        props.setHmacKey(hmacB64);


        SecureRandom deterministic = new SecureRandom(new byte[]{1,2,3,4,5,6,7,8});
        return new CryptoServiceImpl(props, deterministic);
    }

    @Test
    void roundTripEncryption() {
        CryptoService svc = newServiceDeterministic();
        String pan = "4111111111111111";

        byte[] enc = svc.encryptPan(pan);
        assertNotNull(enc);
        assertTrue(enc.length > 12);

        String dec = svc.decryptPan(enc);
        assertEquals(pan, dec);
    }

    @Test
    void fingerprintStableForSamePan() {
        CryptoService svc = newServiceDeterministic();
        String pan = "5555444433331111";

        byte[] fp1 = svc.fingerprintPan(pan);
        byte[] fp2 = svc.fingerprintPan(pan);

        assertArrayEquals(fp1, fp2);
    }

    @Test
    void encryptionProducesDifferentCiphertextsForSamePan() {
        CryptoService svc = newServiceDeterministic(); // тут наоборот лучше НЕ детерминированный, но можно и детерминированный + разный seed
        String pan = "4111111111111111";
        byte[] c1 = svc.encryptPan(pan);
        byte[] c2 = svc.encryptPan(pan);
        assertFalse(java.util.Arrays.equals(c1, c2), "GCM IV должен делать шифротексты разными");
        assertEquals(pan, svc.decryptPan(c1));
        assertEquals(pan, svc.decryptPan(c2));
    }

    @Test
    void decryptionFailsOnTamperedCiphertext() {
        CryptoService svc = newServiceDeterministic();
        byte[] enc = svc.encryptPan("4111111111111111");
        enc[enc.length - 1] ^= 0x01; //Flip last bit
        assertThrows(IllegalStateException.class, () -> svc.decryptPan(enc));
    }

    @Test
    void constructorValidatesKeys() {
        CryptoProperties p = new CryptoProperties();
        p.setAesKey(Base64.getEncoder().encodeToString(new byte[16]));  // 128-bit вместо 256
        p.setHmacKey(Base64.getEncoder().encodeToString(new byte[32]));
        assertThrows(IllegalArgumentException.class, () -> new CryptoServiceImpl(p, new SecureRandom()));

        CryptoProperties p2 = new CryptoProperties();
        p2.setAesKey("not-base64");
        p2.setHmacKey(Base64.getEncoder().encodeToString(new byte[32]));
        assertThrows(IllegalArgumentException.class, () -> new CryptoServiceImpl(p2, new SecureRandom()));
    }

    @Test
    void last4WorksAndPanValidation() {
        CryptoService svc = newServiceDeterministic();
        assertEquals("1111", svc.last4("4111111111111111"));

        // пан — только цифры и длина 12..19
        assertThrows(IllegalArgumentException.class, () -> svc.last4("abcd"));
        assertThrows(IllegalArgumentException.class, () -> svc.last4("123"));
        assertThrows(IllegalArgumentException.class, () -> svc.last4("1234abcd"));
    }
}