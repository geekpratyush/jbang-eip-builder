package com.routebuilder.ui;

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
               "</style></head><body>" + bodyHtml + "</body></html>";
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
            "                                [Offline / Stub]       [Local Live]     [Live Configured]\n" +
            "                                (--stub=all)         (Standard run)     (infra.properties)\n" +
            "```\n\n" +
            "## 1. Play Offline (Stub Mode)\n" +
            "Stubs out all external endpoints using Apache Camel's stub component. Recommended for local route structure testing without external infrastructure dependency.\n\n" +
            "## 2. Play (Local Live)\n" +
            "Runs the integration live on Camel Main, reflecting changes in real-time. Great for verification against local instances.\n\n" +
            "## 3. Play (Configured Infra)\n" +
            "Runs against your actual physical development infrastructure using environment configuration loaded from `infra.properties`."
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
