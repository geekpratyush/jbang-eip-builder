package com.tessera.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.*;

public class RouteBuilderHelpWindow {
    private final Stage stage;
    private TreeView<HelpItem> treeView;
    private WebView webView;
    private TextField searchField;
    private final List<HelpTopic> topics = new ArrayList<>();
    private String categoryFilter;
    private String initialSearch;

    public static class HelpTopic {
        public String title;
        public String category;
        public String keywords;
        public String iconName;
        public String contentHtml;

        public HelpTopic(String title, String category, String keywords, String iconName, String contentHtml) {
            this.title = title;
            this.category = category;
            this.keywords = keywords;
            this.iconName = iconName;
            this.contentHtml = contentHtml;
        }
    }

    private static class HelpItem {
        public String display;
        public HelpTopic topic; // null if category node

        public HelpItem(String display, HelpTopic topic) {
            this.display = display;
            this.topic = topic;
        }

        @Override
        public String toString() {
            return display;
        }
    }

    public RouteBuilderHelpWindow() {
        this(null, null);
    }

    public RouteBuilderHelpWindow(String categoryFilter, String initialSearch) {
        this.stage = new Stage();
        this.categoryFilter = categoryFilter;
        this.initialSearch = initialSearch;
        initializeTopics();
        buildUi();
    }

