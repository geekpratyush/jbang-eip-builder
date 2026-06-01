# Enterprise Camel Route Framework — Implementation Plan

> **Status:** DRAFT — Review before implementation  
> **Scope:** Audit, Encryption/Decryption, Kamelets (MongoDB + IBM MQ), Multi-mode Connectivity, Environment Tiers

---

## 1. High-Level Architecture

```mermaid
flowchart TD
    A[Incoming Message] --> B[Pre-Processor]
    B --> C{Route Logic}
    C --> D[Audit Interceptor]
    C --> E[Encrypt / Decrypt Processor]
    D --> F[Audit Kamelet → MongoDB]
    E --> G{Target}
    G --> H[IBM MQ Kamelet]
    G --> I[Kafka]
    G --> J[REST / HTTP]
    D & H & I --> K[Connectivity Layer\nplain / SSL / mTLS / Kerberos]
    K --> L[Target Infrastructure\nDev · Docker · Prod]
```

---

## 2. Audit Framework

### 2.1 Audit JSON Structure

Every auditable exchange produces a canonical JSON document:

```json
{
  "auditId":        "AUD-<uuid>",
  "timestamp":      "2026-05-23T14:00:00.000Z",
  "routeId":        "payment-processor",
  "correlationId":  "${header.X-Correlation-ID}",
  "operation":      "PAYMENT_SUBMIT",
  "source":         "IBM-MQ:PAYMENT.IN",
  "destination":    "MongoDB:audit.payments",
  "status":         "SUCCESS | FAILURE",
  "durationMs":     142,
  "payload": {
    "before":       { ... original message ... },
    "after":        { ... transformed message ... }
  },
  "metadata": {
    "userId":       "${header.X-User-ID}",
    "environment":  "${env.APP_ENV}",
    "appVersion":   "${env.APP_VERSION}",
    "hostname":     "app-node-01 (resolved dynamically)",
    "ip":           "192.168.1.15 (resolved dynamically)"
  },
  "errors":         null
}
```

### 2.2 Field-Level Audit Controls

Controlled per-route via **route properties or headers**:

| Property | Type | Example | Behaviour |
|---|---|---|---|
| `audit.exclude.fields` | comma-separated | `"password,hostname,ip"` | Fields stripped from payload/metadata dynamically |
| `audit.exclude.payload` | boolean | `true` | Entire payload omitted (audit header only) |
| `audit.include.fields` | comma-separated | `"orderId,amount"` | Whitelist — only these fields audited |
| `audit.delete.fields` | comma-separated | `"ssn,dob"` | Fields physically deleted before audit write |
| `audit.mask.fields` | comma-separated | `"cardNumber"` | Fields replaced with `****` |
| `audit.encrypt.fields` | comma-separated | `"accountNumber"` | Fields AES-encrypted in audit record |

### 2.3 Audit Interceptor Design

```
Route ──→ [AuditInterceptorBean]
              ├── Clone exchange (before snapshot)
              ├── Apply field exclusions / deletions / masking
              ├── Optionally encrypt sensitive audit fields
              ├── Build AuditDocument JSON
              └── Async send → Audit Kamelet (MongoDB)
```

- Implemented as a Camel **Policy** or **InterceptFrom** so it wraps any route without modifying the route YAML
- Runs **asynchronously** — audit write never blocks the main flow
- Configurable **audit sampling rate** (e.g. audit 100% in dev, 10% in prod)

---

## 3. Encryption / Decryption Framework

### 3.1 Supported Algorithms

| Algorithm | Use Case | Key Source |
|---|---|---|
| **AES-256-GCM** | Field-level, authenticated encryption (preferred) | `env.AES_SECRET_KEY` |
| **AES-256-CBC** | Legacy compatibility | `env.AES_SECRET_KEY` + `env.AES_IV` |
| **Base64** | Encoding only (not real encryption — for transport encoding) | n/a |
| **RSA-2048** | Asymmetric — encrypt with public, decrypt with private | Keystore / env |

### 3.2 Key Management

```
Environment Variables (all tiers):
  AES_SECRET_KEY    = base64-encoded 256-bit key
  AES_IV            = base64-encoded 16-byte IV (CBC mode)
  KEYSTORE_PATH     = /etc/secrets/keystore.jks
  KEYSTORE_PASSWORD = ${env.KS_PASS}

Dev local override → .env file loaded at startup
Prod → injected by Vault / Kubernetes secrets
```

> **Rule**: Keys **never** hardcoded in route YAML. Always resolved from `env.*` or Camel Property Placeholders.

### 3.3 Encrypt / Decrypt Processor Bean

```java
// Usage in YAML route:
- bean:
    beanType: com.company.camel.crypto.FieldCryptoProcessor
    method: encryptFields          // or decryptFields, encryptBody, decryptBody

// Configured via exchange properties set before the bean call:
- setProperty:
    name: crypto.fields
    constant: "accountNumber,sortCode"
- setProperty:
    name: crypto.algorithm
    constant: "AES-256-GCM"
- bean:
    beanType: com.company.camel.crypto.FieldCryptoProcessor
    method: encryptFields
```

### 3.4 Selective Encrypt / Decrypt Scenarios

| Scenario | Application & Config |
|---|---|
| **Pre-Database Insert Encryption** | Encrypts specific sensitive fields (e.g. `ssn`, `creditCard`) in the payload *directly prior* to database execution (MongoDB collection writes, JDBC inserts/updates). <br> `encryptFields: "ssn,creditCard" -> to: "mongodb:..."` |
| **Post-Database Fetch Decryption** | Decrypts stored ciphertexts immediately after reading from a database query, restoring plaintext fields for internal application routing. <br> `from: "mongodb:..." -> decryptFields: "ssn,creditCard"` |
| **Decrypt before Kafka publish** | Decrypts selective fields before dispatching to external Kafka topics. <br> `decryptFields: "accountNumber" -> to: "kafka:..."` |
| **Store full encrypted payload to audit** | Protects complete payload structures inside audit tables. <br> `crypto.encryptPayload: true -> to: "direct:audit"` |
| **Re-encryption across boundaries** | Decrypts with source key and re-encrypts with target environment key. <br> `crypto.reEncrypt: true, crypto.targetKey: env.TARGET_AES_KEY` |
| **Base64-encode binary attachment** | Encodes binary file streams for clean XML/JSON transportation. <br> `encodeBase64: "attachmentField"` |

### 3.5 In-IDE Decrypt Tool (UI Window)

A new IDE panel (accessible from Tools menu):

```
┌─────────────────────────────────────────────────────┐
│  🔐 Field Decrypt Tool                          [✕] │
├─────────────────────────────────────────────────────┤
│  Algorithm:  [AES-256-GCM ▼]                        │
│  Key Source: [● Env Var  ○ Manual Entry]             │
│  Env Var:    [AES_SECRET_KEY          ]              │
│                                                      │
│  Encrypted Input:                                    │
│  ┌──────────────────────────────────────────────┐   │
│  │ <paste base64-encoded ciphertext here>       │   │
│  └──────────────────────────────────────────────┘   │
│                                                      │
│  [ Decrypt ▶ ]          [ Copy Result ]              │
│                                                      │
│  Decrypted Output:                                   │
│  ┌──────────────────────────────────────────────┐   │
│  │                                              │   │
│  └──────────────────────────────────────────────┘   │
│  ⚠ Key resolved from environment — never stored      │
└─────────────────────────────────────────────────────┘
```

