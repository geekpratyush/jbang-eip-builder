# 🏗️ Universal Message Validator IDE — Prompt for AntiGravity

## Project Overview
Build a **JavaFX Desktop Application** that serves as a **Universal Message & Schema Validator IDE**. The application should feature a VS Code-like interface with Monaco Editor integration, a file tree explorer, and support for validating multiple message formats and schema types. Users can create, edit, save, and validate schemas and messages for future use.

---

## 🎨 UI/UX Requirements

### Layout (VS Code-Inspired)
```
┌─────────────────────────────────────────────────────────────────────────────┐
│  Menu Bar  │  Toolbar (Validate | Save | New | Load | Settings)              │
├──────────┬──────────────────────────────────────────────────────────────────┤
│          │  Tab Bar (Multiple tabs with close buttons)                       │
│  FILE    │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────────────┐│
│  TREE    │  │ schema.xsd  │ │ message.xml │ │ validator.config            ││
│  (Left)  │  └─────────────┘ └─────────────┘ └─────────────────────────────┘│
│          │                                                                  │
│  📁 proj │  ┌────────────────────────────────────────────────────────────┐│
│  ├── 📄  │  │                                                            ││
│  │   xsd │  │         MONACO EDITOR (VS Code-like)                       ││
│  ├── 📄  │  │         - Syntax highlighting                              ││
│  │   xml │  │         - Error squiggles                                  ││
│  ├── 📄  │  │         - IntelliSense/autocomplete                        ││
│  │   json│  │         - Line numbers                                     ││
│  ├── 📁  │  │         - Minimap (optional)                               ││
│  │   mt  │  │                                                            ││
│  │   └── │  └────────────────────────────────────────────────────────────┘│
│  │       │                                                                  │
│  └── 📁  │  ┌────────────────────────────────────────────────────────────┐│
│      iso │  │  VALIDATION RESULTS PANEL (Bottom)                         ││
│          │  │  ✅ XSD: Valid | ❌ XML: Invalid - Line 23: Element 'amount'││
│          │  │  must be a valid decimal                                   ││
│          │  └────────────────────────────────────────────────────────────┘│
└──────────┴──────────────────────────────────────────────────────────────────┘
```

### Monaco Editor Features Required
- **Syntax highlighting** for: XML, XSD, JSON, YAML, SWIFT MT, Java, and plain text
- **Error/warning squiggles** (red underlines for validation errors)
- **Line numbers** and **folding**
- **Auto-indentation**
- **Find/Replace** (Ctrl+F)
- **Multiple cursors**
- **Minimap** (optional but preferred)
- **Theme support**: Dark (default), Light, High Contrast

### File Tree Features
- **Expandable/collapsible folders**
- **Context menu**: New File, New Folder, Rename, Delete, Duplicate
- **Drag-and-drop** file organization
- **File icons** based on extension
- **Search/filter** within tree
- **Recent files** quick access

---

## 📦 Supported Validation Types & Sample Files

Include the following **pre-loaded sample files** in the application for demonstration:

---

### 1️⃣ XML + XSD Validation

**Sample XSD (`samples/xml/invoice-schema.xsd`)**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
           targetNamespace="http://example.com/invoice"
           xmlns="http://example.com/invoice"
           elementFormDefault="qualified">

    <xs:element name="Invoice">
        <xs:complexType>
            <xs:sequence>
                <xs:element name="InvoiceNumber" type="xs:string"/>
                <xs:element name="IssueDate" type="xs:date"/>
                <xs:element name="Customer">
                    <xs:complexType>
                        <xs:sequence>
                            <xs:element name="Name" type="xs:string"/>
                            <xs:element name="Email" type="xs:string"/>
                        </xs:sequence>
                    </xs:complexType>
                </xs:element>
                <xs:element name="LineItems" minOccurs="1" maxOccurs="unbounded">
                    <xs:complexType>
                        <xs:sequence>
                            <xs:element name="Item">
                                <xs:complexType>
                                    <xs:sequence>
                                        <xs:element name="Description" type="xs:string"/>
                                        <xs:element name="Quantity" type="xs:positiveInteger"/>
                                        <xs:element name="UnitPrice" type="xs:decimal"/>
                                    </xs:sequence>
                                </xs:complexType>
                            </xs:element>
                        </xs:sequence>
                    </xs:complexType>
                </xs:element>
                <xs:element name="TotalAmount" type="xs:decimal"/>
            </xs:sequence>
        </xs:complexType>
    </xs:element>

</xs:schema>
```

**Sample Valid XML (`samples/xml/invoice-valid.xml`)**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Invoice xmlns="http://example.com/invoice">
    <InvoiceNumber>INV-2024-001</InvoiceNumber>
    <IssueDate>2024-01-15</IssueDate>
    <Customer>
        <Name>Acme Corporation</Name>
        <Email>billing@acme.com</Email>
    </Customer>
    <LineItems>
        <Item>
            <Description>Professional Services</Description>
            <Quantity>40</Quantity>
            <UnitPrice>150.00</UnitPrice>
        </Item>
    </LineItems>
    <TotalAmount>6000.00</TotalAmount>
</Invoice>
```

**Sample Invalid XML (`samples/xml/invoice-invalid.xml`)**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Invoice xmlns="http://example.com/invoice">
    <InvoiceNumber>INV-2024-002</InvoiceNumber>
    <IssueDate>not-a-date</IssueDate>  <!-- ERROR: Invalid date format -->
    <Customer>
        <Name>Global Tech Ltd</Name>
        <Email>invalid-email</Email>
    </Customer>
    <LineItems>
        <Item>
            <Description>Consulting</Description>
            <Quantity>-5</Quantity>  <!-- ERROR: Must be positive -->
            <UnitPrice>200.00</UnitPrice>
        </Item>
    </LineItems>
    <!-- ERROR: Missing TotalAmount -->
