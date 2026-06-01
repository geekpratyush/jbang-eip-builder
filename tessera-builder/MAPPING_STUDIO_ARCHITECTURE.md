# Sovereign Mapping Architect - Architecture & Implementation Guide

This document outlines the architecture, technology stack, and visual styling used to build the **Sovereign Mapping Architect**. This serves as a baseline and guide for any future modifications to the XSLT/JSLT visual mapping component.

---

## 1. Core Architecture
The Mapping Studio uses a **Highly Decoupled, Metadata-Driven Architecture**. Instead of manually drawing complex nodes and curves using JavaFX `Canvas` (which is prone to coordinate tracking issues and lag), the architecture splits the responsibility into three domains:

1.  **Metadata Extraction (Java/JAXP)**: Parses the complex XSLT/JSLT into an agnostic graph model (Nodes and Links).
2.  **Host & Code Preview (JavaFX WebView)**: Hosts the visualizer and a Monaco Editor instance.
3.  **Visualization Engine (HTML5/D3.js)**: Receives the graph model and renders an interactive, physics-based Directed Acyclic Graph (DAG).

---

## 2. Technology Stack

### Backend (Java)
*   **JavaFX `WebView` & `WebEngine`**: Embedded browser used to render both the D3.js mapping visualizer and the Monaco code editor.
*   **JAXP (Java API for XML Processing)**: Used in `MappingParser.java` for deep DOM traversal of XSLT files. It handles extraction of `<xsl:template>`, `<xsl:value-of>`, `<xsl:variable>`, `<xsl:choose>`, etc.
*   **Jackson (`ObjectMapper`) & `org.json`**: Used to parse sample payload data (XML/JSON source schemas) and to serialize the entire graph structure into a JSON string that D3.js can ingest.
*   **`com.sun.net.httpserver.HttpServer`**: A lightweight local HTTP server spun up dynamically on an ephemeral port. It serves local assets (Monaco Editor scripts, `mapper.html`, CSS, TTF fonts) to the `WebView` securely, circumventing local file protocol restrictions.

### Frontend (Visualizer)
*   **D3.js (v7)**: The core graphing engine used in `mapper.html`. It handles:
    *   **Force-Directed Layout (`d3.forceSimulation`)**: Gently separates nodes vertically (`d3.forceY`) and resolves collisions (`d3.forceCollide`), preventing overlapping nodes.
    *   **Bezier Curve Generation (`d3.linkHorizontal`)**: Calculates the mathematical path of the wires.
    *   **Drag & Drop (`d3.drag`)**: Enables moving nodes smoothly across the canvas.
*   **CSS & SVG**: Used to style the nodes, animate the particles, and create the deep "Cyberpunk" glow effects.

---

## 3. The Data Flow Pipeline

When the user clicks "Sovereign Mapping Architect" in the Transformation Studio:

1.  **Trigger**: `TransformationStudioWindow.java` captures the current logic code (XSLT/JSLT) and the source payload (XML/JSON).
2.  **Parsing**: `MappingParser.parseXsltToGraph()` is called. 
    *   It recursively parses the Source Schema into `SOURCE` nodes.
    *   It parses the XSLT DOM, creating `TEMPLATE`, `LOGIC`, `VARIABLE`, `CONDITION`, and `TARGET` nodes.
    *   It links these nodes together based on `match` and `select` attributes.
3.  **Injection**: The generated JSON is URI-encoded and injected into the D3.js environment via `engine.executeScript("renderGraph(...)");`.
4.  **Rendering**: D3.js reads the JSON, locks the X-coordinates to strict columns based on the node type (`d.fx`), and lets the physics engine distribute them vertically.

---

## 4. Visual Aesthetics & Styling Rules ("Hacker / Cyberpunk" Look)

To maintain the high-fidelity, visually stunning look, any future updates should adhere to the following styling rules defined in `mapper.html`:

*   **Background**: Deep dark `#010101` with a CSS-generated geometric blueprint grid (`#grid-pattern`).
*   **Color Palette**:
    *   `SOURCE` Nodes: Matrix Green (`#00FF41`)
    *   `TEMPLATE` / `MATCH` Nodes: Neon Cyan (`#00E5FF`)
    *   `LOGIC` (Value-of) Nodes: YellowGreen (`#adff2f`)
    *   `TARGET` Output Nodes: Neon Pink (`#FF00FF`)
    *   `VARIABLE` Nodes: Yellow (`#FFFF00`)
    *   `CONDITION` / `BRANCH` Nodes: Orange (`#FF9800`, `#FF5722`)
*   **Glowing Connections (Neural Streams)**:
    *   Lines use a base opacity of `0.85` and a stroke width of `3.5px`.
    *   Achieved via dual SVG Drop Shadows: `filter: drop-shadow(0 0 5px currentColor) drop-shadow(0 0 10px currentColor);`
*   **Animated Data Packets**: 
    *   White SVG `<circle>` elements travel along the Bezier curves using D3's `attrTween("transform")` and `path.getPointAtLength()`.
*   **Typography**: `JetBrains Mono` and `Monospaced` are enforced for all terminal-like text.

---

## 5. Guide for Future Modifications

If you need to change how the mapping studio behaves, refer to this guide:

**1. I need to add support for a new XSLT tag (e.g., `<xsl:sort>`)**
*   **Edit:** `MappingParser.java`
*   **Method:** `processXslElement(...)`
*   **Action:** Add a new `case "sort":`, generate a node with a specific type (e.g., "LOGIC"), and define how its `select` attribute links to other nodes.

**2. I need to change the horizontal spacing between columns**
*   **Edit:** `src/main/resources/visualizer/mapper.html`
*   **Location:** Inside `renderGraph(...)`, modify the `typeX` dictionary values.
*   *Example:* `const typeX = { "SOURCE": 100, "TEMPLATE": 500, ... };`

**3. I need to change how lines look or behave on hover**
*   **Edit:** `src/main/resources/visualizer/mapper.html`
*   **Location:** The `<style>` block for `.link` and `.link:hover`.

**4. I need to handle large files that crash the force simulation**
*   **Edit:** `src/main/resources/visualizer/mapper.html`
*   **Location:** Adjust the simulation parameters. For extremely large graphs, lower the `strength` of the collision force or limit the maximum simulation ticks to speed up the render.

**5. I need to pass data back from the Graph to the Java Code (Bi-directional mapping)**
*   *Implementation requirement:* You will need to expose a Java object to JavaScript using `JSObject window = (JSObject) engine.executeScript("window"); window.setMember("javaApp", javaCallbackInterface);`. You can then call `window.javaApp.saveMapping(...)` from within `mapper.html` when a user draws a new line.