---

## 4. Kamelets

### 4.1 MongoDB Kamelet

**`kamelet-studio-mongodb-sink.kamelet.yaml`**

Capabilities:
- Insert / upsert / findById / findByQuery / delete
- Connection with or without SSL/TLS
- Auth: username/password, X.509 cert (mTLS), or SCRAM
- Connection string built from env at runtime

```yaml
# Environment variables consumed:
MONGO_URI           = mongodb://user:pass@host:27017/dbname
MONGO_SSL_ENABLED   = true | false
MONGO_SSL_CA_CERT   = /etc/certs/mongo-ca.crt
MONGO_AUTH_METHOD   = scram | x509
MONGO_DATABASE      = auditdb
MONGO_COLLECTION    = audit_events
```

**Usage in route:**
```yaml
- to: "kamelet:kamelet-studio-mongodb-sink?operation=insert"
```

### 4.2 IBM MQ Kamelets

**`kamelet-studio-ibmmq-source.kamelet.yaml`**
```yaml
apiVersion: camel.apache.org/v1
kind: Kamelet
metadata:
  name: kamelet-studio-ibmmq-source
  labels:
    camel.apache.org/kamelet.type: "source"
  annotations:
    camel.apache.org/kamelet.title: "Kamelet Studio IBM MQ Source"
    camel.apache.org/kamelet.version: "1.0.0"
spec:
  definition:
    title: "Kamelet Studio IBM MQ Source"
    description: "Consumes messages from IBM MQ with SSL, mTLS, and Kerberos support."
    required:
      - queuename
    type: object
    properties:
      queuename:
        type: string
      hostname:
        type: string
        default: "localhost"
      port:
        type: integer
        default: 1414
      queuemanager:
        type: string
        default: "QM1"
      channel:
        type: string
        default: "DEV.APP.SVRCONN"
      sslciphersuite:
        type: string
        default: ""
      truststorepath:
        type: string
        default: ""
      truststorepassword:
        type: string
        format: password
      keystorepath:
        type: string
        default: ""
      keystorepassword:
        type: string
        format: password
  template:
    beans:
      - name: sslFactory
        type: "#class:com.tessera.kameletstudio.core.lib.crypto.KameletStudioSslSocketFactory"
        properties:
          trustStorePath: '{{truststorepath}}'
          trustStorePassword: '{{truststorepassword}}'
          keyStorePath: '{{keystorepath}}'
          keyStorePassword: '{{keystorepassword}}'
      - name: mqConnectionFactory
        type: "#class:com.ibm.mq.jakarta.jms.MQConnectionFactory"
        properties:
          hostName: '{{hostname}}'
          port: '{{port}}'
          queueManager: '{{queuemanager}}'
          channel: '{{channel}}'
          sslCipherSuite: '{{sslciphersuite}}'
          sslSocketFactory: '#bean:{{sslFactory}}'
      - name: mqPoolFactory
        type: "#class:org.messaginghub.pooled.jms.JmsPoolConnectionFactory"
        properties:
          connectionFactory: '#bean:{{mqConnectionFactory}}'
          maxConnections: 5
    from:
      uri: "jms:queue:{{queuename}}"
      parameters:
        connectionFactory: "#bean:{{mqPoolFactory}}"
      steps:
        - to: "kamelet:sink"
```

**`kamelet-studio-ibmmq-sink.kamelet.yaml`**
```yaml
apiVersion: camel.apache.org/v1
kind: Kamelet
metadata:
  name: kamelet-studio-ibmmq-sink
  labels:
    camel.apache.org/kamelet.type: "sink"
  annotations:
    camel.apache.org/kamelet.title: "Kamelet Studio IBM MQ Sink"
    camel.apache.org/kamelet.version: "1.0.0"
spec:
  definition:
    title: "Kamelet Studio IBM MQ Sink"
    description: "Publishes messages to IBM MQ with SSL, mTLS, and Kerberos support."
    required:
      - queuename
    type: object
    properties:
      queuename:
        type: string
      hostname:
        type: string
        default: "localhost"
      port:
        type: integer
        default: 1414
      queuemanager:
        type: string
        default: "QM1"
      channel:
        type: string
        default: "DEV.APP.SVRCONN"
      sslciphersuite:
        type: string
        default: ""
      truststorepath:
        type: string
        default: ""
      truststorepassword:
        type: string
        format: password
      keystorepath:
        type: string
        default: ""
      keystorepassword:
        type: string
        format: password
  template:
    beans:
      - name: sslFactory
        type: "#class:com.tessera.kameletstudio.core.lib.crypto.KameletStudioSslSocketFactory"
        properties:
          trustStorePath: '{{truststorepath}}'
          trustStorePassword: '{{truststorepassword}}'
          keyStorePath: '{{keystorepath}}'
          keyStorePassword: '{{keystorepassword}}'
      - name: mqConnectionFactory
        type: "#class:com.ibm.mq.jakarta.jms.MQConnectionFactory"
        properties:
          hostName: '{{hostname}}'
          port: '{{port}}'
          queueManager: '{{queuemanager}}'
          channel: '{{channel}}'
          sslCipherSuite: '{{sslciphersuite}}'
          sslSocketFactory: '#bean:{{sslFactory}}'
      - name: mqPoolFactory
        type: "#class:org.messaginghub.pooled.jms.JmsPoolConnectionFactory"
        properties:
          connectionFactory: '#bean:{{mqConnectionFactory}}'
          maxConnections: 5
    from:
      uri: "kamelet:source"
      steps:
        - to:
            uri: "jms:queue:{{queuename}}"
            parameters:
              connectionFactory: "#bean:{{mqPoolFactory}}"
```