</Invoice>
```

---

### 2️⃣ JSON + JSON Schema Validation

**Sample JSON Schema (`samples/json/customer-schema.json`)**:
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "Customer",
  "type": "object",
  "required": ["id", "name", "email", "status"],
  "properties": {
    "id": {
      "type": "integer",
      "minimum": 1
    },
    "name": {
      "type": "string",
      "minLength": 2,
      "maxLength": 100
    },
    "email": {
      "type": "string",
      "format": "email"
    },
    "phone": {
      "type": "string",
      "pattern": "^\+?[1-9]\d{1,14}$"
    },
    "status": {
      "type": "string",
      "enum": ["active", "inactive", "pending"]
    },
    "createdAt": {
      "type": "string",
      "format": "date-time"
    },
    "orders": {
      "type": "array",
      "items": {
        "type": "object",
        "required": ["orderId", "amount"],
        "properties": {
          "orderId": { "type": "string" },
          "amount": { "type": "number", "minimum": 0 },
          "currency": { "type": "string", "enum": ["USD", "EUR", "GBP"] }
        }
      }
    }
  }
}
```

**Sample Valid JSON (`samples/json/customer-valid.json`)**:
```json
{
  "id": 1001,
  "name": "John Smith",
  "email": "john.smith@example.com",
  "phone": "+14155552671",
  "status": "active",
  "createdAt": "2024-01-15T10:30:00Z",
  "orders": [
    {
      "orderId": "ORD-2024-001",
      "amount": 299.99,
      "currency": "USD"
    }
  ]
}
```

**Sample Invalid JSON (`samples/json/customer-invalid.json`)**:
```json
{
  "id": -5,
  "name": "A",
  "email": "not-an-email",
  "status": "deleted",
  "orders": [
    {
      "orderId": "ORD-001",
      "amount": -50.00,
      "currency": "JPY"
    }
  ]
}
```

---

### 3️⃣ YAML Validation (using JSON Schema)

**Sample YAML Schema (`samples/yaml/config-schema.json`)**:
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "title": "ApplicationConfig",
  "type": "object",
  "required": ["appName", "version", "database", "features"],
  "properties": {
    "appName": { "type": "string", "minLength": 1 },
    "version": { "type": "string", "pattern": "^\d+\.\d+\.\d+$" },
    "debug": { "type": "boolean", "default": false },
    "database": {
      "type": "object",
      "required": ["host", "port", "name"],
      "properties": {
        "host": { "type": "string", "format": "hostname" },
        "port": { "type": "integer", "minimum": 1, "maximum": 65535 },
        "name": { "type": "string", "pattern": "^[a-zA-Z_][a-zA-Z0-9_]*$" },
        "ssl": { "type": "boolean", "default": true }
      }
    },
    "features": {
      "type": "array",
      "minItems": 1,
      "items": {
        "type": "string",
        "enum": ["auth", "logging", "cache", "api", "webhook"]
      }
    }
  }
}
```

**Sample Valid YAML (`samples/yaml/config-valid.yaml`)**:
```yaml
appName: MyApplication
version: "2.1.0"
debug: false
database:
  host: db.example.com
  port: 5432
  name: myapp_prod
  ssl: true
features:
  - auth
  - logging
  - api
```

**Sample Invalid YAML (`samples/yaml/config-invalid.yaml`)**:
```yaml
appName: ""
version: "2.1"  # ERROR: Must be x.y.z format
database:
  host: 192.168.1.1
  port: 70000   # ERROR: Port out of range
  name: "123-invalid"  # ERROR: Invalid identifier
features:
  - auth
  - unknown_feature  # ERROR: Not in enum
