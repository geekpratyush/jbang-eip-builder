# SRE Audit & Crypto - Enterprise Security Manual

## Library Information
- **Artifact ID**: `sre-audit-crypto`
- **Dependency**: `mvn:com.tessera:sre-audit-crypto:1.0.0-SNAPSHOT`

## Core Security Features

### 1. Smart Cryptography Hierarchy
The library resolves encryption keys in the following order of precedence:
1. **Local Override**: Header `CamelAuditCryptoKey`.
2. **Global Property**: Camel property `sre.audit.crypto.key` (set in `application.properties`).
3. **Environment Variable**: `SRE_AUDIT_CRYPTO_KEY`.
4. **Stub Mode**: If no key is found, data is masked as `[STUB_ENCRYPTED::hash]`.

### 2. Intelligent "Safe Masking"
By default, the shaper protects your logs. Any field matching sensitive keywords (`password`, `secret`, `cvv`, `pin`, `apikey`) is automatically replaced with `******` during the audit process.
- **Debug Mode**: To force-print these values (for troubleshooting only), set header `CamelAuditDebugLog=true`.

### 3. Pretty Printing
Enhance log readability by setting header `CamelAuditPrettyPrint=true`. This will serialize the final JSON envelope with indentation and line breaks.

## Storage & Visibility Strategy

### Audit Data (Immutable History)
- **Target**: MongoDB (Audit collection), Elasticsearch (Kibana), or S3 (Cold Storage).
- **Format**: Full Canonical Envelope (including metadata and context headers).
- **Workflow**: Always use **Wiretap** to audit. This ensures that a failure in the audit sink doesn't crash the main business transaction.

### Business Data (Current State)
- **Target**: Primary SQL Database or Business MongoDB.
- **Format**: Original Payload (unprocessed or slightly transformed).
- **Workflow**: Direct routing (`to()`).

## Step-by-Step Patterns

### Pattern: DB Query Auditing (Mongo & SQL)
Shows how the shaper acts as a security gate. Raw data from databases is cleaned, masked, and canonicalized before being passed to the next level (Service Layer).
- **Samples**: `08-mongo-query-audit.camel.yaml`, `09-sql-query-audit.camel.yaml`
