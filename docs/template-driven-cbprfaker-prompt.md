# 🏗️ Template-Driven CBPRFaker — Prompt for AntiGravity

## Project Overview
Refactor the existing `CBPRFaker.java` into a **template-driven, file-based message generator** that reads XML templates from a local folder, intelligently replaces placeholders with fake data, and produces structurally valid ISO 20022 / CBPR+ messages.

The user should be able to:
1. Drop template files (`.xml` or `.txt`) into a `templates/` folder
2. Call `CBPRFaker.generate("pacs.008")` — it auto-finds `templates/pacs.008.xml`
3. The faker reads the template, finds all `{{placeholder}}` tokens, and replaces them with realistic fake data
4. Add new message types simply by adding new template files — **no code changes needed**

---

## 🎯 Core Requirements

### 1. Template Discovery Engine

```java
public class TemplateDiscovery {
    private final Path templatesDir;
    private final Map<String, Template> templateCache;

    /**
     * Scans templates/ folder and loads all .xml / .txt files.
     * Files are indexed by their base name (e.g., "pacs.008.xml" → key "pacs.008")
     */
    public void scanTemplates() {
        // Load from: ./templates/ (current working directory)
        // Support: .xml, .txt, .template extensions
        // Cache templates for performance
        // Auto-reload on file change (optional, via file watcher)
    }

    public Template getTemplate(String messageType) {
        // Look for: templates/{messageType}.xml
        //           templates/{messageType}.txt
        //           templates/{messageType}.template
        // Return cached Template or load from disk
    }
}
```

### 2. Template File Format

Templates use **Mustache-style** `{{placeholder}}` syntax. The faker should support:

| Placeholder Type | Syntax | Example Output |
|-----------------|--------|----------------|
| **Simple variable** | `{{variableName}}` | `ABC123` |
| **Typed generator** | `{{type:generatorName}}` | `{{type:uuid}}` → `a1b2c3d4...` |
| **Parameterized** | `{{type:amount:min:max}}` | `{{type:amount:1000:50000}}` → `34250.00` |
| **Conditional** | `{{?condition}}...{{/condition}}` | Include block only if condition true |
| **Loop** | `{{#items}}...{{/items}}` | Repeat for each item in list |
| **Date format** | `{{type:date:yyyy-MM-dd}}` | `2024-01-15` |
| **Random from list** | `{{type:random:USD,EUR,GBP}}` | `EUR` |
| **IBAN with country** | `{{type:iban:DE}}` | `DE89370400440532013000` |
| **BIC from bank** | `{{type:bic}}` | `DEUTDEDBFRA` |
| **Company name** | `{{type:company}}` | `Apex Global Trading Ltd` |

**Example Template File (`templates/pacs.008.xml`)**:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.10">
  <FIToFICstmrCdtTrf>
    <GrpHdr>
      <MsgId>{{type:msgId}}</MsgId>
      <CreDtTm>{{type:dateTime:iso}}</CreDtTm>
      <NbOfTxs>1</NbOfTxs>
      <SttlmInf>
        <SttlmMtd>CLRG</SttlmMtd>
      </SttlmInf>
      <InstgAgt>
        <FinInstnId><BICFI>{{type:bic}}</BICFI></FinInstnId>
      </InstgAgt>
      <InstdAgt>
        <FinInstnId><BICFI>{{type:bic}}</BICFI></FinInstnId>
      </InstdAgt>
    </GrpHdr>
    <CdtTrfTxInf>
      <PmtId>
        <InstrId>{{type:uid:16}}</InstrId>
        <EndToEndId>{{type:uid:16}}</EndToEndId>
        <UETR>{{type:uetr}}</UETR>
      </PmtId>
      <PmtTpInf>
        <InstrPrty>{{type:priority}}</InstrPrty>
        <SvcLvl><Cd>SDVA</Cd></SvcLvl>
      </PmtTpInf>
      <IntrBkSttlmAmt Ccy="{{type:currency}}">{{type:amount:1000:1000000}}</IntrBkSttlmAmt>
      <IntrBkSttlmDt>{{type:date:yyyy-MM-dd}}</IntrBkSttlmDt>
      <ChrgBr>SHAR</ChrgBr>
      <IntrmyAgt1>
        <FinInstnId><BICFI>{{type:bic}}</BICFI></FinInstnId>
      </IntrmyAgt1>
      <Dbtr>
        <Nm>{{type:company}}</Nm>
        <PstlAdr>
          <Ctry>{{type:country}}</Ctry>
          <AdrLine>{{type:streetAddress}}</AdrLine>
        </PstlAdr>
      </Dbtr>
      <DbtrAcct>
        <Id><IBAN>{{type:iban:debtorCountry}}</IBAN></Id>
      </DbtrAcct>
      <DbtrAgt>
        <FinInstnId><BICFI>{{type:bic}}</BICFI></FinInstnId>
      </DbtrAgt>
      <CdtrAgt>
        <FinInstnId><BICFI>{{type:bic}}</BICFI></FinInstnId>
      </CdtrAgt>
      <Cdtr>
        <Nm>{{type:company}}</Nm>
        <PstlAdr>
          <Ctry>{{type:country}}</Ctry>
          <AdrLine>{{type:streetAddress}}</AdrLine>
        </PstlAdr>
      </Cdtr>
      <CdtrAcct>
        <Id><IBAN>{{type:iban:creditorCountry}}</IBAN></Id>
      </CdtrAcct>
      <Purp><Cd>{{type:purpose}}</Cd></Purp>
      <RmtInf>
        <Ustrd>INV-{{type:number:100000:999999}} / PO-{{type:number:10000:99999}} / {{type:quarter}} {{type:year}}</Ustrd>
      </RmtInf>
    </CdtTrfTxInf>
  </FIToFICstmrCdtTrf>
