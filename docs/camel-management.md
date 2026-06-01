# Dynamic Route & Kamelet Loader for Camel Quarkus

This document outlines the architecture, configuration, schemas, and implementation blueprint for building an enterprise-grade **Camel Quarkus Dynamic Route Loader**. This system enables a Quarkus microservice to dynamically load, version-control, and reload EIP routes and Kamelets from SQL databases, MongoDB collections, and the local filesystem at runtime.

---

## 1. Loader Architecture & Version Resolution

#### A. General Execution Flow
```mermaid
graph TD
    A[Quarkus StartupEvent] --> B[DynamicRouteLoader]
    
    B -->|"1. If loader.transformer.enabled=true"| C[Extract Transformer files to FS]
    C -->|Write files| D["Local Filesystem (loader.transformer.directory)"]
    
    B -->|"2. Scan Route & Kamelet configurations"| E{Scan Configs}
    E -->|Filesystem| F[Filesystem Source Scan]
    E -->|Agroal JDBC| G[SQL Database Query]
    E -->|MongoClient| H[MongoDB Collections Find]
    
    F & G & H -->|Raw DSL Strings| I[Version Deduplicator Engine]
    I -->|"Filter: enabled=true & max(version)"| J[Latest Active Route Resources]
    J -->|"camelContext.getRoutesLoader().loadRoutes"| K[Camel Engine Runtime]
    
    K -->|Reference templates/scripts at runtime| D
```

### B. Versioning & De-duplication Logic
If multiple routes exist with the same `route_id` (e.g. `route-invoice-processing` version `1.0` and `1.2`), the loader automatically applies a **Deduplication Filter** that picks only the latest active version.

```mermaid
stateDiagram-v2
    [*] --> QueryActiveRoutes : "WHERE enabled = true"
    QueryActiveRoutes --> SortByVersion : "ORDER BY route_id ASC, version DESC"
    
    state Iterate_Results {
        [*] --> CheckId : "Next row"
        CheckId --> AlreadyLoaded : "route_id exists in map?"
        AlreadyLoaded --> Discard : "Yes (Older version) -> Skip"
        AlreadyLoaded --> Load : "No (First seen/Highest version) -> Register"
        Load --> CheckId
    }
    
    SortByVersion --> Iterate_Results
    Iterate_Results --> LoadToCamel : "Inject filtered list into RoutesLoader"
    LoadToCamel --> [*]
```

---

## 2. Build Configurations (Maven & Gradle Variants)

Choose the dependency block matching your build tool setup:

### Option A: Maven (`pom.xml`)
```xml
<project>
    <dependencies>
        <!-- Camel YAML DSL and Kamelet support -->
        <dependency>
            <groupId>org.apache.camel.quarkus</groupId>
            <artifactId>camel-quarkus-yaml-dsl</artifactId>
        </dependency>
        <dependency>
            <groupId>org.apache.camel.quarkus</groupId>
            <artifactId>camel-quarkus-kamelet</artifactId>
        </dependency>

        <!-- Relational DB Agroal Pool + PostgreSQL Driver -->
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-agroal</artifactId>
        </dependency>
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-jdbc-postgresql</artifactId>
        </dependency>

        <!-- MongoDB Client Support -->
        <dependency>
            <groupId>io.quarkus</groupId>
            <artifactId>quarkus-mongodb-client</artifactId>
        </dependency>
    </dependencies>
</project>
```

### Option B: Gradle (`build.gradle`)
```groovy
dependencies {
    // Camel YAML DSL and Kamelet support
    implementation 'org.apache.camel.quarkus:camel-quarkus-yaml-dsl'
    implementation 'org.apache.camel.quarkus:camel-quarkus-kamelet'

    // Relational DB Agroal Pool + PostgreSQL Driver
    implementation 'io.quarkus:quarkus-agroal'
    implementation 'io.quarkus:quarkus-jdbc-postgresql'

    // MongoDB Client Support
    implementation 'io.quarkus:quarkus-mongodb-client'
}
```

---

## 3. Application Configurations (Properties & YAML Variants)

Select the style that matches your configuration layout:

### Option A: MicroProfile Properties (`application.properties`)
```properties
# =========================================================================
# Database Connections (mTLS / SSL Configured)
# =========================================================================

# =========================================================================
# Dev Services Settings (Disabled by default to avoid launching testcontainers)
# =========================================================================
quarkus.devservices.enabled=false
quarkus.datasource.devservices=false
quarkus.mongodb.devservices.enabled=false

# To enable Dev Services explicitly in dev mode:
# %dev.quarkus.devservices.enabled=true

# =========================================================================
# Client Active/Deactivation Flags (To completely prevent localhost connection loops)
# =========================================================================
# Set to false to disable Agroal DataSource initialization:
quarkus.datasource.active=true

# Set to false to disable MongoDB Client and stop connection attempts to localhost:
quarkus.mongodb.enabled=true

# 1. SQL Agroal Connection (with mTLS Client Certificate Authentication)
quarkus.datasource.db-kind=postgresql
quarkus.datasource.username=dbuser
quarkus.datasource.password=dbpassword
quarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/routedb?ssl=true&sslmode=verify-full&sslrootcert=/certs/ca.crt&sslcert=/certs/client.crt&sslkey=/certs/client.pk8

# 2. MongoDB Client Connection (with SSL/TLS and Certificate Authentication)
quarkus.mongodb.connection-string=mongodb://dbuser:dbpassword@localhost:27017/routedb?tls=true&tlsCAFile=/certs/ca.crt&tlsCertificateKeyFile=/certs/client.pem

# =========================================================================
# Route Loader Configurations (Customizable Table, Collection, & Directories)
# =========================================================================
loader.file.enabled=true
# Default directory is src/main/resources/routes/ if empty
loader.file.directory=src/main/resources/routes/

loader.sql.enabled=true
# Default database table is camel_routes if loader.sql.table is omitted
loader.sql.table=camel_routes

loader.mongodb.enabled=true
# Default mongo collection is camel_routes_collection if loader.mongodb.collection is omitted
loader.mongodb.collection=camel_routes_collection
```

### Option B: Quarkus YAML Config (`application.yaml`)
```yaml
quarkus:
  # Disable Dev Services by default
  devservices:
    enabled: false

  # SQL Connection (with mTLS)
  datasource:
    active: true
    devservices: false
    db-kind: postgresql
    username: dbuser
    password: dbpassword
    jdbc:
      url: "jdbc:postgresql://localhost:5432/routedb?ssl=true&sslmode=verify-full&sslrootcert=/certs/ca.crt&sslcert=/certs/client.crt&sslkey=/certs/client.pk8"
  
  # MongoDB Client Connection (with SSL/TLS and Client Certificate)
  mongodb:
    enabled: true
    devservices:
      enabled: false
    connection-string: "mongodb://dbuser:dbpassword@localhost:27017/routedb?tls=true&tlsCAFile=/certs/ca.crt&tlsCertificateKeyFile=/certs/client.pem"

# Custom Route Loader Settings
loader:
  file:
    enabled: true
    directory: "src/main/resources/routes/"
  sql:
    enabled: true
    table: "camel_routes"
  mongodb:
    enabled: true
    collection: "camel_routes_collection"
```

---

## 4. Route Authoring Formats

Routes stored in the databases can be authored using standard Camel YAML DSL or Java/XML properties:

### A. Standard EIP Yaml Route
```yaml
- route:
    id: "route-invoice-processing"
    from:
      uri: "timer:tick?period=5000"
      steps:
        - setBody:
            simple: "Generating invoice data at ${date:now:yyyy-MM-dd HH:mm:ss}"
        - to: "log:invoice-logger?showAll=true"
```

