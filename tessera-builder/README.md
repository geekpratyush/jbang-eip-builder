# Camel Quarkus Route Builder IDE

An industrial-grade, visual development environment for building and orchestrating Camel Quarkus integration routes.

---

## How to Run the Application

This is a self-contained distribution package. To start the Route Builder IDE:

### 1. Requirements
* **Java Runtime**: Make sure you have **Java 21** (or newer) installed on your system and set in your `PATH`. Check this by running `java -version` in your terminal.

### 2. Launching the Application
* **On Linux/macOS:**
  1. Open a terminal in the extracted folder.
  3. Run the launch script:
     ```bash
     ./bin/route-builder
     ```
* **On Windows:**
  1. Navigate to the extracted folder.
  2. Double-click the launch script:
     `bin/route-builder.bat`

### 3. What's Included Out-of-the-Box
* **Bundled JBang Runtime**: Execution profiles automatically resolve the packaged offline JBang runtime, meaning you can run and prototype Camel Quarkus routes without having to install JBang on your system.
* **Integrated Language Server**: Code editors automatically connect to the bundled Camel Language Server for autocomplete suggestions.
* **Sample Routes**: Check the `routes/` directory for ready-to-test YAML integration examples.
* **User Manual**: Access the comprehensive manual directly inside the IDE by clicking the **Manual** button in the help portal.

---

## Feature Documentation Index

For detailed guides, architecture diagrams, and configurations of specific IDE components, see the separate feature guides:

1. **[Visual Route Designer](../docs/visual_route_designer.md)**: Real-time Monaco YAML editors, infinite canvas, and hot reloading EIP diagrams.
2. **[Custom Kamelet Builder Studio](../docs/kamelet_studio.md)**: Step-by-step chapter tutorials, parameter forms generation, and JBang test runners.
3. **[Managed Dependency Catalog](../docs/dependency_catalog.md)**: Inline coordinates tables, wildcard Maven injections, and Monaco header writers.
4. **[Universal Validator Studio](../docs/validator_studio.md)**: Standard & Advanced SWIFT parsing, XML schemas (XSD), CSVW metadata, and validator pairs overrides.
5. **[Data Transformation & XSLT Studio](../docs/data_transformation_xslt.md)**: Structural converters, dynamic output previews, and two-tree drag-and-drop XSLT mapping lines.
6. **[Faker Mock & Template Studio](../docs/faker_mock_studio.md)**: Tokenized JSON/YAML mock generators and high-performance direct-to-disk unpacking.
7. **[Cryptography & Security Studio](../docs/crypto_studio.md)**: PBKDF2 salt derivation, AES-256-GCM symmetric bean processors, and manual Decrypt UI verifiers.
8. **[Project Explorer & Database Export](../docs/project_explorer_exports.md)**: Recursive trees, batch runs, drag-and-drop file operations, and Liquibase database changelog exporters.
9. **[Workspace Variables Editor](../docs/variables_editor.md)**: Environment properties, placeholders, overrides, and automated properties exports.
10. **[In-App Help Guide Portal](../docs/help_guide_portal.md)**: Searchable indexing, category trees, HTML viewer, and context-aware toolbar links.
11. **[Dynamic Route & Kamelet Loader](../docs/camel-management.md)**: Design blueprint for loading routes dynamically from SQL, MongoDB, and filesystems with TLS/mTLS parameters.
12. **[Remote Deploy Center & Connectivity Monitor](../docs/camel-management.md#14-heartbeat-api-client-connectivity-status--environment-config-toggles)**: Remote deployment dialog to push routes/transformers, poll container heartbeat (GET `/admin/heartbeat`), and display green/red status indicators with warning drop alerts.

---

## Remote Deploy & Heartbeat Connectivity

The Route Builder Studio includes a integrated **Remote Deploy & Run Test** facility that communicates with running Camel Quarkus containers:

*   **Deploy Operations**: Push Camel routes (YAML/XML/Java) and transform schemas (XSLT/Smooks/JSLT) directly to the target environment.
    *   *Persistent Strategy*: Uploads and seeds database engines (SQL and MongoDB).
    *   *Temporary Strategy*: Loads routes directly into memory via Camel's `RoutesLoader` and writes templates to temporary sandbox directories on the container disk.
*   **Connectivity Heartbeat (`GET /admin/heartbeat`)**: Every 5 seconds, the studio pings the remote container:
    *   **Online (Green)**: UI connection icon turns green; sandbox deployment remains active.
    *   **Offline (Red)**: UI connection icon turns red, and the client displays a connection lost warning alert.
*   **Environment Config Toggles**: Protect production systems by disabling management endpoints:
    ```properties
    # Deactivate /admin/* upload/health endpoints
    loader.management.api.enabled=false
    ```

---

## Next Steps & Roadmap

- **Phase 1: DB Changelog Generation** [COMPLETED]
  - Auto-generate Liquibase YAML changelogs for SQL tables and MongoDB JSON arrays upon project export.
- **Phase 2: Decrypt UI Panel** [COMPLETED]
  - Integrate a secure, in-IDE JavaFX Decrypt tool supporting PBKDF2 with HMAC-SHA256 and AES-256-GCM.
- **Phase 3: Interactive Help Portal & Remote Monitoring** [COMPLETED]
  - Collapsible Search Panel, dark-mode HTML renderers, Remote Deploy controls, and Live Heartbeat status indicators.
- **Phase 4: Undo/Redo Engine** [NOT STARTED]
  - Provide multi-step Undo/Redo capabilities tracking file edits and canvas movements synchronously.