```

---

### 4️⃣ SWIFT MT Message Validation

**Sample MT103 (Customer Credit Transfer) — Valid (`samples/mt/mt103-valid.txt`)**:
```
{1:F01BANKBEBBAXXX1234567890}
{2:I103BANKUS33XXXXN}
{4:
:20:REFERENCE123456
:23B:CRED
:32A:240115USD100000,
:50K:/123456789
ACME CORPORATION
123 BUSINESS STREET
NEW YORK NY 10001
:59:/987654321
GLOBAL TRADING LTD
456 COMMERCE AVE
LONDON EC2A 4DP
:71A:SHA
-}
{5:{CHK:1234567890ABC}}
```

**Sample MT103 — Invalid (`samples/mt/mt103-invalid.txt`)**:
```
{1:F01BANKBEBBAXXX1234567890}
{2:I103BANKUS33XXXXN}
{4:
:20:REF  
:23B:INVALID_CODE
:32A:240115USD-50000,
:50K:/123
ACME
:59:/987
GLOBAL
-}
```

**Validation Rules to Implement for MT103**:
| Rule | Description | Error Example |
|------|-------------|---------------|
| Block 1 | Must start with `{1:` and contain valid BIC11 | Invalid BIC format |
| Block 2 | Must contain message type (103) | Wrong message type |
| Field 20 | 1-16 characters, no blanks | Too long or empty |
| Field 23B | Must be CRED, SPAY, SSTD, or SPRI | INVALID_CODE rejected |
| Field 32A | Date(6) + Currency(3) + Amount | Negative amount rejected |
| Field 50K | Account + Name & Address (min 2 lines) | Insufficient lines |
| Field 59 | Account + Name & Address (min 2 lines) | Insufficient lines |
| Field 71A | Must be BEN, SHA, or OUR | Invalid charge code |
| Block 4 | Must end with `-}` | Missing terminator |

---

### 5️⃣ MT202 (Financial Institution Transfer) — Valid (`samples/mt/mt202-valid.txt`)**:
```
{1:F01BANKBEBBAXXX1234567890}
{2:I202BANKUS33XXXXN}
{4:
:20:REF2024001
:21:RELATEDREF001
:32A:240115EUR500000,
:58A:BANKFRPPXXX
-}
{5:{CHK:ABCDEF123456}}
```

---

### 6️⃣ MT940 (Customer Statement) — Valid (`samples/mt/mt940-valid.txt`)**:
```
{1:F01BANKBEBBAXXX1234567890}
{2:I940BANKUS33XXXXN}
{4:
:20:STATEMENT001
:25:1234567890
:28C:001/01
:60F:C240115EUR1000000,
:61:2401150115D50000,NTRFNONREF
:86:INVOICE PAYMENT
:62F:C240115EUR950000,
-}
{5:{CHK:1234567890ABC}}
```

---

### 7️⃣ ISO 20022 (MX) XML Validation

**Sample pacs.008 Schema (`samples/iso20022/pacs008-schema.xsd`)**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema"
           targetNamespace="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08"
           xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08"
           elementFormDefault="qualified">

    <xs:element name="Document">
        <xs:complexType>
            <xs:sequence>
                <xs:element name="FIToFICstmrCdtTrf">
                    <xs:complexType>
                        <xs:sequence>
                            <xs:element name="GrpHdr">
                                <xs:complexType>
                                    <xs:sequence>
                                        <xs:element name="MsgId" type="Max35Text"/>
                                        <xs:element name="CreDtTm" type="ISODateTime"/>
                                        <xs:element name="NbOfTxs" type="Max15NumericText"/>
                                        <xs:element name="CtrlSum" type="DecimalNumber"/>
                                    </xs:sequence>
                                </xs:complexType>
                            </xs:element>
                            <xs:element name="CdtTrfTxInf" maxOccurs="unbounded">
                                <xs:complexType>
                                    <xs:sequence>
                                        <xs:element name="PmtId">
                                            <xs:complexType>
                                                <xs:sequence>
                                                    <xs:element name="InstrId" type="Max35Text" minOccurs="0"/>
                                                    <xs:element name="EndToEndId" type="Max35Text"/>
                                                </xs:sequence>
                                            </xs:complexType>
                                        </xs:element>
                                        <xs:element name="Amt">
                                            <xs:complexType>
                                                <xs:sequence>
                                                    <xs:element name="InstdAmt">
                                                        <xs:complexType>
                                                            <xs:simpleContent>
                                                                <xs:extension base="DecimalNumber">
                                                                    <xs:attribute name="Ccy" type="CurrencyCode" use="required"/>
                                                                </xs:extension>
                                                            </xs:simpleContent>
                                                        </xs:complexType>
                                                    </xs:element>
                                                </xs:sequence>
                                            </xs:complexType>
                                        </xs:element>
                                        <xs:element name="Cdtr">
                                            <xs:complexType>
                                                <xs:sequence>
                                                    <xs:element name="Nm" type="Max140Text"/>
                                                </xs:sequence>
                                            </xs:complexType>
                                        </xs:element>
                                    </xs:sequence>
                                </xs:complexType>
                            </xs:element>
                        </xs:sequence>
                    </xs:complexType>
                </xs:element>
            </xs:sequence>
        </xs:complexType>
    </xs:element>

    <xs:simpleType name="Max35Text">
        <xs:restriction base="xs:string">
            <xs:maxLength value="35"/>
            <xs:minLength value="1"/>
        </xs:restriction>
    </xs:simpleType>

    <xs:simpleType name="Max15NumericText">
        <xs:restriction base="xs:string">
            <xs:pattern value="[0-9]{1,15}"/>
        </xs:restriction>
    </xs:simpleType>

    <xs:simpleType name="Max140Text">
        <xs:restriction base="xs:string">
            <xs:maxLength value="140"/>
        </xs:restriction>
    </xs:simpleType>

    <xs:simpleType name="ISODateTime">
        <xs:restriction base="xs:dateTime"/>
    </xs:simpleType>

    <xs:simpleType name="DecimalNumber">
        <xs:restriction base="xs:decimal">
            <xs:fractionDigits value="5"/>
            <xs:totalDigits value="18"/>
        </xs:restriction>
    </xs:simpleType>

    <xs:simpleType name="CurrencyCode">
        <xs:restriction base="xs:string">
            <xs:pattern value="[A-Z]{3}"/>
        </xs:restriction>
    </xs:simpleType>

</xs:schema>
```

**Sample Valid pacs.008 (`samples/iso20022/pacs008-valid.xml`)**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08">
    <FIToFICstmrCdtTrf>
        <GrpHdr>
            <MsgId>MSG-2024-001</MsgId>
            <CreDtTm>2024-01-15T10:30:00Z</CreDtTm>
            <NbOfTxs>1</NbOfTxs>
            <CtrlSum>100000.00</CtrlSum>
        </GrpHdr>
        <CdtTrfTxInf>
            <PmtId>
                <InstrId>INST-001</InstrId>
                <EndToEndId>E2E-001</EndToEndId>
            </PmtId>
            <Amt>
                <InstdAmt Ccy="EUR">100000.00</InstdAmt>
            </Amt>
            <Cdtr>
                <Nm>Global Trading Ltd</Nm>
            </Cdtr>
        </CdtTrfTxInf>
    </FIToFICstmrCdtTrf>
</Document>
```

**Sample Invalid pacs.008 (`samples/iso20022/pacs008-invalid.xml`)**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.08">
    <FIToFICstmrCdtTrf>
        <GrpHdr>
            <MsgId></MsgId>  <!-- ERROR: Empty, minLength=1 -->
            <CreDtTm>2024-01-15</CreDtTm>  <!-- ERROR: Not a dateTime -->
            <NbOfTxs>ABC</NbOfTxs>  <!-- ERROR: Not numeric -->
            <CtrlSum>-1000.00</CtrlSum>
        </GrpHdr>
        <CdtTrfTxInf>
            <PmtId>
                <EndToEndId>This is a very long end to end identifier that exceeds thirty five characters limit</EndToEndId>
                <!-- ERROR: Exceeds 35 chars -->
            </PmtId>
            <Amt>
                <InstdAmt Ccy="EURO">100000.00</InstdAmt>
                <!-- ERROR: Currency code must be 3 chars -->
            </Amt>
            <Cdtr>
                <Nm>Global Trading Ltd</Nm>
            </Cdtr>
        </CdtTrfTxInf>
    </FIToFICstmrCdtTrf>
</Document>
```

---

### 8️⃣ CSV Validation (CSVW)

