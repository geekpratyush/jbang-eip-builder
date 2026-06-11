# System Design Tool Engineering Specification Prompt

You are an expert Frontend Engineer and UI/UX Designer specialized in building enterprise-grade, browser-based diagramming software (such as Miro, Lucidchart, or Draw.io). Your goal is to generate a fully complete, production-ready, self-contained single-file prototype (`index.html`) implementing the **Nestable Enterprise Subsystems Topology & Flow Modeler** described in the technical specifications below. 

The application must require zero external compilation steps, zero local servers, and no build frameworks. It should rely exclusively on raw HTML5, CSS3, modern Vanilla JavaScript (ES6+), Font Awesome 6 for system iconography, and a Google Web Font integration.

---

## 1. Core Technical Architecture Requirements

### A. The Structural Taxonomy Registry
The tool requires a left-hand navigation pane split into three animated, distinct tabs. Inside each tab, components must be nested into clear, collapsible grouping accordions:
1. **Actors Tab:**
   - *Internal Enclosures Group:* Staff User (`fa-user-tie`), System Ops Team (`fa-users-gear`), Automated Microservice Agent (`fa-robot`).
   - *External Enclosures Group:* Third-Party Partner Ecosystem (`fa-handshake`), Retail Consumer Group (`fa-user-astronaut`), Regulatory Body (`fa-building-shield`).
2. **Components Tab:**
   - *Streaming & Message Brokers Group:* Apache Kafka (`fa-code-fork`), IBM MQ (`fa-layer-group`), JMS Broker (`fa-envelope-open-text`).
   - *State & Storage Engines Group:* MongoDB Cluster (`fa-leaf`), Oracle Server Core (`fa-database`), SQL Server (`fa-server`).
   - *API Network Interfaces Group:* RESTful Web API (`fa-link`), gRPC Protocol Engine (`fa-bolt`), GraphQL Core Gateway (`fa-diagram-next`).
3. **Logic Flow Nodes Tab:**
   - *Workflow Control Group:* Start Boundary Anchor (`fa-play`), Decision Control Gate (`fa-code-branch`), Dynamic Route Rule Engine (`fa-route`), Splitter/Aggregator Core (`fa-network-wired`), End Target Boundary (`fa-stop`).
   - *Data Payloads Group:* Envelope Message (`fa-file-code`), Single Database Record Tuple (`fa-list-check`), Stream Event Notification (`fa-bolt-lightning`).

### B. Nested Subsystem Topology (Spatial Containment Engine)
- **Actors as Boundary Boxes:** Actors are not standard small icons; they must be generated as large, dashed-border, resizable enclosure layout boxes ("homes/containers"). 
- **Subsystem Nesting:** Users must be able to drag components (like Kafka, IBM MQ, or a REST API) out of the sidebar palette and drop them *directly inside* an Actor home container.
- **Intersection Tracking:** Implement spatial boundary checking logic. When an element is dropped, check if its drop coordinates fall inside the bounds of an Actor box. If true, map that node as a child of the parent Actor (`parentContainerId: "actor_id"`).
- **Cascading Group Movements:** When a user clicks and drags an Actor container, all nested child components must move together perfectly in sync, maintaining their relative inner offsets.

### C. Animated Vector Fluid Flow Lines (Connectors Engine)
- **Flexible Connector Paths:** Compute dynamic, non-overlapping Elastic Cubic-Bézier SVG paths to connect nodes together seamlessly. 
- **Connecting Anything to Anything:** Allow direct connections from an inside nested element to another inside element, an inside element to an outside element, or even from the parent Actor box's outer walls directly to external nodes.
- **Data Flow Animations:** Do not use static lines. Use CSS line-dashing and keyframe animation arrays (`stroke-dasharray`, `stroke-dashoffset`) to make connections look like fluid, moving flow lines carrying active system data.
- **Tri-Directional Flow Modifiers:** Every vector connection line must support three interchangeable routing modes, configurable via the side property deck:
  1. *Left-to-Right:* Fluid data moving forward with an arrowhead pointing at the target element.
  2. *Right-to-Left:* Fluid data moving backwards with a reverse arrowhead pointing at the origin element.
  3. *Bi-Directional Handshake:* Fluid data moving outward simultaneously, displaying dual arrowheads on both ends of the path to represent two-way requests/handshakes.