### B. Custom Kamelet Definition Source
```yaml
apiVersion: camel.apache.org/v1
kind: Kamelet
metadata:
  name: dynamic-timer-source
  labels:
    camel.apache.org/kamelet.type: "source"
spec:
  definition:
    title: "Dynamic Timer Source"
    properties:
      period:
        type: integer
        default: 1000
  template:
    from:
      uri: "timer:tick"
      parameters:
        period: "{{period}}"
      steps:
        - to: "kamelet:sink"
```

---

## 5. Storage Schemas

The storage layers store metadata alongside raw scripts:

### A. SQL DDL Layout (`camel_routes` Table)
```sql
CREATE TABLE camel_routes (
    route_id VARCHAR(255) NOT NULL,
    version DECIMAL(5,2) NOT NULL,  -- supports format: 1.00, 1.20, 2.10
    route_content TEXT NOT NULL,     -- YAML, XML, or Java source string
    route_format VARCHAR(10) DEFAULT 'yaml', -- 'yaml', 'xml', or 'java'
    enabled BOOLEAN DEFAULT TRUE,    -- Flag to turn route ON or OFF
    created_by VARCHAR(100),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (route_id, version)
);
```

### B. MongoDB Document Structure (`kamelets` Collection)
```json
{
  "_id": {
    "route_id": "dynamic-timer-source",
    "version": 1.2
  },
  "route_id": "dynamic-timer-source",
  "version": 1.2,
  "route_format": "yaml",
  "filename": "dynamic-timer-source.kamelet.yaml",
  "content": "apiVersion: camel.apache.org/v1\nkind: Kamelet\nmetadata:\n  name: dynamic-timer-source...",
  "enabled": true,
  "updated_at": "2026-05-29T10:00:00Z"
}
```

---

## 6. Implementation Code (`DynamicRouteLoader.java`)

This component coordinates scanning, reads credentials safely, executes the **version resolution deduplication**, and mounts routes to the Camel engine:

```java
package com.tessera.loader;

import io.quarkus.runtime.StartupEvent;
import io.quarkus.mongodb.MongoClient;
import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import javax.sql.DataSource;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.Resource;
import org.apache.camel.support.ResourceHelper;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

@ApplicationScoped
public class DynamicRouteLoader {

    @Inject
    CamelContext camelContext;

    @Inject
    jakarta.enterprise.inject.Instance<DataSource> dataSourceInstance;

    @Inject
    jakarta.enterprise.inject.Instance<MongoClient> mongoClientInstance;

    @ConfigProperty(name = "loader.file.enabled", defaultValue = "false")
    boolean fileEnabled;

    @ConfigProperty(name = "loader.file.directory", defaultValue = "src/main/resources/routes/")
    String fileDir;

    @ConfigProperty(name = "loader.sql.enabled", defaultValue = "false")
    boolean sqlEnabled;

    @ConfigProperty(name = "loader.sql.table", defaultValue = "camel_routes")
    String sqlTable;

    @ConfigProperty(name = "loader.mongodb.enabled", defaultValue = "false")
    boolean mongoEnabled;

    @ConfigProperty(name = "loader.mongodb.collection", defaultValue = "camel_routes_collection")
    String mongoCollectionName;

    // Track active route IDs registered dynamically to allow clean hot-reloads
    private final Set<String> registeredRouteResources = new HashSet<>();

    public void onStart(@Observes StartupEvent ev) {
        reloadRoutes();
    }

    /**
     * Clears previously loaded dynamic routes and scans databases/filesystem
     * to hot-reload the latest active versions.
     */
    public synchronized void reloadRoutes() {
        System.out.println("Starting dynamic route loading sequence...");
        
        // 0. Extract transformer files to disk first so they are present when routes are loaded
        if (transformerEnabled) {
            extractTransformers();
        }
        
        // 1. Unload old routes to prevent duplicate route execution
        unloadPreviousRoutes();

        Map<String, RoutePayload> routeMap = new HashMap<>();

        // 2. Fetch from filesystem
        if (fileEnabled && !fileDir.isEmpty()) {
            loadFromFilesystem(routeMap);
        }

        // 3. Fetch from SQL Database
        if (sqlEnabled && !sqlTable.isEmpty()) {
            loadFromSql(routeMap);
        }

        // 4. Fetch from MongoDB
        if (mongoEnabled) {
            loadFromMongo(routeMap);
        }

        // 5. Build and Load Resources into Camel
        List<Resource> resourcesToLoad = new ArrayList<>();
        for (Map.Entry<String, RoutePayload> entry : routeMap.entrySet()) {
            String routeId = entry.getKey();
            RoutePayload payload = entry.getValue();
            
            System.out.println("Selecting latest route: " + routeId + " (Version: " + payload.version + ", Format: " + payload.format + ")");
            resourcesToLoad.add(ResourceHelper.fromString(routeId + "." + payload.format, payload.content));
            registeredRouteResources.add(routeId);
        }

        if (!resourcesToLoad.isEmpty()) {
            try {
                camelContext.getRoutesLoader().loadRoutes(resourcesToLoad);
                System.out.println("Successfully registered " + resourcesToLoad.size() + " latest dynamic routes.");
            } catch (Exception e) {
                System.err.println("Fatal error mounting dynamic routes into Camel: " + e.getMessage());
            }
        }
    }

    private void unloadPreviousRoutes() {
        for (String resourceName : registeredRouteResources) {
            try {
                // Stop and remove the routes mapped to this resource ID
                camelContext.getRouteController().stopRoute(resourceName);
                camelContext.removeRoute(resourceName);
            } catch (Exception e) {
                // Route may not be running or initialized yet, ignore
            }
        }
        registeredRouteResources.clear();
    }

    private void loadFromFilesystem(Map<String, RoutePayload> routeMap) {
        File folder = new File(fileDir);
        if (!folder.exists() || !folder.isDirectory()) return;

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yaml") || name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                String content = Files.readString(file.toPath());
                String baseName = file.getName().substring(0, file.getName().lastIndexOf('.'));
                String ext = file.getName().substring(file.getName().lastIndexOf('.') + 1);
                
                // Filesystem routes defaults to version 1.00 unless custom parsed
                double version = 1.00;
                resolveAndMerge(routeMap, baseName, content, ext, version);
            } catch (Exception e) {
                System.err.println("Error reading local route " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    private void loadFromSql(Map<String, RoutePayload> routeMap) {
        if (!dataSourceInstance.isResolvable()) {
            System.err.println("SQL Loader enabled but DataSource bean is not active.");
            return;
        }

        String query = "SELECT route_id, route_content, route_format, version FROM " + sqlTable + 
                       " WHERE enabled = true ORDER BY route_id ASC, version DESC";
        try (Connection conn = dataSourceInstance.get().getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String routeId = rs.getString("route_id");
                String content = rs.getString("route_content");
                String format = rs.getString("route_format");
                double version = rs.getDouble("version");

                resolveAndMerge(routeMap, routeId, content, format, version);
            }
        } catch (Exception e) {
            System.err.println("Error querying SQL Route Loader table " + sqlTable + ": " + e.getMessage());
        }
    }

    private void loadFromMongo(Map<String, RoutePayload> routeMap) {
        if (!mongoClientInstance.isResolvable()) {
            System.err.println("MongoDB Loader enabled but MongoClient bean is not active.");
            return;
        }

        try {
            MongoClient mongoClient = mongoClientInstance.get();
            String dbName = mongoClient.listDatabaseNames().first();
            MongoCollection<Document> collection = mongoClient.getDatabase(dbName).getCollection(mongoCollectionName);

            // Filter for enabled routes, sort by route_id ascending and version descending
            Document query = new Document("enabled", true);
            Document sort = new Document("route_id", 1).append("version", -1);

            try (MongoCursor<Document> cursor = collection.find(query).sort(sort).iterator()) {
                while (cursor.hasNext()) {
                    Document doc = cursor.next();
                    String routeId = doc.getString("route_id");
                    String content = doc.getString("content");
                    String format = doc.getString("route_format");
                    if (format == null) {
                        format = "yaml"; // fallback default
                    }
                    
                    Object verObj = doc.get("version");
                    double version = 1.0;
                    if (verObj instanceof Number) {
                        version = ((Number) verObj).doubleValue();
                    }

                    resolveAndMerge(routeMap, routeId, content, format, version);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading routes from MongoDB client: " + e.getMessage());
        }
    }

    private void resolveAndMerge(Map<String, RoutePayload> routeMap, String routeId, String content, String format, double version) {
        if (routeMap.containsKey(routeId)) {
            // Pick the latest version
            if (version > routeMap.get(routeId).version) {
                routeMap.put(routeId, new RoutePayload(content, format, version));
            }
        } else {
            routeMap.put(routeId, new RoutePayload(content, format, version));
        }
    }

    private static class RoutePayload {
        final String content;
        final String format;
        final double version;

        RoutePayload(String content, String format, double version) {
            this.content = content;
            this.format = format;
            this.version = version;
        }
    }
}
```