**Sample CSVW Metadata (`samples/csv/transactions-metadata.json`)**:
```json
{
  "@context": "http://www.w3.org/ns/csvw",
  "url": "transactions.csv",
  "tableSchema": {
    "columns": [
      {
        "name": "transactionId",
        "titles": "Transaction ID",
        "datatype": "string",
        "required": true,
        "minLength": 5,
        "maxLength": 20
      },
      {
        "name": "date",
        "titles": "Date",
        "datatype": "date",
        "required": true,
        "format": "yyyy-MM-dd"
      },
      {
        "name": "amount",
        "titles": "Amount",
        "datatype": "number",
        "required": true,
        "minimum": 0
      },
      {
        "name": "currency",
        "titles": "Currency",
        "datatype": "string",
        "required": true,
        "format": "^[A-Z]{3}$"
      },
      {
        "name": "status",
        "titles": "Status",
        "datatype": "string",
        "required": true,
        "constraints": {
          "enum": ["PENDING", "COMPLETED", "FAILED", "CANCELLED"]
        }
      }
    ],
    "primaryKey": "transactionId"
  }
}
```

**Sample Valid CSV (`samples/csv/transactions-valid.csv`)**:
```csv
Transaction ID,Date,Amount,Currency,Status
TXN-001,2024-01-15,1500.00,USD,COMPLETED
TXN-002,2024-01-16,2500.50,EUR,PENDING
TXN-003,2024-01-17,999.99,GBP,COMPLETED
```

**Sample Invalid CSV (`samples/csv/transactions-invalid.csv`)**:
```csv
Transaction ID,Date,Amount,Currency,Status
TXN-001,2024-01-15,1500.00,USD,COMPLETED
TXN-002,not-a-date,-500.00,EUR,PENDING
TXN-003,2024-01-17,999.99,EURO,UNKNOWN
TXN-001,2024-01-18,2000.00,GBP,COMPLETED
```

---

### 9️⃣ ASN.1 Sample

**Sample ASN.1 Schema (`samples/asn1/person-schema.asn1`)**:
```asn1
PersonnelRecord DEFINITIONS ::= BEGIN

Person ::= SEQUENCE {
    name            Name,
    title           IA5String,
    number          EmployeeNumber,
    dateOfHire      Date,
    nameOfSpouse    Name,
    children        SEQUENCE OF Child DEFAULT {}
}

Name ::= SEQUENCE {
    givenName       IA5String,
    initial         IA5String,
    familyName      IA5String
}

EmployeeNumber ::= INTEGER

Date ::= IA5String

Child ::= SEQUENCE {
    name            Name,
    dateOfBirth     Date
}

END
```

---

### 🔟 Flat File / Fixed-Width Validation

**Sample Schema (`samples/flatfile/fixedwidth-schema.json`)**:
```json
{
  "recordLength": 80,
  "fields": [
    { "name": "recordType", "start": 1, "length": 2, "type": "string", "required": true },
    { "name": "accountNumber", "start": 3, "length": 10, "type": "string", "pattern": "^[0-9]{10}$" },
    { "name": "customerName", "start": 13, "length": 30, "type": "string", "trim": true },
    { "name": "balance", "start": 43, "length": 15, "type": "decimal", "format": "##########.##" },
    { "name": "currency", "start": 58, "length": 3, "type": "string", "enum": ["USD", "EUR", "GBP"] },
    { "name": "status", "start": 61, "length": 1, "type": "string", "enum": ["A", "I", "C"] },
    { "name": "lastUpdated", "start": 62, "length": 8, "type": "date", "format": "yyyyMMdd" },
    { "name": "filler", "start": 70, "length": 11, "type": "string", "default": " " }
  ]
}
```

**Sample Valid Fixed-Width (`samples/flatfile/fixedwidth-valid.txt`)**:
```
011234567890ACME CORPORATION              000001000000.00USD20240115   
021234567891GLOBAL TRADING LTD           000005000000.50EUR20240116   
```

---

## 🛠️ Core Features to Implement

### 1. Project Workspace
- **Save/Load Projects**: JSON-based project files (`.validatorproj`)
- **Workspace structure**:
  ```
  MyProject.validatorproj
  ├── schemas/
  │   ├── xsd/
  │   ├── json-schema/
  │   ├── mt-rules/
  │   └── iso20022/
  ├── messages/
  │   ├── xml/
  │   ├── json/
  │   ├── yaml/
  │   ├── mt/
  │   └── csv/
  ├── validators/
  │   └── custom-validators.json
  └── settings.json
  ```

### 2. Validator Configuration
Each validator should be configurable via JSON:
```json
{
  "validators": [
    {
      "id": "invoice-xsd",
      "name": "Invoice XSD Validator",
      "type": "xsd",
      "schemaFile": "schemas/xsd/invoice-schema.xsd",
      "targetNamespace": "http://example.com/invoice",
      "enabled": true
    },
    {
      "id": "mt103-validator",
      "name": "MT103 SWIFT Validator",
      "type": "swift-mt",
      "messageType": "MT103",
      "rulesFile": "schemas/mt/mt103-rules.json",
      "enabled": true
    },
    {
      "id": "pacs008-iso",
      "name": "ISO 20022 pacs.008",
      "type": "iso20022",
      "schemaFile": "schemas/iso20022/pacs008.xsd",
      "businessRules": ["CBPR+", "SEPA"],
      "enabled": true
    }
  ]
}
```

### 3. Validation Engine Architecture
```java
// Core interfaces
public interface Validator {
    ValidationResult validate(String content, ValidationContext context);
    boolean supports(String format);
    String getName();
}

public interface SchemaLoader {
    Schema loadSchema(String path);
}

public class ValidationResult {
    private boolean valid;
    private List<ValidationError> errors;
    private List<ValidationWarning> warnings;
    private long durationMs;
    private String validatorName;
}

public class ValidationError {
    private int line;
    private int column;
    private String message;
    private ErrorSeverity severity; // ERROR, WARNING, INFO
    private String ruleId;
    private String suggestion;
}
```

### 4. Validation Result Panel
- **Tree view** of errors grouped by severity
- **Click-to-navigate**: Click error → jump to line in Monaco editor
- **Filter**: Show only errors/warnings/info
- **Export**: Save results to JSON/CSV/HTML
- **Statistics**: Total errors, warnings, validation time

### 5. Monaco Editor Integration
- Use **Monaco Editor for Java** (via WebView or JCEF)
- **Language support**: xml, json, yaml, plaintext (for MT), csv
- **Custom themes**: Match VS Code dark/light themes
- **Minimap**: Enable by default
- **Error decorations**: Red squiggles from validation results

