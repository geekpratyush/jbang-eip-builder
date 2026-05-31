# Faker Mock & Template Studio

Generating realistic test payloads is essential for testing integrations without hitting production resources. The **Faker & Mock Studio** provides a template-driven engine to generate datasets.

---

## 1. Mocking Engine

The engine reads payload templates containing placeholders that reference mock generators:
- **Personal Details**: `{{name.fullName}}`, `{{address.city}}`, `{{phone.phoneNumber}}`.
- **Financial Mock Data**: `{{finance.creditCardNumber}}`, `{{finance.iban}}`, `{{finance.bic}}`.
- **Identifiers & Dates**: `{{internet.uuid}}`, `{{date.past}}`, `{{number.numberBetween 10 100}}`.

---

## 2. Template Syntax

Mock profiles support JSON, YAML, and CSV structures:

```json
{
  "transactionId": "{{internet.uuid}}",
  "sender": "{{name.fullName}}",
  "iban": "{{finance.iban}}",
  "amount": "{{number.randomDouble 2 100 10000}}",
  "currency": "EUR"
}
```

---

## 3. High-Performance Unpacking

The Faker template library packs pre-configured mock definitions and hundreds of SWIFT financial templates. 
- **Direct Disk Writing**: Unpacking bypasses intermediate UI component render loops, resolving thread hangs.
- **Single Refresh Threading**: A single UI thread refresh occurs after the batch write finishes, rendering hundreds of nested template files instantly.