### D. Hover HUD Overlay Menu & Inlined Context Configuration Deck
- **Micro-Overlay HUD Ribbon:** When hovering over *any* element (Actor, component, node, or vector flow line), an elegant micro-toolbar menu (HUD) must gracefully transition into view right above it.
- **HUD Target Capabilities:** The HUD ribbon must feature separate buttons to instantly trigger:
  1. *Focus/Edit Name:* Select item and shift focus to the label field.
  2. *Cycle Accent Color:* Instantly change the item's main theme through a predefined grid array of colors (`#2563eb`, `#16a34a`, `#7c3aed`, `#ea580c`, `#dc2626`).
  3. *Configure Structure Details:* Open an inline sub-builder inside the right-hand panel.
  4. *Delete/Prune:* Safely remove the element or line from the diagram, automatically cleaning up all attached vector paths.
- **Structure Details Modeler Panel:** Selecting a data payload node (`Message`, `Record`, `Event`) or a router node (`Decision Box`, `Route Rule`) must reveal a dynamic list engine in the side configurations deck:
  - For *Data Nodes*, users can build out custom model field schemas (e.g., typing out field definitions like `user_id (String)`, `balance (Double)` and saving them).
  - For *Logic/Routing Nodes*, users can build out conditional execution arrays (e.g., typing out conditional match statements like `if (amount > 10000)` or `if (header.retry_count <= 3)`).
- **Comprehensive Metadata Preservation:** Every single custom detail, field string, route rule statement, element coordinates position, and active connection direction *must* be saved cleanly within the runtime data engine object.

### E. Advanced Infinite Workspace Viewport
- **Matrix Zoom Adjustments:** Implement trackpad/mouse-wheel capturing loops that apply native hardware CSS transformations (`transform: scale(n)`) ranging smoothly from `0.45x` to `2.2x`.
- **Infinite Workspace Panning:** Allow users to pan across a large, hidden grid workspace (e.g., 5000px by 5000px) by clicking and dragging on any empty background area.
- **Visual Grid Tracking:** Set up a clean, minimal linear or radial background grid pattern that scales and shifts accurately alongside all user pans and zoom actions.

### F. Dual-Engine Bidirectional Code Syncing & Asset Exporters
- **Full JSON Serialization Code Box:** Build an accessible code workspace overlay modal that displays the complete structure of the diagram as a clean, formatted JSON schema string. This schema must capture all node positions, hierarchies, structural field parameters, and route rule arrays.
- **Hot-Reload Code Compiler:** Users must be able to edit this code string directly or paste in a previously saved blueprint schema block, clicking "Hot-Reload" to instantly re-render the workspace layout on screen.
- **Independent High-Res SVG Image Exporter:** Create an on-the-fly compiler asset export engine that captures the current active SVG layout elements, inline font definitions, and node blocks, packaging them into a clean, standalone, decoupled `.svg` graphic document ready for instant user downloads.

---

## 2. Visual Style & UI/UX Design Directives

Ensure the entire workspace uses a high-end, premium dark developer theme:
- **Color Theme Matrix:** Deep slate background primitives (`#09090b`), secondary panel frames (`#111115`), configuration fields decks (`#181822`), thin tech borders (`#272732`), and vivid active text elements (`#f4f4f5`).
- **Accent Palette Highlights:** Corporate Indigo Blue (`#2563eb`), Security Emerald Green (`#16a34a`), Integration Amethyst Purple (`#7c3aed`), Warning Tangerine Orange (`#ea580c`), and Operational Crimson Red (`#dc2626`).
- **Typography Layout Guidelines:** Body layout uses clean modern sans-serif values (`Inter`). Code areas, unique block IDs, schema array elements, and live text input boxes must display structured code text formats (`JetBrains Mono`).

---

## 3. Delivery Requirements

Generate the complete solution within a single block of code. Do not write placeholder sections, abbreviated scripts, or omit functions. Ensure all HTML layout containers, inline custom CSS blocks, Font Awesome icon definitions, intersection validation models, and SVG vector connection calculation systems are fully functional and ready to run immediately in a standard modern browser.