### 6. Keyboard Shortcuts
| Shortcut | Action |
|----------|--------|
| Ctrl+O | Open file |
| Ctrl+S | Save file |
| Ctrl+Shift+S | Save all |
| Ctrl+N | New file |
| F5 | Validate current file |
| Ctrl+F5 | Validate all open files |
| Ctrl+Shift+F | Find in files |
| Ctrl+B | Toggle sidebar |
| Ctrl+` | Toggle terminal/results panel |
| Ctrl+Tab | Switch between tabs |

---

## 📋 MT Message Validation Rules Engine

### SWIFT MT Rule Types to Implement

```json
{
  "mt103_rules": {
    "structure": {
      "required_blocks": ["1", "2", "4"],
      "optional_blocks": ["3", "5"],
      "block_4_terminator": "-}"
    },
    "fields": {
      "20": {
        "name": "Transaction Reference Number",
        "format": "16x",
        "maxLength": 16,
        "minLength": 1,
        "pattern": "^[A-Za-z0-9/\-?:().,'+]{1,16}$",
        "required": true
      },
      "23B": {
        "name": "Bank Operation Code",
        "format": "4!c",
        "enum": ["CRED", "SPAY", "SSTD", "SPRI"],
        "required": true
      },
      "32A": {
        "name": "Value Date/Currency/Interbank Settled Amount",
        "format": "6!n3!a15d",
        "pattern": "^\d{6}[A-Z]{3}\d{1,15}$",
        "required": true,
        "subFields": {
          "date": { "format": "YYMMDD", "type": "date" },
          "currency": { "type": "iso4217" },
          "amount": { "type": "decimal", "minimum": 0 }
        }
      },
      "50K": {
        "name": "Ordering Customer",
        "format": "/34x\n4*35x",
        "lines": { "min": 2, "max": 5 },
        "required": true
      },
      "59": {
        "name": "Beneficiary Customer",
        "format": "/34x\n4*35x",
        "lines": { "min": 2, "max": 5 },
        "required": true
      },
      "71A": {
        "name": "Details of Charges",
        "format": "3!a",
        "enum": ["BEN", "SHA", "OUR"],
        "required": true
      }
    },
    "cross_field_rules": [
      {
        "id": "CFR001",
        "description": "If 23B is CRED, 71A must be SHA",
        "condition": "field_23B == 'CRED'",
        "assertion": "field_71A == 'SHA'",
        "severity": "ERROR"
      }
    ]
  }
}
```

---

## 🎨 UI Components Checklist

- [ ] **Menu Bar**: File, Edit, View, Validate, Tools, Help
- [ ] **Toolbar**: New, Open, Save, Validate, Settings, Theme Toggle
- [ ] **File Tree**: Left sidebar with project structure
- [ ] **Tab Bar**: Multiple editor tabs with close buttons
- [ ] **Monaco Editor**: Central editing area with syntax highlighting
- [ ] **Validation Results**: Bottom panel with error tree
- [ ] **Status Bar**: Line/column, file type, validation status, encoding
- [ ] **Settings Panel**: Preferences for editor, validation, themes
- [ ] **Welcome Screen**: Recent projects, create new, open existing
- [ ] **Splash Screen**: App logo, loading progress

---

## 🚀 Getting Started Flow

1. **Launch App** → Welcome Screen
2. **Click "New Project"** → Choose template (Empty, XML Validator, MT Validator, ISO 20022 Validator, Full Suite)
3. **Project loads** with sample files pre-populated
4. **Click any file** in tree → Opens in Monaco editor
5. **Press F5** → Validates file against configured schema
6. **View results** in bottom panel → Click errors to navigate
7. **Save project** → `.validatorproj` file for future use

---

## 📦 Dependencies to Include

```xml
<!-- pom.xml dependencies -->
<dependencies>
    <!-- JavaFX -->
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-controls</artifactId>
        <version>21</version>
    </dependency>
    <dependency>
        <groupId>org.openjfx</groupId>
        <artifactId>javafx-web</artifactId>
        <version>21</version>
    </dependency>

    <!-- Monaco Editor (via JCEF or WebView) -->
    <!-- Use JCEF for better Monaco support -->

    <!-- XML/XSD Validation -->
    <dependency>
        <groupId>xerces</groupId>
        <artifactId>xercesImpl</artifactId>
        <version>2.12.2</version>
    </dependency>

    <!-- JSON Schema Validation -->
    <dependency>
        <groupId>com.networknt</groupId>
        <artifactId>json-schema-validator</artifactId>
        <version>1.0.86</version>
    </dependency>

    <!-- YAML Parsing -->
    <dependency>
        <groupId>org.yaml</groupId>
        <artifactId>snakeyaml</artifactId>
        <version>2.2</version>
    </dependency>

    <!-- Jackson for JSON -->
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
        <version>2.15.2</version>
    </dependency>

    <!-- CSV Parsing -->
    <dependency>
        <groupId>org.apache.commons</groupId>
        <artifactId>commons-csv</artifactId>
        <version>1.10.0</version>
    </dependency>

    <!-- Icons -->
    <dependency>
        <groupId>org.kordamp.ikonli</groupId>
        <artifactId>ikonli-javafx</artifactId>
        <version>12.3.1</version>
    </dependency>
    <dependency>
        <groupId>org.kordamp.ikonli</groupId>
        <artifactId>ikonli-fontawesome5-pack</artifactId>
        <version>12.3.1</version>
    </dependency>

    <!-- Testing -->
    <dependency>
        <groupId>org.junit.jupiter</groupId>
        <artifactId>junit-jupiter</artifactId>
        <version>5.10.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 🎯 Success Criteria

1. ✅ All sample files validate correctly (valid = pass, invalid = show errors)
2. ✅ File tree supports CRUD operations
3. ✅ Monaco editor has syntax highlighting for all formats
4. ✅ Validation results panel shows clickable errors
5. ✅ Projects can be saved and loaded
6. ✅ Keyboard shortcuts work as specified
7. ✅ Dark/Light theme toggle works
8. ✅ Status bar shows real-time info
9. ✅ Welcome screen with templates
10. ✅ Cross-field validation rules work for MT messages

