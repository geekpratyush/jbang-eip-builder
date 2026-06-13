package com.tessera.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.ArrayList;
import java.util.List;

public class HelpPortalPane extends BorderPane {

    public static class HelpTopic {
        public String title;
        public String category;
        public String markdownContent;

        public HelpTopic(String title, String category, String markdownContent) {
            this.title = title;
            this.category = category;
            this.markdownContent = markdownContent;
        }

        @Override
        public String toString() {
            return "[" + category + "] " + title;
        }
    }

    private List<HelpTopic> allTopics = new ArrayList<>();
    private ListView<HelpTopic> topicListView;
    private WebView webView;
    private TextField searchField;
    private Runnable onCloseHandler;

    public HelpPortalPane(Runnable onCloseHandler) {
        this.onCloseHandler = onCloseHandler;
        getStyleClass().add("help-portal-pane");
        setPrefWidth(650);

        // Header / Search Bar
        HBox header = new HBox(10);
        header.setPadding(new Insets(10));
        header.getStyleClass().add("help-header");
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label title = new Label("Interactive Help Portal");
        title.getStyleClass().add("help-title");

        searchField = new TextField();
        searchField.setPromptText("Search by topic, keyword, or component...");
        searchField.setPrefWidth(220);
        HBox.setHgrow(searchField, Priority.ALWAYS);
        searchField.getStyleClass().add("help-search-field");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterTopics(newVal));

        Button btnClose = new Button();
        btnClose.setGraphic(new FontIcon("fas-times"));
        btnClose.setTooltip(new Tooltip("Close Help Portal"));
        btnClose.getStyleClass().add("help-close-btn");
        btnClose.setOnAction(e -> {
            if (onCloseHandler != null) onCloseHandler.run();
        });

        header.getChildren().addAll(title, searchField, btnClose);
        setTop(header);

        // Left topics index vs Right contents webview
        SplitPane splitPane = new SplitPane();
        splitPane.setOrientation(Orientation.HORIZONTAL);
        splitPane.getStyleClass().add("help-split-pane");

