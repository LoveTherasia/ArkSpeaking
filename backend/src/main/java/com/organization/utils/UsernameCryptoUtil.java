package com.organization.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;

public class UsernameCryptoUtil {
    private static SecretKeySpec getKey() {
        String key = System.getenv("USERNAME_ENC_KEY");
        if (key == null || key.isEmpty()) {
            key = "ArkSpeakingDefaultKey"; // 16+ chars; fallback for dev
        }
        byte[] k = Arrays.copyOf(key.getBytes(StandardCharsets.UTF_8), 16);
        return new SecretKeySpec(k, "AES");
    }

    public static String encrypt(String plain) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, getKey());
            byte[] enc = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(enc);
        } catch (Exception e) {
            DebugLogger.logException("username-encrypt", e);
            throw new RuntimeException("username encrypt failed");
        }
    }

    public static String decrypt(String encoded) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, getKey());
            byte[] dec = cipher.doFinal(Base64.getDecoder().decode(encoded));
            return new String(dec, StandardCharsets.UTF_8);
        } catch (Exception e) {
            DebugLogger.logException("username-decrypt", e);
            throw new RuntimeException("username decrypt failed");
        }
    }
}