**`kamelet-studio-ibmmq-xa-source.kamelet.yaml`** (XA/JTA Distributed Transactions)
```yaml
apiVersion: camel.apache.org/v1
kind: Kamelet
metadata:
  name: kamelet-studio-ibmmq-xa-source
  labels:
    camel.apache.org/kamelet.type: "source"
  annotations:
    camel.apache.org/kamelet.title: "Kamelet Studio IBM MQ XA Source"
    camel.apache.org/kamelet.version: "1.0.0"
spec:
  definition:
    title: "Kamelet Studio IBM MQ XA Source"
    description: "Consumes messages from IBM MQ using an XA connection pool enlisted in a JTA transaction."
    required:
      - queuename
    type: object
    properties:
      queuename:
        type: string
      hostname:
        type: string
        default: "localhost"
      port:
        type: integer
        default: 1414
      queuemanager:
        type: string
        default: "QM1"
      channel:
        type: string
        default: "DEV.APP.SVRCONN"
      sslciphersuite:
        type: string
        default: ""
      truststorepath:
        type: string
        default: ""
      truststorepassword:
        type: string
        format: password
      keystorepath:
        type: string
        default: ""
      keystorepassword:
        type: string
        format: password
  template:
    beans:
      - name: sslFactory
        type: "#class:com.tessera.kameletstudio.core.lib.crypto.KameletStudioSslSocketFactory"
        properties:
          trustStorePath: '{{truststorepath}}'
          trustStorePassword: '{{truststorepassword}}'
          keyStorePath: '{{keystorepath}}'
          keyStorePassword: '{{keystorepassword}}'
      - name: mqXAConnectionFactory
        type: "#class:com.ibm.mq.jakarta.jms.MQXAConnectionFactory"
        properties:
          hostName: '{{hostname}}'
          port: '{{port}}'
          queueManager: '{{queuemanager}}'
          channel: '{{channel}}'
          sslCipherSuite: '{{sslciphersuite}}'
          sslSocketFactory: '#bean:{{sslFactory}}'
      - name: mqXAPoolFactory
        type: "#class:org.messaginghub.pooled.jms.JmsPoolXAConnectionFactory"
        properties:
          connectionFactory: '#bean:mqXAConnectionFactory'
          transactionManager: '#bean:jtaTxManager'
          maxConnections: 5
    from:
      uri: "jms:queue:{{queuename}}"
      parameters:
        connectionFactory: "#bean:{{mqXAPoolFactory}}"
        transactionManager: "#bean:jtaTxManager"
        transacted: false
        cacheLevelName: "CACHE_NONE"
      steps:
        - to: "kamelet:sink"
```

**`kamelet-studio-ibmmq-xa-sink.kamelet.yaml`** (XA/JTA Distributed Transactions)
```yaml
apiVersion: camel.apache.org/v1
kind: Kamelet
metadata:
  name: kamelet-studio-ibmmq-xa-sink
  labels:
    camel.apache.org/kamelet.type: "sink"
  annotations:
    camel.apache.org/kamelet.title: "Kamelet Studio IBM MQ XA Sink"
    camel.apache.org/kamelet.version: "1.0.0"
spec:
  definition:
    title: "Kamelet Studio IBM MQ XA Sink"
    description: "Publishes messages to IBM MQ using an XA connection pool enlisted in a JTA transaction."
    required:
      - queuename
    type: object
    properties:
      queuename:
        type: string
      hostname:
        type: string
        default: "localhost"
      port:
        type: integer
        default: 1414
      queuemanager:
        type: string
        default: "QM1"
      channel:
        type: string
        default: "DEV.APP.SVRCONN"
      sslciphersuite:
        type: string
        default: ""
      truststorepath:
        type: string
        default: ""
      truststorepassword:
        type: string
        format: password
      keystorepath:
        type: string
        default: ""
      keystorepassword:
        type: string
        format: password
  template:
    beans:
      - name: sslFactory
        type: "#class:com.tessera.kameletstudio.core.lib.crypto.KameletStudioSslSocketFactory"
        properties:
          trustStorePath: '{{truststorepath}}'
          trustStorePassword: '{{truststorepassword}}'
          keyStorePath: '{{keystorepath}}'
          keyStorePassword: '{{keystorepassword}}'
      - name: mqXAConnectionFactory
        type: "#class:com.ibm.mq.jakarta.jms.MQXAConnectionFactory"
        properties:
          hostName: '{{hostname}}'
          port: '{{port}}'
          queueManager: '{{queuemanager}}'
          channel: '{{channel}}'
          sslCipherSuite: '{{sslciphersuite}}'
          sslSocketFactory: '#bean:{{sslFactory}}'
      - name: mqXAPoolFactory
        type: "#class:org.messaginghub.pooled.jms.JmsPoolXAConnectionFactory"
        properties:
          connectionFactory: '#bean:mqXAConnectionFactory'
          transactionManager: '#bean:jtaTxManager'
          maxConnections: 5
    from:
      uri: "kamelet:source"
      steps:
        - to:
            uri: "jms:queue:{{queuename}}"
            parameters:
              connectionFactory: "#bean:{{mqXAPoolFactory}}"
```

### 4.3 Kafka SSL Kamelets (Standard Catalog Integration)

To configure the standard **`kafka-ssl-source`** / **`kafka-ssl-sink`** Kamelets in target environments, the properties are mapped natively to their core definitions:

**`kafka-ssl-sink-binding.yaml`**
```yaml
apiVersion: camel.apache.org/v1alpha1
kind: KameletBinding
metadata:
  name: kafka-ssl-sink-binding
spec:
  source:
    ref:
      kind: Kamelet
      apiVersion: camel.apache.org/v1alpha1
      name: timer-source
    properties:
      period: 1000
  sink:
    ref:
      kind: Kamelet
      apiVersion: camel.apache.org/v1alpha1
      name: kafka-ssl-sink
    properties:
      bootstrapServers: "kafka.internal.corp:9093"
      topic: "payment-events"
      sslTruststoreLocation: "/etc/certs/kafka-truststore.jks"
      sslTruststorePassword: "${env.KAFKA_TS_PASS}"
      sslKeystoreLocation: "/etc/certs/kafka-client.jks"
      sslKeystorePassword: "${env.KAFKA_KS_PASS}"
      sslKeyPassword: "${env.KAFKA_KEY_PASS}"
```

### 4.4 MongoDB Kamelets

**`kamelet-studio-mongodb-source.kamelet.yaml`** (Change Streams / Tailable Cursors)
```yaml
apiVersion: camel.apache.org/v1
kind: Kamelet
metadata:
  name: kamelet-studio-mongodb-source
  labels:
    camel.apache.org/kamelet.type: "source"
  annotations:
    camel.apache.org/kamelet.title: "Kamelet Studio MongoDB Source"
    camel.apache.org/kamelet.version: "1.0.0"
spec:
  definition:
    title: "Kamelet Studio MongoDB Source"
    description: "Consumes real-time database modifications using Change Streams or Tailable Cursors."
    required:
      - database
      - collection
    type: object
    properties:
      connectionBean:
        type: string
        default: "mongoClient"
      database:
        type: string
      collection:
        type: string
      consumerType:
        type: string
        default: "changeStreams"
        enum: [ "changeStreams", "tailable" ]
      streamFilter:
        type: string
        description: "Aggregation pipeline JSON filter to match criteria (e.g. '[{\"$match\": {\"fullDocument.status\": \"CRITICAL\"}}]')."
        default: ""
  template:
    from:
      uri: "mongodb:{{connectionBean}}?database={{database}}&collection={{collection}}&consumerType={{consumerType}}&streamFilter={{?streamFilter}}&createCollection=true"
      steps:
        - to: "kamelet:sink"
```

