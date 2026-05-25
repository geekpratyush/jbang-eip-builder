# Camel Quarkus IDE - User Manual

Welcome to the Camel Quarkus Route Builder IDE! This environment is purpose-built to help you rapidly prototype, develop, and test Enterprise Integration Patterns (EIP) utilizing the Apache Camel YAML DSL running atop Quarkus.

---

## Chapter 1: Introduction to the YAML DSL

Camel's YAML DSL provides a human-readable, declarative structure for designing integration routes. A route generally consists of a `from` endpoint (the source) and a series of `steps` (processors, enterprise integration patterns, and `to` endpoints).

### Basic Route Example
```yaml
- route:
    id: "hello-timer"
    from:
      uri: "timer:hello?period=3000"
      steps:
        - log:
            message: "[Chapter 1] Hello from Apache Camel! Timestamp: ${date:now:HH:mm:ss}"
```

In the IDE, you can create a new `.yaml` file, and the integrated Language Server will provide you with precise autocomplete based on the Quarkus catalog.

---

## Chapter 2: Resiliency and Error Handling

Enterprise applications require robust error handling. Camel provides several mechanisms out of the box.

### DoTry / DoCatch
Catch exceptions inline using a try-catch pattern.
```yaml
- route:
    id: "resilient-route"
    from:
      uri: "direct:start"
      steps:
        - doTry:
            steps:
              - to: "http://unreliable-service.com/api"
            doCatch:
              - exception:
                  - "java.net.SocketTimeoutException"
                steps:
                  - log: "Service timed out, falling back!"
                  - setBody:
                      constant: "Fallback Data"
```

### Circuit Breakers & Retries
Utilize Fault Tolerance features like Circuit Breaker for failing fast.
```yaml
- route:
    id: "circuit-breaker-route"
    from:
      uri: "direct:invoke"
      steps:
        - circuitBreaker:
            steps:
              - to: "http://brittle-service.com"
            onFallback:
              steps:
                - transform:
                    constant: "Default Fallback Response"
```

---

## Chapter 3: Data Transformation

Camel shines at transforming data between various formats and structures.

### Standard File/Script Mapping
You can pass payloads through transformation scripts dynamically.

**Using XSLT (XML to XML):**
```yaml
- route:
    id: "xslt-transform"
    from: "direct:xml-input"
    steps:
      - to: "xslt-saxon:file:routes/sample-project/transform.xslt"
      - log: "Transformed XML: ${body}"
```

**Using JSLT / JOLT (JSON to JSON):**
```yaml
      - to: "jolt:file:routes/sample-project/spec.json"
```

**Using Groovy / Kotlin (Dynamic Scripting):**
If you need complex logic, map via a groovy script:
```yaml
      - setBody:
          groovy: "resource:file:routes/sample-project/script.groovy"
```

### Advanced Enrichment (MT/MX Swift Example)
In banking, you frequently deal with SWIFT MT messages. Using `camel-quarkus-swift`, you can unmarshal, enrich from a database, and output to MX XML.

```yaml
- route:
    id: "swift-enrichment-route"
    from: "direct:mt-input"
    steps:
      - unmarshal:
          swiftMt: {}
      - enrich:
          expression:
            constant: "direct:fetch-db-details"
          aggregationStrategy: "myCustomMergeStrategy"
      - to: "xslt-saxon:file:routes/sample-project/mt-to-mx-route.xslt"
      - to: "jms:queue:mx-outbound"
```

---

## Chapter 4: Testing & Mocking (Zero-Infrastructure)

You do not need an actual Kafka, JMS, or IBM MQ broker installed to test routes!

### Using Flapdoodle MongoDB / SQLite
If your app connects to a database, you can use in-memory stubs or embedded servers provided by Quarkus test extensions.

### Stubbing Components and Kamelets
If you want to mock `kafka:` or `jms:`, simply use `mock:kafka:topic` or `stub:kafka:topic` during development.

**Dummy Kamelet Structure:**
```yaml
- route:
    id: "kamelet-test"
    from: "kamelet:my-dummy-source"
    steps:
      - to: "mock:result"
```

---

## Chapter 5: IDE Features & Help Portal