</Document>
```

### 3. Context-Aware Generation

The faker should maintain a **generation context** so that related placeholders produce consistent data:

```java
public class GenerationContext {
    private final Map<String, Object> variables = new HashMap<>();
    private final Map<String, String[]> bankCache = new HashMap<>();

    /**
     * When {{type:bic}} is first encountered, pick a random bank and cache it.
     * Subsequent {{type:bic}} in same template returns the SAME bank's BIC
     * (for consistency: debtor agent BIC = debtor bank BIC)
     */
    public String getBic(String role) {
        if (!bankCache.containsKey(role)) {
            bankCache.put(role, randomBank());
        }
        return bankCache.get(role)[1]; // BIC is index 1
    }

    /**
     * When {{type:country}} is called after {{type:bic}}, 
     * return the cached bank's country (not a random new one)
     */
    public String getCountry(String role) {
        String[] bank = bankCache.get(role);
        return bank != null ? bank[2] : randomCountry(); // Country is index 2
    }

    /**
     * Cross-reference: {{type:iban:debtorCountry}} uses the country 
     * from the debtor's bank, not a random country
     */
    public String getIban(String countryRef) {
        String country = resolveCountryRef(countryRef);
        return generateIban(country);
    }
}
```

### 4. Template Engine Interface

```java
public interface TemplateEngine {
    String process(Template template, GenerationContext context);
    boolean supports(String placeholderSyntax);
    void registerGenerator(String name, DataGenerator generator);
}

public interface DataGenerator {
    String generate(String... parameters);
    String getName();
}

// Built-in generators
public class UuidGenerator implements DataGenerator {
    public String generate(String... params) { return UUID.randomUUID().toString(); }
    public String getName() { return "uuid"; }
}

public class UetrGenerator implements DataGenerator {
    public String generate(String... params) { return UUID.randomUUID().toString(); }
    public String getName() { return "uetr"; }
}

public class AmountGenerator implements DataGenerator {
    public String generate(String... params) {
        double min = params.length > 0 ? Double.parseDouble(params[0]) : 1000;
        double max = params.length > 1 ? Double.parseDouble(params[1]) : 1000000;
        return String.format("%.2f", min + Math.random() * (max - min));
    }
    public String getName() { return "amount"; }
}

public class BankGenerator implements DataGenerator {
    private final Map<String, String[]> roleBanks = new HashMap<>();

    public String generate(String... params) {
        String role = params.length > 0 ? params[0] : "default";
        if (!roleBanks.containsKey(role)) {
            roleBanks.put(role, randomBank());
        }
        return roleBanks.get(role)[1]; // Return BIC
    }
    public String getName() { return "bic"; }

    public String[] getBank(String role) { return roleBanks.get(role); }
}
```

### 5. Smart Placeholder Resolution

```java
public class PlaceholderResolver {

