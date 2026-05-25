package com.routebuilder.kameletstudio.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.Map;

/**
 * Field-level encryption and decryption processor for Camel routes.
 * Supports AES-256-GCM with PBKDF2 key derivation.
 */
public class FieldCryptoProcessor {

    private static final String DEFAULT_ALGORITHM = "AES-256-GCM";
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int KEY_LENGTH = 256;
    private static final int ITERATION_COUNT = 65536;

    public void encryptFields(Object body, Map<String, Object> properties) throws Exception {
        processFields(body, properties, true);
    }

    public void decryptFields(Object body, Map<String, Object> properties) throws Exception {
        processFields(body, properties, false);
    }

    private void processFields(Object body, Map<String, Object> properties, boolean encrypt) throws Exception {
        String fieldsStr = (String) properties.get("crypto.fields");
        if (fieldsStr == null || fieldsStr.isEmpty()) {
            return;
        }

        String secret = System.getenv("AES_SECRET_KEY");
        if (secret == null || secret.isEmpty()) {
            secret = (String) properties.get("crypto.secret");
        }

        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("AES_SECRET_KEY environment variable or crypto.secret property must be set.");
        }

        String[] fields = fieldsStr.split(",");
        if (body instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) body;
            for (String field : fields) {
                field = field.trim();
                Object value = map.get(field);
                if (value instanceof String) {
                    if (encrypt) {
                        map.put(field, encrypt((String) value, secret));
                    } else {
                        map.put(field, decrypt((String) value, secret));
                    }
                }
            }
        }
    }

    public String encrypt(String plainText, String secret) throws Exception {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);

        byte[] iv = new byte[IV_LENGTH];
        new SecureRandom().nextBytes(iv);

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(secret.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec);

        byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

        byte[] combined = new byte[SALT_LENGTH + IV_LENGTH + cipherText.length];
        System.arraycopy(salt, 0, combined, 0, SALT_LENGTH);
        System.arraycopy(iv, 0, combined, SALT_LENGTH, IV_LENGTH);
        System.arraycopy(cipherText, 0, combined, SALT_LENGTH + IV_LENGTH, cipherText.length);

        return Base64.getEncoder().encodeToString(combined);
    }

    public String decrypt(String base64Text, String secret) throws Exception {
        byte[] decode = Base64.getDecoder().decode(base64Text);
        if (decode.length < SALT_LENGTH + IV_LENGTH) {
            throw new IllegalArgumentException("Ciphertext is too short.");
        }

        byte[] salt = new byte[SALT_LENGTH];
        System.arraycopy(decode, 0, salt, 0, SALT_LENGTH);

        byte[] iv = new byte[IV_LENGTH];
        System.arraycopy(decode, 16, iv, 0, IV_LENGTH);

        byte[] cipherBytes = new byte[decode.length - SALT_LENGTH - IV_LENGTH];
        System.arraycopy(decode, SALT_LENGTH + IV_LENGTH, cipherBytes, 0, cipherBytes.length);

        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        KeySpec spec = new PBEKeySpec(secret.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH);
        SecretKey tmp = factory.generateSecret(spec);
        SecretKeySpec secretKey = new SecretKeySpec(tmp.getEncoded(), "AES");

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec);

        byte[] decryptedBytes = cipher.doFinal(cipherBytes);
        return new String(decryptedBytes, StandardCharsets.UTF_8);
    }
}
