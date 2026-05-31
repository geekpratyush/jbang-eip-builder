# Chapter 12: SRE Audit Shaper

This chapter demonstrates the **SRE Audit Shaper**, a core component for generating canonical audit records.

## Usage
The processor transforms any JSON body into a standard envelope containing:
- `metadata`: System identifiers and timestamps.
- `originalHeaders`: Serializable context.
- `payload`: The transformed data.

## Running Locally

### 1. Library Mode (Recommended)
First, build the library:
```bash
cd sre-audit-shaper
mvn clean install
```
Then run any step:
```bash
camel run 01-basic-enveloping.camel.yaml
```

### 2. Source Mode
Run without installing:
```bash
camel run 01-basic-enveloping.camel.yaml ../../../../../../sre-audit-shaper/src/main/java/com/sre/engine/audit/AuditShaperProcessor.java
```

| Header | Use Case |
| :--- | :--- |
| `CamelAuditExcludeFields` | Privacy/Cleanup |
| `CamelAuditModifyFields` | Enrichment/Tagging |
| `CamelAuditEncryptFields` | Masking sensitive PII |