    /**
     * Parse a placeholder like: {{type:amount:1000:50000:currency:USD}}
     * Into: type=amount, params=[1000, 50000], contextHint=currency:USD
     */
    public Placeholder parse(String placeholder) {
        // Remove {{ and }}
        String inner = placeholder.substring(2, placeholder.length() - 2);

        // Split by colon
        String[] parts = inner.split(":");

        // First part is always the type
        String type = parts[0];

        // Remaining parts are parameters
        List<String> params = Arrays.asList(Arrays.copyOfRange(parts, 1, parts.length));

        return new Placeholder(type, params);
    }

    /**
     * Resolve cross-references:
     * {{type:iban:debtorCountry}} → look up "debtor" bank's country from context
     * {{type:company:debtor}} → use "debtor" role (not random)
     */
    public String resolve(Placeholder ph, GenerationContext ctx) {
        DataGenerator generator = registry.get(ph.getType());

        // Check for role references in parameters
        String[] resolvedParams = ph.getParams().stream()
            .map(param -> resolveParam(param, ctx))
            .toArray(String[]::new);

        return generator.generate(resolvedParams);
    }

    private String resolveParam(String param, GenerationContext ctx) {
        // "debtorCountry" → look up debtor bank's country
        if (param.endsWith("Country")) {
            String role = param.replace("Country", "");
            return ctx.getCountry(role);
        }
        // "creditorBic" → look up creditor bank's BIC
        if (param.endsWith("Bic")) {
            String role = param.replace("Bic", "");
            return ctx.getBic(role);
        }
        return param;
    }
}
```

---

## 📁 Directory Structure

```
my-project/
├── src/
│   └── main/
│       └── java/
│           └── com/
│               └── example/
│                   ├── CBPRFaker.java              # Main entry point
│                   ├── TemplateDiscovery.java      # File scanner
│                   ├── TemplateEngine.java         # Mustache-style engine
│                   ├── GenerationContext.java      # Context-aware state
│                   ├── PlaceholderResolver.java    # {{...}} parser
│                   ├── generators/
│                   │   ├── DataGenerator.java      # Interface
│                   │   ├── UuidGenerator.java
│                   │   ├── UetrGenerator.java
│                   │   ├── AmountGenerator.java
│                   │   ├── BankGenerator.java
│                   │   ├── CompanyGenerator.java
│                   │   ├── DateGenerator.java
│                   │   ├── IbanGenerator.java
│                   │   ├── PriorityGenerator.java
│                   │   ├── PurposeGenerator.java
│                   │   └── CurrencyGenerator.java
│                   └── model/
│                       ├── Template.java
│                       ├── Placeholder.java
│                       └── BankData.java
├── templates/                                      # USER-EDITABLE TEMPLATES
│   ├── pacs.008.xml
│   ├── pacs.009.xml
│   ├── pacs.004.xml
│   ├── pain.001.xml
│   ├── camt.056.xml
│   ├── camt.029.xml
│   ├── camt.053.xml
│   ├── camt.054.xml
│   ├── mt103.txt                                   # SWIFT MT support too!
│   ├── mt202.txt
│   ├── mt940.txt
│   └── custom/
│       └── my-bank-format.xml                      # Custom templates
├── data/                                           # Seed data (optional)
│   ├── banks.json                                  # Custom bank list
│   ├── companies.json                              # Custom company list
│   └── purposes.json                               # Custom purpose codes
└── application.properties
```

---

## 🔄 Updated CBPRFaker API

```java
public class CBPRFaker {
    private final TemplateDiscovery discovery;
    private final TemplateEngine engine;
    private final GenerationContext context;

    public CBPRFaker() {
        this(Path.of("templates")); // Default: ./templates/
    }

    public CBPRFaker(Path templatesDir) {
        this.discovery = new TemplateDiscovery(templatesDir);
        this.engine = new TemplateEngine();
        this.context = new GenerationContext();

        // Auto-scan templates on startup
        this.discovery.scanTemplates();

        // Register all built-in generators
        registerDefaultGenerators();
    }

