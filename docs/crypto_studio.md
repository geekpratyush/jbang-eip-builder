# Cryptography & Security Studio

Security is paramount in enterprise routes. The IDE packages dedicated cryptographic utilities to encrypt, decrypt, and verify transaction payloads.

---

## 1. Algorithmic Foundation

The studio uses strong encryption mechanisms to protect sensitive properties and payloads:
- **AES-256-GCM**: Authenticated symmetric encryption with random IVs (Initialization Vectors).
- **PBKDF2 Key Derivation**: High-security password-based key derivation using HMAC-SHA256 with random salt inputs to derive encryption keys.
- **Base64 Encoding**: Encrypted payloads are formatted as compact, URL-safe Base64 strings.

---

## 2. Dynamic Cryptographic Beans

Developers can instantiate pre-packaged cryptography beans inside their routes to secure steps automatically:

```yaml
- beans:
    - name: cryptoProcessor
      type: "#class:com.tessera.kameletstudio.core.lib.crypto.KameletStudioCryptoProcessor"
      properties:
        password: "secretPassword"
        salt: "randomSaltStringValue"
```

Any message passing through the route targeting this bean will be encrypted (or decrypted) on the fly.

---

## 3. Interactive Decrypt UI

The IDE includes an in-app utility window to verify and troubleshoot encrypted strings manually:
- Input password, salt, and base64-encoded encrypted cipher text.
- Click **Verify & Decrypt** to view derived parameters (key, IV, tag) and inspect decrypted plaintext immediately.
