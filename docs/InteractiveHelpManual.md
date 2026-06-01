# Route Builder IDE - Interactive Help Portal

Welcome to the Route Builder IDE Help Portal. This document is organized as a structured reference guide mapping all key modules of the system.

---

## 🗂️ Interactive Index & Search Map

Use the following index map to navigate through the details of each studio interface:

```
[HELP PORTAL MAIN INDEX]
 ├── 1. Validation Studio
 ├── 2. Crypto Studio
 ├── 3. Map Studio (MAP)
 ├── 4. Transform (Transformation Studio)
 ├── 5. Diagram Studio (Diagrams Studio)
 ├── 6. Faker Studio
 └── 7. Export Studio (Liquibase, SQL, File System)
```

---

## 1. Validation Studio

Validation Studio provides in-memory validation for several industry standards. It uses a three-editor Monaco layout (Data Payload, Schema Definition, Validation Report) with sidebar history mapping.

### Layout Flow
```
+-------------------------------------------------------------+
|                      [Validation Studio]                    |
|  +----------------+  +---------------+  +----------------+  |
|  | Source Payload |  | Schema/Rules  |  | Reports/Errors |  |
|  |                |  |               |  |                |  |
|  | Monaco Editor  |  | Monaco Editor |  | Monaco Editor  |  |
|  | (Edit Raw Msg) |  | (Edit Rules)  |  | (Markdown View)|  |
|  +----------------+  +---------------+  +----------------+  |
+-------------------------------------------------------------+
```

### Supported Validation Specs
- **XML + XSD**: Standard W3C schema validation with line/column reporting.
- **JSON + Schema**: Validates properties against draft-07 JSON Schema (type, minLength/maxLength, required, enum, pattern regex).
- **YAML + Schema**: Converts YAML to JSON in-memory and validates against a JSON Schema.
- **SWIFT MT Message**:
  - **Standard**: Validates MT syntax (Block 1 BIC, block terminators, tag lengths).
  - **Enhanced Mode**: Processes custom JSON-defined rules like maximum amounts, restricted currencies (e.g. RUB), or high-risk destinations (e.g. Tehran).
- **ISO 20022 MX**: Schema-level XSD validation for pacs.008 and other structured MX messages.
- **CSV + CSVW**: Table-level constraints (required fields, datatypes, min/max limits, and primary key uniqueness) validated via CSVW JSON metadata.
- **Flat File**: Parses and checks fixed-width lines using character offset layouts.

### Dynamic SWIFT Rules JSON Configuration
In Enhanced Mode, custom rules are defined in the schema panel:
```json
{
  "enhanced": true,
  "refPrefix": "EXB-",
  "amountLimit": 5000000.0,
  "restrictedCurrencies": ["RUB", "IRR"],
  "highRiskJurisdictions": ["Iran", "Tehran"]
}
```

---

## 2. Crypto Studio

Crypto Studio secures configuration variables and databases using authenticated encryption. It includes a utility to decrypt configurations in the IDE.

### Cryptographic Protocol Specifications
- **Cipher Algorithm**: `AES-256-GCM` (authenticated encryption).
- **Key Derivation Function**: `PBKDF2WithHmacSHA256` with `65,536` iterations.
- **Salt**: `16 bytes` (prefixed to ciphertext).
- **Initialization Vector (IV)**: `12 bytes` (prefixed after salt).
- **Authentication Tag**: `128 bits` (appended to ciphertext).

### In-Flight Crypto Processing Flow
```
[Plaintext Payload] --> [FieldCryptoProcessor] --> [AES-256-GCM] --> [Base64 Ciphertext]
```

### Camel Integration DSL Example
```yaml
- setProperty:
    name: "crypto.fields"
    constant: "ssn,creditCard"
- setProperty:
    name: "crypto.algorithm"
    constant: "AES-256-GCM"
- bean:
    beanType: "com.tessera.kameletstudio.crypto.FieldCryptoProcessor"
    method: "encryptFields"
```

---

## 3. Map Studio (MAP)

Map Studio defines visual mappings between message payloads and structural schemas. It manages the global `validation-mapping.json` registry.

### Features
- **Visual Mapping Canvas**: Drag fields from the Source tree and drop them onto the Target tree to automatically define mapping relations.
- **Live Relationship Map**: Uses Mermaid.js to display active mappings in a flowchart.
- **Configuration Registry**: Updates are saved in `validation-mapping.json` to preserve links.

