package com.example.bankcards.security.crypto;

public interface CryptoService {
    /** Шифрует PAN (цифры карты) в AES-GCM, возвращает байты формата: IV(12) || CIPHERTEXT||TAG */
    byte[] encryptPan(String pan);

    /** Дешифрует то, что вернул encryptPan */
    String decryptPan(byte[] panEnc);

    /** Возвращает HMAC-SHA256(PAN) как байтовый массив (для pan_fp) */
    byte[] fingerprintPan(String pan);

    /** Последние 4 цифры (для хранения/маскирования) c базовой валидацией */
    String last4(String pan);
}