**`kamelet-studio-mongodb-action.kamelet.yaml`** (Mid-Route Database Operations)
```yaml
apiVersion: camel.apache.org/v1
kind: Kamelet
metadata:
  name: kamelet-studio-mongodb-action
  labels:
    camel.apache.org/kamelet.type: "action"
  annotations:
    camel.apache.org/kamelet.title: "Kamelet Studio MongoDB Action"
    camel.apache.org/kamelet.version: "1.0.0"
spec:
  definition:
    title: "Kamelet Studio MongoDB Action"
    description: "Executes dynamic queries, updates, aggregations, or bulk operations mid-route."
    required:
      - database
      - collection
    type: object
    properties:
      connectionBean:
        type: string
        default: "mongoClient"
      database:
        type: string
      collection:
        type: string
      operation:
        type: string
        default: "findOneByQuery"
        enum: [ "findOneByQuery", "findAll", "update", "remove", "aggregate", "bulkWrite" ]
      upsert:
        type: boolean
        default: false
      filter:
        type: string
        description: "Inline query criteria filter (e.g. '{\"_id\": \"123\"}') to override body content."
        default: ""
  template:
    from:
      uri: "kamelet:source"
      steps:
        - setHeader:
            name: "CamelMongoDbCriteria"
            simple: "{{?filter}}"
        - setHeader:
            name: "CamelMongoDbUpsert"
            simple: "{{upsert}}"
        - to: "mongodb:{{connectionBean}}?database={{database}}&collection={{collection}}&operation={{operation}}"
```

### 4.5 Solace PubSub+ Kamelets

**`kamelet-studio-solace-source.kamelet.yaml`** (Local-TX / SSL / mTLS)
```yaml
apiVersion: camel.apache.org/v1
kind: Kamelet
metadata:
  name: kamelet-studio-solace-source
  labels:
    camel.apache.org/kamelet.type: "source"
  annotations:
    camel.apache.org/kamelet.title: "Kamelet Studio Solace Source"
    camel.apache.org/kamelet.version: "1.0.0"
spec:
  definition:
    title: "Kamelet Studio Solace Source"
    description: "Consumes messages from Solace PubSub+ queue using SMF or SMFS TLS."
    required:
      - queuename
      - host
      - vpn
    type: object
    properties:
      queuename:
        type: string
      host:
        type: string
        default: "smf://localhost:55555"
      vpn:
        type: string
        default: "default"
      username:
        type: string
      password:
        type: string
        format: password
      transacted:
        type: boolean
        default: true
      ssltruststorepath:
        type: string
      ssltruststorepassword:
        type: string
        format: password
      sslkeystorepath:
        type: string
      sslkeystorepassword:
        type: string
        format: password
  template:
    beans:
      - name: solaceConnectionFactory
        type: "#class:com.solacesystems.jms.SolJmsUtility"
        factory-method: "createConnectionFactory"
        properties:
          host: '{{host}}'
          vpn: '{{vpn}}'
          username: '{{username}}'
          password: '{{password}}'
          sslTrustStore: '{{?ssltruststorepath}}'
          sslTrustStorePassword: '{{?ssltruststorepassword}}'
          sslKeyStore: '{{?sslkeystorepath}}'
          sslKeyStorePassword: '{{?sslkeystorepassword}}'
      - name: solacePoolFactory
        type: "#class:org.messaginghub.pooled.jms.JmsPoolConnectionFactory"
        properties:
          connectionFactory: '#bean:{{solaceConnectionFactory}}'
          maxConnections: 5
    from:
      uri: "jms:queue:{{queuename}}"
      parameters:
        connectionFactory: "#bean:{{solacePoolFactory}}"
        transacted: "{{transacted}}"
      steps:
        - to: "kamelet:sink"
```

**`kamelet-studio-solace-xa-source.kamelet.yaml`** (XA / JTA Narayana Transactions)
```yaml
apiVersion: camel.apache.org/v1
kind: Kamelet
metadata:
  name: kamelet-studio-solace-xa-source
  labels:
    camel.apache.org/kamelet.type: "source"
  annotations:
    camel.apache.org/kamelet.title: "Kamelet Studio Solace XA Source"
    camel.apache.org/kamelet.version: "1.0.0"
spec:
  definition:
    title: "Kamelet Studio Solace XA Source"
    description: "Consumes messages from Solace using an XA connection pool managed by JTA."
    required:
      - queuename
      - host
      - vpn
    type: object
    properties:
      queuename:
        type: string
      host:
        type: string
        default: "smfs://localhost:55443"
      vpn:
        type: string
        default: "default"
      username:
        type: string
      password:
        type: string
        format: password
  template:
    beans:
      - name: solaceXAConnectionFactory
        type: "#class:com.solacesystems.jms.SolJmsUtility"
        factory-method: "createXAConnectionFactory"
        properties:
          host: '{{host}}'
          vpn: '{{vpn}}'
          username: '{{username}}'
          password: '{{password}}'
      - name: solaceXAPoolFactory
        type: "#class:org.messaginghub.pooled.jms.JmsPoolXAConnectionFactory"
        properties:
          connectionFactory: '#bean:{{solaceXAConnectionFactory}}'
          transactionManager: '#bean:jtaTxManager'
          maxConnections: 5
    from:
      uri: "jms:queue:{{queuename}}"
      parameters:
        connectionFactory: "#bean:{{solaceXAPoolFactory}}"
        transactionManager: "#bean:jtaTxManager"
        transacted: false
        cacheLevelName: "CACHE_NONE"
      steps:
        - to: "kamelet:sink"
```

### 4.6 Relational Database (SQL) Kamelets

**`kamelet-studio-sql-action.kamelet.yaml`** (Unified CRUD Interface)
```yaml
apiVersion: camel.apache.org/v1
kind: Kamelet
metadata:
  name: kamelet-studio-sql-action
  labels:
    camel.apache.org/kamelet.type: "action"
  annotations:
    camel.apache.org/kamelet.title: "Kamelet Studio SQL Action"
    camel.apache.org/kamelet.version: "1.0.0"
spec:
  definition:
    title: "Kamelet Studio SQL Action"
    description: "Performs dynamic SQL database select, update, insert, and delete operations."
    required:
      - table
    type: object
    properties:
      connectionBean:
        type: string
        default: "dataSource"
      table:
        type: string
      operation:
        type: string
        default: "findAll"
        enum: [ "findAll", "update", "delete", "query" ]
  template:
    from:
      uri: "kamelet:source"
      steps:
        - setHeader:
            name: "eip.sql.table"
            constant: "{{table}}"
        - setHeader:
            name: "eip.sql.operation"
            constant: "{{operation}}"
        - setHeader:
            name: "dynamicQuery"
            groovy: |
              import com.fasterxml.jackson.databind.ObjectMapper
              def table = request.headers.get('eip.sql.table')
              def op = request.headers.get('eip.sql.operation')
              def body = request.body
              def mapper = new ObjectMapper()
              
              if (op == "findAll") {
                  if (!(body instanceof Map) || body.isEmpty()) {
                      return "SELECT * FROM ${table}"
                  }
                  def parts = []
                  body.each { k, v ->
                      request.headers.put("f_" + k, v)
                      parts.add("${k} = :?f_${k}")
                  }
                  return "SELECT * FROM ${table} WHERE " + parts.join(" AND ")
              }
              if (op == "update") {
                  // body is: [filterMap, updateMap]
                  def filter = body[0]
                  def update = body[1]
                  def setParts = []
                  update.each { k, v ->
                      request.headers.put("u_" + k, v)
                      setParts.add("${k} = :?u_${k}")
                  }
                  def whereParts = []
                  filter.each { k, v ->
                      request.headers.put("f_" + k, v)
                      whereParts.add("${k} = :?f_${k}")
                  }
                  return "UPDATE ${table} SET ${setParts.join(', ')} WHERE " + whereParts.join(" AND ")
              }
              if (op == "delete") {
                  def parts = []
                  body.each { k, v ->
                      request.headers.put("f_" + k, v)
                      parts.add("${k} = :?f_${k}")
                  }
                  return "DELETE FROM ${table} WHERE " + parts.join(" AND ")
              }
              if (op == "query") {
                  return body.get("_sql") // Custom query string direct from JSON
              }
              return ""
        - setBody:
            simple: "${header.dynamicQuery}"
        - to: "sql:dynamic?dataSource=#{{connectionBean}}&useMessageBodyForSql=true"
```