### `validation-mapping.json` Structure
```json
{
  "mappings": [
    {
      "name": "Invoice Mapping",
      "messagePath": "messages/xml/invoice-valid.xml",
      "schemaPath": "schemas/xsd/invoice-schema.xsd",
      "type": "XML + XSD"
    }
  ]
}
```

---

## 4. Transform (Transformation Studio)

Transformation Studio converts data between different messaging schemas using visual and script-based engines.

### Supported Mapping Engines
- **Smooks Engine**: Handles high-performance mapping of CSV, XML, JSON, and fixed-width formats. Supports binding parsed streams directly to Java Beans.
- **FreeMarker (FTL)**: Generates text outputs (XML, JSON, HTML) from template parameters.
- **JSLT**: High-performance JSON-to-JSON mapping.
- **Groovy Scripting**: For procedural data mapping:
  ```groovy
  // transform.groovy
  def xml = new XmlParser().parseText(request.body)
  request.body = [OrderId: xml.OrderId.text(), Total: xml.Total.text()]
  ```
- **jOOR Java Mapper**: Compiles custom Java mapping files at runtime.

---

## 5. Diagram Studio (Diagrams Studio)

Diagram Studio automatically renders a visual representation of Apache Camel routes from XML or YAML files.

### Features
- **Mermaid.js Flowcharts**: Renders routes instantly as they are typed in the editor.
- **Component Identification**: Identifies endpoints by type (e.g. database icon for `sql:`, queue icon for `jms:`, HTTP globe icon for `http:`).
- **Logical Flow Visualizer**: Formats routing steps (Splitters, Aggregators, Filters) as flowchart branches.
- **SVG / PNG Export**: Diagrams can be exported for project documentation.

---

## 6. Faker Studio

Faker Studio generates mock datasets to simulate high-volume transaction environments.

### Features
- **Data-Driven Templates**: Inject synthetic properties using double-braces:
  - `{{name.fullName}}` -> Random full names.
  - `{{internet.email}}` -> Synthetic emails.
  - `{{finance.iban}}` / `{{finance.bic}}` -> Bank identification codes.
  - `{{random.number(min, max)}}` -> Range-bounded values.
- **Custom Providers**: Allows developers to register custom lists (e.g. client accounts).
- **Background Simulators**: Streams synthetic files to directories at custom rates (e.g. 5 files/second).

---

## 7. Export Studio (Liquibase, SQL, File System)

Export Studio packages Camel integrations for deployment. It also exports database scripts, migration files, and directory structures.

### Build Targets
- **Fat JAR**: Packages all dependencies, routes, and properties into a single runnable JAR.
- **GraalVM Native Binary**: Compiles Java files into a native executable for serverless environments.
- **JBang Runnable Script**: Export folder with route DSLs, schemas, properties, and startup files (`run.sh` / `run.bat`).

### Database & Storage Exports

#### A. Liquibase Migration Export
Packages database migration configurations into Liquibase changelogs:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
                   xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                   xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.4.xsd">
    <changeSet id="1" author="routebuilder">
        <createTable tableName="CUSTOMERS">
            <column name="ID" type="numeric">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="NAME" type="varchar(100)"/>
            <column name="EMAIL" type="varchar(100)"/>
            <column name="STATUS" type="varchar(20)"/>
        </createTable>
    </changeSet>
</databaseChangeLog>
```

#### B. SQL Database Export (Oracle & Postgres)
Generates direct database scripts for Postgres or Oracle SQL database engines.

**Oracle DDL Export (`oracle-schema.sql`):**
```sql
CREATE TABLE CUSTOMERS (
    ID NUMBER PRIMARY KEY,
    NAME VARCHAR2(100) NOT NULL,
    EMAIL VARCHAR2(100),
    STATUS VARCHAR2(20) DEFAULT 'PENDING'
);
```

**PostgreSQL DDL Export (`postgres-schema.sql`):**
```sql
CREATE TABLE IF NOT EXISTS customers (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    status VARCHAR(20) DEFAULT 'PENDING'
);
```

#### C. File System Export
Creates deployment filesystems (e.g., config maps, log locations, input/output directories).
```
/deployments/route-app/
 ├── bin/
 │    └── run.sh
 ├── config/
 │    └── application.properties
 ├── routes/
 │    └── eip-routing.yaml
 └── data/
      ├── input/
      └── output/
```