    private void initializeTopics() {
        // --- Category: Overview & Architecture ---
        String introHtml = "<div class='logo-container' style='text-align: center; margin-bottom: 30px;'>" +
            "  <svg id='tessera-logo' xmlns='http://www.w3.org/2000/svg' viewBox='190 30 620 560' width='100%' height='220px'>" +
            "    <defs>" +
            "      <linearGradient id='topBlueGrad' x1='-50' y1='50' x2='50' y2='-50' gradientUnits='userSpaceOnUse'>" +
            "        <stop offset='49.8%' stop-color='#3A8DB5' />" +
            "        <stop offset='50.2%' stop-color='#64ACD0' />" +
            "      </linearGradient>" +
            "      <linearGradient id='bottomDarkGrad' x1='-50' y1='50' x2='50' y2='-50' gradientUnits='userSpaceOnUse'>" +
            "        <stop offset='49.8%' stop-color='#0E505E' />" +
            "        <stop offset='50.2%' stop-color='#176E7D' />" +
            "      </linearGradient>" +
            "      <filter id='pieceShadow' x='-30%' y='-30%' width='160%' height='160%'>" +
            "        <feDropShadow dx='3' dy='6' stdDeviation='5' flood-color='#002233' flood-opacity='0.18' />" +
            "      </filter>" +
            "    </defs>" +
            "    <g transform='translate(500, 215) scale(1.1)'>" +
            "      <g stroke-width='12' stroke-linejoin='round'>" +
            "        <path class='animated-element tri-left' d='M -120 45 L -55 110 L -185 110 Z' fill='#136070' stroke='#136070' />" +
            "        <path class='animated-element tri-right' d='M 120 45 L 185 110 L 55 110 Z' fill='#207886' stroke='#207886' />" +
            "      </g>" +
            "      <g transform='rotate(45)' stroke-linejoin='round'>" +
            "        <path class='animated-element piece-br' d='M 100 100 L 4 100 L 4 62 L -2 62 C -6 72, -20 68, -20 50 C -20 32, -6 28, -2 38 L 4 38 L 4 4 L 38 4 L 38 -2 C 28 -6, 32 -20, 50 -20 C 68 -20, 72 -6, 62 -2 L 62 4 L 100 4 Z' fill='url(#bottomDarkGrad)' filter='url(#pieceShadow)' />" +
            "        <path class='animated-element piece-bl' d='M -100 100 L -100 4 L -62 4 L -62 -2 C -72 -6, -68 -20, -50 -20 C -32 -20, -28 -6, -38 -2 L -38 4 L -4 4 L -4 38 L -10 38 C -14 28, -28 32, -28 50 C -28 68, -14 72, -10 62 L -4 62 L -4 100 Z' fill='#136070' filter='url(#pieceShadow)' />" +
            "        <path class='animated-element piece-tr' d='M 100 -100 L 100 -4 L 62 -4 L 62 -10 C 72 -14, 68 -28, 50 -28 C 32 -28, 28 -14, 38 -10 L 38 -4 L 4 -4 L 4 -38 L -2 -38 C -6 -28, -20 -32, -20 -50 C -20 -68, -6 -72, -2 -62 L 4 -62 L 4 -100 Z' fill='#F1A463' filter='url(#pieceShadow)' />" +
            "        <path class='animated-element piece-tl' d='M -100 -100 L -4 -100 L -4 -62 L -10 -62 C -14 -72, -28 -68, -28 -50 C -28 -32, -14 -28, -10 -38 L -4 -38 L -4 -4 L -38 -4 L -38 -10 C -28 -14, -32 -28, -50 -28 C -68 -28, -72 -14, -62 -10 L -62 -4 L -100 -4 Z' fill='url(#topBlueGrad)' filter='url(#pieceShadow)' />" +
            "      </g>" +
            "    </g>" +
            "    <text class='animated-element text-title' x='500' y='470' font-family='sans-serif' font-weight='900' font-size='82' fill='currentColor' text-anchor='middle' letter-spacing='6'>TESSERA</text>" +
            "    <text class='animated-element text-sub' x='500' y='525' font-family='sans-serif' font-weight='400' font-size='26' fill='currentColor' opacity='0.7' text-anchor='middle' letter-spacing='0.5'>The foundational tiles of enterprise architecture</text>" +
            "  </svg>" +
            "</div>" +
            "<script>" +
            "  setTimeout(function() {" +
            "    var logo = document.getElementById('tessera-logo');" +
            "    if(logo && !logo.classList.contains('play-animation')) {" +
            "      logo.classList.add('play-animation');" +
            "      try {" +
            "          var ctx = new (window.AudioContext || window.webkitAudioContext)();" +
            "          var osc = ctx.createOscillator();" +
            "          var gain = ctx.createGain();" +
            "          osc.connect(gain);" +
            "          gain.connect(ctx.destination);" +
            "          osc.type = 'sine';" +
            "          osc.frequency.setValueAtTime(300, ctx.currentTime);" +
            "          osc.frequency.exponentialRampToValueAtTime(800, ctx.currentTime + 0.15);" +
            "          osc.frequency.exponentialRampToValueAtTime(150, ctx.currentTime + 0.5);" +
            "          gain.gain.setValueAtTime(0, ctx.currentTime);" +
            "          gain.gain.linearRampToValueAtTime(0.3, ctx.currentTime + 0.1);" +
            "          gain.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.5);" +
            "          osc.start(ctx.currentTime);" +
            "          osc.stop(ctx.currentTime + 0.5);" +
            "      } catch(e) {}" +
            "    }" +
            "  }, 300);" +
            "</script>" +
            "<h1>About TESSERA</h1>" +
            "<p><strong>TESSERA empowers developers and architects to build stunning, production-ready enterprise applications and integrations without writing a single line of code.</strong></p>" +
            "<p>By providing a unified, visual, and highly interactive workspace, Tessera abstracts away the complexities of Apache Camel, enterprise messaging, and data transformation. You can visually design routes, map complex data schemas, simulate infrastructure, and enforce rigorous security policies—all through an intuitive point-and-click interface.</p>" +
            "<h3>Next-Generation Capabilities</h3>" +
            "<ul>" +
            "  <li><strong>Zero-Code Visual Routing:</strong> Drag, drop, and connect enterprise integration patterns (EIPs). Your visual design is instantly translated into deployable Camel DSL.</li>" +
            "  <li><strong>Universal Faker Studio:</strong> Generate realistic, randomized datasets for databases and messages instantly using our dual-tab visual JSON editor.</li>" +
            "  <li><strong>Tessera Audit Shaper:</strong> Automatically secure, mask, and standardize your data payloads and audit trails with built-in cryptographic tools.</li>" +
            "  <li><strong>Transformation & Validation:</strong> Map JSON, XML, SWIFT MT/MX, and CSV files visually, and validate them against industry standards in real-time.</li>" +
            "</ul>";

        topics.add(new HelpTopic("Introduction to TESSERA Studio", "Overview & Architecture", "introduction overview hello help welcome get started", "fas-home", introHtml));

        topics.add(new HelpTopic("Camel JBang Runner", "Overview & Architecture", "jbang camel run compile debug execution engine runner process", "fas-terminal",
            "<h1>Camel JBang Runner</h1>" +
            "<p>RouteBuilder Studio leverages <strong>JBang</strong> under the hood to bootstrap and execute integration projects without requiring pre-configured Maven or Gradle runtime projects.</p>" +
            "<h3>How It Works</h3>" +
            "<p>When you click <strong>Run</strong> in the toolbar, the studio launches a background JBang command:</p>" +
            "<pre>jbang camel@apache/camel run &lt;route-files&gt; --dependency=mvn:... --properties=...</pre>" +
            "<p>This fetches dependency artifacts dynamically from Maven Central and hosts a local execution daemon, feeding console logs back to the IDE's unified output drawer.</p>"));

        topics.add(new HelpTopic("Getting Started & JBang Run profiles", "Overview & Architecture", "jbang profile stub run live play offline execution", "fas-play-circle",
            "<h1>Developer Environments & Execution Profiles</h1>" +
            "<p>The studio lets you run integrations locally under three runtime profiles using JBang and Camel Main:</p>" +
            "<pre>[Kamelet Studio UI] ---&gt; [Local Workspace Dir] ---&gt; [JBang Executable]\n" +
            "                                                           |\n" +
            "                                        +------------------+------------------+\n" +
            "                                        |                                     |\n" +
            "                                [Offline / Stub]                       [Local Live]\n" +
            "                                  (--stub=all)                        (Standard run)</pre>" +
            "<h3>1. Play Offline (Stub Mode)</h3>" +
            "<p>Stubs out all external endpoints using Apache Camel's stub component. Recommended for local route structure testing without external infrastructure dependency.</p>" +
            "<h3>2. Play (Local Live)</h3>" +
            "<p>Runs the integration live on Camel Main, reflecting changes in real-time. Great for verification against local instances.</p>"));

        topics.add(new HelpTopic("Path to Production & Containers", "Overview & Architecture", "production containers docker dockerfile native compile cloud graalvm secrets", "fas-box-open",
            "<h1>Path to Production & Containerization</h1>" +
            "<p>Transition your routes from local JBang testing into enterprise microservices.</p>" +
            "<h3>1. Native Build Compilation</h3>" +
            "<p>Build a native Linux container binary using Maven and GraalVM:</p>" +
            "<pre>mvn package -Pnative</pre>" +
            "<p>Native builds compile JVM code into standalone native code, reducing startup time to &lt;10ms and RSS memory footprint to &lt;30MB.</p>" +
            "<h3>2. Secrets Management</h3>" +
            "<blockquote><strong>Important:</strong> Never bundle credentials (keystores, passwords, krb5 keytabs) in container filesystems. Resolve them dynamically at runtime via Kubernetes ConfigMaps/Secrets mounts or HashiCorp Vault.</blockquote>"));

        // --- Category: Route Designer & Sync ---
        topics.add(new HelpTopic("Visual Route Designer", "Route Designer & Sync", "diagram visual flow designer canvas eip nodes palette", "fas-project-diagram",
            "<h1>Visual Route Designer</h1>" +
            "<p>The right pane houses the <strong>Visual Diagram Canvas</strong> which translates Camel DSL code into diagram structures.</p>" +
            "<h3>Working with the Canvas</h3>" +
            "<ul>" +
            "  <li><strong>Adding EIP Nodes:</strong> Use the palette or context menu to append EIPs like timers, loggers, splitters, aggregators, or recipient lists.</li>" +
            "  <li><strong>Connecting Nodes:</strong> Set connections to create message flows.</li>" +
            "  <li><strong>Node Configuration:</strong> Selecting a node reveals its properties in the property panel for inline edits.</li>" +
            "</ul>"));

        topics.add(new HelpTopic("Bidirectional Sync (Monaco & Canvas)", "Route Designer & Sync", "monaco webview synchronization code diagram yaml sync parser xml dsl", "fas-exchange-alt",
            "<h1>Bidirectional Code & Diagram Sync</h1>" +
            "<p>RouteBuilder features a <strong>Zero-Compile Synchronization Engine</strong> between the Monaco Editor (Code) and the Diagram Canvas.</p>" +
            "<h3>Sync Behavior</h3>" +
            "<ul>" +
            "  <li><strong>Code to Diagram:</strong> As you type Camel YAML DSL or XML DSL in Monaco, the code is parsed asynchronously and the visual layout updates automatically.</li>" +
            "  <li><strong>Diagram to Code:</strong> Repositioning components, adding connections, or editing node properties modifies the underlying DSL code instantly without corrupting comments or structure.</li>" +
            "</ul>"));

        topics.add(new HelpTopic("Universal Diagram Studio Reference", "Route Designer & Sync", "diagram studio mermaid flowcharts icons endpoints svg png export", "fas-paint-brush",
            "<h1>Universal Diagram Studio</h1>" +
            "<p>Diagram Studio automatically renders a visual representation of Apache Camel routes from XML or YAML files.</p>" +
            "<h3>Key Features</h3>" +
            "<ul>" +
            "  <li><strong>Mermaid.js Flowcharts:</strong> Renders routes instantly as they are typed in the editor.</li>" +
            "  <li><strong>Component Identification:</strong> Identifies endpoints by type (e.g. database icon for <code>sql:</code>, queue icon for <code>jms:</code>, HTTP globe icon for <code>http:</code>).</li>" +
            "  <li><strong>Logical Flow Visualizer:</strong> Formats routing steps (Splitters, Aggregators, Filters) as flowchart branches.</li>" +
            "  <li><strong>SVG / PNG Export:</strong> Diagrams can be exported for project documentation.</li>" +
            "</ul>"));

        // --- Category: Kamelet Studio ---
        topics.add(new HelpTopic("Understanding Kamelets", "Kamelet Studio", "kamelets custom spec connector binding templates source sink action definition", "fas-puzzle-piece",
            "<h1>Understanding Kamelets</h1>" +
            "<p><strong>Kamelets</strong> (Camel Route Snippets) are simplified templates that represent connectors (Sources, Sinks, or Actions) with defined properties.</p>" +
            "<h3>Kamelet Types</h3>" +
            "<ul>" +
            "  <li><strong>Source:</strong> Produces data (e.g. fetches from IBM MQ, MongoDB) and pushes it into your route.</li>" +
            "  <li><strong>Sink:</strong> Consumes data (e.g. inserts into database, sends messaging payload) from your route.</li>" +
            "  <li><strong>Action:</strong> Performs intermediate transformation, enrichment, or validation (e.g. SQL query mapping).</li>" +
            "</ul>"));

        topics.add(new HelpTopic("Designing Custom Kamelets", "Kamelet Studio", "kamelet builder design editor properties metadata metadata title", "fas-drafting-compass",
            "<h1>Designing Custom Kamelets</h1>" +
            "<p>Open the **Kamelet Builder** under <code>Tools -> Kamelet Builder...</code> to visual design templates.</p>" +
            "<h3>Design Sections</h3>" +
            "<ul>" +
            "  <li><strong>Metadata Definitions:</strong> Specify title, description, and connector type (Source/Sink/Action).</li>" +
            "  <li><strong>Property Fields:</strong> Configure arguments (such as hostname, database URL, credentials) and whether they are required.</li>" +
            "  <li><strong>Template Flow:</strong> Implement the underlying Apache Camel routes that execute when the Kamelet is invoked.</li>" +
            "</ul>"));

        topics.add(new HelpTopic("Kamelet Testing Harness", "Kamelet Studio", "test kamelet run test route jbang execution form dynamic parameters", "fas-vial",
            "<h1>Kamelet Testing Harness</h1>" +
            "<p>You can execute testing scenarios for custom Kamelets directly from the editor pane without writing integration code manually.</p>" +
            "<h3>Testing Workflow</h3>" +
            "<ul>" +
            "  <li><strong>Click Test:</strong> The studio extracts <code>spec.definition.properties</code> and constructs a dynamic settings input form.</li>" +
            "  <li><strong>Auto-route Generator:</strong> Generates a test route feeding mock data (for Sinks/Actions) or reading payloads (for Sources).</li>" +
            "  <li><strong>JBang Execution:</strong> Spawns a test runner process with all dependencies, outputting logs directly to the console.</li>" +
            "</ul>"));

        // --- Category: Dependency Catalog ---
        topics.add(new HelpTopic("Managing Dependencies", "Dependency Catalog", "dependencies catalog registry coordinate version maven catalog file", "fas-book",
            "<h1>Managing Dependencies</h1>" +
            "<p>Open the **Dependency Catalog** (<code>Tools -> Dependency Catalog...</code>) to manage dependencies.</p>" +
            "<h3>Catalog Options</h3>" +
            "<ul>" +
            "  <li><strong>Add/Remove Coordinate Entries:</strong> Maintain Maven coordinates (`groupId:artifactId:version`).</li>" +
            "  <li><strong>Update Versions:</strong> Overwrite specific dependencies to use the latest security releases.</li>" +
            "</ul>"));

        topics.add(new HelpTopic("Wildcard Auto-injection", "Dependency Catalog", "auto-inject dependency injection catalog flag enabled run jbang headers", "fas-syringe",
            "<h1>Wildcard Auto-injection</h1>" +
            "<p>You do not need to hardcode Maven coordinates inside comments at the top of your YAML files.</p>" +
            "<h3>Auto-injection Behavior</h3>" +
            "<p>Any dependency catalog item marked as <strong>Enabled</strong> in the registry is automatically appended as a <code>--dependency</code> CLI option to JBang runs.</p>" +
            "<p>You can also click <strong>Inject Headers</strong> to paste coordinate comments into the Monaco editor header automatically.</p>"));

        // --- Category: Validation Studio ---
        topics.add(new HelpTopic("Supported Validation Formats", "Validation Studio", "validation xsd schema json-schema yaml csvw metadata swift mt xml iso20022 flatfile", "fas-check-double",
            "<h1>Supported Validation Formats</h1>" +
            "<p>Open the **Validator Studio** to perform real-time diagnostic checks on enterprise documents.</p>" +
            "<h3>Supported Syntax Schemes</h3>" +
            "<ul>" +
            "  <li><strong>XML + XSD:</strong> Checks XML structures against W3C Schema definitions.</li>" +
            "  <li><strong>JSON / YAML + Schema:</strong> Evaluates data compliance against JSON Schema drafts.</li>" +
            "  <li><strong>ISO 20022 MX:</strong> Parses financial transaction messages against official bank schemas.</li>" +
            "  <li><strong>CSV + CSVW:</strong> Validates tabular data constraints against CSV-on-the-Web metadata.</li>" +
            "  <li><strong>Flat File:</strong> Maps and validates fixed-width records using JSON layouts.</li>" +
            "</ul>"));

        topics.add(new HelpTopic("SWIFT MT Validation Modes", "Validation Studio", "swift mt validation standard enhanced rules restricted currencies sanctions high risk jurisdictions AML", "fas-university",
            "<h1>SWIFT MT Validation Modes</h1>" +
            "<p>The validator supports checking SWIFT MT messages (e.g. MT103, MT202) under two modes:</p>" +
            "<ul>" +
            "  <li><strong>Standard Mode:</strong> Validates core SWIFT layout structures, BIC codes (Block 1), references (Field 20), operation codes (Field 23B), currency/amount syntax (Field 32A), and charges (Field 71A).</li>" +
            "  <li><strong>Enhanced Mode:</strong> Integrates dynamic rule logic from JSON files (such as <code>validators/custom-mt-rules.json</code>) to validate corporate reference prefixes, transaction limit thresholds, blacklisted currencies (e.g. RUB), and high-risk jurisdictions.</li>" +
            "</ul>"));

        topics.add(new HelpTopic("Scenario & Pair Updates", "Validation Studio", "save scenario scenario pair update context menu rename files mapping overwrite schema", "fas-sync-alt",
            "<h1>Scenario & Pair Updates</h1>" +
            "<p>Validation layouts are structured as message + schema pairs.</p>" +
            "<ul>" +
            "  <li><strong>Overwrite/Save Scenario:</strong> Clicking **Save Scenario Content** saves both your message payload and schema editor texts.</li>" +
            "  <li><strong>Overwrite Schema:</strong> The **Overwrite Schema...** header button allows importing any local schema file to replace the active validation rules.</li>" +
            "  <li><strong>Context Menu Controls:</strong> Right-click scenarios in the history explorer to add, delete, or select <strong>Update Validation Pair...</strong> to change names, formats, or file paths instantly.</li>" +
            "</ul>"));

        topics.add(new HelpTopic("Validation Studio Reference", "Validation Studio", "validator studio layout report execution rules xml xsd csv csvw json schema flatfile", "fas-clipboard-check",
            "<h1>Validation Studio Reference</h1>" +
            "<p>Validation Studio provides in-memory validation for several industry standards. It uses a three-editor Monaco layout (Data Payload, Schema Definition, Validation Report) with sidebar history mapping.</p>" +
            "<pre>+-------------------------------------------------------------+\n" +
            "|                      [Validation Studio]                    |\n" +
            "|  +----------------+  +---------------+  +----------------+  |\n" +
            "|  | Source Payload |  | Schema/Rules  |  | Reports/Errors |  |\n" +
            "|  |                |  |               |  |                |  |\n" +
            "|  | Monaco Editor  |  | Monaco Editor |  | Monaco Editor  |  |\n" +
            "|  | (Edit Raw Msg) |  | (Edit Rules)  |  | (Markdown View)|  |\n" +
            "|  +----------------+  +---------------+  +----------------+  |\n" +
            "+-------------------------------------------------------------+</pre>" +
            "<h3>Executing Validations</h3>" +
            "<ol>" +
            "  <li>Select a validation scenario from the sidebar tree.</li>" +
            "  <li>Click <strong>Validate</strong> in the toolbar or press <strong>F5</strong>.</li>" +
            "  <li>Analyze the results in the Validation Report editor.</li>" +
            "</ol>"));

        // --- Category: Advanced Tools ---
        topics.add(new HelpTopic("Universal Faker Studio", "Advanced Tools", "faker mock simulator databases dataset profiles mx mt templates generation generator", "fas-mask",
            "<h1>Universal Faker Studio</h1>" +
            "<p>Open the **Faker Studio** to create realistic dummy datasets for testing workflows.</p>" +
            "<h3>Faker Capabilities</h3>" +
            "<ul>" +
            "  <li><strong>Dual-Tab Explorer:</strong> Seamlessly switch between managing Templates and your Faker DB datasets.</li>" +
            "  <li><strong>Synchronized Editors:</strong> Edit JSON datasets visually or in raw text mode. Changes are instantly synchronized between views.</li>" +
            "  <li><strong>Multi-Selection & Batch Actions:</strong> Easily copy, paste, delete, or rename multiple database files at once.</li>" +
            "  <li><strong>Universal Faker Bean:</strong> Integrates <code>UniversalFaker.java</code> which dynamically replaces tokens in template files (such as MX xml or MT text templates) with randomized data from the active database.</li>" +
            "</ul>"));

        topics.add(new HelpTopic("Tessera Audit Shaper", "Advanced Tools", "audit shaper sre crypto cryptography pii mask security database sql mongo", "fas-shield-alt",
            "<h1>Tessera Audit Shaper & Crypto</h1>" +
            "<p>The Tessera Audit Shaper is a centralized security backbone designed to standardize metadata, mask PII, and handle row-level encryption.</p>" +
            "<h3>Core Features</h3>" +
            "<ul>" +
            "  <li><strong>Smart Cryptography:</strong> Automatically encrypts and decrypts sensitive fields using AES-256-GCM via global property keys or environment variables (<code>TESSERA_AUDIT_CRYPTO_KEY</code>).</li>" +
            "  <li><strong>Auto-Masking:</strong> PII fields (like passwords, CVVs) are automatically obfuscated from standard logs.</li>" +
            "  <li><strong>Enveloping:</strong> Wraps diverse message formats into standard JSON audit payloads suitable for data lakes or message queues.</li>" +
            "</ul>"));

        topics.add(new HelpTopic("Schema Mapping Studio (MAP)", "Advanced Tools", "schema mapping map studio visual mapping target tree live flow registry", "fas-sitemap",
            "<h1>Schema Mapping Studio (MAP)</h1>" +
            "<p>The MAP Studio defines mappings between message structures and schemas.</p>" +
            "<h3>Core Features</h3>" +
            "<ul>" +
            "  <li><strong>Visual Mapping:</strong> Drag-and-drop source tree properties onto target tree properties.</li>" +
            "  <li><strong>Live Flowchart:</strong> Renders active relationship structures using Mermaid.js flowcharts.</li>" +
            "  <li><strong>Registry Sync:</strong> Updates are written to the global registry <code>validation-mapping.json</code>.</li>" +
            "</ul>"));

        topics.add(new HelpTopic("Transformation Studio (Transform)", "Advanced Tools", "transformation transform studio smooks freemarker jslt groovy scripting joor", "fas-random",
            "<h1>Transformation Studio (Transform)</h1>" +
            "<p>Transformation Studio converts messages between different messaging formats.</p>" +
            "<h3>Supported Engines</h3>" +
            "<ul>" +
            "  <li><strong>Smooks:</strong> High-performance streaming transformation engine for CSV, XML, JSON, and fixed-width formats.</li>" +
            "  <li><strong>FreeMarker (FTL):</strong> Generates text outputs (XML, JSON) using template parameters.</li>" +
            "  <li><strong>JSLT:</strong> Declarative JSON-to-JSON mapper.</li>" +
            "  <li><strong>Groovy Scripting:</strong> Evaluates dynamic mapping scripts.</li>" +
            "  <li><strong>jOOR Java Mapper:</strong> Compiles custom Java mappings at runtime.</li>" +
            "</ul>"));

        topics.add(new HelpTopic("Variables Editor Reference", "Advanced Tools", "variables properties configurations keys workspace placeholders environment", "fas-sliders-h",
            "<h1>Variables Editor Reference</h1>" +
            "<p>The Variables Editor manages project-wide configurations, system properties, and properties placeholders inside <code>application.properties</code>.</p>" +
            "<h3>Key Features</h3>" +
            "<ul>" +
            "  <li><strong>Properties Grid:</strong> Edit, add, or delete configuration key-value entries inline.</li>" +
            "  <li><strong>Profile Bindings:</strong> Bind settings to Dev, Test, or Prod environments.</li>" +
            "  <li><strong>System Resolution:</strong> Properties are resolved via Camel's placeholder resolver engine.</li>" +
            "</ul>"));

        topics.add(new HelpTopic("XSLT Mapper Reference", "Advanced Tools", "xslt xml transformation styles layout mapping xslt-mapper", "fas-code-branch",
            "<h1>XSLT Mapper Reference</h1>" +
            "<p>The XSLT Mapper creates stylesheet mapping flows to transform XML documents.</p>" +
            "<h3>Core Capabilities</h3>" +
            "<ul>" +
            "  <li><strong>Structure Trees:</strong> Compares source and target XML schemas side-by-side.</li>" +
            "  <li><strong>Style Generation:</strong> Automatically builds XSL templates (`.xsl`) when connecting nodes.</li>" +
            "</ul>"));

        topics.add(new HelpTopic("Crypto Studio Reference", "Advanced Tools", "crypto studio encryption decryption algorithms aes base64 url pbkdf2 keys", "fas-key",
            "<h1>Crypto Studio Reference</h1>" +
            "<p>Crypto Studio manages encryption and decryption of configuration secrets.</p>" +
            "<h3>Cryptographic Specifications</h3>" +
            "<ul>" +
            "  <li><strong>Algorithm:</strong> <code>AES-256-GCM</code></li>" +
            "  <li><strong>Key Derivation:</strong> <code>PBKDF2WithHmacSHA256</code> (65,536 iterations)</li>" +
            "  <li><strong>Salt / IV:</strong> <code>16 bytes</code> salt &amp; <code>12 bytes</code> IV prefixed to ciphertext.</li>" +
            "</ul>"));

        topics.add(new HelpTopic("IBM MQ Connector (JMS 3.0 & XA)", "Advanced Tools", "ibm mq connections queues jms transaction manager narayana cache level leaks transacted", "fas-server",
            "<h1>IBM MQ Integration (Jakarta JMS 3.0)</h1>" +
            "<p>The studio supports connecting to IBM MQ using the modern Jakarta JMS client.</p>" +
            "<h3>Distributed XA JTA Configuration</h3>" +
            "<p>When using XA connection factories (<code>MQXAConnectionFactory</code> and <code>JmsPoolXAConnectionFactory</code>) bound to Narayana Transaction Managers, ensure the following local settings are applied:</p>" +
            "<ul>" +
            "  <li><code>transacted: false</code></li>" +
            "  <li><code>cacheLevelName: CACHE_NONE</code></li>" +
            "</ul>" +
            "<blockquote><strong>Note:</strong> Distributed transaction scopes are managed by the JTA Manager rather than local session caching layers. Session caching under XA leads to connection leaks and lockouts.</blockquote>"));

        topics.add(new HelpTopic("Solace PubSub+ Integration", "Advanced Tools", "solace connection smf smfs ssl keystore truststore mtls queues connection parameters", "fas-broadcast-tower",
            "<h1>Solace PubSub+ (SMF/SMFS)</h1>" +
            "<p>The studio enables reliable connection parameters to Solace brokers using SMF (<code>smf://</code>) or secure SMFS (<code>smfs://</code>) protocols.</p>" +
            "<h3>Connection Schema Parameters</h3>" +
            "<table>" +
            "  <thead>" +
            "    <tr><th>Parameter</th><th>Description</th><th>Required For</th></tr>" +
            "  </thead>" +
            "  <tbody>" +
            "    <tr><td><code>brokerUrl</code></td><td>Broker SMF/SMFS Address</td><td>All Connections</td></tr>" +
            "    <tr><td><code>sslTrustStore</code></td><td>Absolute path to SSL truststore</td><td>SMFS One-way SSL</td></tr>" +
            "    <tr><td><code>sslKeyStore</code></td><td>Absolute path to client keystore</td><td>SMFS Mutual TLS (mTLS)</td></tr>" +
            "    <tr><td><code>sslKeyStorePassword</code></td><td>Client keystore password</td><td>SMFS Mutual TLS (mTLS)</td></tr>" +
            "  </tbody>" +
            "</table>"));

        topics.add(new HelpTopic("Apache Kafka (SSL & Kerberos)", "Advanced Tools", "kafka ssl truststore keystore mtls kerberos gssapi sasl-jaas jaas login keytab", "fas-stream",
            "<h1>Apache Kafka SSL &amp; Kerberos Security</h1>" +
            "<p>Configure Kafka endpoints with standard TLS security or enterprise Kerberos (SASL/GSSAPI) settings.</p>" +
            "<h3>1. mTLS Configuration</h3>" +
            "<p>Ensure keystores and truststores are resolved securely via external properties:</p>" +
            "<pre>camel.component.kafka.security-protocol=SSL\n" +
            "camel.component.kafka.ssl-truststore-location=/secrets/truststore.jks\n" +
            "camel.component.kafka.ssl-truststore-password={{vault:kafka-ts-pass}}\n" +
            "camel.component.kafka.ssl-keystore-location=/secrets/keystore.jks\n" +
            "camel.component.kafka.ssl-keystore-password={{vault:kafka-ks-pass}}</pre>" +
            "<h3>2. GSSAPI/Kerberos Configuration</h3>" +
            "<p>Attach external system configurations:</p>" +
            "<pre>camel.component.kafka.sasl-mechanism=GSSAPI\n" +
            "camel.component.kafka.security-protocol=SASL_SSL\n" +
            "camel.component.kafka.sasl-jaas-config=com.sun.security.auth.module.Krb5LoginModule required useKeyTab=true storeKey=true keyTab=\"/secrets/client.keytab\" principal=\"camel-service@REALM.COM\";</pre>"));

        topics.add(new HelpTopic("MongoDB Change Streams & Auditing", "Advanced Tools", "mongodb changestreams database collection audit auditing metadata hostname privacy logs exclusion", "fas-database",
            "<h1>MongoDB Change Streams and Dynamic Auditing</h1>" +
            "<p>Capture database mutations dynamically and write structural audits.</p>" +
            "<h3>1. Change Stream Filter Criteria</h3>" +
            "<p>Filter MongoDB stream mutations using JSON-formatted criteria:</p>" +
            "<pre>- from:\n" +
            "    uri: \"mongodb:myDb?consumerType=changeStream&amp;database=audit&amp;collection=orders\"\n" +
            "    parameters:\n" +
            "      streamFilter: '{\"operationType\": {\"$in\": [\"insert\", \"update\"]}}'</pre>" +
            "<h3>2. Host Audit Processing Metadata</h3>" +
            "<p>The audit component resolves the hostname and IP of the executor machine automatically to log origin data.</p>"));

        topics.add(new HelpTopic("SQL Dynamic CRUD Engine", "Advanced Tools", "sql dynamic CRUD database rest json mapper query insert update", "fas-table",
            "<h1>SQL Dynamic CRUD Engine</h1>" +
            "<p>Use Camel's <code>sql:dynamic</code> component to map REST JSON payloads directly to database operations.</p>" +
            "<h3>Dynamic Insert &amp; Update Flow</h3>" +
            "<pre>[HTTP POST JSON] ---&gt; [Jackson Map Deserializer] ---&gt; [Groovy Query Compiler] ---&gt; [sql:dynamic]</pre>" +
            "<h3>Route Definition Snippet</h3>" +
            "<pre>- route:\n" +
            "    id: dynamic-insert-route\n" +
            "    from:\n" +
            "      uri: \"direct:insert-user\"\n" +
            "      steps:\n" +
            "        - unmarshal:\n" +
            "            json: {}\n" +
            "        - setBody:\n" +
            "            simple: \"INSERT INTO users (name, email) VALUES (:?name, :?email)\"\n" +
            "        - to: \"sql:dynamic\"</pre>"));

        topics.add(new HelpTopic("Export Studio & DB Mappings", "Advanced Tools", "export liquibase schema oracle postgres ddl dml changelog deploy run scripts", "fas-file-export",
            "<h1>Export Studio (Liquibase, SQL, File System)</h1>" +
            "<p>Export Studio packages Camel integrations and databases for target environments.</p>" +
            "<h3>1. Liquibase Migration Export</h3>" +
            "<p>Packages database schema migrations into Liquibase XML changelogs (<code>changelog.xml</code>) for Oracle, Postgres, and SQL database engines.</p>" +
            "<h3>2. SQL Database Export</h3>" +
            "<p>Generates DDL and DML scripts tailored for PostgreSQL or Oracle database engines.</p>" +
            "<h3>3. File System Export</h3>" +
            "<p>Generates complete directory structures containing runnable shell scripts (<code>run.sh</code> / <code>run.bat</code>), properties files, and routing configurations.</p>"));

        topics.add(new HelpTopic("Remote Deploy, Copy & Run (Studio)", "Advanced Tools", "remote deploy push upload container test hotload run remotely API_URL copy files absolute path transformations liquibase", "fas-cloud-upload-alt",
            "<h1>Remote Deploy, Copy & Run Studio</h1>" +
            "<p>The <strong>Remote Deploy Center</strong> allows developers to deploy routes, copy transformation files, and transfer Liquibase schemas to a remote Camel Quarkus container and instantly test execution flows.</p>" +
            "<h3>1. Deployment (Routes)</h3>" +
            "<p>Allows hot-loading routes dynamically. You can start/stop individual routes using row-level actions, select/deselect items using checkboxes, or execute bulk Start/Stop actions.</p>" +
            "<h3>2. Static File Copying (Transformations & Liquibase)</h3>" +
            "<p>For static resources like XSLT/JSLT maps, XML schemas, and Liquibase seed changelogs, you specify an absolute destination path under <strong>Server Target Path</strong> (e.g. <code>/opt/camel/resources</code>).</p>" +
            "<ul>" +
            "  <li><strong>Row-level Actions:</strong> Copy files individually using the upload action button in each row.</li>" +
            "  <li><strong>Checkbox & Bulk Actions:</strong> Select multiple files to copy in a single batch. Includes a Select All checkbox in the column header.</li>" +
            "  <li><strong>Response Path Verification:</strong> The activity log will display the exact path where the file was written on the remote server (e.g. <code>[SUCCESS] File changelog copied to: /opt/camel/resources/changelog.xml</code>). This is the path to use in your route URIs.</li>" +
            "</ul>"));

        topics.add(new HelpTopic("Remote Connectivity & Heartbeat", "Advanced Tools", "heartbeat ping connectivity green red alert remote status online offline", "fas-heartbeat",
            "<h1>Remote Connectivity & Heartbeat Monitoring</h1>" +
            "<p>RouteBuilder Studio features a built-in background heartbeat monitor that continuously polls the remote container's status.</p>" +
            "<h3>How Connectivity Works</h3>" +
            "<ul>" +
            "  <li><strong>Background Ping:</strong> Every few seconds, a client listener issues a <code>GET /admin/heartbeat</code> request to the configured <code>API_URL</code>.</li>" +
            "  <li><strong>Capabilities Listing:</strong> The server replies with a list of active capabilities (e.g. <code>dynamic-routes-sql</code>, <code>temporary-hotload-routes</code>) and environment information.</li>" +
            "</ul>" +
            "<h3>Visual Indicators & Alerts</h3>" +
            "<ul>" +
            "  <li><strong>Green Icon:</strong> Indicates the container is online and responding. Remote hot-loading is active.</li>" +
            "  <li><strong>Red Icon:</strong> Triggered when the container is offline, returns an error, or the connection timed out.</li>" +
            "  <li><strong>Connection Drop Alert:</strong> If the connection changes from Online to Offline, the client triggers a warning pop-up or notification alerting the developer.</li>" +
            "</ul>" +
            "<h3>Security & Environments Configuration</h3>" +
            "<p>The remote heartbeat and management endpoints can be disabled based on target environments to prevent unauthorized access. The following properties control the extension on the server side:</p>" +
            "<pre># Enable/disable admin endpoints\n" +
            "loader.management.api.enabled=true\n\n" +
            "# Active environment tag\n" +
            "loader.management.api.environment=development</pre>"));

        topics.add(new HelpTopic("Interactive Examples & Architecture Guide", "Advanced Tools", "architecture examples mongodb h2 faker timer kafka mq stubs", "fas-sitemap",
            "<h1>Comprehensive Architecture & Examples Guide</h1>" +
            "<p>This guide walks you through the core architectural arrangement of Tessera's sample projects, showing you how they reference assets, execute routing from start to finish, and leverage embedded databases (MongoDB, H2) and simulation tools (Faker, Timer, MQ/Kafka Stubs).</p>" +
            "<h3>1. Project Arrangement & Asset Referencing</h3>" +
            "<p>Tessera workspaces follow a decoupled, modular design. Routes and their supporting files are strictly separated:</p>" +
            "<ul>" +
            "  <li><strong>Camel Routes Directory (<code>camel/</code>):</strong> Contains the YAML DSL definitions for your routes. E.g., <code>camel/chapter-16-ui-ux/09-sql-workbench.camel.yaml</code>.</li>" +
            "  <li><strong>Assets Directory (<code>assets/</code>):</strong> Contains static HTML, CSS, JavaScript, and images. E.g., <code>assets/chapter-16-ui-ux/09-sql-workbench/ui/index.html</code>.</li>" +
            "  <li><strong>Referencing Mechanism:</strong> Camel routes use the <code>platform-http</code> or <code>jetty</code> components to serve these static assets directly from the file system.</li>" +
            "</ul>" +
            "<pre>- route:\n" +
            "    from: \"platform-http:/?matchOnUriPrefix=true\"\n" +
            "    steps:\n" +
            "      - to: \"file://{{WORKSPACE_ROOT_DIR}}/assets/chapter-16-ui-ux/09-sql-workbench/ui\"</pre>" +
            "<h3>2. In-Memory Databases: MongoDB to H2 SQL</h3>" +
            "<p>Tessera can seamlessly bootstrap embedded, offline databases to support local development.</p>" +
            "<h4>H2 Embedded SQL Database</h4>" +
            "<p>The H2 database runs directly in-memory and can be initialized with SQL DDL/DML statements at startup:</p>" +
            "<pre>- route:\n" +
            "    id: \"sql-workbench-init\"\n" +
            "    from: \"timer:init?repeatCount=1\"\n" +
            "    steps:\n" +
            "      - to: \"sql:CREATE SCHEMA IF NOT EXISTS core\"\n" +
            "      - to: \"sql:CREATE TABLE IF NOT EXISTS core.credit_earmarks (earmark_id VARCHAR(50), status VARCHAR(50))\"\n" +
            "      - to: \"sql:INSERT INTO core.credit_earmarks VALUES ('EMK-7011', 'APPROVED')\"</pre>" +
            "<h4>MongoDB (Stubbed/Embedded)</h4>" +
            "<p>Similarly, when you need NoSQL capabilities, you can interface with MongoDB. In a stubbed or testing environment, Camel can route JSON directly to Mongo collections:</p>" +
            "<pre>- route:\n" +
            "    id: \"mongo-ingestion\"\n" +
            "    from: \"direct:insert-mongo\"\n" +
            "    steps:\n" +
            "      - to: \"mongodb:myDb?database=records&amp;collection=transactions&amp;operation=insert\"</pre>" +
            "<h3>3. Simulating Workloads with Faker, Timer, Kafka, and MQ</h3>" +
            "<p>You don't need live upstream systems to test high-throughput scenarios. Tessera's <code>Faker</code> component generates realistic mock data, driven by a <code>timer</code>, which is then pushed to messaging stubs (Kafka or IBM MQ).</p>" +
            "<h4>Example: Faker -&gt; Timer -&gt; Kafka</h4>" +
            "<pre>- route:\n" +
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
            "      - to: \"kafka:financial-transactions-topic?brokers=localhost:9092\"</pre>" +
            "<h4>Example: Faker -&gt; IBM MQ</h4>" +
            "<p>You can swap the target endpoint to IBM MQ seamlessly:</p>" +
            "<pre>      # 4. Push to IBM MQ Queue\n" +
            "      - to: \"jms:queue:DEV.QUEUE.1\"</pre>"));

        topics.add(new HelpTopic("Visual Pipeline Composer", "Advanced Tools", "visual pipeline composer drag drop canvas pan zoom monaco simulation", "fas-project-diagram",
            "<h1>Visual Pipeline Composer</h1>" +
            "<p>The Visual Pipeline Composer enables drag-and-drop routing and dynamic flow simulation.</p>" +
            "<h3>Core Features</h3>" +
            "<ul>" +
            "  <li><strong>Interactive Canvas:</strong> Pan, zoom, and fit-to-window operations.</li>" +
            "  <li><strong>Collapsible Catalog:</strong> Sidebar category grouping for sources, processors, and sinks.</li>" +
            "  <li><strong>Monaco Editor Integration:</strong> Custom scripts (Java, Groovy) with syntax highlighting.</li>" +
            "  <li><strong>Data Flow Simulation:</strong> Particle-based animations tracing payloads from source to sink.</li>" +
            "</ul>"));

        topics.add(new HelpTopic("Global Earmark & Credit Services Architecture", "Advanced Tools", "global earmark credit services architecture gee clue gls citi", "fas-sitemap",
            "<h1>Global Earmark & Credit Services Architecture</h1>" +
            "<p>A detailed architectural overview of the GEE (Global Earmarking Engine) and CLUE (Credit Engine) subsystems.</p>" +
            "<h3>Extended Reading Resources</h3>" +
            "<ul>" +
            "  <li><a href=\"file:///home/pratyush/Downloads/global-earmark-architecture.html\">Global Earmark Architecture Page (HTML)</a></li>" +
            "  <li><a href=\"file:///home/pratyush/Downloads/architecture.md\">Target State Architecture Document (Markdown)</a></li>" +
            "</ul>" +
            "<h3>Architectural Blueprint</h3>" +
            "<p>The <strong>Global Earmark and Credit Services</strong> platform is a real-time financial processing system that manages fund reservations across Citibank's global networks.</p>" +
            "<h3>Subsystems & Engines</h3>" +
            "<h4>1. Global Earmarking Engine (GEE)</h4>" +
            "<p>Acts as the brain of the platform. It handles:</p>" +
            "<ul>" +
            "  <li><strong>Sequencing & Prioritization:</strong> Redis-backed transactional queues order incoming requests.</li>" +
            "  <li><strong>Limit Verification:</strong> Checks transaction amounts against daily aggregate thresholds.</li>" +
            "  <li><strong>Partner Rules:</strong> Pluggable Drools-based rules engine for custom client integrations.</li>" +
            "</ul>" +
            "<h4>2. Credit Engine (CLUE)</h4>" +
            "<p>Acts as the underwriting engine for deficit transactions. It evaluates:</p>" +
            "<ul>" +
            "  <li><strong>Credit Checks:</strong> Validates client ratings against AMCAR.</li>" +
            "  <li><strong>Intraday Liquidity Pools:</strong> Allocates overnight credit lines via Treasury Services (FTS).</li>" +
            "</ul>" +
            "<h3>Pipeline Workflow Map</h3>" +
            "<pre>Request ──► [GEE: Sequencing] ──► [GEE: Limit Check] ──► [GEE: Partner Rules]\n" +
            "                                                              │\n" +
            "             Earmark Active ◄── Sufficient Balance ◄──────────┤\n" +
            "                                                              │ Insufficient\n" +
            "             Earmark Active ◄── [CLUE: Approve] ◄─────────────┘</pre>"));
    }