---

## 5. Connectivity Tiers

### 5.1 Connection Mode Matrix

| Mode | IBM MQ | MongoDB | Kafka | Config Key |
|---|---|---|---|---|
| **Plain** (no TLS) | ✅ | ✅ | ✅ | `CONN_MODE=plain` |
| **SSL (one-way)** | ✅ | ✅ | ✅ | `CONN_MODE=ssl` |
| **mTLS** | ✅ | ✅ | ✅ | `CONN_MODE=mtls` |
| **Kerberos** | ✅ | ❌ | ✅ | `CONN_MODE=kerberos` |
| **SSL + Kerberos** | ✅ | ❌ | ✅ | `CONN_MODE=ssl-kerberos` |

### 5.2 Certificate / Secret Resolution

```
Priority chain (lowest to highest):
  1. Defaults (plain/no-auth)
  2. .env file (local dev)
  3. application.properties (Quarkus profile)
  4. Environment variables
  5. Kubernetes Secrets (prod)
  6. Vault (enterprise prod)
```

### 5.3 SSL / mTLS Config Structure

```properties
# One-way SSL
camel.ssl.enabled=true
camel.ssl.truststore.path=/etc/certs/truststore.jks
camel.ssl.truststore.password=${env.SSL_TRUST_PASS}

# Mutual TLS (mTLS) — Requires BOTH Keystore (client identity) and Truststore (server cert verification)
camel.mtls.enabled=true
camel.mtls.keystore.path=/etc/certs/client-keystore.jks
camel.mtls.keystore.password=${env.SSL_KEY_PASS}
camel.mtls.key.alias=camel-client
camel.mtls.truststore.path=/etc/certs/truststore.jks
camel.mtls.truststore.password=${env.SSL_TRUST_PASS}

# Kerberos (with optional SSL/TLS Transport - SASL_SSL requires Truststore JKS)
camel.kerberos.enabled=true
camel.kerberos.principal=camel@REALM.CORP
camel.kerberos.keytab=/etc/kerberos/camel.keytab
camel.kerberos.krb5.conf=/etc/kerberos/krb5.conf
camel.kerberos.ssl.truststore.path=/etc/certs/truststore.jks
camel.kerberos.ssl.truststore.password=${env.SSL_TRUST_PASS}
```

---

## 6. Environment Tiers

### 6.1 Tier Definitions

| Tier | Name | Infrastructure | Connectivity | Config Profile |
|---|---|---|---|---|
| 0 | **Mock (Offline Pure)** | None — all in-memory stubs | `stub:` / `mock:` / `direct:` | `mock` |
| 1 | **Local DevServices (Offline Docker)** | Local Docker Containers (no external network) | Plain or SSL | `devservices` |
| 2 | **Dev (Enterprise)** | Real corp dev env | SSL / mTLS / Kerberos | `dev` |
| 3 | **SIT / UAT** | Real corp env + certs | mTLS / Kerberos | `sit` |
| 4 | **Production** | Hardened | mTLS + Kerberos + Vault | `prod` |

### 6.1.1 Offline Testing & Execution Strategy

When developers run routes locally in an air-gapped/offline environment, the studio supports two primary mechanisms to mock or host database, Kafka, and MongoDB components:

#### A. Pure Offline (No Docker) — Profile: `mock`
This mode has **zero system dependencies** and starts instantly.
- **Camel Stub Component:** Any URI prefixed with `stub:` (e.g., `stub:kafka:orders`, `stub:mongodb:cameldb`) redirects to an in-memory queue. It accepts inputs and generates dummy return values without throwing connection exceptions.
- **Mock Endpoints:** Standard Camel `mock:sink` endpoints act as assertion targets to verify that messages reached their destination.
- **Mock DB Fetches:** SQL databases are mocked using the `direct:fetch-db-details` route template which intercepts database requests and immediately returns static JSON/XML payload structures (as shown in the `enrichment-route.yaml` sample).

#### B. Local Containers (With Docker Offline) — Profile: `devservices`
If the developer has Docker installed locally (even without internet connection):
- **Quarkus DevServices:** At application startup, Quarkus automatically detects the missing database/Kafka brokers and launches lightweight local container instances (e.g. `mongo:7`, `confluentinc/cp-kafka:latest`) using images cached in the local Docker daemon registry.
- **Auto-Configuration:** Port numbers, database names, and credentials are dynamically injected into the running Camel context, allowing developers to test real integration logic offline.

### 6.2 Quarkus Profile Mapping

```properties
# application-mock.properties
quarkus.camel.stub.enabled=true
mongodb.uri=stub
ibmmq.host=stub

# application-devservices.properties
quarkus.mongodb.devservices.enabled=true
quarkus.artemis.devservices.enabled=true   # ActiveMQ ≈ IBM MQ for local test
kafka.bootstrap=localhost:9092

# application-dev.properties
ibmmq.host=dev-mq.corp.internal
ibmmq.ssl.enabled=true
ibmmq.ssl.cipher=TLS_RSA_WITH_AES_256_CBC_SHA256
ibmmq.ssl.keystore.path=/etc/certs/mq-client.jks
ibmmq.ssl.keystore.password=${env.MQ_KS_PASS}
ibmmq.ssl.truststore.path=/etc/certs/mq-truststore.jks
ibmmq.ssl.truststore.password=${env.MQ_TS_PASS}
mongodb.uri=mongodb+srv://dev-mongo.corp.internal

# application-prod.properties
ibmmq.kerberos.enabled=true
vault.url=https://vault.corp.internal
```

### 6.3 DevServices Docker Compose (for Tier 1)

```yaml
# devservices-compose.yaml (included in project)
services:
  mongodb:
    image: mongo:7
    ports: ["27017:27017"]
    environment:
      MONGO_INITDB_ROOT_USERNAME: camel
      MONGO_INITDB_ROOT_PASSWORD: camel123

  activemq:           # IBM MQ compatible for local dev
    image: apache/activemq-artemis:latest
    ports: ["61616:61616", "8161:8161"]

  kafka:
    image: confluentinc/cp-kafka:latest
    ports: ["9092:9092"]

  mongo-express:      # MongoDB UI
    image: mongo-express:latest
    ports: ["8081:8081"]
```

---

## 7. Deployment Model

### 7.1 Route-Only Deployment (Camel JBang / Quarkus)

Routes + dependencies → packaged as:

```
deploy/
├── routes/
│   ├── audit-interceptor.camel.yaml
│   ├── payment-processor.camel.yaml
│   └── ...
├── kamelets/
│   ├── kamelet-studio-mongodb-sink.kamelet.yaml
│   ├── kamelet-studio-mongodb-source.kamelet.yaml
│   ├── kamelet-studio-mongodb-action.kamelet.yaml
│   ├── kamelet-studio-ibmmq-source.kamelet.yaml
│   ├── kamelet-studio-ibmmq-sink.kamelet.yaml
│   ├── kamelet-studio-ibmmq-xa-source.kamelet.yaml
│   └── kamelet-studio-ibmmq-xa-sink.kamelet.yaml
├── processors/
│   └── FieldCryptoProcessor.java    (JBang single-file)
├── config/
│   ├── application.properties
│   └── application-prod.properties
└── certs/                            (gitignored, mounted at runtime)
```

**Run anywhere:**
```bash
# Mock mode
jbang camel@apache/camel run routes/ --profile=mock

# DevServices (Docker)
jbang camel@apache/camel run routes/ --profile=devservices

# Dev (corp SSL)
jbang camel@apache/camel run routes/ --profile=dev \
  -Dquarkus.ssl.truststore.password=$SSL_TRUST_PASS

# Quarkus fat JAR for prod
./mvnw package -Pnative
```

### 7.2 Target Environment Deployment & Promotion (UAT, NFT, Prod)

When promoting routes from the local IDE sandbox to actual enterprise environments, a structured CI/CD pipeline packages, validates, and deploys the Camel routes:

```
[Local IDE Route] ──→ [Git Repo] ──→ [CI Pipeline (Maven Build)]
                                            ├── Runs JUnit & LSP Syntax Checks
                                            ├── Packages into Native Container Image
                                            └── Deploys to Kubernetes / OpenShift
                                                     ├── Mounts Secrets & Certs (Vault)
                                                     └── Sets Environment (UAT / NFT / PROD)
```

#### A. Packaging Models

The project artifacts (YAML routes, Kamelets, custom Java processors, configs) are promoted using one of two cloud-native packaging systems:

1. **Quarkus Native Container Image (Standard Microservice Package):**
   The CI pipeline compiles the codebase using GraalVM into a single, high-performance native Linux executable:
   ```bash
   ./mvnw package -Pnative -Dquarkus.container-image.build=true -Dquarkus.container-image.group=camel-apps -Dquarkus.container-image.name=payment-router
   ```
   This produces a tiny, ultra-fast container image (~50MB startup in 20ms) containing only the compiled routes, runtime, and cryptographic beans. The resulting image is pushed to the corporate container registry (e.g. Nexus, Harbor, AWS ECR).

2. **Serverless Camel K Operator (Resource-Friendly Package):**
   Alternatively, in environments with the Camel K Operator installed, routes and Kamelets are deployed as plain text resources:
   ```bash
   kamel run routes/payment-processor.camel.yaml --dependency=mvn:org.apache.camel:camel-crypto --config=secret:app-secrets
   ```
   The Operator automatically provisions a minimal container at runtime and injects dependencies on the fly.

---

#### B. Secret Injection & Configuration in Target Environments

In actual corporate environments, configuration files containing credentials, Keystores, Truststores, and Keytabs are **never** bundled into the deployment container. Instead:

- **ConfigMaps:** Environment settings (e.g., broker hostnames, queue names, Mongo URI hosts, connection modes) are stored in Kubernetes ConfigMaps and bound to the container as environment variables.
- **Secrets:** Keystore files (`.jks`), Keytab files (`.keytab`), truststores, and decryption/API secrets are retrieved dynamically from a central vaults system (e.g., HashiCorp Vault, CyberArk) and mounted onto the pod's filesystem (typically at `/etc/secrets/certs/` and `/etc/secrets/krb5/`) via Kubernetes CSI drivers.
- **Environment Variables Resolution:** The Camel properties resolver resolves values at boot-up:
  ```properties
  # In application.properties
  camel.ssl.truststore.path=/etc/secrets/certs/truststore.jks
  camel.ssl.truststore.password=${env.TRUSTSTORE_PASSWORD}
  ```

---

#### C. Running in Target Environments

- **UAT (User Acceptance Testing):** Runs with profile `QUARKUS_PROFILE=uat`. Connected to mock/stub systems initially, then switched to real core banking systems over mTLS (using keystores/truststores mounted in the pod) for end-to-end integration tests.
- **NFT (Non-Functional Testing / Performance):** Runs with profile `QUARKUS_PROFILE=nft`. Deployed with high CPU/Memory resource allocations inside the cluster. Stubs are tuned to simulate high-throughput responses (e.g., sub-millisecond latencies) or actual IBM MQ/Kafka load generators are used to evaluate execution speed, connection pools, and circuit-breaker thresholds under stress.
- **Production:** Runs with profile `QUARKUS_PROFILE=prod` with strict security audits enabled, full Kerberos/mTLS connection validation, read-only filesystem mounts, and metrics gathered via Prometheus scrape endpoints `/metrics` (configured in Chapter 8).

### 7.3 Scaffolding & Output Deliverables (List of Generated Items)

When the developer creates or builds a project in this studio, the workspace is populated with the following structured deliverables:

```
workspace-root/
├── routes/                            # 1. YAML DSL Routes (Chapter-wise & Custom)
│   ├── chapter-01-basics/
│   ├── chapter-02-routing/
│   ├── chapter-03-messaging/
│   ├── chapter-04-error-handling/
│   ├── chapter-05-transformation/
│   ├── chapter-06-rest-api/
│   ├── chapter-07-enterprise/
│   ├── chapter-08-rest-server/
│   └── chapter-09-crypto-audit/      # Custom enterprise routes
├── kamelets/                          # 2. Custom Reuseable Connectors (Kamelets)
│   ├── kamelet-studio-mongodb-sink.kamelet.yaml
│   ├── kamelet-studio-mongodb-source.kamelet.yaml
│   ├── kamelet-studio-mongodb-action.kamelet.yaml
│   ├── kamelet-studio-ibmmq-source.kamelet.yaml
│   ├── kamelet-studio-ibmmq-sink.kamelet.yaml
│   ├── kamelet-studio-ibmmq-xa-source.kamelet.yaml
│   └── kamelet-studio-ibmmq-xa-sink.kamelet.yaml
├── src/main/java/                     # 3. Custom Java Beans & Processors
│   └── com/routebuilder/crypto/
│       ├── FieldCryptoProcessor.java  # AES-256-GCM / Base64 implementation
│       └── AuditInterceptorBean.java  # Exclusions, Masking, and payload auditing
├── config/                            # 4. Environment Configurations & Keystores
│   ├── application.properties         # Common properties
│   ├── application-mock.properties    # Offline/Stub profiles
│   ├── application-devservices.properties # Docker DevServices profiles
│   └── application-dev.properties     # Dev/mTLS/Kerberos placeholder template
├── docker/                            # 5. Local Orchestration Setup
│   └── devservices-compose.yaml       # Offline Docker compose (Mongo, ActiveMQ, Kafka)
└── README-connectivity.md             # 6. Certification & Connection Guides
```

#### Detailed Breakdown of Delivered Items:

1. **Structured Route Files (`.camel.yaml`):** Standardized Camel YAML DSL files containing routes (`- route:`) and REST definitions (`- rest:`). Each file is injected with metadata headers `# ID`, `# ENABLED`, `# DESCRIPTION`, and `# STUB`.
2. **Kamelets:** Reusable connectors encapsulating the complex connection mechanics for **MongoDB** and **IBM MQ**, shielding individual route files from connection details.
3. **Java Cryptographic & Audit Processors:** Core processing beans compiled alongside the routes to perform AES encryption, decryption, field-level deletions, and masking.
4. **Environment Profiles:** Ready-to-use properties configurations mapping out specific connectivity modes (Plain/SSL/mTLS/Kerberos) across environment tiers.
5. **Docker Compose Stack:** Offline infrastructure blueprint allowing local Docker daemons to launch necessary databases and brokers instantly without cloud access.
6. **Diagram Artifacts:** Visual documentation exports (such as exported `.svg` images and `.png` snapshots) of the route flow diagram generated inside the studio.

### 7.4 Gradle Build Dependencies

For compiling and running the routes framework (under both the local development server and target Quarkus containers), the following dependencies must be added to the project's `build.gradle` file:

```groovy
dependencies {
    // IBM MQ Jakarta JMS Client (JMS 3.0 support)
    implementation 'com.ibm.mq:com.ibm.mq.jakarta.client:9.4.5.1'

    // JMS Connection Pooling (with Jakarta support)
    implementation 'org.messaginghub:pooled-jms:3.1.2'

    // Camel Quarkus / Camel components
    implementation 'org.apache.camel.quarkus:camel-quarkus-yaml-dsl'
    implementation 'org.apache.camel.quarkus:camel-quarkus-jms'
    implementation 'org.apache.camel.quarkus:camel-quarkus-mongodb'
    implementation 'org.apache.camel.quarkus:camel-quarkus-jackson'
    implementation 'org.apache.camel.quarkus:camel-quarkus-jta'
}
```

---



---

## 8. Implementation Phases

### Phase 1 — Core Framework (implement first)
- [ ] `FieldCryptoProcessor.java` — AES-256-GCM + Base64 encrypt/decrypt
- [ ] `AuditInterceptorBean.java` — field exclusion, deletion, masking, async write
- [ ] `AuditDocument.java` — canonical audit JSON builder
- [ ] In-IDE Decrypt Tool window (JavaFX)
- [ ] `crypto-utils.camel.yaml` — reusable encrypt/decrypt route fragments

### Phase 2 — Kamelets
- [ ] `kamelet-studio-mongodb-sink.kamelet.yaml` — insert / upsert / query
- [ ] `kamelet-studio-mongodb-source.kamelet.yaml` — Change Streams with stream filter/criteria
- [ ] `kamelet-studio-mongodb-action.kamelet.yaml` — dynamic queries, updates, aggregations
- [ ] `kamelet-studio-ibmmq-source.kamelet.yaml` — consume from MQ (Local Transactions)
- [ ] `kamelet-studio-ibmmq-sink.kamelet.yaml` — publish to MQ (Local Transactions)
- [ ] `kamelet-studio-ibmmq-xa-source.kamelet.yaml` — consume from MQ (XA Transactions)
- [ ] `kamelet-studio-ibmmq-xa-sink.kamelet.yaml` — publish to MQ (XA Transactions)
- [ ] Connectivity factory beans (SSL/mTLS/Kerberos builders)

### Phase 3 — Sample Routes (chapter-09)
- [ ] `01-audit-payment-flow.camel.yaml`
- [ ] `02-encrypt-to-mongo-decrypt-to-kafka.camel.yaml`
- [ ] `03-ibmmq-ssl-consumer.camel.yaml`
- [ ] `04-ibmmq-mtls-request-reply.camel.yaml`
- [ ] `05-ibmmq-kerberos.camel.yaml`
- [ ] `06-mongodb-audit-store.camel.yaml`
- [ ] `07-full-audit-encrypt-route.camel.yaml`

### Phase 4 — Environment Config
- [ ] `application-mock.properties`
- [ ] `application-devservices.properties`
- [ ] `application-dev.properties` (template — no real secrets)
- [ ] `devservices-compose.yaml`
- [ ] `README-connectivity.md`

---

## 9. Open Questions / Decisions Needed

> [!IMPORTANT]
> Please review these before implementation begins.

| # | Question | Options | Default Assumed |
|---|---|---|---|
| 1 | **AES key rotation strategy?** | Static env var / Vault dynamic secrets / KMS | Static env var (Phase 1) |
| 2 | **Audit storage: one collection or sharded by route?** | Single `audit_events` / per-route collections | Single collection |
| 3 | **Audit async or sync?** | Async (non-blocking) / Sync (guaranteed) | Async |
| 4 | **IBM MQ client lib: official IBM jar or Qpid?** | `com.ibm.mq.allclient` / Apache Qpid | IBM allclient |
| 5 | **mTLS cert format: JKS or PEM?** | JKS / PEM / PKCS12 | Support all three |
| 6 | **Kerberos: use keytab or ticket cache?** | Keytab file / existing krb5cc | Keytab |
| 7 | **DevServices: Artemis as IBM MQ substitute or real IBM MQ Developer image?** | Artemis / `ibmcom/mq` Docker image | `ibmcom/mq` (free developer edition) |
| 8 | **Audit sampling: per-route config or global?** | Per-route property / Global env var | Per-route property |
| 9 | **Decrypt UI: read key from env at tool open time, or allow manual paste?** | Env only / Env + manual | Both (env preferred) |
| 10 | **Re-encryption across environments: same key or derived key per env?** | Same / Per-env derived | Per-env (separate env vars) |

---

## 10. Security Rules (Non-Negotiable)

> [!CAUTION]
> These apply across all phases and environments.

- 🔒 **No secrets in YAML** — all keys via `${env.VAR}` or `{{prop}}`
- 🔒 **No secrets in logs** — `audit.exclude.fields` always includes key names
- 🔒 **No plaintext key in IDE Decrypt Tool memory** — cleared after each use
- 🔒 **mTLS keystores gitignored** — never committed to repo
- 🔒 **AES-GCM preferred** over CBC — prevents padding oracle attacks
- 🔒 **Kerberos keytab files** stored outside project directory
- 🔒 **Audit records immutable** — no update/delete operations on audit collection

---

## 11. Hands-on Enterprise Integration Scenarios & DSL Templates

This section provides production-grade, architectural YAML templates designed to handle complex data and message routing pipelines across heterogeneous systems (relational SQL databases, MongoDB, Kafka, IBM MQ, Solace, and REST web services).

Each route is fully documented, contains explicit audit hooks, and utilizes dynamic metadata headers to configure behavior.

---

### Scenario 1: REST API Gateway to SQL Database (Oracle/PostgreSQL) CRUD

* **Architecture Goal:** Expose a REST web service that acts as a proxy for database operations. It intercepts payloads, encrypts sensitive customer fields (e.g. `creditCardNumber`), inserts or updates rows dynamically in a relational database, and fires an asynchronous audit event (complete with host, IP, and duration metadata).
* **Connection Mode:** Plain/SSL depending on JNDI data source pools.