---

**Build this as a production-ready, polished JavaFX desktop application with modern UI/UX.**


---

## 🔧 Appendix B: MT Validator Configuration Modes

SWIFT MT validation supports **two distinct modes**. Unlike XSD (which is fully declarative and user-defined), MT rules are **fixed by SWIFT**, but you can choose how to apply them:

| Mode | Description | Use Case |
|------|-------------|----------|
| **Standard MT Validator** | Validates against SWIFT's published fixed rules only. No custom rules. | Basic compliance checking, quick validation |
| **MT Validator with Rules** | Validates against SWIFT's fixed rules **plus** user-defined custom rules and cross-field validations. | Enhanced compliance, institutional policies, custom business logic |

---

### Mode 1: Standard MT Validator (No Custom Rules)

In this mode, the validator only enforces SWIFT's **official fixed rules** as published in the Message Reference Guide. No user-defined rules are applied.

**Configuration (`validators/standard-mt-config.json`)**:
```json
{
  "id": "mt-standard",
  "name": "Standard MT Validator",
  "type": "swift-mt",
  "mode": "standard",
  "description": "Validates MT messages using only SWIFT's official fixed rules",
  "ruleSource": {
    "type": "swift_official",
    "version": "2024",
    "source": "SWIFT Message Reference Guide + Network Validated Rules"
  },
  "validationLayers": {
    "structure": true,
    "fieldFormat": true,
    "dataValidation": true,
    "networkRules": true
  },
  "customRules": {
    "enabled": false,
    "rulesFile": null
  },
  "supportedMessages": [
    "MT101", "MT103", "MT103+", "MT104", "MT107",
    "MT200", "MT202", "MT202COV", "MT205", "MT210",
    "MT540", "MT541", "MT542", "MT543", "MT544", "MT545", "MT546", "MT547", "MT548",
    "MT564", "MT565", "MT566", "MT567",
    "MT700", "MT701", "MT705", "MT707", "MT710", "MT720", "MT730", "MT740", "MT747", "MT750", "MT752", "MT754", "MT756", "MT760", "MT767", "MT768", "MT769",
    "MT900", "MT910", "MT940", "MT941", "MT942", "MT950",
    "MTn90", "MTn91", "MTn92", "MTn95", "MTn96", "MTn98", "MTn99"
  ]
}
```

**What it validates**:
- ✅ Block structure (`{1:}`, `{2:}`, `{4:}`, `-}`)
- ✅ Field presence (mandatory vs optional per message type)
- ✅ Field format codes (`16x`, `6!n3!a15d`, `4*35x`, etc.)
- ✅ Character sets (SWIFT X, Y, Z sets)
- ✅ Field length constraints
- ✅ Valid code values (e.g., `23B` must be `CRED|SPAY|SSTD|SPRI`)
- ✅ Network validated rules (conditional presence)
- ✅ Cross-field dependencies (official SWIFT rules only)

**What it does NOT validate**:
- ❌ Custom business rules
- ❌ Institution-specific policies
- ❌ Extended cross-field validations beyond SWIFT's official rules

---

### Mode 2: MT Validator with Custom Rules

In this mode, the validator enforces **SWIFT's fixed rules PLUS** user-defined custom rules. This allows institutions to add their own business logic, compliance checks, and extended validations.

**Configuration (`validators/enhanced-mt-config.json`)**:
```json
{
  "id": "mt-enhanced",
  "name": "Enhanced MT Validator with Rules",
  "type": "swift-mt",
  "mode": "enhanced",
  "description": "Validates MT messages using SWIFT's fixed rules plus custom institutional rules",
  "ruleSource": {
    "type": "swift_official",
    "version": "2024",
    "source": "SWIFT Message Reference Guide + Network Validated Rules"
  },
  "validationLayers": {
    "structure": true,
    "fieldFormat": true,
    "dataValidation": true,
    "networkRules": true,
    "customRules": true
  },
  "customRules": {
    "enabled": true,
    "rulesFile": "validators/custom-mt-rules.json",
    "ruleEngine": "javascript",
    "allowRuleOverrides": false
  },
  "supportedMessages": [
    "MT101", "MT103", "MT103+", "MT104", "MT107",
    "MT200", "MT202", "MT202COV", "MT205", "MT210",
    "MT540", "MT541", "MT542", "MT543", "MT544", "MT545", "MT546", "MT547", "MT548",
    "MT564", "MT565", "MT566", "MT567",
    "MT700", "MT701", "MT705", "MT707", "MT710", "MT720", "MT730", "MT740", "MT747", "MT750", "MT752", "MT754", "MT756", "MT760", "MT767", "MT768", "MT769",
    "MT900", "MT910", "MT940", "MT941", "MT942", "MT950",
    "MTn90", "MTn91", "MTn92", "MTn95", "MTn96", "MTn98", "MTn99"
  ]
}
```