---

## 7. Operational REST Reload Endpoint

To trigger updates without rebooting the Quarkus VM container, configure an administrative REST resource to reload mappings instantly:

```java
package com.tessera.loader;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.Map;

@Path("/admin/routes")
@ApplicationScoped
public class RouteControlResource {

    @Inject
    DynamicRouteLoader loader;

    @POST
    @Path("/reload")
    @Produces(MediaType.APPLICATION_JSON)
    public Response reloadRoutes() {
        try {
            loader.reloadRoutes();
            return Response.ok(Map.of(
                "status", "success", 
                "message", "Dynamic routes and Kamelets reloaded successfully"
            )).build();
        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("status", "error", "message", e.getMessage()))
                .build();
        }
    }
}
```
Run `curl -X POST http://localhost:8080/admin/routes/reload` to hot-redeploy the routes in-place.

---

## 8. Seeding & Upgrading Routes via Liquibase

Seeding and upgrading YAML routes/Kamelets inside the relational database table `camel_routes` is simplified by Liquibase. To avoid complex inline escaping of newlines and indentations in SQL/Changelog scripts, use the **`valueClobFile`** attribute to load YAML files directly from your classpath.

### A. Directory Structure
```text
src/main/resources/
 ├── db/
 │    └── changeLog/
 │         ├── db.changelog-master.xml
 │         ├── seed-routes-changelog.xml
 │         └── seed-routes-changelog.yaml
 └── routes/
      ├── invoice-processing-v1.20.yaml
      └── dynamic-timer-source-v1.2.yaml
```

---

### B. Seeding via XML Changelog (`seed-routes-changelog.xml`)

Using `valueClobFile` allows referencing external YAML route scripts cleanly. Use `<loadUpdateData>` to ensure updates override old rows when redeployed:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
    xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-latest.xsd">

    <changeSet id="seed-initial-routes-v1" author="routebuilder-ide">
        <!-- 1. Insert/Seed Invoice Processing Route -->
        <insert tableName="camel_routes">
            <column name="route_id" value="route-invoice-processing"/>
            <column name="version" valueNumeric="1.20"/>
            <column name="route_content" valueClobFile="routes/invoice-processing-v1.20.yaml"/>
            <column name="enabled" valueBoolean="true"/>
            <column name="created_by" value="liquibase-migration"/>
        </insert>

        <!-- 2. Insert/Seed Kamelet Timer Source -->
        <insert tableName="camel_routes">
            <column name="route_id" value="dynamic-timer-source"/>
            <column name="version" valueNumeric="1.20"/>
            <column name="route_content" valueClobFile="routes/dynamic-timer-source-v1.2.yaml"/>
            <column name="enabled" valueBoolean="true"/>
            <column name="created_by" value="liquibase-migration"/>
        </insert>
    </changeSet>

    <changeSet id="upgrade-invoice-route-v2" author="routebuilder-ide">
        <!-- Seeding a new version alongside version 1.20 -->
        <insert tableName="camel_routes">
            <column name="route_id" value="route-invoice-processing"/>
            <column name="version" valueNumeric="2.00"/>
            <!-- Points to the updated YAML source file -->
            <column name="route_content" valueClobFile="routes/invoice-processing-v2.00.yaml"/>
            <column name="enabled" valueBoolean="true"/>
            <column name="created_by" value="liquibase-migration"/>
        </insert>
    </changeSet>
</databaseChangeLog>
```

---

### C. Seeding via YAML Changelog (`seed-routes-changelog.yaml`)

The equivalent YAML syntax uses the `valueClobFile` attribute for clean formatting:

```yaml
databaseChangeLog:
  - changeSet:
      id: seed-initial-routes-v1
      author: routebuilder-ide
      changes:
        # 1. Insert Invoice Processing Route
        - insert:
            tableName: camel_routes
            columns:
              - column:
                  name: route_id
                  value: route-invoice-processing
              - column:
                  name: version
                  valueNumeric: 1.20
              - column:
                  name: route_content
                  valueClobFile: routes/invoice-processing-v1.20.yaml
              - column:
                  name: enabled
                  valueBoolean: true
              - column:
                  name: created_by
                  value: liquibase-migration

        # 2. Insert Kamelet Timer Source
        - insert:
            tableName: camel_routes
            columns:
              - column:
                  name: route_id
                  value: dynamic-timer-source
              - column:
                  name: version
                  valueNumeric: 1.20
              - column:
                  name: route_content
                  valueClobFile: routes/dynamic-timer-source-v1.2.yaml
              - column:
                  name: enabled
                  valueBoolean: true
              - column:
                  name: created_by
                  value: liquibase-migration
