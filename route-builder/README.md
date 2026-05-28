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

## Current Features & Implementations

### Visual Diagram Designer
- **Infinite Canvas**: Unrestricted panning and zooming capabilities across an infinite workspace.
- **Intelligent Connectors**: Smooth, modern SVG Bézier curves for complex EIPs (Splits, Multicasts, Aggregators) rather than basic icons.
- **Geometric Arrows**: Pixel-perfect directional arrows for standard route connections.

### Project & File Explorer
- **Multi-File Selection**: Full support for `Ctrl+Click` and `Shift+Click` for batch selections.
- **Advanced Drag and Drop**: Robust D&D payload handling supporting mass-movement and mass-copy of multiple files/folders simultaneously.
- **Intelligent Collision Detection**: Automatically prevents file overwrite during copy operations by appending numbered suffixes (e.g., `file_1.yaml`).
- **Context Menu Operations**: Mass-Delete, Mass-Cut, Mass-Copy capabilities, and a context-aware **"Run Route(s)..."** submenu allowing developers to launch selected files or directories recursively under two runtime profiles: *Run Offline (Stub Mode)* and *Run Live (Standard)*.

### Code Editor & Workspace
- **Layout Management**: Fully customizable workspace through the `View` menu. Toggle visibility for the Explorer, Code Editor, and Diagram panels independently.
- **Panel Swapping**: Instantly swap the horizontal positions of the Code Editor and Diagram Canvas via the toolbar or View menu.
- **Dynamic Syntax Highlighting**: Reactive Monaco-style tokenization built directly into the UI text areas.
- **Integrated Language Server**: Boots alongside the IDE (`camel-lsp-server.jar`), pre-configured to utilize the **Quarkus** catalog provider for precise, framework-aware autocomplete and validation.

### Enterprise Connectivity & Security (Kamelet Studio Integration)
- **Unified Kamelet Connectors**:
  - **IBM MQ (Local & XA)**: Integrates JMS 3.0 client libraries (`com.ibm.mq.jakarta.client`) and pooled connection factories (`pooled-jms`) with Narayana JTA transaction controls.
  - **Solace PubSub+ (Local & XA)**: Supports plain SMF and secure SMFS TLS connection VPNs with certificate verification.
  - **MongoDB Connectors**: Aggregation pipeline-based Change Streams and mid-route CRUD actions (`findOneByQuery`, `update`, `bulkWrite`).
  - **SQL Action CRUD**: Dynamically compiles JSON request payloads into parameterized SQL statements (`select`, `update`, `delete`) via dynamic data sources.
- **Field-Level Cryptography**: PBKDF2 key derivation and AES-256-GCM authenticated encryption/decryption processor beans.
- **Canonical Audit Pipeline**: Built-in logs capturing operation steps, environment tags, dynamic node hostnames, and host IP addresses.
- **In-IDE Decrypt Tool**: Interactive JavaFX pane executing GCM salt/IV extraction and decryption verification.
- **Interactive Help Portal**: Embedded SplitPane utility containing a top search bar, left collapsible index tree, and right-hand documentation viewer rendering architecture diagrams and code templates.

## Next Steps & Roadmap

- **Phase 1: DB Changelog Generation** [COMPLETED]
  - Auto-generate Liquibase YAML changelogs for SQL tables and MongoDB JSON arrays upon project export.
- **Phase 2: Decrypt UI Panel** [COMPLETED]
  - Integrate a secure, in-IDE JavaFX Decrypt tool supporting PBKDF2 with HMAC-SHA256 and AES-256-GCM.
- **Phase 3: Interactive Help Portal** [COMPLETED]
  - Embedded collapsible SplitPane panel supporting real-time filter search and dark-mode HTML rendering of EIP scenarios.
- **Phase 4: Undo/Redo Engine** [NOT STARTED]
  - Provide multi-step Undo/Redo capabilities tracking file edits and canvas movements synchronously.