    /**
     * Generate a message by template name.
     * Looks for: templates/{messageType}.xml
     *            templates/{messageType}.txt
     *            templates/{messageType}.template
     */
    public String generate(String messageType) {
        Template template = discovery.getTemplate(messageType);
        if (template == null) {
            throw new IllegalArgumentException(
                "No template found for: '" + messageType + "'. " +
                "Expected file: templates/" + messageType + ".xml (or .txt/.template)"
            );
        }
        return engine.process(template, context.fork()); // Fresh context per generation
    }

    /**
     * Generate with explicit parameters that override template defaults
     */
    public String generate(String messageType, Map<String, Object> overrides) {
        Template template = discovery.getTemplate(messageType);
        GenerationContext ctx = context.fork();
        ctx.setOverrides(overrides);
        return engine.process(template, ctx);
    }

    /**
     * List all available templates
     */
    public List<String> listTemplates() {
        return discovery.getTemplateNames();
    }

    /**
     * Reload templates (call after adding new files)
     */
    public void reloadTemplates() {
        discovery.scanTemplates();
    }

    /**
     * Register a custom generator
     */
    public void registerGenerator(String name, DataGenerator generator) {
        engine.registerGenerator(name, generator);
    }

    private void registerDefaultGenerators() {
        engine.registerGenerator("uuid", new UuidGenerator());
        engine.registerGenerator("uetr", new UetrGenerator());
        engine.registerGenerator("amount", new AmountGenerator());
        engine.registerGenerator("bic", new BankGenerator());
        engine.registerGenerator("company", new CompanyGenerator());
        engine.registerGenerator("date", new DateGenerator());
        engine.registerGenerator("dateTime", new DateTimeGenerator());
        engine.registerGenerator("iban", new IbanGenerator());
        engine.registerGenerator("priority", new PriorityGenerator());
        engine.registerGenerator("purpose", new PurposeGenerator());
        engine.registerGenerator("currency", new CurrencyGenerator());
        engine.registerGenerator("country", new CountryGenerator());
        engine.registerGenerator("streetAddress", new StreetAddressGenerator());
        engine.registerGenerator("msgId", new MsgIdGenerator());
        engine.registerGenerator("uid", new UidGenerator());
        engine.registerGenerator("number", new NumberGenerator());
        engine.registerGenerator("quarter", new QuarterGenerator());
        engine.registerGenerator("year", new YearGenerator());
    }
}
```

---

## 🧪 Usage Examples

```java
// 1. Basic usage — auto-discovers templates/ folder
CBPRFaker faker = new CBPRFaker();

// Generate pacs.008 from templates/pacs.008.xml
String pacs008 = faker.generate("pacs.008");
System.out.println(pacs008);

// Generate pain.001 from templates/pain.001.xml
String pain001 = faker.generate("pain.001");
System.out.println(pain001);

// Generate MT103 from templates/mt103.txt
String mt103 = faker.generate("mt103");
System.out.println(mt103);

// 2. With custom templates directory
CBPRFaker faker = new CBPRFaker(Path.of("/path/to/custom/templates"));

// 3. With parameter overrides
Map<String, Object> overrides = Map.of(
    "amount", 500000.00,
    "currency", "EUR",
    "priority", "HIGH",
    "debtorBic", "DEUTDEDBFRA",
    "creditorBic", "BNPAFRPPXXX"
);
String customPacs = faker.generate("pacs.008", overrides);

// 4. List available templates
List<String> templates = faker.listTemplates();
// Returns: ["pacs.008", "pacs.009", "pacs.004", "pain.001", "camt.056", "camt.029", "mt103", "mt202"]

// 5. Add new template without code changes
// Just drop templates/my-new-format.xml into the folder
faker.reloadTemplates();
String newMsg = faker.generate("my-new-format");