* **Live Language Server:** Press `Ctrl+Space` inside the YAML editor to trigger autocomplete recommendations populated directly from the loaded Apache Camel and Quarkus catalog.
* **Visual Builder Canvas:** Toggle the Diagram panel to render interactive representations of your routes. Supports splits, exception handlers, aggregates, and direct node selection.
* **Toolbar Controls:** The **Play** and **Stop** icons in the main toolbar compile and run your current YAML route on-the-fly using JBang's ultra-fast Camel Main runner (under 1-second startup) for instant local testing.
* **Context-Aware Execution Menus:** Right-clicking any file or folder in the project tree panel displays the **"Run Route(s)"** menu item. Selecting a folder scans all contents recursively to run all routes/rests together in offline/stub mode (loading mock endpoints from the workspace `application.properties`) for zero-infrastructure sandbox testing. This automatically redirects calls to Solace, IBM MQ, Kafka, and databases through lightweight local stubs.
* **Interactive Help System:** Clicking the **"Manual"** icon launches a Microsoft HTML Help-style classic viewer. Features:
  * **Hide/Show toolbar toggle:** Collapses the sidebar to maximize reading space.
  * **Contents tree:** Expands hierarchical chapters and sections mapping the IDE framework.
  * **Index tab:** Indexes key terms and integration concepts for rapid navigation.
  * **Search tab:** Executes full-text search query scans across manual sections to find keyword matching topics.
  * **Options menu:** Provides zoom adjustment controls.


---

## Chapter 6: Field-Level Cryptography & Audit Interceptors

Production deployments must satisfy corporate security requirements regarding data masking and transaction audits.

### Selective Encryption & Decryption
The Studio includes a cryptographic processor (`FieldCryptoProcessor.java`) performing authenticated AES-256-GCM encryption on selected fields.

To encrypt fields pre-database or pre-outbound write, apply:
```yaml
- setProperty:
    name: "crypto.fields"
    constant: "ssn,creditCardNumber"
- setProperty:
    name: "crypto.algorithm"
    constant: "AES-256-GCM"
- bean:
    beanType: "com.routebuilder.kameletstudio.crypto.FieldCryptoProcessor"
    method: "encryptFields"
```

To restore the plain-text fields after fetching or consuming:
```yaml
- setProperty:
    name: "crypto.fields"
    constant: "ssn,creditCardNumber"
- bean:
    beanType: "com.routebuilder.kameletstudio.crypto.FieldCryptoProcessor"
    method: "decryptFields"
```

### Canonical Auditing (with exclusion rules)
Every transaction publishes audit logs containing execution metrics, hostnames, and IP info. Developers can selectively exclude variables from audit scopes using `audit.exclude.fields` (e.g. `audit.exclude.fields="password,hostname,ip"`).

```yaml
- setHeader:
    name: "AuditOperation"
    constant: "TRANSACTION_PAYMENT"
- bean:
    beanType: "com.routebuilder.kameletstudio.crypto.AuditInterceptorBean"
    method: "processAudit"
```

---

## Chapter 7: Custom Kamelet Catalog (MongoDB, IBM MQ, Solace, SQL)

To hide connection mechanics from individual route definitions, the Studio bundles specialized, reusable connectors (Kamelets).

### MongoDB Kamelets
* **`kamelet-studio-mongodb-source`**: Capture database modifications via MongoDB Change Streams. It supports aggregation criteria:
  ```yaml
  streamFilter: '[{"$match": {"fullDocument.status": "CRITICAL"}}]'
  ```
* **`kamelet-studio-mongodb-action`**: Perform mid-route CRUD (including `findOneByQuery`, `update`, `aggregate`, and `bulkWrite`).

### IBM MQ (JMS 3.0 Local-TX and JTA/XA)
* **`kamelet-studio-ibmmq-source` & `sink`**: Connect via client channels. Uses standard transaction boundaries.
* **`kamelet-studio-ibmmq-xa-source` & `sink`**: Connects via `com.ibm.mq.jakarta.jms.MQXAConnectionFactory` and `JmsPoolXAConnectionFactory` bound to Narayana JTA transaction managers. Sets `transacted: false` and `cacheLevelName: CACHE_NONE` as required by the XA contract.

### Solace Messaging (SMF & SMFS)
* **`kamelet-studio-solace-source` & `sink`**: Consumes from Solace Message VPNs. Supports TLS (`smfs://`) with truststore certificate validation and mTLS keystores.
* **`kamelet-studio-solace-xa-source`**: Solace messaging XA connector governed under distributed transaction managers.

### Relational SQL Action (Oracle / PostgreSQL)
* **`kamelet-studio-sql-action`**: Unified CRUD builder using `sql:dynamic` connection pools. Resolves incoming JSON filters into parametric queries:
  ```json
  [{"status": "ACTIVE"}, {"$set": {"status": "ARCHIVED"}}]
  ```
  The SQL Action dynamically generates: `UPDATE table SET status = :?u_status WHERE status = :?f_status`.