        topicListView = new ListView<>();
        topicListView.getStyleClass().add("help-topic-list");
        // Custom cells for list view to keep dark mode style
        topicListView.setCellFactory(lv -> new ListCell<>() {
            @Override
            protected void updateItem(HelpTopic item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item.title);
                    setTooltip(new Tooltip(item.category));
                }
            }
        });

        webView = new WebView();
        webView.getStyleClass().add("help-web-view");

        splitPane.getItems().addAll(topicListView, webView);
        splitPane.setDividerPositions(0.35);

        setCenter(splitPane);

        loadHelpTopics();
        
        topicListView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                webView.getEngine().loadContent(markdownToHtml(newVal.markdownContent));
            }
        });

        // Default to first topic
        if (!allTopics.isEmpty()) {
            topicListView.getSelectionModel().select(0);
        }
    }

    private void filterTopics(String query) {
        ObservableList<HelpTopic> filtered = FXCollections.observableArrayList();
        for (HelpTopic topic : allTopics) {
            if (query.isEmpty() || 
                topic.title.toLowerCase().contains(query.toLowerCase()) || 
                topic.markdownContent.toLowerCase().contains(query.toLowerCase()) ||
                topic.category.toLowerCase().contains(query.toLowerCase())) {
                filtered.add(topic);
            }
        }
        topicListView.setItems(filtered);
    }

    private String currentTheme = "VSCode Dark";

    public void setTheme(String theme) {
        this.currentTheme = theme;
        // Refresh current topic if one is selected
        HelpTopic selected = topicListView.getSelectionModel().getSelectedItem();
        if (selected != null) {
            webView.getEngine().loadContent(markdownToHtml(selected.markdownContent));
        }
    }

    private String markdownToHtml(String markdown) {
        org.commonmark.parser.Parser parser = org.commonmark.parser.Parser.builder().build();
        org.commonmark.node.Node document = parser.parse(markdown);
        org.commonmark.renderer.html.HtmlRenderer renderer = org.commonmark.renderer.html.HtmlRenderer.builder().build();
        String bodyHtml = renderer.render(document);
        
        String bgColor = "#1e1e1e";
        String textColor = "#d4d4d4";
        String headerColor = "#4CAF50";
        String subHeaderColor = "#569cd6";
        String codeBg = "#2d2d2d";
        String borderColor = "#3c3c3c";

        if ("IntelliJ Light".equalsIgnoreCase(currentTheme)) {
            bgColor = "#ffffff";
            textColor = "#333333";
            headerColor = "#2e7d32";
            subHeaderColor = "#1565c0";
            codeBg = "#f3f3f3";
            borderColor = "#cccccc";
        } else if ("Dracula".equalsIgnoreCase(currentTheme)) {
            bgColor = "#282a36";
            textColor = "#f8f8f2";
            headerColor = "#bd93f9";
            subHeaderColor = "#8be9fd";
            codeBg = "#44475a";
            borderColor = "#6272a4";
        } else if ("Monokai".equalsIgnoreCase(currentTheme)) {
            bgColor = "#272822";
            textColor = "#f8f8f2";
            headerColor = "#a6e22e";
            subHeaderColor = "#66d9ef";
            codeBg = "#3e3d32";
            borderColor = "#75715e";
        } else if ("Hacker".equalsIgnoreCase(currentTheme)) {
            bgColor = "#050505";
            textColor = "#00ff00";
            headerColor = "#00ff00";
            subHeaderColor = "#00cc00";
            codeBg = "#001a00";
            borderColor = "#004d00";
        }
        boolean isLight = "IntelliJ Light".equalsIgnoreCase(currentTheme);
        
        return "<html><head><style>" +
               "body { font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, sans-serif; background-color: " + bgColor + "; color: " + textColor + "; padding: 15px; line-height: 1.6; }" +
               "h1 { color: " + headerColor + "; border-bottom: 1px solid " + borderColor + "; padding-bottom: 5px; font-size: 1.5em; margin-top: 0; }" +
               "h2 { color: " + subHeaderColor + "; border-bottom: 1px solid " + borderColor + "; padding-bottom: 3px; font-size: 1.25em; }" +
               "h3 { color: #ce9178; font-size: 1.1em; }" +
               "code { font-family: 'Consolas', 'Courier New', monospace; background-color: " + codeBg + "; color: " + subHeaderColor + "; padding: 2px 5px; border-radius: 3px; font-size: 0.9em; }" +
               "pre { background-color: " + codeBg + "; border: 1px solid " + borderColor + "; border-radius: 4px; padding: 10px; overflow-x: auto; font-family: 'Consolas', 'Courier New', monospace; }" +
               "pre code { background-color: transparent; color: " + textColor + "; padding: 0; }" +
               "table { border-collapse: collapse; width: 100%; margin: 15px 0; font-size: 0.9em; }" +
               "th, td { border: 1px solid " + borderColor + "; padding: 8px; text-align: left; }" +
               "th { background-color: " + codeBg + "; color: " + headerColor + "; }" +
               "blockquote { border-left: 4px solid " + headerColor + "; background-color: " + codeBg + "; margin: 10px 0; padding: 10px; color: #b5cea8; }" +
               "hr { border: 0; border-top: 1px solid " + borderColor + "; margin: 20px 0; }" +
               "ul { padding-left: 20px; }" +
               "li { margin-bottom: 5px; }" +
               "</style></head><body>" + bodyHtml + 
               "<script src=\"https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.38.0/min/vs/loader.min.js\"></script>\n" +
               "<script>\n" +
               "if (typeof require !== 'undefined') {\n" +
               "  require.config({ paths: { 'vs': 'https://cdnjs.cloudflare.com/ajax/libs/monaco-editor/0.38.0/min/vs' }});\n" +
               "  require(['vs/editor/editor.main'], function() {\n" +
               "    var isLight = " + isLight + ";\n" +
               "    var themeName = isLight ? 'custom-light' : 'custom-dark';\n" +
               "    monaco.editor.defineTheme('custom-dark', {\n" +
               "      base: 'vs-dark',\n" +
               "      inherit: true,\n" +
               "      rules: [],\n" +
               "      colors: {\n" +
               "        'editor.background': '" + bgColor + "',\n" +
               "        'editorGutter.background': '" + bgColor + "'\n" +
               "      }\n" +
               "    });\n" +
               "    monaco.editor.defineTheme('custom-light', {\n" +
               "      base: 'vs',\n" +
               "      inherit: true,\n" +
               "      rules: [],\n" +
               "      colors: {\n" +
               "        'editor.background': '" + bgColor + "',\n" +
               "        'editorGutter.background': '" + bgColor + "'\n" +
               "      }\n" +
               "    });\n" +
               "    document.querySelectorAll('pre').forEach(function(pre) {\n" +
               "      var codeEl = pre.querySelector('code');\n" +
               "      var lang = 'plaintext';\n" +
               "      var codeText = '';\n" +
               "      if (codeEl) {\n" +
               "        codeText = codeEl.textContent;\n" +
               "        if (codeEl.className) {\n" +
               "          var match = codeEl.className.match(/language-(\\\\w+)/);\n" +
               "          if (match) lang = match[1];\n" +
               "        }\n" +
               "      } else {\n" +
               "        codeText = pre.textContent;\n" +
               "      }\n" +
               "      pre.innerHTML = '';\n" +
               "      pre.style.padding = '0';\n" +
               "      pre.style.border = '1px solid " + borderColor + "';\n" +
               "      pre.style.overflow = 'hidden';\n" +
               "      var lineCount = codeText.split('\\n').length;\n" +
               "      var height = Math.max(45, Math.min(600, lineCount * 19 + 10));\n" +
               "      pre.style.height = height + 'px';\n" +
               "      monaco.editor.create(pre, {\n" +
               "        value: codeText,\n" +
               "        language: lang,\n" +
               "        theme: themeName,\n" +
               "        readOnly: true,\n" +
               "        automaticLayout: true,\n" +
               "        minimap: { enabled: false },\n" +
               "        fontSize: 12,\n" +
               "        fontFamily: 'monospace',\n" +
               "        lineHeight: 19,\n" +
               "        scrollBeyondLastLine: false,\n" +
               "        scrollbar: {\n" +
               "          vertical: 'visible',\n" +
               "          horizontal: 'visible',\n" +
               "          useShadows: false,\n" +
               "          verticalScrollbarSize: 8,\n" +
               "          horizontalScrollbarSize: 8\n" +
               "        }\n" +
               "      });\n" +
               "    });\n" +
               "  }, function(err) {\n" +
               "    console.log('Monaco load failed, using fallback standard styling.', err);\n" +
               "  });\n" +
               "}\n" +
               "</script>\n" +
               "</body></html>";
    }

    private void loadHelpTopics() {
        allTopics.add(new HelpTopic("1. Getting Started & JBang Run profiles", "General",
            "# Developer Environments & Execution Profiles\n" +
            "The studio lets you run integrations locally under three runtime profiles using JBang and Camel Main:\n\n" +
            "```\n" +
            "[Kamelet Studio UI] ---> [Local Workspace Dir] ---> [JBang Executable]\n" +
            "                                                           |\n" +
            "                                        +------------------+------------------+\n" +
            "                                        |                  |                  |\n" +
            "                                [Offline / Stub]       [Local Live]\n" +
            "                                (--stub=all)         (Standard run)\n" +
            "```\n\n" +
            "## 1. Play Offline (Stub Mode)\n" +
            "Stubs out all external endpoints using Apache Camel's stub component. Recommended for local route structure testing without external infrastructure dependency.\n\n" +
            "## 2. Play (Local Live)\n" +
            "Runs the integration live on Camel Main, reflecting changes in real-time. Great for verification against local instances."
        ));

        allTopics.add(new HelpTopic("2. IBM MQ Connector (JMS 3.0 & XA)", "Messaging",
            "# IBM MQ Integration (Jakarta JMS 3.0)\n" +
            "The studio supports connecting to IBM MQ using the modern Jakarta JMS client.\n\n" +
            "## Maven/Gradle Dependency\n" +
            "```groovy\n" +
            "implementation 'com.ibm.mq:com.ibm.mq.jakarta.client:9.4.5.1'\n" +
            "```\n\n" +
            "## Distributed XA JTA Configuration\n" +
            "When using XA connection factories (`MQXAConnectionFactory` and `JmsPoolXAConnectionFactory`) bound to Narayana Transaction Managers, ensure the following local settings are applied:\n\n" +
            "* `transacted: false`\n" +
            "* `cacheLevelName: CACHE_NONE`\n\n" +
            "> **Note:** Distributed transaction scopes are managed by the JTA Manager rather than local session caching layers. Session caching under XA leads to connection leaks and lockouts."
        ));

        allTopics.add(new HelpTopic("3. Solace PubSub+ Integration", "Messaging",
            "# Solace PubSub+ (SMF/SMFS)\n" +
            "The studio enables reliable connection parameters to Solace brokers using SMF (`smf://`) or secure SMFS (`smfs://`) protocols.\n\n" +
            "## Connection Schema Parameters\n" +
            "| Parameter | Description | Required For |\n" +
            "|---|---|---|\n" +
            "| `brokerUrl` | Broker SMF/SMFS Address | All Connections |\n" +
            "| `sslTrustStore` | Absolute path to SSL truststore | SMFS One-way SSL |\n" +
            "| `sslKeyStore` | Absolute path to client keystore | SMFS Mutual TLS (mTLS) |\n" +
            "| `sslKeyStorePassword` | Client keystore password | SMFS Mutual TLS (mTLS) |\n\n" +
            "## Configuration Example\n" +
            "```yaml\n" +
            "- route:\n" +
            "    id: solace-consumer\n" +
            "    from:\n" +
            "      uri: \"solace:queue:my-queue\"\n" +
            "      steps:\n" +
            "        - to: \"log:received-from-solace\"\n" +
            "```"
        ));

        allTopics.add(new HelpTopic("4. Apache Kafka (SSL & Kerberos)", "Messaging",
            "# Apache Kafka SSL & Kerberos Security\n" +
            "Configure Kafka endpoints with standard TLS security or enterprise Kerberos (SASL/GSSAPI) settings.\n\n" +
            "## 1. mTLS Configuration\n" +
            "Ensure keystores and truststores are resolved securely via external properties:\n" +
            "```properties\n" +
            "camel.component.kafka.security-protocol=SSL\n" +
            "camel.component.kafka.ssl-truststore-location=/secrets/truststore.jks\n" +
            "camel.component.kafka.ssl-truststore-password={{vault:kafka-ts-pass}}\n" +
            "camel.component.kafka.ssl-keystore-location=/secrets/keystore.jks\n" +
            "camel.component.kafka.ssl-keystore-password={{vault:kafka-ks-pass}}\n" +
            "```\n\n" +
            "## 2. GSSAPI/Kerberos Configuration\n" +
            "Attach external system configurations:\n" +
            "```properties\n" +
            "camel.component.kafka.sasl-mechanism=GSSAPI\n" +
            "camel.component.kafka.security-protocol=SASL_SSL\n" +
            "camel.component.kafka.sasl-jaas-config=com.sun.security.auth.module.Krb5LoginModule required useKeyTab=true storeKey=true keyTab=\"/secrets/client.keytab\" principal=\"camel-service@REALM.COM\";\n" +
            "```"
        ));

        allTopics.add(new HelpTopic("5. MongoDB Change Streams & Auditing", "NoSQL",
            "# MongoDB Change Streams and Dynamic Auditing\n" +
            "Capture database mutations dynamically and write structural audits.\n\n" +
            "## 1. Change Stream Filter Criteria\n" +
            "Filter MongoDB stream updates using JSON-formatted criteria:\n" +
            "```yaml\n" +
            "- from:\n" +
            "    uri: \"mongodb:myDb?consumerType=changeStream&database=audit&collection=orders\"\n" +
            "    parameters:\n" +
            "      streamFilter: '{\"operationType\": {\"$in\": [\"insert\", \"update\"]}}'\n" +
            "```\n\n" +
            "## 2. Host Audit Processing Metadata\n" +
            "The audit component resolves the hostname and IP of the executor machine automatically to log origin data.\n" +
            "Exclusion properties are respected:\n" +
            "```properties\n" +
            "# Exclude hostname or specific payload values from logs for privacy:\n" +
            "audit.exclude.fields=password,credit_card,hostname\n" +
            "```"
        ));

        allTopics.add(new HelpTopic("6. SQL Dynamic CRUD Engine", "RDBMS",
            "# SQL Dynamic CRUD Engine\n" +
            "Use Camel's `sql:dynamic` component to map REST JSON payloads directly to database operations.\n\n" +
            "## Dynamic Insert & Update Flow\n" +
            "```\n" +
            "   [HTTP POST JSON] ---> [Jackson Map Deserializer] ---> [Groovy Query Compiler] ---> [sql:dynamic]\n" +
            "```\n\n" +
            "## Route Definition Snippet\n" +
            "```yaml\n" +
            "- route:\n" +
            "    id: dynamic-insert-route\n" +
            "    from:\n" +
            "      uri: \"direct:insert-user\"\n" +
            "      steps:\n" +
            "        - unmarshal:\n" +
            "            json: {}\n" +
            "        - setBody:\n" +
            "            simple: \"INSERT INTO users (name, email) VALUES (:?name, :?email)\"\n" +
            "        - to: \"sql:dynamic\"\n" +
            "```"
        ));

        allTopics.add(new HelpTopic("7. AES-256-GCM Cryptographic Tool", "Security",
            "# AES-256-GCM Base64 Decrypt Tool\n" +
            "The studio provides a built-in cryptographic tool window to decrypt configuration secrets safely in the IDE.\n\n" +
            "## Protocol Specifications\n" +
            "* **Key Derivation Algorithm:** `PBKDF2WithHmacSHA256`\n" +
            "* **PBKDF2 Iteration Count:** `65536`\n" +
            "* **Salt length:** `16 bytes` (prefixed to ciphertext)\n" +
            "* **GCM IV length:** `12 bytes` (prefixed next)\n" +
            "* **Authenticating Tag size:** `128 bits`\n\n" +
            "## To Use the Tool:\n" +
            "1. Click the **Decrypt** button on the toolbar or select **Edit -> Decrypt Ciphertext...**\n" +
            "2. Enter the encryption password.\n" +
            "3. Paste the Base64 ciphertext into the payload text box.\n" +
            "4. Click **Decrypt Payload** to review plaintext secrets."
        ));

        allTopics.add(new HelpTopic("8. Path to Production & Containers", "DevOps",
            "# Path to Production & Containerization\n" +
            "Transition your routes from local JBang testing into enterprise microservices.\n\n" +
            "## 1. Native Build Compilation\n" +
            "Build a native Linux container binary using Maven and GraalVM:\n" +
            "```bash\n" +
            "mvn package -Pnative\n" +
            "```\n" +
            "Native builds compile JVM code into standalone native code, reducing startup time to <10ms and RSS memory footprint to <30MB.\n\n" +
            "## 2. Secrets Management\n" +
            "**Important:** Never bundle credentials (keystores, passwords, krb5 keytabs) in container filesystems. Resolve them dynamically at runtime via:\n" +
            "* Kubernetes ConfigMaps & Secrets mounts.\n" +
            "* HashiCorp Vault or Cloud Secret Managers via environment variables."
        ));

        allTopics.add(new HelpTopic("9. Validation Studio", "Validation",
            "# Validation Studio User Manual\n\n" +
            "Validation Studio validates messages of various formats against schemas or custom rules.\n\n" +
            "## Supported Formats\n" +
            "* **XML + XSD**: Standard XML instance schema matching.\n" +
            "* **JSON + Schema**: Property-level validation against draft-07 JSON Schema.\n" +
            "* **YAML + Schema**: Automated translation and JSON Schema evaluation.\n" +
            "* **SWIFT MT Message**: Standard syntax validation & custom JSON rules.\n" +
            "* **ISO 20022 MX**: ISO standard XSD schema matching.\n" +
            "* **CSV + CSVW**: Table-level data validation.\n" +
            "* **Flat File**: Parses and checks fixed-width columns using character offset layouts.\n\n" +
            "## Executing Validations\n" +
            "1. Select a validation scenario from the sidebar tree.\n" +
            "2. Click **Validate** in the toolbar or press **F5**.\n" +
            "3. Analyze the results in the Validation Report editor."
        ));

        allTopics.add(new HelpTopic("10. Crypto Studio", "Security",
            "# Crypto Studio\n\n" +
            "Crypto Studio manages encryption and decryption of configuration secrets.\n\n" +
            "## Cryptographic Specifications\n" +
            "* **Algorithm**: `AES-256-GCM`\n" +
            "* **Key Derivation**: `PBKDF2WithHmacSHA256` (65,536 iterations)\n" +
            "* **Salt / IV**: `16 bytes` salt & `12 bytes` IV prefixed to ciphertext.\n\n" +
            "## Using the Decryption Tool\n" +
            "1. Select **Edit -> Decrypt Ciphertext...** from the menu.\n" +
            "2. Enter the secret password.\n" +
            "3. Paste the Base64 ciphertext into the payload text box.\n" +
            "4. Click **Decrypt Payload** to review plaintext secrets."
        ));

        allTopics.add(new HelpTopic("11. Schema Mapping Studio (MAP)", "Transformation",
            "# Schema Mapping Studio (MAP)\n\n" +
            "The MAP Studio defines mappings between message structures and schemas.\n\n" +
            "## Core Features\n" +
            "* **Visual Mapping**: Drag-and-drop source tree properties onto target tree properties.\n" +
            "* **Live Flowchart**: Renders active relationship structures using Mermaid.js flowcharts.\n" +
            "* **Registry Sync**: Updates are written to the global registry `validation-mapping.json`."
        ));

        allTopics.add(new HelpTopic("12. Transformation Studio (Transform)", "Transformation",
            "# Transformation Studio (Transform)\n\n" +
            "Transformation Studio converts messages between different messaging formats.\n\n" +
            "## Supported Engines\n" +
            "* **Smooks**: High-performance streaming transformation engine.\n" +
            "* **FreeMarker (FTL)**: Generates text outputs (XML, JSON) using template parameters.\n" +
            "* **JSLT**: Declarative JSON-to-JSON mapper.\n" +
            "* **Groovy Scripting**: Evaluates dynamic mapping scripts.\n" +
            "* **jOOR Java Mapper**: Compiles custom Java mappings at runtime."
        ));

        allTopics.add(new HelpTopic("13. Diagram Studio", "Visualization",
            "# Diagram Studio\n\n" +
            "Diagram Studio renders Apache Camel integration routes as flowcharts.\n\n" +
            "## Key Features\n" +
            "* **Mermaid.js Flowcharts**: Renders routes automatically as you type.\n" +
            "* **Endpoint Visuals**: Displays custom icons representing databases, messaging queues, and HTTP endpoints.\n" +
            "* **Logical Flow Visualizer**: Formats routing steps (Splitters, Aggregators, Filters) as decision nodes."
        ));

        allTopics.add(new HelpTopic("14. Faker Studio", "Simulation",
            "# Faker Studio\n\n" +
            "Faker Studio generates mock datasets to simulate transaction workloads.\n\n" +
            "## Features\n" +
            "* **Data-Driven Templates**: Inject synthetic properties using double-braces:\n" +
            "  * `{{name.fullName}}` -> Random full names.\n" +
            "  * `{{finance.iban}}` / `{{finance.bic}}` -> Bank identifiers.\n" +
            "* **Continuous Simulation**: Streams generated files to directories at custom rates."
        ));

        allTopics.add(new HelpTopic("15. Export Studio & DB Mappings", "Export",
            "# Export Studio (Liquibase, SQL, File System)\n\n" +
            "Export Studio packages Camel integrations and databases for target environments.\n\n" +
            "## Database & Storage Exports\n\n" +
            "### 1. Liquibase Migration Export\n" +
            "Packages database schema migrations into Liquibase XML changelogs (`changelog.xml`) for Oracle, Postgres, and SQL database engines.\n\n" +
            "### 2. SQL Database Export\n" +
            "Generates DDL and DML scripts tailored for PostgreSQL or Oracle database engines.\n\n" +
            "### 3. File System Export\n" +
            "Generates complete directory structures containing runnable shell scripts (`run.sh` / `run.bat`), properties files, and routing configurations."
        ));

        allTopics.add(new HelpTopic("16. Interactive Examples & Architecture Guide", "Architecture",
            "# Comprehensive Architecture & Examples Guide\n\n" +
            "This guide walks you through the core architectural arrangement of Tessera's sample projects, showing you how they reference assets, execute routing from start to finish, and leverage embedded databases (MongoDB, H2) and simulation tools (Faker, Timer, MQ/Kafka Stubs).\n\n" +
            "## 1. Project Arrangement & Asset Referencing\n" +
            "Tessera workspaces follow a decoupled, modular design. Routes and their supporting files are strictly separated:\n\n" +
            "* **Camel Routes Directory (`camel/`)**: Contains the YAML DSL definitions for your routes. E.g., `camel/chapter-16-ui-ux/09-sql-workbench.camel.yaml`.\n" +
            "* **Assets Directory (`assets/`)**: Contains static HTML, CSS, JavaScript, and images. E.g., `assets/chapter-16-ui-ux/09-sql-workbench/ui/index.html`.\n" +
            "* **Referencing Mechanism**: Camel routes use the `platform-http` or `jetty` components to serve these static assets directly from the file system.\n" +
            "```yaml\n" +
            "- route:\n" +
            "    from: \"platform-http:/?matchOnUriPrefix=true\"\n" +
            "    steps:\n" +
            "      - to: \"file://{{WORKSPACE_ROOT_DIR}}/assets/chapter-16-ui-ux/09-sql-workbench/ui\"\n" +
            "```\n" +
            "This decoupling ensures that front-end logic (HTML/JS) and integration logic (Camel/SQL) remain independent.\n\n" +
            "## 2. In-Memory Databases: MongoDB to H2 SQL\n" +
            "Tessera can seamlessly bootstrap embedded, offline databases to support local development.\n\n" +
            "### H2 Embedded SQL Database\n" +
            "The H2 database runs directly in-memory and can be initialized with SQL DDL/DML statements at startup:\n" +
            "```yaml\n" +
            "- route:\n" +
            "    id: \"sql-workbench-init\"\n" +
            "    from: \"timer:init?repeatCount=1\"\n" +
            "    steps:\n" +
            "      - to: \"sql:CREATE SCHEMA IF NOT EXISTS core\"\n" +
            "      - to: \"sql:CREATE TABLE IF NOT EXISTS core.credit_earmarks (earmark_id VARCHAR(50), status VARCHAR(50))\"\n" +
            "      - to: \"sql:INSERT INTO core.credit_earmarks VALUES ('EMK-7011', 'APPROVED')\"\n" +
            "```\n\n" +
            "### MongoDB (Stubbed/Embedded)\n" +
            "Similarly, when you need NoSQL capabilities, you can interface with MongoDB. In a stubbed or testing environment, Camel can route JSON directly to Mongo collections:\n" +
            "```yaml\n" +
            "- route:\n" +
            "    id: \"mongo-ingestion\"\n" +
            "    from: \"direct:insert-mongo\"\n" +
            "    steps:\n" +
            "      - to: \"mongodb:myDb?database=records&collection=transactions&operation=insert\"\n" +
            "```\n\n" +
            "## 3. Simulating Workloads with Faker, Timer, Kafka, and MQ\n" +
            "You don't need live upstream systems to test high-throughput scenarios. Tessera's `Faker` component generates realistic mock data, driven by a `timer`, which is then pushed to messaging stubs (Kafka or IBM MQ).\n\n" +
            "### The Architecture of a Simulation\n" +
            "1. **Timer**: Triggers the route every `N` milliseconds.\n" +
            "2. **Faker**: Generates random names, addresses, or financial data based on templates.\n" +
            "3. **Kafka/MQ Stub**: Receives the payload. If you run the workspace in `--stub=all` mode, Camel creates a mock endpoint automatically.\n\n" +
            "### Example: Faker -> Timer -> Kafka\n" +
            "```yaml\n" +
            "- route:\n" +
            "    id: \"faker-to-kafka\"\n" +
            "    # 1. Trigger every 1 second\n" +
            "    from: \"timer:faker-stream?period=1000\"\n" +
            "    steps:\n" +
            "      # 2. Generate Fake JSON Payload\n" +
            "      - setBody:\n" +
            "          constant: |\n" +
            "            {\n" +
            "              \"transactionId\": \"{{uuid}}\",\n" +
            "              \"customer\": \"{{name.fullName}}\",\n" +
            "              \"amount\": \"{{commerce.price}}\",\n" +
            "              \"iban\": \"{{finance.iban}}\"\n" +
            "            }\n" +
            "      # 3. Process the Faker template\n" +
            "      - to: \"faker:process\"\n" +
            "      # 4. Push to Kafka (or MQ)\n" +
            "      - to: \"kafka:financial-transactions-topic?brokers=localhost:9092\"\n" +
            "```\n\n" +
            "### Example: Faker -> IBM MQ\n" +
            "You can swap the target endpoint to IBM MQ seamlessly:\n" +
            "```yaml\n" +
            "      # 4. Push to IBM MQ Queue\n" +
            "      - to: \"jms:queue:DEV.QUEUE.1\"\n" +
            "```\n\n" +
            "### Summary\n" +
            "By combining **Timers**, **Faker**, and **Embedded Databases/Stubs**, you can build, visualize, and execute complex enterprise integration scenarios entirely offline and with realistic simulated data."
        ));

        allTopics.add(new HelpTopic("17. Visual Pipeline Composer", "Visualization",
            "# Visual Pipeline Composer\n\n" +
            "The Visual Pipeline Composer enables drag-and-drop routing and dynamic flow simulation.\n\n" +
            "## Core Features\n" +
            "* **Interactive Canvas**: Pan, zoom, and fit-to-window operations.\n" +
            "* **Collapsible Catalog**: Sidebar category grouping for sources, processors, and sinks.\n" +
            "* **Monaco Editor Integration**: Custom scripts (Java, Groovy) with syntax highlighting.\n" +
            "* **Data Flow Simulation**: Particle-based animations tracing payloads from source to sink."
        ));

        allTopics.add(new HelpTopic("18. Global Earmark & Credit Services Architecture", "Architecture",
            "# Global Earmark & Credit Services Architecture\n\n" +
            "A detailed architectural overview of the GEE (Global Earmarking Engine) and CLUE (Credit Engine) subsystems.\n\n" +
            "### Extended Reading Resources\n" +
            "* [Global Earmark Architecture Page (HTML)](docs/InteractiveHelpManual.html)\n" +
            "* [Target State Architecture Document (Markdown)](docs/RouteBuilderStudio.md)\n\n" +
            "## Architectural Blueprint\n" +
            "The **Global Earmark and Credit Services** platform is a real-time financial processing system that manages fund reservations across Citibank's global networks.\n\n" +
            "### Subsystems & Engines\n\n" +
            "#### 1. Global Earmarking Engine (GEE)\n" +
            "Acts as the brain of the platform. It handles:\n" +
            "* **Sequencing & Prioritization**: Redis-backed transactional queues order incoming requests.\n" +
            "* **Limit Verification**: Checks transaction amounts against daily aggregate thresholds.\n" +
            "* **Partner Rules**: Pluggable Drools-based rules engine for custom client integrations.\n\n" +
            "#### 2. Credit Engine (CLUE)\n" +
            "Acts as the underwriting engine for deficit transactions. It evaluates:\n" +
            "* **Credit Checks**: Validates client ratings against AMCAR.\n" +
            "* **Intraday Liquidity Pools**: Allocates overnight credit lines via Treasury Services (FTS).\n\n" +
            "## Pipeline Workflow Map\n" +
            "```\n" +
            "Request ──► [GEE: Sequencing] ──► [GEE: Limit Check] ──► [GEE: Partner Rules]\n" +
            "                                                              │\n" +
            "             Earmark Active ◄── Sufficient Balance ◄──────────┤\n" +
            "                                                              │ Insufficient\n" +
            "             Earmark Active ◄── [CLUE: Approve] ◄─────────────┘\n" +
            "```"
        ));

        topicListView.setItems(FXCollections.observableArrayList(allTopics));
    }

    public void search(String query) {
        if (searchField != null) {
            searchField.setText(query);
            filterTopics(query);
            searchField.requestFocus();
        }
    }
}