// 6. Register custom generator
faker.registerGenerator("myCustomField", new DataGenerator() {
    public String generate(String... params) {
        return "CUSTOM-" + System.currentTimeMillis();
    }
    public String getName() { return "myCustomField"; }
});
```

---

## 📋 Sample Template Files to Include

### `templates/pacs.008.xml` (FI-to-FI Customer Credit Transfer)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.008.001.10">
  <FIToFICstmrCdtTrf>
    <GrpHdr>
      <MsgId>{{type:msgId}}</MsgId>
      <CreDtTm>{{type:dateTime:iso}}</CreDtTm>
      <NbOfTxs>1</NbOfTxs>
      <SttlmInf><SttlmMtd>CLRG</SttlmMtd></SttlmInf>
      <InstgAgt><FinInstnId><BICFI>{{type:bic:instg}}</BICFI></FinInstnId></InstgAgt>
      <InstdAgt><FinInstnId><BICFI>{{type:bic:instd}}</BICFI></FinInstnId></InstdAgt>
    </GrpHdr>
    <CdtTrfTxInf>
      <PmtId>
        <InstrId>{{type:uid:16}}</InstrId>
        <EndToEndId>{{type:uid:16}}</EndToEndId>
        <UETR>{{type:uetr}}</UETR>
      </PmtId>
      <PmtTpInf>
        <InstrPrty>{{type:priority}}</InstrPrty>
        <SvcLvl><Cd>SDVA</Cd></SvcLvl>
      </PmtTpInf>
      <IntrBkSttlmAmt Ccy="{{type:currency}}">{{type:amount:1000:1000000}}</IntrBkSttlmAmt>
      <IntrBkSttlmDt>{{type:date:yyyy-MM-dd}}</IntrBkSttlmDt>
      <ChrgBr>SHAR</ChrgBr>
      <IntrmyAgt1><FinInstnId><BICFI>{{type:bic:intrmy}}</BICFI></FinInstnId></IntrmyAgt1>
      <Dbtr>
        <Nm>{{type:company:debtor}}</Nm>
        <PstlAdr><Ctry>{{type:country:debtor}}</Ctry><AdrLine>{{type:streetAddress}}</AdrLine></PstlAdr>
      </Dbtr>
      <DbtrAcct><Id><IBAN>{{type:iban:debtorCountry}}</IBAN></Id></DbtrAcct>
      <DbtrAgt><FinInstnId><BICFI>{{type:bic:debtor}}</BICFI></FinInstnId></DbtrAgt>
      <CdtrAgt><FinInstnId><BICFI>{{type:bic:creditor}}</BICFI></FinInstnId></CdtrAgt>
      <Cdtr>
        <Nm>{{type:company:creditor}}</Nm>
        <PstlAdr><Ctry>{{type:country:creditor}}</Ctry><AdrLine>{{type:streetAddress}}</AdrLine></PstlAdr>
      </Cdtr>
      <CdtrAcct><Id><IBAN>{{type:iban:creditorCountry}}</IBAN></Id></CdtrAcct>
      <Purp><Cd>{{type:purpose}}</Cd></Purp>
      <RmtInf><Ustrd>INV-{{type:number:100000:999999}} / {{type:quarter}} {{type:year}}</Ustrd></RmtInf>
    </CdtTrfTxInf>
  </FIToFICstmrCdtTrf>
</Document>
```

### `templates/pacs.009.xml` (FI Credit Transfer)
```xml
<?xml version="1.0" encoding="UTF-8"?>
<Document xmlns="urn:iso:std:iso:20022:tech:xsd:pacs.009.001.10">
  <FICdtTrf>
    <GrpHdr>
      <MsgId>{{type:msgId}}</MsgId>
      <CreDtTm>{{type:dateTime:iso}}</CreDtTm>
      <NbOfTxs>1</NbOfTxs>
      <SttlmInf><SttlmMtd>CLRG</SttlmMtd></SttlmInf>
    </GrpHdr>
    <CdtTrfTxInf>
      <PmtId>
        <InstrId>{{type:uid:16}}</InstrId>
        <EndToEndId>{{type:uid:16}}</EndToEndId>
        <UETR>{{type:uetr}}</UETR>
      </PmtId>
      <PmtTpInf><InstrPrty>{{type:priority}}</InstrPrty></PmtTpInf>
      <IntrBkSttlmAmt Ccy="{{type:currency}}">{{type:amount:100000:50000000}}</IntrBkSttlmAmt>
      <IntrBkSttlmDt>{{type:date:yyyy-MM-dd}}</IntrBkSttlmDt>
      <ChrgBr>DEBT</ChrgBr>
      <InstgAgt><FinInstnId><BICFI>{{type:bic:instg}}</BICFI></FinInstnId></InstgAgt>
      <InstdAgt><FinInstnId><BICFI>{{type:bic:instd}}</BICFI></FinInstnId></InstdAgt>
      <Dbtr><FinInstnId><BICFI>{{type:bic:debtor}}</BICFI></FinInstnId></Dbtr>
      <Cdtr><FinInstnId><BICFI>{{type:bic:creditor}}</BICFI></FinInstnId></Cdtr>
    </CdtTrfTxInf>
  </FICdtTrf>
</Document>
```