---

## Chapter 8: Build Setup & Target Deployment

### Gradle Dependencies (`build.gradle`)
Add the official IBM MQ Jakarta libraries and transaction pools to your build file:
```groovy
dependencies {
    implementation 'com.ibm.mq:com.ibm.mq.jakarta.client:9.4.5.1'
    implementation 'org.messaginghub:pooled-jms:3.1.2'
    implementation 'org.apache.camel.quarkus:camel-quarkus-yaml-dsl'
    implementation 'org.apache.camel.quarkus:camel-quarkus-jms'
    implementation 'org.apache.camel.quarkus:camel-quarkus-mongodb'
    implementation 'org.apache.camel.quarkus:camel-quarkus-jackson'
    implementation 'org.apache.camel.quarkus:camel-quarkus-jta'
}
```

### Packaging for Target Environments
Compile the route bundle into a native Linux microservice container using:
```bash
./mvnw package -Pnative -Dquarkus.container-image.build=true
```

### Configuration & Secret Injection
* **Environment Configuration:** Inject settings via Kubernetes ConfigMaps mapping properties like broker hosts.
* **Keystore/Truststore Protection:** Keystore files (`.jks`) are mounted dynamically into container directories (e.g. `/etc/secrets/certs/`) from secure secrets stores (Vault).

---

## Chapter 9: Hands-On Architectural Integration Scenarios

The following YAML configurations demonstrate multi-protocol, secure data routing.

### Scenario 1: REST API Gateway to SQL CRUD with Encryption
```yaml
# ID: sql-crud-gateway
# ENABLED: true
# DESCRIPTION: Receives customer payloads over REST, encrypts credit cards, executes SQL, and records audits.

- rest:
    path: "/api/v1/customers"
    post:
      - to: "direct:create-customer"

- route:
    id: "create-customer"
    from:
      uri: "direct:create-customer"
      steps:
        # Step 1: Encrypt creditCardNumber
        - setProperty:
            name: "crypto.fields"
            constant: "creditCardNumber"
        - setProperty:
            name: "crypto.algorithm"
            constant: "AES-256-GCM"
        - bean:
            beanType: "com.routebuilder.kameletstudio.crypto.FieldCryptoProcessor"
            method: "encryptFields"

        # Step 2: Insert into table via SQL Action Kamelet
        - to: "kamelet:kamelet-studio-sql-action?table=customers&operation=update"

        # Step 3: Trigger Audit Log
        - setHeader:
            name: "AuditOperation"
            constant: "CUSTOMER_CREATE_SQL"
        - bean:
            beanType: "com.routebuilder.kameletstudio.crypto.AuditInterceptorBean"
            method: "processAudit"
```

### Scenario 2: Solace XA Message Router to MongoDB & Kafka SSL
```yaml
# ID: solace-xa-mongo-kafka
# ENABLED: true
# DESCRIPTION: Reads events from Solace under XA control, updates MongoDB, and publishes transaction receipts to Kafka SSL.

- route:
    id: "solace-xa-consumer"
    from:
      uri: "kamelet:kamelet-studio-solace-xa-source?queuename=payments.in&host=smfs://solace-broker:55443&vpn=production"
      steps:
        # Step 1: Decrypt payload details
        - setProperty:
            name: "crypto.fields"
            constant: "accountBalance,taxId"
        - bean:
            beanType: "com.routebuilder.kameletstudio.crypto.FieldCryptoProcessor"
            method: "decryptFields"

        # Step 2: Write updates to database
        - to: "kamelet:kamelet-studio-mongodb-action?database=banking&collection=accounts&operation=update"

        # Step 3: Publish to secure Kafka SSL broker
        - to: "kamelet:kafka-ssl-sink?bootstrapServers=kafka-secure:9093&topic=tx-receipts&sslTruststoreLocation=/etc/certs/kafka-truststore.jks&sslTruststorePassword=${env.KAFKA_TS_PASS}"
```