    private void buildUi() {
        stage.setTitle("RouteBuilder Studio Help System");

        BorderPane root = new BorderPane();
        // Carry both app-root and current theme class so CSS kicks in
        root.getStyleClass().addAll("app-root", RouteBuilderApp.currentThemeClass);
        com.tessera.ui.components.ThemeManager.registerRoot(root);
        // Keep the theme class in sync when user switches themes
        com.tessera.ui.components.ThemeManager.addListener(newTheme -> {
            root.getStyleClass().removeIf(c -> c.startsWith("theme-"));
            root.getStyleClass().add(com.tessera.ui.components.ThemeManager.getCurrentThemeClass());
            // Reload the currently displayed topic so its HTML colours refresh
            if (treeView != null && treeView.getSelectionModel().getSelectedItem() != null) {
                HelpItem item = treeView.getSelectionModel().getSelectedItem().getValue();
                if (item != null && item.topic != null) loadTopic(item.topic);
            }
        });

        // Top Toolbar
        ToolBar toolBar = new ToolBar();
        toolBar.getStyleClass().add("editor-toolbar");
        Label lblSearchIcon = new Label("", new FontIcon("fas-search"));
        searchField = new TextField();
        searchField.setPromptText("Search index or keywords...");
        searchField.setPrefWidth(280);
        searchField.getStyleClass().add("studio-search-field");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterTopics(newVal));

