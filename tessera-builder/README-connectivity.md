# Enterprise Connectivity Guide

This guide explains how to configure and run Camel routes across different environment tiers using the built-in connectivity profiles.

## Connectivity Modes

| Mode | Description | Config Key |
|---|---|---|
| **Plain** | No encryption or authentication | `CONN_MODE=plain` |
| **SSL** | One-way TLS (server verification) | `CONN_MODE=ssl` |
| **mTLS** | Mutual TLS (client + server verification) | `CONN_MODE=mtls` |
| **Kerberos** | GSSAPI authentication | `CONN_MODE=kerberos` |

## Environment Tiers

### 1. Mock (Tier 0)
Purely offline. All external systems are stubbed in-memory.
```bash
jbang camel run routes/ --profile=mock
```

### 2. DevServices (Tier 1)
Uses local Docker containers to host real MongoDB, Kafka, and Artemis.
```bash
docker-compose -f docker/devservices-compose.yaml up -d
jbang camel run routes/ --profile=devservices
```

### 3. Dev (Tier 2)
Connected to core enterprise development brokers and databases. Requires certificates.
```bash
export MQ_KS_PASS=...
export MQ_TS_PASS=...
jbang camel run routes/ --profile=dev
```

## Security Requirements

- **AES_SECRET_KEY**: Must be set for routes using `FieldCryptoProcessor`.
- **Certs**: JKS files must be placed in `/etc/certs/` or as configured in properties.
- **Keytabs**: For Kerberos mode, ensure your keytab is accessible to the process.

---
*Created by Kamelet Studio Connectivity Framework*