### Scenario 3: HTTPS REST Proxy to IBM MQ (mTLS / Kerberos Fallback)
```yaml
# ID: rest-ibmmq-mtls-proxy
# ENABLED: true
# DESCRIPTION: Synchronizes HTTP REST gateway requests to IBM MQ using mTLS with correlation matching.

- rest:
    path: "/api/v1/integrations"
    post:
      - to: "direct:send-to-mq"

- route:
    id: "send-to-mq"
    from:
      uri: "direct:send-to-mq"
      steps:
        - setHeader:
            name: "X-Correlation-ID"
            simple: "${uuid}"
        - to: "kamelet:kamelet-studio-ibmmq-xa-sink?queuename=requests.out&hostname=mq-srv.corp.internal&port=1414&queuemanager=QM1&channel=SSL.SVRCONN&sslciphersuite=TLS_RSA_WITH_AES_256_CBC_SHA256&truststorepath=/etc/certs/mq-truststore.jks&truststorepassword=${env.MQ_TS_PASS}&keystorepath=/etc/certs/mq-client.jks&keystorepassword=${env.MQ_KS_PASS}"
        - setHeader:
            name: "AuditOperation"
            constant: "IBMMQ_REQUEST_REPLY"
        - bean:
            beanType: "com.routebuilder.kameletstudio.crypto.AuditInterceptorBean"
            method: "processAudit"

---

## Chapter 10: Environment Configuration & Property Placeholders

Rather than hardcoding environment-specific URIs directly in your route definitions (e.g. `stub:jms:queue:REQUEST.Q` or `jms:queue:PROD.Q`), use Camel property placeholders. This keeps your routes decoupled from the infrastructure.

### Property Placeholder Syntax in Routes
```yaml
- route:
    id: "property-placeholder-example"
    from:
      uri: "{{kafka.orders.endpoint}}"
      steps:
        - to: "{{ibmmq.reply.queue}}"
```

### How Properties are Loaded and Populated

Apache Camel and JBang provide several mechanisms for defining and injecting configuration variables before the application starts:

#### 1. Automatic Local Properties (IDE Default)
When running routes inside the IDE (via the **Play** button or **Run Route** context menus), the execution working directory is set to the current folder containing the active route file. JBang automatically scans this current folder for a file named `application.properties` and loads its properties.

When you use the **Generate Samples** action, the IDE automatically populates an `application.properties` file with local sandbox stubs in the generated directories.

**Workspace Variables Editor (GUI):**
The IDE features a dedicated interface to manage these properties. You can open it by clicking the **Variables** button in the main Toolbar or selecting **Edit** > **Workspace Variables...** from the menu bar. 
* It automatically opens the properties file corresponding to the *current folder* of the active file or selected tree folder.
* Add new key-value placeholders dynamically, or double-click empty cells to edit inline directly.
* Define optional descriptions for variables, which are saved as comments (`# Description`) directly above the key-value pair in `application.properties`.
* Edit existing keys, values, or descriptions inline by double-clicking on them in the table.
* Delete selected variables.
* Clicking **Save** writes the configuration directly to `application.properties` in the current folder, which is then automatically loaded on subsequent route runs.

#### 2. Command-Line Properties Override
If you run JBang manually via CLI, you can explicitly point to property files or pass individual values:
* **Point to a property file:**
  ```bash
  jbang camel run --properties=application.properties my-route.yaml
  ```
* **Inject individual key-value properties:**
  ```bash
  jbang camel run --prop kafka.orders.endpoint=stub:kafka:topic:orders my-route.yaml
  ```

#### 3. Operating System Environment Variables
Camel supports reading directly from the operating system's environment variables. You can reference them inside your YAML routes using the `{{env:VAR_NAME}}` syntax:
```yaml
- route:
    id: "env-var-route"
    from:
      uri: "{{env:KAFKA_ORDERS_ENDPOINT:stub:kafka:topic:orders}}"
      steps:
        - to: "mock:out"
```
*(The syntax above uses `stub:kafka:topic:orders` as a fallback default value if the `KAFKA_ORDERS_ENDPOINT` environment variable is not defined).*

To populate the environment variables before running JBang from a shell:
```bash
export KAFKA_ORDERS_ENDPOINT="kafka:my-real-topic?brokers=prod-broker:9092"
jbang camel run my-route.yaml
```

### Production Execution in OpenShift
When promoting your routes to OpenShift, the exact same YAML code is deployed. The target platform resolves the placeholders using externalized config:
1. Define your real endpoints in an OpenShift **ConfigMap** or **Secret**:
   ```properties
   kafka.orders.endpoint=kafka:live-orders-topic?brokers=prod-kafka:9092
   ibmmq.reply.queue=jms:queue:REAL.PROCESSING.QUEUE?connectionFactory=#ibmMQFactory
   ```
2. Mount the ConfigMap/Secret dynamically to your pod. The Quarkus runtime reads these keys and automatically binds them to Camel's property placeholder manager.