```

---

## 9. Deep-Dive: TLS, One-Way SSL & mTLS Configurations

Securing connections to SQL databases and MongoDB requires specific parameters depending on the security protocol (One-Way SSL vs. Mutual TLS) and the target database platform.

---

### A. PostgreSQL SSL/TLS Reference

PostgreSQL uses standard query parameters in the JDBC URL to configure SSL certificates.

#### 1. One-Way SSL (Verify Server Certificate)
The client encrypts the channel and verifies that the database server's certificate was issued by a trusted Certificate Authority (CA):

*   **URL Options:**
    *   `ssl=true`: Enables SSL.
    *   `sslmode=verify-ca`: Verifies that the server certificate is signed by a trusted CA.
    *   `sslmode=verify-full`: Verifies CA signature AND matches server hostname against the certificate CN/SAN.
    *   `sslrootcert`: Path to the CA certificate file (PEM format).
*   **JDBC URL Example:**
    ```properties
    quarkus.datasource.jdbc.url=jdbc:postgresql://postgres.internal:5432/routedb?ssl=true&sslmode=verify-full&sslrootcert=/certs/server-ca.crt
    ```

#### 2. Mutual TLS (mTLS / Client Auth)
Both the server and client verify each other's certificates. The PostgreSQL JDBC driver requires client keys to be in **unencrypted PKCS#8 DER format** (usually `.pk8` files).

*   **URL Options:**
    *   `sslcert`: Path to the client's public certificate (`.crt` / `.pem`).
    *   `sslkey`: Path to the client's private key (`.pk8` in PKCS#8 format).
*   **JDBC URL Example:**
    ```properties
    quarkus.datasource.jdbc.url=jdbc:postgresql://postgres.internal:5432/routedb?ssl=true&sslmode=verify-full&sslrootcert=/certs/server-ca.crt&sslcert=/certs/client.crt&sslkey=/certs/client.pk8
    ```

*   **Command to convert PEM private key to PKCS#8 DER (if needed):**
    ```bash
    openssl pkcs8 -topk8 -inform PEM -outform DER -in client.key -out client.pk8 -nocrypt
    ```

---

### B. Oracle DB SSL/TLS Reference

Oracle uses TCPS protocol descriptors inside the JDBC connection block. Keystores and truststores must be supplied as JVM-level properties or via custom Agroal datasource properties.

#### 1. Connection URL Descriptor (TCPS)
Ensure you target port `2484` (default Oracle TCPS port) and protocols:
```properties
quarkus.datasource.jdbc.url=jdbc:oracle:thin:@(DESCRIPTION=(ADDRESS=(PROTOCOL=tcps)(HOST=oracle.internal)(PORT=2484))(CONNECT_DATA=(SERVICE_NAME=orcl)))
```

#### 2. One-Way SSL (TrustStore Configuration)
Reference the Java TrustStore (`JKS` or `PKCS12`) containing the Oracle server CA:
```properties
quarkus.datasource.jdbc.additional-jdbc-properties.javax.net.ssl.trustStore=/certs/truststore.jks
quarkus.datasource.jdbc.additional-jdbc-properties.javax.net.ssl.trustStorePassword=truststoreSecretPassword
quarkus.datasource.jdbc.additional-jdbc-properties.javax.net.ssl.trustStoreType=PKCS12
```

#### 3. Mutual TLS (mTLS - KeyStore Configuration)
Reference the Java KeyStore containing the client's private key and certificate alias:
```properties
quarkus.datasource.jdbc.additional-jdbc-properties.javax.net.ssl.keyStore=/certs/keystore.jks
quarkus.datasource.jdbc.additional-jdbc-properties.javax.net.ssl.keyStorePassword=keystoreSecretPassword
quarkus.datasource.jdbc.additional-jdbc-properties.javax.net.ssl.keyStoreType=PKCS12
```

---

### C. MongoDB SSL/TLS Reference

MongoDB client handles SSL configuration directly inside the MongoDB Connection URI.

#### 1. One-Way SSL (Server CA Verification)
Instructs the client to use TLS and trust the server's certs using the CA file:

*   **URI Options:**
    *   `tls=true`: Enables TLS.
    *   `tlsCAFile`: Path to the server CA certificate file (PEM format).
*   **Properties Example:**
    ```properties
    quarkus.mongodb.connection-string=mongodb://dbuser:dbpwd@mongo.internal:27017/routedb?tls=true&tlsCAFile=/certs/ca.crt
    ```

#### 2. Mutual TLS (mTLS / Client Auth)
Instructs the client to present a certificate to the server. MongoDB requires the client key and certificate to be combined into a single `.pem` file.

*   **URI Options:**
    *   `tlsCertificateKeyFile`: Path to the combined client certificate & private key file (PEM format).
*   **Properties Example:**
    ```properties
    quarkus.mongodb.connection-string=mongodb://dbuser:dbpwd@mongo.internal:27017/routedb?tls=true&tlsCAFile=/certs/ca.crt&tlsCertificateKeyFile=/certs/client-combined.pem
    ```

    cat client.crt client.key > client-combined.pem
    ```

---

## 10. Dependency Management: Agnostic vs. Embedded Driver Architecture

When packaging this dynamic route loader as a reusable Quarkus extension or shared library, **the best practice is to design it to be completely database-agnostic**. You should avoid embedding specific database drivers (like PostgreSQL, Oracle, or MongoDB clients) directly inside the extension.

---

### A. Why Agnostic Design is Superior

1.  **Eliminates Dependency Bloat:**
    If the extension hardcodes PostgreSQL and MongoDB client dependencies, every microservice importing the loader will inherit those drivers. If a consuming service only uses MySQL or local files, it inherits hundreds of megabytes of unused database classes.
2.  **Prevents Driver Version Conflicts:**
    Microservices often run different versions of databases (e.g. MongoDB 4.x client vs. 5.x client). Forcing a specific driver version in the extension can cause classpath overrides and build-time issues.
3.  **Encourages Quarkus Build Optimizations:**
    By decoupling the drivers, Quarkus can compile small native images containing only the specific database client utilized by the host microservice.

---

### B. Implementing Agnostic Compilation via Build Scopes

To compile the extension without packaging the drivers in the final JAR, declare the database APIs as **Optional** (Maven) or **Compile Only** (Gradle). This compiles the extension against the database interfaces but expects the parent project to provide the actual runtime driver dependencies.

#### 1. Maven (`pom.xml` in extension)
```xml
<dependencies>
    <!-- Mark as optional so consuming apps must declare them if needed -->
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-agroal</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>io.quarkus</groupId>
        <artifactId>quarkus-mongodb-client</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

#### 2. Gradle (`build.gradle` in extension)
```groovy
dependencies {
    // Compile-only dependencies are not transitively passed to consumer apps
    compileOnly 'io.quarkus:quarkus-agroal'
    compileOnly 'io.quarkus:quarkus-mongodb-client'
}
```

---

### C. Runtime Behavior Matrix

With the CDI `Instance<T>` injection strategy, the extension automatically adapts at startup based on the client libraries present on the classpath:

| Client Driver in Parent App | Extension Injection State | Loader Behavior |
| :--- | :--- | :--- |
| **No DB Drivers Added** | `Instance<DataSource>.isResolvable() == false`<br>`Instance<MongoClient>.isResolvable() == false` | Runs in filesystem-only mode. Database steps are safely skipped. |
| **PostgreSQL Added** | `Instance<DataSource>.isResolvable() == true` | Automatically hooks into the PostgreSQL connection and loads SQL routes. |
| **MongoDB Added** | `Instance<MongoClient>.isResolvable() == true` | Hooks into MongoDB connection and reads document collections. |
| **Both Added** | Both resolvable | Loads and merges routes from both SQL and MongoDB dynamically. |

---

## 11. Dynamic Transformer File Extractor

Certain integration components, such as **Smooks (XML mappings), Groovy (scripts), XSLT (templates), JSLT (JSON transformations), and Flatpack (fixed-width parser schemas)**, cannot load configurations directly from in-memory string streams. They require physical resource files to exist on the filesystem to execute properly.

To accommodate this, the loader includes a **Transformer File Extractor** that pulls active transformation scripts from the database on startup, deduplicates them by version, and writes them to the local filesystem.

---

### A. Database Storage Schema

#### 1. SQL Schema (`camel_transformers` Table)
```sql
CREATE TABLE camel_transformers (
    id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,          -- e.g. 'xslt', 'jslt', 'groovy', 'smooks', 'flatpack'
    description VARCHAR(500),
    version DECIMAL(5,2) NOT NULL,
    file_name VARCHAR(255) NOT NULL,    -- e.g. 'invoice-transformer.xslt'
    file_content TEXT NOT NULL,         -- holds script / schema text
    enabled BOOLEAN DEFAULT TRUE,
    PRIMARY KEY (id, version)
);
```

#### 2. MongoDB Collection Schema (`camel_transformers_collection`)
```json
{
  "_id": {
    "id": "invoice-jslt",
    "version": 2.1
  },
  "id": "invoice-jslt",
  "type": "jslt",
  "description": "Transforms order payloads to canonical invoice JSON",
  "version": 2.1,
  "file_name": "order-to-invoice.jslt",
  "content": "{\n  \"invoiceId\": .orderId,\n  ...\n}",
  "enabled": true
}
```

---

### B. Configuration Options (`application.properties`)

```properties
# Enable the dynamic transformer file writer
loader.transformer.enabled=true