### `templates/mt103.txt` (SWIFT MT Message)
```
{1:F01{{type:bic:sender}}XXXX{{type:date:yyyyMMdd}}{{type:number:1000:9999}}{{type:number:100000:999999}}}
{2:I103{{type:bic:receiver}}XXXXN}
{4:
:20:{{type:uid:16}}
:23B:CRED
:32A:{{type:date:yyMMdd}}{{type:currency}}{{type:amount:1000:1000000}},
:50K:/{{type:number:100000000:999999999}}
{{type:company:debtor}}
{{type:number:100:999}} {{type:streetAddress}}
{{type:city:debtorCountry}} {{type:number:10000:99999}}
:59:/{{type:number:100000000:999999999}}
{{type:company:creditor}}
{{type:number:100:999}} {{type:streetAddress}}
{{type:city:creditorCountry}} {{type:number:10000:99999}}
:71A:{{type:random:BEN,SHA,OUR}}
-}
{5:{CHK:{{type:uid:10}}ABC}}
```

---

## 🎨 Advanced Features

### 1. Conditional Blocks
```xml
{{?highValue}}
  <SplmtryData>
    <Envlp>
      <Cnts><HighValueInd>true</HighValueInd></Cnts>
    </Envlp>
  </SplmtryData>
{{/highValue}}
```

### 2. Loops for Multiple Transactions
```xml
<NbOfTxs>{{count}}</NbOfTxs>
{{#transactions}}
<CdtTrfTxInf>
  <PmtId><InstrId>{{type:uid:16}}</InstrId></PmtId>
  <IntrBkSttlmAmt Ccy="{{type:currency}}">{{type:amount:100:10000}}</IntrBkSttlmAmt>
</CdtTrfTxInf>
{{/transactions}}
```

### 3. Custom Data Files
```java
// Load custom banks from data/banks.json
public class CustomDataLoader {
    public List<BankData> loadBanks(Path file) {
        // JSON format: [{"name":"...","bic":"...","country":"...","city":"..."}]
        return objectMapper.readValue(file, new TypeReference<List<BankData>>() {});
    }
}
```

### 4. Template Inheritance
```xml
<!-- templates/base/cbpr-base.xml -->
<Document xmlns="urn:iso:std:iso:20022:tech:xsd:{{schema}}">
  <GrpHdr>
    <MsgId>{{type:msgId}}</MsgId>
    <CreDtTm>{{type:dateTime:iso}}</CreDtTm>
  </GrpHdr>
  {{content}}
</Document>

<!-- templates/pacs.008.xml extends base -->
{{extends:base/cbpr-base}}
{{schema:pacs.008.001.10}}
{{content}}
  <FIToFICstmrCdtTrf>
    ...
  </FIToFICstmrCdtTrf>
```

---

## ✅ Success Criteria

1. ✅ `CBPRFaker.generate("pacs.008")` reads from `templates/pacs.008.xml` automatically
2. ✅ Adding a new `templates/myformat.xml` works without code changes — just call `generate("myformat")`
3. ✅ All `{{type:...}}` placeholders are replaced with realistic fake data
4. ✅ Context-aware: `{{type:bic:debtor}}` and `{{type:country:debtor}}` return consistent data for the same role
5. ✅ Cross-references work: `{{type:iban:debtorCountry}}` uses the debtor's actual country
6. ✅ Template discovery auto-scans `templates/` folder on startup
7. ✅ `reloadTemplates()` picks up new files without restarting
8. ✅ Custom generators can be registered at runtime
9. ✅ Parameter overrides allow explicit values: `generate("pacs.008", Map.of("amount", 500000))`
10. ✅ Supports both XML (MX) and text (MT) templates
11. ✅ Built-in 20+ data generators (uuid, uetr, amount, bic, company, date, iban, etc.)
12. ✅ Clean separation: templates are user-editable files, code never changes for new formats

---

**Build this as a clean, extensible Java library with the existing CBPRFaker as the foundation. The key insight: templates are data, not code.**