**`01-sql-crud-gateway.camel.yaml`**
```yaml
# ID: sql-crud-gateway
# ENABLED: true
# DESCRIPTION: Receives customer request payloads over REST, encrypts sensitive database fields, executes dynamic SQL CRUD, and records asynchronous audits.
# STUB: stub:sql:dynamic?dataSource=#dataSource

- rest:
    path: "/api/v1/customers"
    get:
      - to: "direct:get-customers"
    post:
      - to: "direct:create-customer"

- route:
    id: "get-customers"
    from:
      uri: "direct:get-customers"
      steps:
        # Step 1: Query all records matching the filter parameters
        - setBody:
            simple: "${header.CamelHttpQuery}"  # Maps JSON query criteria directly
        - to: "kamelet:kamelet-studio-sql-action?table=customers&operation=findAll"
        
        # Step 2: Decrypt output data fields before returning response
        - setProperty:
            name: "crypto.fields"
            constant: "creditCardNumber"
        - bean:
            beanType: "com.tessera.kameletstudio.crypto.FieldCryptoProcessor"
            method: "decryptFields"

- route:
    id: "create-customer"
    from:
      uri: "direct:create-customer"
      steps:
        # Step 1: Pre-Database Encryption
        - setProperty:
            name: "crypto.fields"
            constant: "creditCardNumber"
        - setProperty:
            name: "crypto.algorithm"
            constant: "AES-256-GCM"
        - bean:
            beanType: "com.tessera.kameletstudio.crypto.FieldCryptoProcessor"
            method: "encryptFields"

        # Step 2: Insert into Database via dynamic SQL action
        # SQL Kamelet will dynamically generate the INSERT query using body parameters
        - to: "kamelet:kamelet-studio-sql-action?table=customers&operation=update"

        # Step 3: Trigger Audit Interceptor
        - setHeader:
            name: "AuditOperation"
            constant: "CUSTOMER_CREATE_SQL"
        - setHeader:
            name: "AuditDestination"
            constant: "SQLTable:customers"
        - bean:
            beanType: "com.tessera.kameletstudio.crypto.AuditInterceptorBean"
            method: "processAudit"
            
        - setBody:
            constant: "{\"status\": \"SUCCESS\", \"message\": \"Customer record stored successfully.\"}"
```

---

### Scenario 2: Solace XA Message Router to MongoDB Change Streams & Kafka SSL

* **Architecture Goal:** Consume messages from a Solace queue under strict XA/JTA distributed transaction control. The route decrypts incoming metadata, performs a MongoDB CRUD update, and writes confirmation back to a secure Kafka SSL broker. Simultaneously, a MongoDB Change Stream listener filters changes matching a status query and propagates them to a Kafka event bus.
* **Connection Mode:** mTLS + SSL.

**`02-solace-xa-mongo-kafka.camel.yaml`**
```yaml
# ID: solace-xa-mongo-kafka
# ENABLED: true
# DESCRIPTION: Consumes events from Solace using XA/JTA, updates MongoDB, and forwards transaction receipts to Kafka SSL. Contains a second route tracking MongoDB change stream modifications.
# STUB: stub:jms:queue:payments.in

- route:
    id: "solace-xa-consumer"
    from:
      # Solace source governed by distributed JTA (Narayana) transaction manager
      uri: "kamelet:kamelet-studio-solace-xa-source?queuename=payments.in&host=smfs://solace-broker:55443&vpn=production"
      steps:
        # Step 1: Decrypt incoming payload parameters
        - setProperty:
            name: "crypto.fields"
            constant: "accountBalance,taxId"
        - bean:
            beanType: "com.tessera.kameletstudio.crypto.FieldCryptoProcessor"
            method: "decryptFields"

        # Step 2: Persist state changes in MongoDB database
        - to: "kamelet:kamelet-studio-mongodb-action?database=banking&collection=accounts&operation=update"

        # Step 3: Publish transaction trace to secure Kafka SSL broker
        - to: "kamelet:kafka-ssl-sink?bootstrapServers=kafka-secure:9093&topic=tx-receipts&sslTruststoreLocation=/etc/certs/kafka-truststore.jks&sslTruststorePassword=${env.KAFKA_TS_PASS}"

- route:
    id: "mongo-change-stream-listener"
    from:
      # Capture modifications on accounts collection matching status="CRITICAL"
      uri: "kamelet:kamelet-studio-mongodb-source?database=banking&collection=accounts&streamFilter=[{\"$match\": {\"fullDocument.status\": \"CRITICAL\"}}]"
      steps:
        - log: "Critical modification detected in database: ${body}"
        # Forward event data to secure Kafka Topic for fraud monitoring
        - to: "kamelet:kafka-ssl-sink?bootstrapServers=kafka-secure:9093&topic=fraud-alerts&sslTruststoreLocation=/etc/certs/kafka-truststore.jks&sslTruststorePassword=${env.KAFKA_TS_PASS}"
```

---

### Scenario 3: Secure REST Proxy to IBM MQ with mTLS and Kerberos Fallback

* **Architecture Goal:** Expose an HTTP REST endpoint with strict server identity verification. Requests are forwarded directly to an IBM MQ queue manager over mTLS. If the client certificate is not present, authentication falls back to Kerberos (GSSAPI SASL_SSL via Keytab mapping). The response uses MQ Request-Reply correlation IDs to synchronize the asynchronous reply block back to the HTTP gateway client.
* **Connection Mode:** mTLS or Kerberos.

**`03-rest-ibmmq-mtls-proxy.camel.yaml`**
```yaml
# ID: rest-ibmmq-mtls-proxy
# ENABLED: true
# DESCRIPTION: Routes HTTP client payloads to IBM MQ over SSL/mTLS or Kerberos authentication with request-reply synchronization.
# STUB: stub:jms:queue:requests.out

- rest:
    path: "/api/v1/integrations"
    post:
      - to: "direct:send-to-mq"

- route:
    id: "send-to-mq"
    from:
      uri: "direct:send-to-mq"
      steps:
        # Step 1: Assign correlation ID for tracking
        - setHeader:
            name: "X-Correlation-ID"
            simple: "${uuid}"
            
        # Step 2: Route request to IBM MQ with mTLS credentials
        # Uses request-reply pattern (InOut exchange mode) waiting for a matching correlation ID on the reply queue
        - to: "kamelet:kamelet-studio-ibmmq-xa-sink?queuename=requests.out&hostname=mq-srv.corp.internal&port=1414&queuemanager=QM1&channel=SSL.SVRCONN&sslciphersuite=TLS_RSA_WITH_AES_256_CBC_SHA256&truststorepath=/etc/certs/mq-truststore.jks&truststorepassword=${env.MQ_TS_PASS}&keystorepath=/etc/certs/mq-client.jks&keystorepassword=${env.MQ_KS_PASS}"
        
        # Step 3: Trigger Audit intercept block
        - setHeader:
            name: "AuditOperation"
            constant: "IBMMQ_REQUEST_REPLY"
        - setHeader:
            name: "AuditDestination"
            constant: "IBM-MQ:requests.out"
        - bean:
            beanType: "com.tessera.kameletstudio.crypto.AuditInterceptorBean"
            method: "processAudit"
            
        - log: "Transaction reply received and audited successfully: ${body}"
```