# Destination folder where files will be written
loader.transformer.directory=src/main/resources/transformers/

# Organize files into type folders (e.g., .../transformers/xslt/, .../transformers/groovy/)
loader.transformer.categorize-by-type=true

# Database storage definitions
loader.transformer.sql.table=camel_transformers
loader.transformer.mongodb.collection=camel_transformers_collection
```

---

### C. Implementation Code Update (`DynamicRouteLoader.java`)

Declare the following properties and methods in the loader component to parse and dump transformer files:

```java
    // =========================================================================
    // Transformer Properties
    // =========================================================================
    @ConfigProperty(name = "loader.transformer.enabled", defaultValue = "false")
    boolean transformerEnabled;

    @ConfigProperty(name = "loader.transformer.directory", defaultValue = "src/main/resources/transformers/")
    String transformerDir;

    @ConfigProperty(name = "loader.transformer.categorize-by-type", defaultValue = "true")
    boolean transformerCategorize;

    @ConfigProperty(name = "loader.transformer.sql.table", defaultValue = "camel_transformers")
    String transformerSqlTable;

    @ConfigProperty(name = "loader.transformer.mongodb.collection", defaultValue = "camel_transformers_collection")
    String transformerMongoCollection;

    // Invoke this during reloadRoutes():
    // if (transformerEnabled) {
    //     extractTransformers();
    // }

    public void extractTransformers() {
        System.out.println("Initiating transformer file extraction...");
        Map<String, TransformerPayload> transMap = new HashMap<>();

        // 1. Fetch from SQL Table
        if (sqlEnabled && dataSourceInstance.isResolvable()) {
            loadTransformersFromSql(transMap);
        }

        // 2. Fetch from MongoDB Collection
        if (mongoEnabled && mongoClientInstance.isResolvable()) {
            loadTransformersFromMongo(transMap);
        }

        // 3. Write deduplicated files to the filesystem
        File baseDir = new File(transformerDir);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }

        for (TransformerPayload payload : transMap.values()) {
            try {
                File targetFile;
                if (transformerCategorize && payload.type != null && !payload.type.trim().isEmpty()) {
                    File typeDir = new File(baseDir, payload.type.trim().toLowerCase());
                    if (!typeDir.exists()) {
                        typeDir.mkdirs();
                    }
                    targetFile = new File(typeDir, payload.fileName);
                } else {
                    targetFile = new File(baseDir, payload.fileName);
                }

                Files.writeString(targetFile.toPath(), payload.content);
                System.out.println("Written file: " + targetFile.getAbsolutePath() + " (Version: " + payload.version + ")");
            } catch (Exception e) {
                System.err.println("Error writing file " + payload.fileName + " to filesystem: " + e.getMessage());
            }
        }
    }

    private void loadTransformersFromSql(Map<String, TransformerPayload> transMap) {
        String query = "SELECT type, version, file_name, file_content FROM " + transformerSqlTable +
                       " WHERE enabled = true ORDER BY file_name ASC, version DESC";
        try (Connection conn = dataSourceInstance.get().getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String type = rs.getString("type");
                double version = rs.getDouble("version");
                String fileName = rs.getString("file_name");
                String content = rs.getString("file_content");

                resolveAndMergeTransformer(transMap, fileName, content, type, version);
            }
        } catch (Exception e) {
            System.err.println("SQL Transformer loading failed: " + e.getMessage());
        }
    }

    private void loadTransformersFromMongo(Map<String, TransformerPayload> transMap) {
        try {
            String dbName = mongoClientInstance.get().listDatabaseNames().first();
            MongoCollection<Document> collection = mongoClientInstance.get().getDatabase(dbName).getCollection(transformerMongoCollection);

            Document query = new Document("enabled", true);
            Document sort = new Document("file_name", 1).append("version", -1);

            try (MongoCursor<Document> cursor = collection.find(query).sort(sort).iterator()) {
                while (cursor.hasNext()) {
                    Document doc = cursor.next();
                    String type = doc.getString("type");
                    String fileName = doc.getString("file_name");
                    String content = doc.getString("content");

                    Object verObj = doc.get("version");
                    double version = 1.0;
                    if (verObj instanceof Number) {
                        version = ((Number) verObj).doubleValue();
                    }

                    resolveAndMergeTransformer(transMap, fileName, content, type, version);
                }
            }
        } catch (Exception e) {
            System.err.println("MongoDB Transformer loading failed: " + e.getMessage());
        }
    }

    private void resolveAndMergeTransformer(Map<String, TransformerPayload> transMap, 
                                            String fileName, String content, String type, double version) {
        if (transMap.containsKey(fileName)) {
            // Pick the latest version
            if (version > transMap.get(fileName).version) {
                transMap.put(fileName, new TransformerPayload(content, type, version, fileName));
            }
        } else {
            transMap.put(fileName, new TransformerPayload(content, type, version, fileName));
        }
    }

    private static class TransformerPayload {
        final String content;
        final String type;
        final double version;
        final String fileName;

        TransformerPayload(String content, String type, double version, String fileName) {
            this.content = content;
            this.type = type;
            this.version = version;
            this.fileName = fileName;
        }
    }
```

---

## 12. Seeding Strategies for Dynamic Resources

Loading routes, Kamelets, and transformer scripts into your database via Liquibase migrations ensures reproducible, version-controlled environments. Below is the step-by-step pipeline from preparation to runtime execution.

---

### Step 1: Preparation of Resource Files

Create dedicated resource folders in your classpath (typically inside your database migrations project structure) to house script files:

```text
my-project/
└── src/
    └── main/
        └── resources/
            └── db/
                └── changelog/
                    ├── db.changelog-master.xml
                    └── seeds/
                        ├── routes/
                        │   ├── order-route-v1.0.yaml
                        │   └── custom-bean-v1.0.xml
                        └── transformers/
                            ├── order-to-invoice.xslt
                            └── transform-payload.groovy