        Button btnClear = new Button("", new FontIcon("fas-times"));
        btnClear.getStyleClass().add("editor-btn");
        btnClear.setOnAction(e -> {
            searchField.clear();
            filterTopics("");
        });

        toolBar.getItems().addAll(lblSearchIcon, searchField, btnClear);
        root.setTop(toolBar);

        // Split Pane
        SplitPane splitPane = new SplitPane();
        splitPane.setDividerPositions(0.28);
        splitPane.getStyleClass().add("main-split-pane");

        // Sidebar: TreeView Index
        treeView = new TreeView<>();
        treeView.setShowRoot(false);
        treeView.getStyleClass().add("sidebar-tree-view");
        treeView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && newVal.getValue().topic != null) {
                loadTopic(newVal.getValue().topic);
            }
        });

        populateTree(null);

        VBox sidebar = new VBox(treeView);
        VBox.setVgrow(treeView, Priority.ALWAYS);
        sidebar.getStyleClass().add("studio-sidebar");

        // Right side: WebView Details
        webView = new WebView();
        webView.getStyleClass().add("help-web-view");
        webView.getEngine().loadContent("<html><body></body></html>");

        splitPane.getItems().addAll(sidebar, webView);
        root.setCenter(splitPane);

        Scene scene = new Scene(root, 1100, 750);
        scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
        stage.setScene(scene);

        // Selection Initialization
        if (initialSearch != null) {
            searchField.setText(initialSearch);
            filterTopics(initialSearch);
        }

        selectFirstAvailableTopic();
    }

    public void show() {
        stage.show();
        stage.toFront();
    }

    private void populateTree(String filter) {
        TreeItem<HelpItem> rootItem = new TreeItem<>(new HelpItem("Root", null));
        Map<String, TreeItem<HelpItem>> categoryNodes = new LinkedHashMap<>();

        boolean isFiltering = filter != null && !filter.trim().isEmpty();
        String query = isFiltering ? filter.toLowerCase() : "";

        for (HelpTopic topic : topics) {
            if (categoryFilter != null && !topic.category.equalsIgnoreCase(categoryFilter)) {
                continue;
            }

            if (isFiltering) {
                boolean matchesTitle = topic.title.toLowerCase().contains(query);
                boolean matchesKeywords = topic.keywords.toLowerCase().contains(query);
                boolean matchesContent = topic.contentHtml.toLowerCase().contains(query);
                if (!matchesTitle && !matchesKeywords && !matchesContent) {
                    continue; // Skip
                }
            }

            TreeItem<HelpItem> catNode = categoryNodes.get(topic.category);
            if (catNode == null) {
                catNode = new TreeItem<>(new HelpItem(topic.category, null));
                catNode.setExpanded(true);
                catNode.setGraphic(new FontIcon("fas-folder"));
                catNode.getGraphic().setStyle("-fx-text-fill: #d8a05e;");
                categoryNodes.put(topic.category, catNode);
                rootItem.getChildren().add(catNode);
            }

            TreeItem<HelpItem> topicNode = new TreeItem<>(new HelpItem(topic.title, topic));
            String iconName = (topic.iconName != null && !topic.iconName.isEmpty()) ? topic.iconName : "fas-file-alt";
            topicNode.setGraphic(new FontIcon(iconName));
            topicNode.getGraphic().setStyle("-fx-text-fill: #61afef;");
            catNode.getChildren().add(topicNode);
        }

        treeView.setRoot(rootItem);
    }

    private void filterTopics(String query) {
        populateTree(query);
    }

    private void loadTopic(HelpTopic topic) {
        // Pick colours to match the current app theme
        String themeName = RouteBuilderApp.currentThemeName != null ? RouteBuilderApp.currentThemeName : "VSCode Dark";
        boolean isLight = themeName.contains("Light") || themeName.contains("GitHub");
        boolean isDracula = themeName.contains("Dracula");
        boolean isMonokai = themeName.contains("Monokai");
        boolean isHacker = themeName.contains("Hacker");
        boolean isOneDark = themeName.contains("One Dark");
        boolean isSolarized = themeName.contains("Solarized");

        String bg, fg, h1, h2, h3, strong, pre, code, border, blockBg, blockBorder, trEven, thBg;
        if (isLight) {
            bg="#ffffff"; fg="#24292f"; h1="#1f2328"; h2="#0969da"; h3="#0550ae";
            strong="#953800"; pre="#f6f8fa"; code="#cf222e"; border="#d0d7de";
            blockBg="#f6f8fa"; blockBorder="#0969da"; trEven="#f6f8fa"; thBg="#eaeef2";
        } else if (isDracula) {
            bg="#282a36"; fg="#f8f8f2"; h1="#f8f8f2"; h2="#ffb86c"; h3="#8be9fd";
            strong="#ffb86c"; pre="#21222c"; code="#ff79c6"; border="#44475a";
            blockBg="#21222c"; blockBorder="#bd93f9"; trEven="#21222c"; thBg="#282a36";
        } else if (isMonokai) {
            bg="#272822"; fg="#f8f8f2"; h1="#f8f8f2"; h2="#e6db74"; h3="#66d9e8";
            strong="#e6db74"; pre="#1e1f1c"; code="#f92672"; border="#75715e";
            blockBg="#1e1f1c"; blockBorder="#a6e22e"; trEven="#1e1f1c"; thBg="#272822";
        } else if (isHacker) {
            bg="#050505"; fg="#00cc00"; h1="#00ff00"; h2="#00cc00"; h3="#00ff41";
            strong="#00ff00"; pre="#0d0d0d"; code="#00ff41"; border="#004d00";
            blockBg="#0d0d0d"; blockBorder="#00ff00"; trEven="#0d0d0d"; thBg="#050505";
        } else if (isOneDark) {
            bg="#282c34"; fg="#abb2bf"; h1="#ffffff"; h2="#e5c07b"; h3="#61afef";
            strong="#e5c07b"; pre="#21252b"; code="#e06c75"; border="#3e4451";
            blockBg="#21252b"; blockBorder="#c678dd"; trEven="#21252b"; thBg="#282c34";
        } else if (isSolarized) {
            bg="#002b36"; fg="#839496"; h1="#93a1a1"; h2="#b58900"; h3="#268bd2";
            strong="#b58900"; pre="#073642"; code="#dc322f"; border="#073642";
            blockBg="#073642"; blockBorder="#268bd2"; trEven="#073642"; thBg="#002b36";
        } else {
            // VSCode Dark (default)
            bg="#1e1e22"; fg="#abb2bf"; h1="#ffffff"; h2="#e5c07b"; h3="#61afef";
            strong="#e5c07b"; pre="#282c34"; code="#e06c75"; border="#3e4451";
            blockBg="#21252b"; blockBorder="#c678dd"; trEven="#21252b"; thBg="#282c34";
        }

        String html = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "  <meta charset=\"utf-8\">\n" +
            "  <style>\n" +
            "    body { font-family: 'Segoe UI', -apple-system, BlinkMacSystemFont, Roboto, Helvetica, Arial, sans-serif; padding: 25px 30px; background-color: " + bg + "; color: " + fg + "; line-height: 1.6; }\n" +
            "    h1 { color: " + h1 + "; border-bottom: 2px solid " + border + "; padding-bottom: 8px; font-weight: 500; font-size: 28px; margin-top: 0; }\n" +
            "    h2 { color: " + h2 + "; border-bottom: 1px solid " + border + "; padding-bottom: 6px; font-weight: 400; font-size: 22px; margin-top: 25px; }\n" +
            "    h3 { color: " + h3 + "; font-weight: 500; font-size: 16px; margin-top: 20px; }\n" +
            "    p { margin: 10px 0 15px 0; }\n" +
            "    a { color: " + h3 + "; text-decoration: none; }\n" +
            "    a:hover { text-decoration: underline; }\n" +
            "    ul { padding-left: 20px; margin: 10px 0; }\n" +
            "    li { margin-bottom: 8px; }\n" +
            "    strong { color: " + strong + "; font-weight: 600; }\n" +
            "    table { border-collapse: collapse; width: 100%; margin: 20px 0 10px 0; font-size: 14px; }\n" +
            "    th, td { border: 1px solid " + border + "; padding: 10px 12px; text-align: left; }\n" +
            "    th { background-color: " + thBg + "; color: " + h2 + "; font-weight: 500; }\n" +
            "    tr:nth-child(even) { background-color: " + trEven + "; }\n" +
            "    pre { background-color: " + pre + "; padding: 12px 16px; border-radius: 6px; overflow-x: auto; border: 1px solid " + border + "; color: " + fg + "; font-family: 'Consolas', 'Fira Code', 'Courier New', monospace; font-size: 13px; }\n" +
            "    code { font-family: 'Consolas', 'Fira Code', 'Courier New', monospace; background-color: " + pre + "; padding: 2px 5px; border-radius: 4px; color: " + code + "; font-size: 90%; }\n" +
            "    blockquote { border-left: 4px solid " + blockBorder + "; margin: 15px 0; padding: 5px 15px; color: " + fg + "; background-color: " + blockBg + "; border-radius: 0 4px 4px 0; }\n" +
            "    .animated-element { backface-visibility: hidden; will-change: transform, opacity; }\n" +
            "    .piece-tl { transform: translate(-350px, -350px) rotate(-60deg); opacity: 0; }\n" +
            "    .piece-tr { transform: translate(350px, -350px) rotate(60deg); opacity: 0; }\n" +
            "    .piece-bl { transform: translate(-350px, 350px) rotate(-60deg); opacity: 0; }\n" +
            "    .piece-br { transform: translate(350px, 350px) rotate(60deg); opacity: 0; }\n" +
            "    .tri-left { transform: translate(-180px, 20px); opacity: 0; }\n" +
            "    .tri-right { transform: translate(180px, 20px); opacity: 0; }\n" +
            "    .text-title { transform: translateY(35px); opacity: 0; }\n" +
            "    .text-sub { transform: translateY(35px); opacity: 0; }\n" +
            "    .play-animation .piece-tl { animation: assembleTL 1.4s cubic-bezier(0.16, 1, 0.3, 1) forwards; }\n" +
            "    .play-animation .piece-tr { animation: assembleTR 1.4s cubic-bezier(0.16, 1, 0.3, 1) forwards; }\n" +
            "    .play-animation .piece-bl { animation: assembleBL 1.4s cubic-bezier(0.16, 1, 0.3, 1) forwards; }\n" +
            "    .play-animation .piece-br { animation: assembleBR 1.4s cubic-bezier(0.16, 1, 0.3, 1) forwards; }\n" +
            "    .play-animation .tri-left { animation: slideTriLeft 1.2s cubic-bezier(0.16, 1, 0.3, 1) 0.2s forwards; }\n" +
            "    .play-animation .tri-right { animation: slideTriRight 1.2s cubic-bezier(0.16, 1, 0.3, 1) 0.2s forwards; }\n" +
            "    .play-animation .text-title { animation: textFadeUp 1.2s cubic-bezier(0.16, 1, 0.3, 1) 0.5s forwards; }\n" +
            "    .play-animation .text-sub { animation: textFadeUp 1.2s cubic-bezier(0.16, 1, 0.3, 1) 0.75s forwards; }\n" +
            "    @keyframes assembleTL { 100% { transform: translate(0, 0) rotate(0deg); opacity: 1; } }\n" +
            "    @keyframes assembleTR { 100% { transform: translate(0, 0) rotate(0deg); opacity: 1; } }\n" +
            "    @keyframes assembleBL { 100% { transform: translate(0, 0) rotate(0deg); opacity: 1; } }\n" +
            "    @keyframes assembleBR { 100% { transform: translate(0, 0) rotate(0deg); opacity: 1; } }\n" +
            "    @keyframes slideTriLeft { 100% { transform: translate(0, 0); opacity: 1; } }\n" +
            "    @keyframes slideTriRight { 100% { transform: translate(0, 0); opacity: 1; } }\n" +
            "    @keyframes textFadeUp { 100% { transform: translateY(0); opacity: 1; } }\n" +
            "  </style>\n" +
            "</head>\n" +
            "<body>\n" +
            topic.contentHtml +
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
            "        'editor.background': '" + bg + "',\n" +
            "        'editorGutter.background': '" + bg + "'\n" +
            "      }\n" +
            "    });\n" +
            "    monaco.editor.defineTheme('custom-light', {\n" +
            "      base: 'vs',\n" +
            "      inherit: true,\n" +
            "      rules: [],\n" +
            "      colors: {\n" +
            "        'editor.background': '" + bg + "',\n" +
            "        'editorGutter.background': '" + bg + "'\n" +
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
            "      pre.style.border = '1px solid " + border + "';\n" +
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
            "</body>\n" +
            "</html>";

        Platform.runLater(() -> webView.getEngine().loadContent(html));
    }

    private void selectFirstAvailableTopic() {
        if (treeView.getRoot() == null || treeView.getRoot().getChildren().isEmpty()) return;
        TreeItem<HelpItem> firstCat = treeView.getRoot().getChildren().get(0);
        if (!firstCat.getChildren().isEmpty()) {
            TreeItem<HelpItem> firstTopicNode = firstCat.getChildren().get(0);
            treeView.getSelectionModel().select(firstTopicNode);
            loadTopic(firstTopicNode.getValue().topic);
        }
    }
}