**Custom Rules File (`validators/custom-mt-rules.json`)**:
```json
{
  "customRules": {
    "description": "Institution-specific rules layered on top of SWIFT fixed rules",
    "version": "1.0",
    "institution": "Example Bank Ltd",
    "effectiveDate": "2024-01-01",
    "rules": [
      {
        "id": "CUSTOM-001",
        "name": "Maximum Amount Limit",
        "description": "Reject MT103 with amount exceeding institution limit",
        "appliesTo": ["MT103", "MT103+"],
        "severity": "ERROR",
        "condition": {
          "field": "32A",
          "subField": "amount",
          "operator": "GREATER_THAN",
          "value": 10000000
        },
        "action": "REJECT",
        "errorMessage": "Amount exceeds maximum limit of 10,000,000.00",
        "suggestion": "Split into multiple transactions or use MT202"
      },
      {
        "id": "CUSTOM-002",
        "name": "Restricted Currency Check",
        "description": "Block transactions in restricted currencies",
        "appliesTo": ["MT103", "MT202", "MT205"],
        "severity": "ERROR",
        "condition": {
          "field": "32A",
          "subField": "currency",
          "operator": "IN_LIST",
          "value": ["RUB", "IRR", "KPW", "SYR"]
        },
        "action": "REJECT",
        "errorMessage": "Currency {currency} is restricted by compliance policy",
        "suggestion": "Contact compliance department for approval"
      },
      {
        "id": "CUSTOM-003",
        "name": "High-Risk Country Beneficiary",
        "description": "Flag beneficiaries in high-risk jurisdictions",
        "appliesTo": ["MT103", "MT103+"],
        "severity": "WARNING",
        "condition": {
          "field": "59",
          "operator": "CONTAINS_PATTERN",
          "patterns": [
            "\b(Afghanistan|Belarus|Myanmar|North Korea|Syria|Iran)\b"
          ]
        },
        "action": "FLAG",
        "errorMessage": "Beneficiary may be in a high-risk jurisdiction",
        "suggestion": "Enhanced due diligence required"
      },
      {
        "id": "CUSTOM-004",
        "name": "Reference Number Format",
        "description": "Enforce institution-specific reference number format",
        "appliesTo": ["MT103", "MT202", "MT205"],
        "severity": "ERROR",
        "condition": {
          "field": "20",
          "operator": "NOT_MATCHES",
          "pattern": "^EXB-[0-9]{4}-[0-9]{6}$"
        },
        "action": "REJECT",
        "errorMessage": "Reference number must match format EXB-YYYY-NNNNNN",
        "suggestion": "Use format: EXB-2024-000001"
      },
      {
        "id": "CUSTOM-005",
        "name": "Weekend Value Date Check",
        "description": "Warn if value date falls on weekend",
        "appliesTo": ["MT103", "MT202", "MT205", "MT210"],
        "severity": "WARNING",
        "condition": {
          "field": "32A",
          "subField": "date",
          "operator": "IS_WEEKEND"
        },
        "action": "FLAG",
        "errorMessage": "Value date {date} falls on a weekend",
        "suggestion": "Value date will be adjusted to next business day"
      },
      {
        "id": "CUSTOM-006",
        "name": "Duplicate Reference Prevention",
        "description": "Check for duplicate transaction references within 24 hours",
        "appliesTo": ["MT103", "MT202"],
        "severity": "ERROR",
        "condition": {
          "field": "20",
          "operator": "DUPLICATE_WITHIN",
          "timeWindow": "24h",
          "scope": "institution"
        },
        "action": "REJECT",
        "errorMessage": "Duplicate reference number detected within 24 hours",
        "suggestion": "Generate a unique reference number"
      },
      {
        "id": "CUSTOM-007",
        "name": "Ordering Customer Blacklist",
        "description": "Block transactions from blacklisted ordering customers",
        "appliesTo": ["MT103", "MT103+"],
        "severity": "ERROR",
        "condition": {
          "field": "50K",
          "operator": "MATCHES_BLACKLIST",
          "blacklistFile": "validators/blacklist.json"
        },
        "action": "REJECT",
        "errorMessage": "Ordering customer is on restricted list",
        "suggestion": "Contact compliance for review"
      },
      {
        "id": "CUSTOM-008",
        "name": "BIC Validation Level",
        "description": "Require live BIC validation for high-value transactions",
        "appliesTo": ["MT103", "MT202"],
        "severity": "ERROR",
        "condition": {
          "and": [
            {
              "field": "32A",
              "subField": "amount",
              "operator": "GREATER_THAN",
              "value": 1000000
            },
            {
              "field": "57A",
              "operator": "BIC_NOT_LIVE"
            }
          ]
        },
        "action": "REJECT",
        "errorMessage": "Live BIC validation required for amounts > 1,000,000",
        "suggestion": "Use a live BIC or reduce amount"
      }
    ]
  }
}
```

---

### Sample Files for Both Modes

#### Standard Mode Samples

**Valid MT103 — Standard Check (`samples/mt/standard/mt103-valid-standard.txt`)**:
```
{1:F01BANKBEBBAXXX1234567890}
{2:I103BANKUS33XXXXN}
{4:
:20:REFERENCE123456
:23B:CRED
:32A:240115USD100000,
:50K:/123456789
ACME CORPORATION
123 BUSINESS STREET
NEW YORK NY 10001
:59:/987654321
GLOBAL TRADING LTD
456 COMMERCE AVE
LONDON EC2A 4DP
:71A:SHA
-}
{5:{CHK:1234567890ABC}}
```

**Invalid MT103 — Standard Check (`samples/mt/standard/mt103-invalid-standard.txt`)**:
```
{1:F01BANKBEBBAXXX1234567890}
{2:I103BANKUS33XXXXN}
{4:
:20:REF  
:23B:INVALID_CODE
:32A:240115USD-50000,
:50K:/123
ACME
:59:/987
GLOBAL
-}
```

**Expected Standard Validation Results**:
| Line | Field | Error | Rule |
|------|-------|-------|------|
| 4 | 20 | Invalid length/format | T-rule: 16x format |
| 5 | 23B | Invalid code | D-rule: Must be CRED/SPAY/SSTD/SPRI |
| 6 | 32A | Negative amount | T-rule: Amount must be positive |
| 7-8 | 50K | Insufficient lines | T-rule: Min 2 lines required |
| 9-10 | 59 | Insufficient lines | T-rule: Min 2 lines required |

---

#### Enhanced Mode Samples (with Custom Rules)

**Valid MT103 — Enhanced Check (`samples/mt/enhanced/mt103-valid-enhanced.txt`)**:
```
{1:F01BANKBEBBAXXX1234567890}
{2:I103BANKUS33XXXXN}
{4:
:20:EXB-2024-000001
:23B:CRED
:32A:240115USD500000,
:50K:/123456789
ACME CORPORATION
123 BUSINESS STREET
NEW YORK NY 10001
:59:/987654321
GLOBAL TRADING LTD
456 COMMERCE AVE
LONDON EC2A 4DP
:71A:SHA
-}
{5:{CHK:1234567890ABC}}
```