```

---

### Step 2: Parameter & Variable Mappings

When defining changelog datasets, align your columns with the database fields parsed by the route loader:

#### 1. Route Table Variables (`camel_routes`)
*   `route_id` (VARCHAR): Unique identifier for the route flow (e.g. `order-processing-route`).
*   `version` (NUMERIC): Version decimal (e.g., `1.00`, `1.20`).
*   `route_content` (CLOB/TEXT): The raw YAML, XML, or Java source template text.
*   `route_format` (VARCHAR): The syntax compiler type. Options: `yaml`, `xml`, `java`.
*   `enabled` (BOOLEAN): Flag (`true`/`false`) used to activate/deactivate execution.

#### 2. Transformer Table Variables (`camel_transformers`)
*   `id` (VARCHAR): Unique identifier for the transformation step (e.g. `order-to-invoice-xslt`).
*   `type` (VARCHAR): Used as the subfolder directory name when categorized. Options: `xslt`, `jslt`, `groovy`, `smooks`, `flatpack`.
*   `version` (NUMERIC): Version decimal (e.g., `1.00`, `1.20`).
*   `file_name` (VARCHAR): The physical filename created on disk (e.g. `order-transform.xslt`).
*   `file_content` (CLOB/TEXT): Raw script or template text.
*   `enabled` (BOOLEAN): Flag (`true`/`false`) used to activate/deactivate execution.

---

### Step 3: Loading Strategies in Liquibase

Developers have two strategies for importing data:

#### Option A: Loading inline string content (CLOB/TEXT)
Best for short scripts. Declares definitions directly within the changelog.

*   **XML CDATA Syntax:**
    ```xml
    <insert tableName="camel_routes">
        <column name="route_id" value="inline-log-route"/>
        <column name="version" valueNumeric="1.00"/>
        <column name="route_format" value="yaml"/>
        <column name="route_content"><![CDATA[
    - route:
        id: "inline-log-route"
        from:
          uri: "timer:tick?period=5000"
          steps:
            - log: "Hello from dynamic inline DB route"
        ]]></column>
        <column name="enabled" valueBoolean="true"/>
    </insert>
    ```

*   **YAML Multiline Syntax:**
    ```yaml
    - insert:
        tableName: camel_routes
        columns:
          - column:
              name: route_id
              value: inline-log-route
          - column:
              name: version
              valueNumeric: 1.00
          - column:
              name: route_format
              value: yaml
          - column:
              name: route_content
              value: |
                - route:
                    id: "inline-log-route"
                    from:
                      uri: "timer:tick?period=5000"
                      steps:
                        - log: "Hello from dynamic inline DB route"
          - column:
              name: enabled
              valueBoolean: true
    ```

---

#### Option B: Loading External Files (`valueClobFile` / `valueBlobFile`)
Recommended for complex files (XSLT templates, Smooks schemas, Groovy scripts) to avoid escaping strings.

*   **XML Migration Syntax:**
    ```xml
    <changeSet id="seed-transformers-v1" author="dev-team">
        <insert tableName="camel_transformers">
            <column name="id" value="order-xslt-transformer"/>
            <column name="type" value="xslt"/>
            <column name="version" valueNumeric="1.20"/>
            <column name="file_name" value="order-transform.xslt"/>
            <!-- Reads file content from classpath and binds it to the CLOB column -->
            <column name="file_content" valueClobFile="db/changelog/seeds/transformers/order-to-invoice.xslt" encoding="UTF-8"/>
            <column name="enabled" valueBoolean="true"/>
        </insert>
    </changeSet>
    ```

*   **YAML Migration Syntax:**
    ```yaml
    - changeSet:
        id: seed-transformers-v1
        author: dev-team
        changes:
          - insert:
              tableName: camel_transformers
              columns:
                - column:
                    name: id
                    value: order-xslt-transformer
                - column:
                    name: type
                    value: xslt
                - column:
                    name: version
                    valueNumeric: 1.20
                - column:
                    name: file_name
                    value: order-transform.xslt
                - column:
                    name: file_content
                    valueClobFile: db/changelog/seeds/transformers/order-to-invoice.xslt
                    encoding: UTF-8
                - column:
                    name: enabled
                    valueBoolean: true
    ```

---

### Step 4: Verification & Runtime Results

When the application boots:
1.  **Liquibase Execution**: Liquibase runs, reading the XML/YAML seed configurations and inserting files directly into `camel_routes` and `camel_transformers` database tables.
2.  **Startup Lifecycle Hook**: Quarkus triggers the `DynamicRouteLoader` component on startup.
3.  **Transformer File Extraction**: The extractor queries the `camel_transformers` table for active elements (`enabled=true`), deduplicates versions, and writes the contents of `file_content` directly to the local disk at `loader.transformer.directory` (categorized under the script `type` directory, e.g. `transformers/xslt/order-transform.xslt`).
4.  **Route Registration**: The loader scans `camel_routes`, loads the XML/YAML/Java text as Camel `Resource` streams, and mounts them.
5.  **Camel Boot**: Routes load cleanly. When route steps reference files (e.g. `to("xslt-saxon:file:src/main/resources/transformers/xslt/order-transform.xslt")`), the file is present on the filesystem, executing the transformation flow successfully.



---

### C. Design Pattern Distinction: Stream vs. Disk

```mermaid
graph TD
    A[Database Row] --> B{Asset Type?}
    
    B -->|Routes / Kamelets / Rest / Beans| C[Stream Loader]
    C -->|Read database CLOB directly as in-memory stream| D[ResourceHelper.fromString]
    D -->|Register route| E[CamelContext Registry]
    
    B -->|Transformers: XSLT, Smooks, JSLT, Groovy| F[File Extractor]
    F -->|Write CLOB content to local file| G[Target Directory /opt/camel/...]
    G -->|Referenced via file path URI| H[Camel Route Step: xslt-saxon:file:/opt/camel/...]
```

---

## 13. Bidirectional Management REST APIs: Dynamic DB Inserts & FS Synchronization

To allow administrators and remote CD/CI pipelines to dynamically register new integration flows at runtime, the extension provides REST API endpoints. 

When a payload is uploaded:
1.  It is persisted into the configured SQL database or MongoDB collection.
2.  If it is a Route, the engine triggers `loader.reloadRoutes()` to update the active Camel registry in-memory.
3.  If it is a Transformer, the engine triggers `loader.extractTransformers()`, which writes/overwrites the script to the local disk immediately.

```mermaid
graph LR
    Client[CD/CI or Web Client] -->|POST /admin/transformers/upload| API[Management REST API]
    API -->|Write/Upsert| DB[(Database SQL/Mongo)]
    API -->|Trigger sync| Loader[DynamicRouteLoader]
    Loader -->|extractTransformers| Disk[Local File System]
    Disk -->|Read file| Step[Camel Transform Step]
```

---

### A. Endpoint Implementation (`RouteControlResource.java`)

This JAX-RS resource is exposed by the extension. It uses the same injected `DataSource` and `MongoClient` instances to write changes:

```java
package com.tessera.loader;

import io.quarkus.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;

@Path("/admin")
@ApplicationScoped
public class RouteControlResource {

    @Inject
    DynamicRouteLoader loader;

    @Inject
    jakarta.enterprise.inject.Instance<DataSource> dataSourceInstance;

    @Inject
    jakarta.enterprise.inject.Instance<MongoClient> mongoClientInstance;

