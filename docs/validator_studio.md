# Universal Validator Studio

The **Universal Validator Studio** is an interactive workbench designed to write and test data validations across multiple standard financial and enterprise formats.

---

## 1. Supported Document Formats

The studio provides dedicated parsing and validation schemas for:
- **XML + XSD**: Standard XML document structures validated against schemas.
- **JSON + Schema**: Payload structures checked against JSON Schema draft-07 constraints.
- **YAML + Schema**: Configuration structures checked against JSON schema validators.
- **SWIFT MT Message**: Standard financial SWIFT message checks (such as Block structures and tag formatting).
- **ISO 20022 MX**: Financial XML pacs, pain, and camt formats checked against MX schemas.
- **CSV + CSVW**: Tabular spreadsheets validated using CSV on the Web metadata schemas.
- **Flat File**: Fixed-width and delimited text layouts parsed and checked via schema indices.

---

## 2. Standard vs. Advanced SWIFT Mode

- **Standard Mode**: Performs structural parsing. Validates Block 1 (BIC checks), Block 2, Block 4 format rules, and basic field syntax rules (e.g., tag field lengths and character sets).
- **Advanced Mode**: Injects a custom JSON ruleset to evaluate jurisdiction exclusions, transactional balance bounds, and risk blocks.
  - *Example*: Blocks transactions referencing high-risk countries like `"Iran"` or amounts exceeding `$10,000,000` via a custom rules configuration.

---

## 3. Scenario & Pair Managers

The studio implements robust save and edit synchronization controls:
- **Scenario Save/Overwrite**: Persists edits made in both the left editor (active payload) and the middle editor (schema/validation rules) in a single action.
- **Inline Schema Overwrite**: HBox header upload button that allows overwrite of the active schema with any external document instantly.
- **Update Validation Pair**: Context-menu action on the explorer tree that launches a configuration dialog to update scenario names, file targets, and format type mappings on the fly.