**Invalid MT103 — Enhanced Check (`samples/mt/enhanced/mt103-invalid-enhanced.txt`)**:
```
{1:F01BANKBEBBAXXX1234567890}
{2:I103BANKUS33XXXXN}
{4:
:20:REF-123456
:23B:CRED
:32A:240115RUB15000000,
:50K:/123456789
ACME CORPORATION
123 BUSINESS STREET
NEW YORK NY 10001
:59:/987654321
GLOBAL TRADING LTD
456 COMMERCE AVE
TEHRAN IRAN
:71A:SHA
-}
{5:{CHK:1234567890ABC}}
```

**Expected Enhanced Validation Results**:
| Line | Field | Error | Rule | Type |
|------|-------|-------|------|------|
| 4 | 20 | Invalid format | CUSTOM-004 | Custom |
| 6 | 32A | Restricted currency RUB | CUSTOM-002 | Custom |
| 6 | 32A | Amount exceeds 10M limit | CUSTOM-001 | Custom |
| 10 | 59 | High-risk jurisdiction Iran | CUSTOM-003 | Custom |

---

### Rule Engine Implementation

```java
public interface MTRule {
    String getId();
    String getName();
    boolean appliesTo(String messageType);
    ValidationResult evaluate(MTMessage message, ValidationContext context);
}

public class FixedMTRule implements MTRule {
    // SWIFT's official fixed rules - cannot be modified
    private final SWIFTRuleDefinition definition;

    @Override
    public ValidationResult evaluate(MTMessage message, ValidationContext context) {
        // Apply SWIFT's fixed validation logic
        return definition.validate(message);
    }
}

public class CustomMTRule implements MTRule {
    // User-defined custom rules
    private final CustomRuleDefinition definition;

    @Override
    public ValidationResult evaluate(MTMessage message, ValidationContext context) {
        // Apply custom business logic
        return definition.evaluate(message, context);
    }
}

public class MTValidator {
    private final List<FixedMTRule> fixedRules;
    private final List<CustomMTRule> customRules;
    private final ValidatorMode mode;

    public ValidationResult validate(MTMessage message) {
        ValidationResult result = new ValidationResult();

        // Always apply fixed rules first
        for (FixedMTRule rule : fixedRules) {
            if (rule.appliesTo(message.getType())) {
                result.merge(rule.evaluate(message, context));
            }
        }

        // Apply custom rules only in enhanced mode
        if (mode == ValidatorMode.ENHANCED) {
            for (CustomMTRule rule : customRules) {
                if (rule.appliesTo(message.getType())) {
                    result.merge(rule.evaluate(message, context));
                }
            }
        }

        return result;
    }
}

public enum ValidatorMode {
    STANDARD,    // Fixed rules only
    ENHANCED     // Fixed + custom rules
}
```

---

### UI Toggle Between Modes

Add a **mode selector** in the toolbar:

```java
// Toolbar component
ToggleGroup validatorMode = new ToggleGroup();
RadioButton standardMode = new RadioButton("Standard");
RadioButton enhancedMode = new RadioButton("Enhanced (+Rules)");
standardMode.setToggleGroup(validatorMode);
enhancedMode.setToggleGroup(validatorMode);
standardMode.setSelected(true);

validatorMode.selectedToggleProperty().addListener((obs, old, newVal) -> {
    if (newVal == enhancedMode) {
        // Load custom rules
        validator.loadCustomRules("validators/custom-mt-rules.json");
        statusBar.setText("Mode: Enhanced (with custom rules)");
    } else {
        // Unload custom rules
        validator.unloadCustomRules();
        statusBar.setText("Mode: Standard (SWIFT rules only)");
    }
});
```

---

### File Tree Structure with Both Modes

```
MyProject.validatorproj
├── schemas/
│   ├── xsd/
│   ├── json-schema/
│   └── iso20022/
├── messages/
│   ├── xml/
│   ├── json/
│   ├── yaml/
│   ├── csv/
│   └── mt/
│       ├── standard/           # Standard mode samples
│       │   ├── mt103-valid-standard.txt
│       │   ├── mt103-invalid-standard.txt
│       │   ├── mt202-valid-standard.txt
│       │   ├── mt202-invalid-standard.txt
│       │   ├── mt940-valid-standard.txt
│       │   └── mt940-invalid-standard.txt
│       └── enhanced/           # Enhanced mode samples
│           ├── mt103-valid-enhanced.txt
│           ├── mt103-invalid-enhanced.txt
│           ├── mt202-valid-enhanced.txt
│           └── mt202-invalid-enhanced.txt
├── validators/
│   ├── standard-mt-config.json      # Standard mode config
│   ├── enhanced-mt-config.json      # Enhanced mode config
│   ├── custom-mt-rules.json         # Custom rules definition
│   ├── blacklist.json               # Blacklist for custom rules
│   └── swift-fixed-rules/           # SWIFT fixed rules (read-only)
│       ├── mt103-rules.json
│       ├── mt202-rules.json
│       ├── mt940-rules.json
│       └── ...
└── settings.json
```

---

### Summary Table: Standard vs Enhanced

| Feature | Standard Mode | Enhanced Mode |
|---------|--------------|---------------|
| **SWIFT fixed rules** | ✅ Yes | ✅ Yes |
| **Structure validation** | ✅ Yes | ✅ Yes |
| **Field format (T-rules)** | ✅ Yes | ✅ Yes |
| **Data validation (D-rules)** | ✅ Yes | ✅ Yes |
| **Network rules (C-rules)** | ✅ Yes | ✅ Yes |
| **Custom business rules** | ❌ No | ✅ Yes |
| **Amount limits** | ❌ No | ✅ Yes |
| **Currency restrictions** | ❌ No | ✅ Yes |
| **Blacklist checks** | ❌ No | ✅ Yes |
| **Reference format enforcement** | ❌ No | ✅ Yes |
| **Weekend date warnings** | ❌ No | ✅ Yes |
| **Duplicate detection** | ❌ No | ✅ Yes |
| **BIC live validation** | ❌ No | ✅ Yes |
| **Rule file editable** | ❌ No (fixed) | ✅ Yes (custom rules file) |
| **Performance** | Faster | Slightly slower |

---

**Note**: The SWIFT fixed rules (`swift-fixed-rules/`) are **read-only** and represent the official SWIFT standard. They cannot be modified by users. Only the `custom-mt-rules.json` file is user-editable in Enhanced mode.