    // =========================================================================
    // 1. Upload Route Endpoint
    // =========================================================================
    @POST
    @Path("/routes/upload")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadRoute(RouteUploadRequest req) {
        try {
            if (loader.sqlEnabled && dataSourceInstance.isResolvable()) {
                upsertRouteSql(req);
            } else if (loader.mongoEnabled && mongoClientInstance.isResolvable()) {
                upsertRouteMongo(req);
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("status", "error", "message", "No active database database configured for writes"))
                    .build();
            }

            // Sync with Camel Engine in-memory immediately
            loader.reloadRoutes();

            return Response.ok(Map.of(
                "status", "success",
                "message", "Route " + req.routeId + " version " + req.version + " uploaded and hot-reloaded"
            )).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("status", "error", "message", e.getMessage()))
                .build();
        }
    }

    // =========================================================================
    // 2. Upload Transformer Endpoint (Groovy, XSLT, Smooks, JSLT, etc.)
    // =========================================================================
    @POST
    @Path("/transformers/upload")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadTransformer(TransformerUploadRequest req) {
        try {
            if (loader.sqlEnabled && dataSourceInstance.isResolvable()) {
                upsertTransformerSql(req);
            } else if (loader.mongoEnabled && mongoClientInstance.isResolvable()) {
                upsertTransformerMongo(req);
            } else {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("status", "error", "message", "No active database database configured for writes"))
                    .build();
            }

            // Write updated files to disk immediately
            if (loader.transformerEnabled) {
                loader.extractTransformers();
            }

            return Response.ok(Map.of(
                "status", "success",
                "message", "Transformer file " + req.fileName + " version " + req.version + " written to database and disk"
            )).build();

        } catch (Exception e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("status", "error", "message", e.getMessage()))
                .build();
        }
    }

    // =========================================================================
    // Database Writer Helpers
    // =========================================================================
    
    private void upsertRouteSql(RouteUploadRequest req) throws Exception {
        try (Connection conn = dataSourceInstance.get().getConnection()) {
            // Check if record exists
            String checkQuery = "SELECT count(*) FROM " + loader.sqlTable + " WHERE route_id = ? AND version = ?";
            boolean exists = false;
            try (PreparedStatement checkPs = conn.prepareStatement(checkQuery)) {
                checkPs.setString(1, req.routeId);
                checkPs.setDouble(2, req.version);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        exists = true;
                    }
                }
            }

            if (exists) {
                String update = "UPDATE " + loader.sqlTable + 
                                " SET route_content = ?, route_format = ?, enabled = ? WHERE route_id = ? AND version = ?";
                try (PreparedStatement ps = conn.prepareStatement(update)) {
                    ps.setString(1, req.content);
                    ps.setString(2, req.format);
                    ps.setBoolean(3, req.enabled);
                    ps.setString(4, req.routeId);
                    ps.setDouble(5, req.version);
                    ps.executeUpdate();
                }
            } else {
                String insert = "INSERT INTO " + loader.sqlTable + 
                                " (route_id, version, route_content, route_format, enabled, created_by) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insert)) {
                    ps.setString(1, req.routeId);
                    ps.setDouble(2, req.version);
                    ps.setString(3, req.content);
                    ps.setString(4, req.format);
                    ps.setBoolean(5, req.enabled);
                    ps.setString(6, req.createdBy != null ? req.createdBy : "rest-api");
                    ps.executeUpdate();
                }
            }
        }
    }

    private void upsertRouteMongo(RouteUploadRequest req) {
        String dbName = mongoClientInstance.get().listDatabaseNames().first();
        MongoCollection<Document> collection = mongoClientInstance.get().getDatabase(dbName).getCollection(loader.mongoCollectionName);

        Document docId = new Document("route_id", req.routeId).append("version", req.version);
        Document doc = new Document("_id", docId)
            .append("route_id", req.routeId)
            .append("version", req.version)
            .append("content", req.content)
            .append("route_format", req.format)
            .append("enabled", req.enabled);

        collection.replaceOne(new Document("_id", docId), doc, new ReplaceOptions().upsert(true));
    }

    private void upsertTransformerSql(TransformerUploadRequest req) throws Exception {
        try (Connection conn = dataSourceInstance.get().getConnection()) {
            String checkQuery = "SELECT count(*) FROM " + loader.transformerSqlTable + " WHERE id = ? AND version = ?";
            boolean exists = false;
            try (PreparedStatement checkPs = conn.prepareStatement(checkQuery)) {
                checkPs.setString(1, req.id);
                checkPs.setDouble(2, req.version);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        exists = true;
                    }
                }
            }

            if (exists) {
                String update = "UPDATE " + loader.transformerSqlTable + 
                                " SET type = ?, file_name = ?, file_content = ?, enabled = ? WHERE id = ? AND version = ?";
                try (PreparedStatement ps = conn.prepareStatement(update)) {
                    ps.setString(1, req.type);
                    ps.setString(2, req.fileName);
                    ps.setString(3, req.content);
                    ps.setBoolean(4, req.enabled);
                    ps.setString(5, req.id);
                    ps.setDouble(6, req.version);
                    ps.executeUpdate();
                }
            } else {
                String insert = "INSERT INTO " + loader.transformerSqlTable + 
                                " (id, type, version, file_name, file_content, enabled) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insert)) {
                    ps.setString(1, req.id);
                    ps.setString(2, req.type);
                    ps.setDouble(3, req.version);
                    ps.setString(4, req.fileName);
                    ps.setString(5, req.content);
                    ps.setBoolean(6, req.enabled);
                    ps.executeUpdate();
                }
            }
        }
    }

    private void upsertTransformerMongo(TransformerUploadRequest req) {
        String dbName = mongoClientInstance.get().listDatabaseNames().first();
        MongoCollection<Document> collection = mongoClientInstance.get().getDatabase(dbName).getCollection(loader.transformerMongoCollection);

        Document docId = new Document("id", req.id).append("version", req.version);
        Document doc = new Document("_id", docId)
            .append("id", req.id)
            .append("type", req.type)
            .append("version", req.version)
            .append("file_name", req.fileName)
            .append("content", req.content)
            .append("enabled", req.enabled);

        collection.replaceOne(new Document("_id", docId), doc, new ReplaceOptions().upsert(true));
    }

    // =========================================================================
    // DTO Classes
    // =========================================================================
    public static class RouteUploadRequest {
        public String routeId;
        public double version;
        public String content;
        public String format; // yaml, xml, java
        public boolean enabled;
        public String createdBy;
    }

    public static class TransformerUploadRequest {
        public String id;
        public String type; // xslt, groovy, smooks, jslt, flatpack
        public double version;
        public String fileName;
        public String content;
        public boolean enabled;
    }
}
```

---

### B. Upload Examples via cURL

#### 1. Uploading a new dynamic Route
```bash
curl -X POST http://localhost:8080/admin/routes/upload \
  -H "Content-Type: application/json" \
  -d '{
    "routeId": "dynamic-logger-route",
    "version": 1.5,
    "content": "- route:\n    id: \"dynamic-logger-route\"\n    from:\n      uri: \"timer:tick?period=3000\"\n      steps:\n        - log: \"Updated dynamic logger run...\"",
    "format": "yaml",
    "enabled": true
  }'
```

#### 2. Uploading a new XSLT mapping file
```bash
curl -X POST http://localhost:8080/admin/transformers/upload \
  -H "Content-Type: application/json" \
  -d '{
    "id": "order-xslt",
    "type": "xslt",
    "version": 1.0,
    "fileName": "order-to-invoice.xslt",
    "content": "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n  <xsl:template match=\"/\">\n    <invoice><xsl:value-of select=\"/order/id\"/></invoice>\n  </xsl:template>\n</xsl:stylesheet>",
    "enabled": true
  }'
```
This writes `order-to-invoice.xslt` to the database first, and then dumps it to `/src/main/resources/transformers/xslt/order-to-invoice.xslt` in real time, making it instantly queryable by the dynamic routes!

#### 3. Temporary Route Hotloading (In-Memory Only)
```bash
curl -X POST http://localhost:8080/admin/routes/hotload-temp \
  -H "Content-Type: application/json" \
  -d '{
    "routeId": "sandbox-temp-route",
    "version": 1.0,
    "content": "- route:\n    id: \"sandbox-temp-route\"\n    from:\n      uri: \"timer:tick?period=5000\"\n      steps:\n        - log: \"Temporary in-memory sandbox route tick...\"",
    "format": "yaml"
  }'
```

#### 4. Temporary Transformer Hotloading (Disk-Only Sandbox)
```bash
curl -X POST http://localhost:8080/admin/transformers/hotload-temp \
  -H "Content-Type: application/json" \
  -d '{
    "id": "sandbox-xslt",
    "type": "xslt",
    "version": 1.0,
    "fileName": "sandbox-transform.xslt",
    "content": "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n  <xsl:template match=\"/\">\n    <sandbox><xsl:value-of select=\"/event/message\"/></sandbox>\n  </xsl:template>\n</xsl:stylesheet>"
  }'
