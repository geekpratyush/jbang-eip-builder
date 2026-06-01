package com.tessera.kameletstudio.crypto;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class FieldCryptoProcessorTest {

    @Test
    public void testEncryptDecrypt() throws Exception {
        FieldCryptoProcessor processor = new FieldCryptoProcessor();
        String secret = "my-very-secret-key-1234567890123";
        String plainText = "Hello World";

        String encrypted = processor.encrypt(plainText, secret);
        assertNotNull(encrypted);
        assertNotEquals(plainText, encrypted);

        String decrypted = processor.decrypt(encrypted, secret);
        assertEquals(plainText, decrypted);
    }

    @Test
    public void testProcessFields() throws Exception {
        FieldCryptoProcessor processor = new FieldCryptoProcessor();
        String secret = "test-secret";
        
        Map<String, Object> body = new HashMap<>();
        body.put("accountNumber", "12345678");
        body.put("name", "John Doe");

        Map<String, Object> properties = new HashMap<>();
        properties.put("crypto.fields", "accountNumber");
        properties.put("crypto.secret", secret);

        processor.encryptFields(body, properties);
        
        String encrypted = (String) body.get("accountNumber");
        assertNotEquals("12345678", encrypted);
        assertEquals("John Doe", body.get("name"));

        processor.decryptFields(body, properties);
        assertEquals("12345678", body.get("accountNumber"));
    }
}