```

### C. Detailed Execution Mechanics & Synchronization Lifecycle

#### 1. Inbound Upload Phase (Database / Temporary Memory Insertion)
When a client (such as the Route Builder's **Remote Deploy Studio** or a CI/CD pipeline) issues a `POST` request to the management endpoint:
- **Persistent Strategy**: The payload is committed to the database (`camel_routes` or `camel_transformers`) with an auto-upsert. Once committed, the extension triggers full filesystem extraction and Camel context updates.
- **Temporary Strategy**: The payload bypasses database storage entirely. 
  * For Routes: Loaded directly in-memory to the running `CamelContext` via `RoutesLoader` using dynamic streams.
  * For Transformers: Dumped directly to the local sandbox folder (`loader.transformer.directory/<type>/`) so it is immediately resolvable by the file-prefix URI.

#### 2. Synchronization Phase (Disk Extraction)
Since certain Apache Camel transformation engines (e.g. `xslt-saxon`, `smooks`, `jslt`, `flatpack`) require physical templates or configuration scripts to exist on the filesystem path, database persistence alone is insufficient. 
- **Trigger**: Once the database transaction is committed successfully, the controller invokes `loader.extractTransformers()`.
- **Deduplication & Extraction**: The loader queries the database for active resources (`enabled = true`). It groups them by their ID, isolates the record containing the highest version number, and writes its content directly to the target local folder (determined by `loader.transformer.directory`).
- **Path Mapping**: The files are organized automatically in subfolders according to their resource type (e.g., `<loader.transformer.directory>/xslt/` or `<loader.transformer.directory>/smooks/`), matching the exact directory layouts referenced in the Camel endpoints.

#### 3. Route Execution Phase (Outbound Reference)
When a Camel route is registered and triggers a step referencing a file (for example, `to("xslt-saxon:file:/opt/camel/xslt/order-to-invoice.xslt?contentCache=false")`):
- **On-Demand Loading**: Camel's file resource resolver locates the extracted file on disk.
- **Cache-Busting Integration**: By appending `contentCache=false` (or configuring the component globally via `camel.component.xslt-saxon.content-cache=false`), Camel is forced to reload compile templates directly from the filesystem on-demand, bypassing stale in-memory cached files and achieving immediate hot-swapping behavior when new versions are written by the management APIs.

---

## 14. Heartbeat API, Client Connectivity Status, & Environment Config Toggles

To guarantee structural visibility and alert users of communication dropouts when performing remote testing, the extension implements a proactive **Heartbeat & Health Monitoring Protocol**.

```mermaid
sequenceDiagram
    autonumber
    participant Client as Route Builder Client (IDE)
    participant Server as Remote Camel Quarkus Container
    
    Note over Client,Server: Heartbeat Loop (Interval: 5s)
    Client->>Server: GET /admin/heartbeat
    alt API Enabled & Server Active
        Server-->>Client: HTTP 200 { status: "UP", capabilities: [...] }
        Note over Client: Set Icon state to GREEN
    else API Disabled (404/403) or Host Unreachable
        Server-->>Client: Timeout / Connection Refused
        Note over Client: Trigger Connection Drop Alert
        Note over Client: Set Icon state to RED
    end
```

### A. Heartbeat Endpoint Specification (`GET /admin/heartbeat`)

This endpoint returns server metadata, current loading capability flags, uptime details, and the environment profile:

```json
{
  "status": "UP",
  "engine": "Camel Quarkus Dynamic Route Loader",
  "version": "1.0.0-SNAPSHOT",
  "uptimeSeconds": 18240,
  "timestamp": 1785293810000,
  "environment": "staging",
  "capabilities": [
    "dynamic-routes-sql",
    "dynamic-routes-mongo",
    "dynamic-routes-file",
    "dynamic-transformers",
    "temporary-hotload-routes",
    "temporary-hotload-transformers"
  ]
}
```

### B. Environment-Based Config Switch (On / Off Toggles)

The administration REST endpoint and heartbeat listeners are fully configurable and can be deactivated in highly restricted environments (such as Production) to prevent unauthorized remote hot-loading:

```properties
# Enable/Disable all /admin management endpoints (including uploads and heartbeats)
loader.management.api.enabled=true

# Set the active environment metadata signature
loader.management.api.environment=development

# Frequency expected for client polling (ms)
loader.management.api.heartbeat-interval-ms=5000
```

If `loader.management.api.enabled=false`, the server automatically intercepts requests to `/admin/*` and returns `403 Forbidden` or `404 Not Found`, terminating any background threads or endpoints.

### C. Client UI Visual States & Warning Alerts (Route Builder Studio)

The client application (Route Builder IDE) polls `/admin/heartbeat` using a background scheduled loop based on the `API_URL` variable:

1.  **Icon Status Coloring (Green vs. Red)**:
    *   **Connected (Green)**: When the server responds with a valid HTTP `200` JSON payload, the remote icon in the status bar/toolbar remains green.
    *   **Disconnected (Red)**: If the ping fails (network timeout, TCP refusal, or non-200 response), the icon changes to a high-contrast red warning style.
2.  **Connection Drop Alert**:
    *   If the state transitions from **Connected** to **Disconnected**, the client triggers a non-blocking warning notification toast or an alert pop-up window:
        > ⚠️ **Warning: Remote Container Connection Lost!**
        > The remote container at `http://localhost:8080` is no longer reachable. Hot-reloading and remote sandbox tests have been paused.

---

## 15. Static File Copying for Data Transformations & Liquibase Exports

Certain assets, such as XSLT/JSLT/Smooks mapping files, XML schemas, or Liquibase database seed files, are static resources rather than running Camel routes. Instead of deploying and running them, they must be copied directly to specific absolute paths on the remote server so they can be referenced inside your routes.

### A. General File Copy Endpoint (`POST /admin/files/upload`)
Exposes a generic endpoint to upload any text-based configuration file directly to a custom directory on the server filesystem.

*   **URL:** `http://localhost:8080/admin/files/upload`
*   **Method:** `POST`
*   **Request Payload (JSON):**
    ```json
    {
      "fileName": "order-to-invoice.xslt",
      "content": "<?xml version=\"1.0\" encoding=\"UTF-8\"?>...",
      "targetDir": "/opt/camel/transformations"
    }
    ```
*   **Response Payload (JSON):**
    ```json
    {
      "status": "success",
      "message": "File written successfully",
      "copiedPath": "/opt/camel/transformations/order-to-invoice.xslt"
    }
    ```
    The server creates all parent directories if they do not exist, writes the file content, and returns the absolute destination path in the `copiedPath` field.

### B. Client Operations (Checkbox & Bulk Action)
In both the **Data Transformation Studio** (via "Copy to Remote") and **Liquibase Export** (via "Copy to Remote..."), the Remote Deploy dialog provides advanced file transfer utilities:
1.  **Row Checkboxes & Bulk Uploads:**
    A checkbox column is displayed on the left side of the table. You can check/uncheck individual files, select/deselect all files using the master header checkbox, and perform bulk uploads using the **Copy Selected** button at the bottom.
2.  **Absolute Server Path Specification & Nested Structure:**
    *   A text field labeled **Server Target Path** allows typing any remote absolute directory (e.g., `/tmp/camel/resources` or `/opt/camel/resources`).
    *   **Transformation Folder Nesting:** If the files being copied are data transformations, the client automatically nests the files into a clean layout:
        $$\text{Target Path} \rightarrow \text{server\_target\_path} / \text{transformer\_type} / \text{id} /$$
        The file is saved as: `<server_target_path>/<transformer_type>/<id>/<filename>`.
        For example, copying transformation `example1` (type `xml`, file `definition.xml`) to `/opt/camel/resources` writes to `/opt/camel/resources/xml/example1/definition.xml`.
3.  **Directory Creation & Error Handling:**
    *   The remote server validates directory creation. If the directories cannot be created (due to permissions in directories like `/opt`), it throws a descriptive exception:
        `Permission denied or invalid path: failed to create target directory: /opt/camel/resources`
4.  **Result Logs:**
    *   The upload activity log captures the exact response returned by the server, outputting the absolute file destination path (e.g. `[SUCCESS] File example1 copied to: /opt/camel/resources/xml/example1/definition.xml`). You can copy this path directly into your route definitions to reference the uploaded file.

