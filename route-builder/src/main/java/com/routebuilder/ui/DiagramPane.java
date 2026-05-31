package com.routebuilder.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiagramPane extends VBox {

    private Pane diagramContainer;
    private StackPane canvasPane;
    private Group zoomGroup;
    private ObjectMapper yamlMapper;
    private Scale zoomScale;
    
    private boolean isHorizontal = false;
    private boolean isStackedSideBySide = true;
    private String currentYaml = "";
    private List<String> currentYamls = null;
    private File currentFile;
    private String currentTheme = "VSCode Dark";
    private Consumer<String> yamlUpdater;
    private JsonNode rootNode;
    private VBox propertyPane;
    private Pane selectedNodeUi;
    private StackPane contentStack;
    private ScrollPane propScroll;
    private Runnable onClose, onMaximize;

    // Shared toolbar action state (reused by both inline and detached toolbars)
    private Button inlineLBtn, inlineStackBtn;
    private javafx.stage.FileChooser svgFileChooser;
    private ContextMenu activeContextMenu;
    private ClassDiagramPane classDiagramPane;
    
    private Stage detachedStage;
    private Label detachedPlaceholder;
    private Preferences prefs = Preferences.userNodeForPackage(DiagramPane.class);

    public void setCurrentFile(File file) { this.currentFile = file; }
    public void setOnClose(Runnable onClose) { this.onClose = onClose; }
    public void setOnMaximize(Runnable onMaximize) { this.onMaximize = onMaximize; }

    public void setTheme(String theme) {
        this.currentTheme = theme;
        if (classDiagramPane != null) classDiagramPane.setTheme(theme);
        if (currentYaml != null && !currentYaml.isEmpty()) renderDiagram(currentYaml);
    }

    public DiagramPane(Consumer<String> themeChanger, Consumer<String> yamlUpdater) {
        this.yamlUpdater = yamlUpdater;
        com.routebuilder.ui.components.ThemeManager.registerRoot(this);
        this.isHorizontal = prefs.getBoolean("isHorizontal", false);
        this.isStackedSideBySide = prefs.getBoolean("isStackedSideBySide", true);

        setMinWidth(0); setMinHeight(0);
        getStyleClass().add("diagram-pane");
        yamlMapper = new ObjectMapper(new YAMLFactory()).enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);

        Label title = new Label("VISUAL: EIP DIAGRAM");
        title.getStyleClass().add("pane-title");

        HBox toolbar = buildToolbar(false);
        rebuildContainer();

        zoomGroup = new Group(diagramContainer);
        canvasPane = createScrollableCanvas();

        propertyPane = new VBox(15);
        propertyPane.setPadding(new Insets(15));
        propertyPane.getStyleClass().add("property-pane");
        propertyPane.setStyle("-fx-background-color: -sui-bg-primary; -fx-border-color: -sui-accent-primary; -fx-border-width: 1px; -fx-background-radius: 8; -fx-border-radius: 8;");

        propScroll = new ScrollPane(propertyPane);
        propScroll.getStyleClass().add("property-scroll");
        propScroll.setFitToWidth(true);
        propScroll.setMinWidth(450); propScroll.setMaxWidth(500);
        propScroll.setPrefHeight(600); propScroll.setMaxHeight(800);
        propScroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        propScroll.setEffect(new javafx.scene.effect.DropShadow(20, Color.rgb(0, 0, 0, 0.7)));

        contentStack = new StackPane(canvasPane);
        VBox.setVgrow(contentStack, Priority.ALWAYS);

        detachedPlaceholder = new Label("DIAGRAM IS OPEN IN A SEPARATE WINDOW");
        detachedPlaceholder.setStyle("-fx-text-fill: #555; -fx-font-size: 18px; -fx-font-weight: bold;");
        detachedPlaceholder.setAlignment(Pos.CENTER);
        detachedPlaceholder.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        getChildren().addAll(title, toolbar, contentStack);
    }

    /** Builds the diagram toolbar. When {@code detached=true} the close button re-docks instead of
     *  closing the panel, and the detach button is replaced by a "Return to Panel" button. */
    private HBox buildToolbar(boolean detached) {
        HBox tb = new HBox(12);
        tb.setAlignment(Pos.CENTER_LEFT);
        tb.setPadding(new Insets(10));
        tb.getStyleClass().add("diagram-toolbar");

        // --- Flow direction toggle ---
        Button lBtn = new Button();
        lBtn.setGraphic(new FontIcon(isHorizontal ? "fas-long-arrow-alt-right" : "fas-long-arrow-alt-down"));
        lBtn.getStyleClass().add("editor-btn");
        lBtn.setTooltip(new Tooltip("Toggle Flow (Top-Down / Left-Right)"));
        lBtn.setOnAction(e -> {
            isHorizontal = !isHorizontal;
            prefs.putBoolean("isHorizontal", isHorizontal);
            // keep both toolbar copies in sync
            lBtn.setGraphic(new FontIcon(isHorizontal ? "fas-long-arrow-alt-right" : "fas-long-arrow-alt-down"));
            if (inlineLBtn != null && inlineLBtn != lBtn)
                inlineLBtn.setGraphic(new FontIcon(isHorizontal ? "fas-long-arrow-alt-right" : "fas-long-arrow-alt-down"));
            rebuildContainer(); renderFromRoot();
        });
        if (!detached) inlineLBtn = lBtn;

        // --- Multi-route stacking toggle ---
        Button stackBtn = new Button();
        stackBtn.setGraphic(new FontIcon("fas-layer-group"));
        stackBtn.getStyleClass().add("editor-btn");
        stackBtn.setTooltip(new Tooltip("Toggle Multi-Route Stacking (Side-by-Side / Vertical)"));
        stackBtn.setOnAction(e -> {
            isStackedSideBySide = !isStackedSideBySide;
            prefs.putBoolean("isStackedSideBySide", isStackedSideBySide);
            rebuildContainer(); renderFromRoot();
        });

        // --- Zoom controls (initialise zoomScale once) ---
        if (!detached && zoomScale == null) zoomScale = new Scale(1, 1);
        Button zIn = new Button("", new FontIcon("fas-search-plus")); zIn.getStyleClass().add("editor-btn");
        zIn.setTooltip(new Tooltip("Zoom In"));
        zIn.setOnAction(e -> { zoomScale.setX(zoomScale.getX() * 1.1); zoomScale.setY(zoomScale.getY() * 1.1); });
        Button zOut = new Button("", new FontIcon("fas-search-minus")); zOut.getStyleClass().add("editor-btn");
        zOut.setTooltip(new Tooltip("Zoom Out"));
        zOut.setOnAction(e -> { zoomScale.setX(zoomScale.getX() * 0.9); zoomScale.setY(zoomScale.getY() * 0.9); });

        Button fitBtn = new Button("", new FontIcon("fas-compress")); fitBtn.getStyleClass().add("editor-btn");
        fitBtn.setTooltip(new Tooltip("Fit to Screen / Reset View"));
        fitBtn.setOnAction(e -> {
            zoomScale.setX(1.0); zoomScale.setY(1.0);
            zoomGroup.setTranslateX(0); zoomGroup.setTranslateY(0);
            rebuildContainer(); renderFromRoot();
        });

        // --- Copy image ---
        Button copyImg = new Button("", new FontIcon("fas-copy")); copyImg.getStyleClass().add("editor-btn");
        copyImg.setTooltip(new Tooltip("Copy Diagram to Clipboard"));
        copyImg.setOnAction(e -> {
            javafx.scene.image.WritableImage img = snapshotDiagram();
            if (img == null) return;
            ClipboardContent c = new ClipboardContent(); c.putImage(img);
            Clipboard.getSystemClipboard().setContent(c);
        });

        // --- Save as SVG ---
        Button svgBtn = new Button("", new FontIcon("fas-file-export")); svgBtn.getStyleClass().add("editor-btn");
        svgBtn.setTooltip(new Tooltip("Save Diagram as SVG"));
        svgBtn.setOnAction(e -> saveSvg());

        // --- Pop-out / Re-dock ---
        Button detachBtn = new Button();
        detachBtn.setGraphic(new FontIcon(detached ? "fas-compress-arrows-alt" : "fas-external-link-alt"));
        detachBtn.getStyleClass().add("editor-btn");
        detachBtn.setTooltip(new Tooltip(detached ? "Return to Panel" : "Pop-out Diagram to Separate Window"));
        detachBtn.setOnAction(e -> toggleDetach());

        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        // --- Close (only on the inline panel) ---
        if (!detached) {
            Button clsBtn = new Button("", new FontIcon("fas-times")); clsBtn.getStyleClass().add("editor-btn");
            clsBtn.setTooltip(new Tooltip("Close Diagram Panel"));
            clsBtn.setOnAction(e -> { if (onClose != null) onClose.run(); });
            tb.getChildren().addAll(lBtn, stackBtn, zIn, zOut, fitBtn, new Separator(), copyImg, svgBtn, detachBtn, spacer, clsBtn);
        } else {
            tb.getChildren().addAll(lBtn, stackBtn, zIn, zOut, fitBtn, new Separator(), copyImg, svgBtn, detachBtn, spacer);
        }
        return tb;
    }

    /** Exports the current diagram as a basic SVG using a JavaFX snapshot converted to a PNG data-URI embedded in SVG. */
    private void saveSvg() {
        if (diagramContainer == null) return;
        if (svgFileChooser == null) {
            svgFileChooser = new javafx.stage.FileChooser();
            svgFileChooser.setTitle("Save Diagram as SVG");
            svgFileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("SVG Image", "*.svg"));
            if (currentFile != null) svgFileChooser.setInitialFileName(
                currentFile.getName().replaceAll("\\.[^.]+$", "") + "-diagram.svg");
        }
        javafx.stage.Window owner = getScene() != null ? getScene().getWindow() :
                (detachedStage != null ? detachedStage : null);
        File out = svgFileChooser.showSaveDialog(owner);
        if (out == null) return;

        try {
            // Snapshot to PNG then embed as a data-URI inside an SVG envelope
            javafx.scene.image.WritableImage wi = snapshotDiagram();
            if (wi == null) return;
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            javax.imageio.ImageIO.write(
                javafx.embed.swing.SwingFXUtils.fromFXImage(wi, null), "PNG", bos);
            String b64 = java.util.Base64.getEncoder().encodeToString(bos.toByteArray());
            int w = (int) wi.getWidth(), h = (int) wi.getHeight();
            String svg = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"
                + "<svg xmlns=\"http://www.w3.org/2000/svg\" "
                + "xmlns:xlink=\"http://www.w3.org/1999/xlink\" "
                + "width=\"" + w + "\" height=\"" + h + "\">\n"
                + "  <image xlink:href=\"data:image/png;base64," + b64 + "\" "
                + "x=\"0\" y=\"0\" width=\"" + w + "\" height=\"" + h + "\"/>\n"
                + "</svg>\n";
            java.nio.file.Files.writeString(out.toPath(), svg);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void toggleDetach() {
        if (detachedStage == null) {
            detachedStage = new Stage();
            detachedStage.setTitle("EIP Diagram — " + (currentFile != null ? currentFile.getName() : "Untitled"));
            getChildren().remove(contentStack); getChildren().add(detachedPlaceholder);

            // Build a full toolbar for the detached window
            HBox detachedToolbar = buildToolbar(true);

            BorderPane detachedRoot = new BorderPane();
            detachedRoot.setTop(detachedToolbar);
            detachedRoot.setCenter(contentStack);
            detachedRoot.getStyleClass().addAll("app-root", RouteBuilderApp.currentThemeClass);
            com.routebuilder.ui.components.ThemeManager.registerRoot(detachedRoot);

            Scene scene = new Scene(detachedRoot, 1280, 860);
            scene.getStylesheets().add(getClass().getResource("/styles/main.css").toExternalForm());
            detachedStage.setScene(scene);
            detachedStage.setOnCloseRequest(e -> toggleDetach());
            detachedStage.show();
        } else {
            detachedStage.close(); detachedStage = null;
            getChildren().remove(detachedPlaceholder); getChildren().add(contentStack);
        }
    }

    private StackPane createScrollableCanvas() {
        StackPane cp = new StackPane(zoomGroup); cp.setAlignment(Pos.TOP_LEFT);
        Rectangle clip = new Rectangle(); clip.widthProperty().bind(cp.widthProperty()); clip.heightProperty().bind(cp.heightProperty()); cp.setClip(clip);
        cp.setOnScroll(ev -> {
            if (ev.getDeltaY() != 0) {
                double f = ev.getDeltaY() > 0 ? 1.1 : 0.9;
                double s = Math.max(0.1, Math.min(10.0, zoomScale.getX()*f));
                zoomScale.setX(s); zoomScale.setY(s); ev.consume();
            }
        });
        
        ContextMenu canvasMenu = new ContextMenu();
        canvasMenu.getItems().addAll(
            new MenuItem("New Route", new FontIcon("fas-route")) {{ setOnAction(e -> {
                try {
                    JsonNode n = yamlMapper.readTree("- route:\n    from:\n      uri: direct:start\n      steps:\n        - log: New Route started");
                    if (rootNode == null || !rootNode.isArray()) rootNode = yamlMapper.createArrayNode();
                    ((ArrayNode)rootNode).add(n.get(0)); updateYamlFromRoot();
                } catch (Exception ignored) {}
            }); }},
            new MenuItem("New Beans Configuration", new FontIcon("fas-cubes")) {{ setOnAction(e -> {
                try {
                    JsonNode n = yamlMapper.readTree("- beans:\n    - name: \"auditProcessor\"\n      type: \"com.sre.engine.audit.AuditShaperProcessor\"");
                    if (rootNode == null || !rootNode.isArray()) rootNode = yamlMapper.createArrayNode();
                    ((ArrayNode)rootNode).add(n.get(0)); updateYamlFromRoot();
                } catch (Exception ignored) {}
            }); }},
            new MenuItem("New Global Exception", new FontIcon("fas-exclamation-triangle")) {{ setOnAction(e -> {
                try {
                    JsonNode n = yamlMapper.readTree("- onException:\n    exception: [\"java.lang.Exception\"]\n    steps:\n      - log: Caught Exception");
                    if (rootNode != null && rootNode.isArray()) ((ArrayNode)rootNode).add(n.get(0));
                    updateYamlFromRoot();
                } catch (Exception ignored) {}
            }); }},
            new MenuItem("New REST Configuration", new FontIcon("fas-server")) {{ setOnAction(e -> {
                try {
                    JsonNode n = yamlMapper.readTree("- rest:\n    path: /api\n    get:\n      - path: /hello\n        to: direct:hello");
                    if (rootNode != null && rootNode.isArray()) ((ArrayNode)rootNode).add(n.get(0));
                    updateYamlFromRoot();
                } catch (Exception ignored) {}
            }); }}
        );
        cp.setOnContextMenuRequested(e -> { if(activeContextMenu!=null) activeContextMenu.hide(); canvasMenu.show(cp, e.getScreenX(), e.getScreenY()); activeContextMenu=canvasMenu; e.consume(); });

        final double[] drag = new double[4];
        cp.setOnMousePressed(e -> { if(activeContextMenu!=null) activeContextMenu.hide(); drag[0]=e.getScreenX(); drag[1]=e.getScreenY(); drag[2]=zoomGroup.getTranslateX(); drag[3]=zoomGroup.getTranslateY(); cp.setCursor(javafx.scene.Cursor.CLOSED_HAND); });
        cp.setOnMouseDragged(e -> { zoomGroup.setTranslateX(drag[2] + e.getScreenX()-drag[0]); zoomGroup.setTranslateY(drag[3] + e.getScreenY()-drag[1]); });
        cp.setOnMouseReleased(e -> cp.setCursor(javafx.scene.Cursor.OPEN_HAND));
        return cp;
    }

    private void rebuildContainer() {
        if (isStackedSideBySide) { HBox h = new HBox(120); h.setAlignment(Pos.TOP_LEFT); diagramContainer = h; }
        else { VBox v = new VBox(120); v.setAlignment(Pos.TOP_LEFT); diagramContainer = v; }
        diagramContainer.setPadding(new Insets(50)); diagramContainer.getTransforms().setAll(zoomScale);
        if (zoomGroup != null) { zoomGroup.getChildren().clear(); zoomGroup.getChildren().add(diagramContainer); }
    }

    public void renderDiagrams(List<String> yamls) {
        this.currentYamls = yamls;
        ArrayNode compositeRoot = yamlMapper.createArrayNode();
        for (String yaml : yamls) {
            try {
                JsonNode node = yamlMapper.readTree(yaml);
                if (node.isArray()) {
                    for (JsonNode sub : node) compositeRoot.add(sub);
                } else {
                    compositeRoot.add(node);
                }
            } catch (Exception ignored) {}
        }
        this.rootNode = compositeRoot;
        renderFromRoot();
    }

    public void renderDiagram(String yaml) {
        this.currentYaml = yaml;
        try {
            this.rootNode = yamlMapper.readTree(yaml);
        } catch (Exception e) {
            this.rootNode = yamlMapper.createArrayNode();
        }
        renderFromRoot();
    }

    private void renderFromRoot() {
        diagramContainer.getChildren().clear();
        if (rootNode != null && rootNode.isArray()) {
            ArrayNode arr = (ArrayNode) rootNode;
            for (int i = 0; i < arr.size(); i++) {
                JsonNode item = arr.get(i);
                if (item.has("route")) renderRouteNode(item.get("route"), i, arr);
                else if (item.has("onException")) renderOnExceptionNode(item.get("onException"), i, arr);
                else if (item.has("rest")) renderRestNode(item.get("rest"), i, arr);
                else if (item.has("beans")) renderBeansNode(item.get("beans"), i, arr);
            }
        }
    }

    private void renderBeansNode(JsonNode beans, int idx, ArrayNode arr) {
        Pane w = createBaseContainer(0);
        StackPane node = createEipNode("BEANS", "Configuration", "fas-cubes", "node-nested");
        node.setOnMouseClicked(e -> selectNode(node, "beans", beans, arr, idx));
        
        ContextMenu m = new ContextMenu();
        m.getItems().addAll(
            new MenuItem("Edit Beans", new FontIcon("fas-edit")) {{ setOnAction(e -> selectNode(node, "beans", beans, arr, idx)); }},
            new SeparatorMenuItem(),
            new MenuItem("Delete Beans Config", new FontIcon("fas-trash")) {{ setOnAction(e -> { arr.remove(idx); renderFromRoot(); updateYamlFromRoot(); closePropertyPane(); }); }}
        );
        node.setOnContextMenuRequested(e -> { if(activeContextMenu!=null) activeContextMenu.hide(); m.show(node, e.getScreenX(), e.getScreenY()); activeContextMenu=m; e.consume(); });
        
        w.getChildren().add(node);
        diagramContainer.getChildren().add(w);
    }

    private Pane createBaseContainer(double spacing) {
        if (isHorizontal) { HBox h = new HBox(spacing); h.setAlignment(Pos.CENTER_LEFT); return h; }
        else { VBox v = new VBox(spacing); v.setAlignment(Pos.TOP_CENTER); return v; }
    }
    
    private Pane createBranchContainer(double spacing) {
        if (isHorizontal) { VBox v = new VBox(spacing); v.setAlignment(Pos.CENTER_LEFT); return v; }
        else { HBox h = new HBox(spacing); h.setAlignment(Pos.TOP_CENTER); return h; }
    }

    private void renderRouteNode(JsonNode route, int idx, ArrayNode arr) {
        if (!route.has("from")) return;
        JsonNode from = route.get("from");
        String uri = from.has("uri") ? from.get("uri").asText() : (from.isTextual() ? from.asText() : "unknown");
        Pane wrapper = createBaseContainer(0);
        StackPane node = createEipNode("FROM", uri, determineIcon(uri, "fas-play-circle"), "node-from");
        node.setOnMouseClicked(e -> { if (e.getButton()==javafx.scene.input.MouseButton.PRIMARY) selectFromNode(node, route, uri); });
        
        ContextMenu m = new ContextMenu();
        m.getItems().addAll(createInsertMenu("Add Step After", (ArrayNode)from.get("steps"), 0), 
            new SeparatorMenuItem(), new MenuItem("Delete Entire Route", new FontIcon("fas-trash")) {{ setOnAction(e -> { arr.remove(idx); renderFromRoot(); updateYamlFromRoot(); }); }});
        node.setOnContextMenuRequested(e -> { if(activeContextMenu!=null) activeContextMenu.hide(); m.show(node, e.getScreenX(), e.getScreenY()); activeContextMenu=m; e.consume(); });

        wrapper.getChildren().add(node);
        if (from.has("steps")) renderSteps(from.get("steps"), wrapper);
        diagramContainer.getChildren().add(wrapper);
    }

    private void renderSteps(JsonNode steps, Pane parent) {
        if (steps == null || !steps.isArray()) return;
        ArrayNode arr = (ArrayNode) steps;
        for (int i = 0; i < arr.size(); i++) {
            final int index = i; JsonNode s = arr.get(i);
            parent.getChildren().add(createConnector("standard"));
            Pane wrapper = createBaseContainer(0);
            renderSingleStep(s, wrapper, arr, index);
            parent.getChildren().add(wrapper);
        }
    }

    private void renderSingleStep(JsonNode step, Pane parent, ArrayNode array, int idx) {
        Iterator<Map.Entry<String, JsonNode>> fields = step.fields();
        if (!fields.hasNext()) return;
        Map.Entry<String, JsonNode> f = fields.next();
        String name = f.getKey(); String details = extractDetails(f.getValue());

        StackPane node;
        switch (name.toLowerCase()) {
            case "choice": renderChoice(f.getValue(), parent, array, idx); return;
            case "split":
            case "multicast": renderParallelEip(name, f.getValue(), "fas-code-branch", parent, array, idx); return;
            case "dotry": renderDoTry(f.getValue(), parent, array, idx); return;
            case "circuitbreaker": renderCircuitBreaker(f.getValue(), parent, array, idx); return;
            case "log": node = createEipNode("LOG", details, "fas-terminal", "node-step"); break;
            case "to": node = createEipNode("TO", details, determineIcon(details, "fas-paper-plane"), "node-to"); break;
            case "transform": node = createEipNode("TRANSFORM", details, "fas-exchange-alt", "node-step"); break;
            case "setbody": node = createEipNode("SETBODY", details, "fas-edit", "node-step"); break;
            case "setheader": node = createEipNode("SETHEADER", details, "fas-edit", "node-step"); break;
            case "marshal":
            case "unmarshal": node = createEipNode(name.toUpperCase(), details, "fas-file-code", "node-step"); break;
            default: node = createEipNode(name.toUpperCase(), details, "fas-cog", "node-step"); break;
        }
        node.setOnMouseClicked(e -> { if (e.getButton()==javafx.scene.input.MouseButton.PRIMARY) selectNode(node, name, f.getValue(), array, idx); });
        setupNodeContextMenu(node, array, idx);
        parent.getChildren().add(node);
    }

    private void setupNodeContextMenu(StackPane node, ArrayNode array, int idx) {
        ContextMenu m = new ContextMenu();
        m.getItems().addAll(createInsertMenu("Insert Above", array, idx), createInsertMenu("Insert Below", array, idx + 1),
            new SeparatorMenuItem(),
            new MenuItem("Edit Properties", new FontIcon("fas-edit")) {{ setOnAction(e -> selectNode(node, "node", null, array, idx)); }},
            new MenuItem("Delete Step", new FontIcon("fas-trash")) {{ setOnAction(e -> { array.remove(idx); renderFromRoot(); updateYamlFromRoot(); closePropertyPane(); }); }}
        );
        node.setOnContextMenuRequested(e -> { if(activeContextMenu!=null) activeContextMenu.hide(); m.show(node, e.getScreenX(), e.getScreenY()); activeContextMenu=m; e.consume(); });
    }

    private Menu createInsertMenu(String label, ArrayNode array, int idx) {
        Menu m = new Menu(label, new FontIcon("fas-plus-circle"));
        Menu mapping = new Menu("Mapping & Templating", new FontIcon("fas-project-diagram"));
        mapping.getItems().addAll(
            createInsertMenuItem("To (XSLT)", "to_xslt", array, idx),
            createInsertMenuItem("To (JSLT)", "to_jslt", array, idx),
            createInsertMenuItem("To (FreeMarker)", "to_freemarker", array, idx),
            new SeparatorMenuItem(),
            createInsertMenuItem("Unmarshal (Flatpack)", "unmarshal_flatpack", array, idx),
            createInsertMenuItem("Unmarshal (Smooks)", "unmarshal_smooks", array, idx),
            new SeparatorMenuItem(),
            createInsertMenuItem("Transform (Groovy)", "transform_groovy", array, idx),
            createInsertMenuItem("Transform (jOOR Java)", "transform_joor", array, idx)
        );
        Menu formats = new Menu("Data Formats", new FontIcon("fas-database"));
        formats.getItems().addAll(
            createInsertMenuItem("Marshal JSON", "marshal_json", array, idx),
            createInsertMenuItem("Unmarshal JSON", "unmarshal_json", array, idx),
            createInsertMenuItem("Marshal XML", "marshal_xml", array, idx),
            createInsertMenuItem("Unmarshal XML", "unmarshal_xml", array, idx),
            new SeparatorMenuItem(),
            createInsertMenuItem("Marshal SWIFT MT", "marshal_swiftmt", array, idx),
            createInsertMenuItem("Unmarshal SWIFT MT", "unmarshal_swiftmt", array, idx),
            new SeparatorMenuItem(),
            createInsertMenuItem("Marshal SWIFT MX", "marshal_swiftmx", array, idx),
            createInsertMenuItem("Unmarshal SWIFT MX", "unmarshal_swiftmx", array, idx)
        );
        
        m.getItems().addAll(createInsertMenuItem("Log", "log", array, idx), createInsertMenuItem("To", "to", array, idx), createInsertMenuItem("SetBody", "setBody", array, idx), createInsertMenuItem("SetHeader", "setHeader", array, idx),
            new SeparatorMenuItem(), mapping, formats, new SeparatorMenuItem(), createInsertMenuItem("Choice", "choice", array, idx), createInsertMenuItem("Split", "split", array, idx), createInsertMenuItem("Filter", "filter", array, idx), createInsertMenuItem("DoTry", "doTry", array, idx));
        return m;
    }

    private MenuItem createInsertMenuItem(String lbl, String type, ArrayNode arr, int idx) {
        return new MenuItem(lbl) {{ setOnAction(e -> {
            try {
                JsonNode n;
                if (type.equals("to_xslt")) n = yamlMapper.readTree("{\"to\":{\"uri\":\"xslt-saxon:file:///path/your-logic.xslt\"}}");
                else if (type.equals("to_jslt")) n = yamlMapper.readTree("{\"to\":{\"uri\":\"jslt:file:///path/your-logic.jslt\"}}");
                else if (type.equals("to_freemarker")) n = yamlMapper.readTree("{\"to\":{\"uri\":\"freemarker:file:///path/template.ftl\"}}");
                else if (type.equals("unmarshal_flatpack")) n = yamlMapper.readTree("{\"unmarshal\":{\"flatpack\":{\"fixed\":true,\"definition\":\"file:///path/definition.xml\"}}}");
                else if (type.equals("unmarshal_smooks")) n = yamlMapper.readTree("{\"unmarshal\":{\"smooks\":{\"smooksConfig\":\"file:///path/smooks-config.xml\"}}}");
                else if (type.equals("transform_groovy")) n = yamlMapper.readTree("{\"transform\":{\"groovy\":\"resource:file:///path/transform.groovy\"}}");
                else if (type.equals("transform_joor")) n = yamlMapper.readTree("{\"transform\":{\"joor\":\"resource:file:///path/Transform.java\"}}");
                else if (type.equals("marshal_json")) n = yamlMapper.readTree("{\"marshal\":{\"json\":{\"library\":\"Jackson\"}}}");
                else if (type.equals("unmarshal_json")) n = yamlMapper.readTree("{\"unmarshal\":{\"json\":{\"library\":\"Jackson\"}}}");
                else if (type.equals("marshal_xml")) n = yamlMapper.readTree("{\"marshal\":{\"jaxb\":{\"contextPath\":\"com.example.model\"}}}");
                else if (type.equals("unmarshal_xml")) n = yamlMapper.readTree("{\"unmarshal\":{\"jaxb\":{\"contextPath\":\"com.example.model\"}}}");
                else if (type.equals("marshal_swiftmt")) n = yamlMapper.readTree("{\"marshal\":{\"swiftMt\":{}}}");
                else if (type.equals("unmarshal_swiftmt")) n = yamlMapper.readTree("{\"unmarshal\":{\"swiftMt\":{}}}");
                else if (type.equals("marshal_swiftmx")) n = yamlMapper.readTree("{\"marshal\":{\"swiftMx\":{}}}");
                else if (type.equals("unmarshal_swiftmx")) n = yamlMapper.readTree("{\"unmarshal\":{\"swiftMx\":{}}}");
                else n = yamlMapper.readTree("{\""+type+"\":\"mock:dest\"}");
                arr.insert(idx, n); renderFromRoot(); updateYamlFromRoot();
            } catch (Exception ignored) {}
        }); }};
    }

    private StackPane createEipNode(String title, String details, String icon, String style) {
        VBox box = new VBox(1); box.setAlignment(Pos.CENTER);
        box.getChildren().addAll(new FontIcon(icon){{setIconSize(20); getStyleClass().add("node-icon"); }}, new Label(title) {{ getStyleClass().add("node-title"); }}, new Label(limitString(details, 20)) {{ getStyleClass().add("node-details"); }});
        StackPane rootNodeWrapper = new StackPane(box); rootNodeWrapper.setPadding(new Insets(2)); rootNodeWrapper.getStyleClass().addAll("diagram-node", style);
        return rootNodeWrapper;
    }

    private void renderChoice(JsonNode conf, Pane parent, ArrayNode array, int idx) {
        StackPane node = createEipNode("CHOICE", "Router", "fas-random", "node-choice");
        node.setOnMouseClicked(e -> selectNode(node, "choice", conf, array, idx));
        
        ContextMenu m = new ContextMenu();
        m.getItems().addAll(createInsertMenu("Insert Above Choice", array, idx), createInsertMenu("Insert After Choice Block", array, idx + 1), new SeparatorMenuItem(),
            new MenuItem("Add WHEN Branch", new FontIcon("fas-code-branch")) {{ setOnAction(e -> {
                ObjectNode w = yamlMapper.createObjectNode(); w.putObject("simple").put("expression", "${header.foo} == 'bar'"); w.putArray("steps").add(yamlMapper.createObjectNode().put("log", "Branch Log"));
                if (conf.has("when") && conf.get("when").isArray()) ((ArrayNode)conf.get("when")).add(w); else ((ObjectNode)conf).putArray("when").add(w);
                renderFromRoot(); updateYamlFromRoot();
            }); }},
            new MenuItem("Add OTHERWISE Block", new FontIcon("fas-question-circle")) {{ setOnAction(e -> {
                ObjectNode o = yamlMapper.createObjectNode(); o.putArray("steps").add(yamlMapper.createObjectNode().put("log", "Fallback Log"));
                ((ObjectNode)conf).set("otherwise", o); renderFromRoot(); updateYamlFromRoot();
            }); }},
            new SeparatorMenuItem(), new MenuItem("Delete Choice", new FontIcon("fas-trash")) {{ setOnAction(e -> { array.remove(idx); renderFromRoot(); updateYamlFromRoot(); }); }});
        node.setOnContextMenuRequested(e -> { if(activeContextMenu!=null) activeContextMenu.hide(); m.show(node, e.getScreenX(), e.getScreenY()); activeContextMenu=m; e.consume(); });

        parent.getChildren().add(node);
        Pane bb = createBranchContainer(40);
        if (conf.has("when") && conf.get("when").isArray()) {
            for (JsonNode w : conf.get("when")) {
                Pane b = createBaseContainer(0); b.getChildren().add(new Label("WHEN: " + extractDetails(w.get("simple"))) {{ getStyleClass().add("branch-title"); }});
                if (w.has("steps")) renderSteps(w.get("steps"), b); bb.getChildren().add(b);
            }
        }
        if (conf.has("otherwise")) {
            Pane b = createBaseContainer(0); b.getChildren().add(new Label("OTHERWISE") {{ getStyleClass().add("branch-title"); }});
            if (conf.get("otherwise").has("steps")) renderSteps(conf.get("otherwise").get("steps"), b); bb.getChildren().add(b);
        }
        if (!bb.getChildren().isEmpty()) { parent.getChildren().addAll(createConnector("split"), bb); }
    }

    private void renderParallelEip(String name, JsonNode conf, String icon, Pane parent, ArrayNode array, int idx) {
        StackPane node = createEipNode(name.toUpperCase(), extractDetails(conf), icon, "node-nested");
        node.setOnMouseClicked(e -> selectNode(node, name, conf, array, idx));
        setupNodeContextMenu(node, array, idx); parent.getChildren().add(node);
        if (conf.has("steps") && conf.get("steps").isArray()) {
            Pane bb = createBranchContainer(40);
            for (JsonNode s : conf.get("steps")) { Pane b = createBaseContainer(0); renderSingleStep(s, b, (ArrayNode)conf.get("steps"), 0); bb.getChildren().add(b); }
            parent.getChildren().addAll(createConnector("split"), bb);
        }
    }

    private void renderDoTry(JsonNode conf, Pane parent, ArrayNode array, int idx) {
        StackPane node = createEipNode("DOTRY", "Try Block", "fas-shield-alt", "node-nested");
        node.setOnMouseClicked(e -> selectNode(node, "doTry", conf, array, idx));
        setupNodeContextMenu(node, array, idx); parent.getChildren().add(node);
        Pane bb = createBranchContainer(40);
        if (conf.has("steps")) bb.getChildren().add(createBranch("TRY", "", conf.get("steps")));
        if (conf.has("doCatch") && conf.get("doCatch").isArray()) {
            for (JsonNode c : conf.get("doCatch")) {
                String exc = (c.has("exception") && c.get("exception").isArray() && c.get("exception").size()>0) ? c.get("exception").get(0).asText() : "Exception";
                bb.getChildren().add(createBranch("CATCH", exc, c.get("steps")));
            }
        }
        if (!bb.getChildren().isEmpty()) { parent.getChildren().addAll(createConnector("split"), bb); }
    }

    private void renderCircuitBreaker(JsonNode conf, Pane parent, ArrayNode array, int idx) {
        StackPane node = createEipNode("CIRCUITBREAKER", "Circuit Breaker", "fas-heartbeat", "node-nested");
        node.setOnMouseClicked(e -> selectNode(node, "circuitBreaker", conf, array, idx));
        setupNodeContextMenu(node, array, idx); parent.getChildren().add(node);
        Pane bb = createBranchContainer(40);
        if (conf.has("steps")) bb.getChildren().add(createBranch("MAIN", "", conf.get("steps")));
        if (conf.has("onFallback")) bb.getChildren().add(createBranch("FALLBACK", "", conf.get("onFallback").get("steps")));
        parent.getChildren().addAll(createConnector("split"), bb);
    }

    private Pane createBranch(String title, String cond, JsonNode steps) {
        Pane b = createBaseContainer(0); b.getChildren().add(new Label(title + (cond.isEmpty()?"":": "+cond)) {{ getStyleClass().add("branch-title"); }});
        if (steps != null) renderSteps(steps, b); return b;
    }

    private Pane createConnector(String type) {
        Pane box = isHorizontal ? new HBox() : new VBox(); if (box instanceof HBox) ((HBox)box).setAlignment(Pos.CENTER); else ((VBox)box).setAlignment(Pos.CENTER);
        if ("standard".equals(type)) {
            Line l = new Line(0, 0, isHorizontal?15:0, isHorizontal?0:15) {{ getStyleClass().add("diagram-connector"); setStrokeWidth(2.5); }};
            javafx.scene.shape.Polygon a = isHorizontal ? new javafx.scene.shape.Polygon(0,-4, 0,4, 6,0) : new javafx.scene.shape.Polygon(-4,0, 4,0, 0,6);
            a.getStyleClass().add("diagram-arrow"); box.getChildren().addAll(l, a);
        } else {
            javafx.scene.shape.SVGPath b = new javafx.scene.shape.SVGPath() {{ setContent(isHorizontal ? "M 0,15 L 10,15 Q 20,15 20,0 M 10,15 L 20,15 M 10,15 Q 20,15 20,30" : "M 15,0 L 15,10 Q 15,20 0,20 M 15,10 L 15,20 M 15,10 Q 15,20 30,20"); setStroke(Color.web("#FF9800")); setStrokeWidth(3); setFill(Color.TRANSPARENT); setEffect(new javafx.scene.effect.DropShadow(5, Color.web("#FF9800"))); }};
            box.getChildren().add(b);
        }
        return box;
    }

    private void selectNode(Pane ui, String name, JsonNode conf, ArrayNode arr, int idx) {
        if (selectedNodeUi!=null) selectedNodeUi.setStyle(""); selectedNodeUi=ui; selectedNodeUi.setStyle("-fx-border-color: #007acc; -fx-border-width: 3; -fx-border-radius: 5;");
        renderPropertyForm(name, conf, arr, idx); openPropertyPane();
    }

    private void selectFromNode(Pane ui, JsonNode route, String uri) {
        if (selectedNodeUi!=null) selectedNodeUi.setStyle(""); selectedNodeUi=ui; selectedNodeUi.setStyle("-fx-border-color: #007acc; -fx-border-width: 3; -fx-border-radius: 5;");
        renderFromPropertyForm(route, uri); openPropertyPane();
    }

    private void renderPropertyForm(String name, JsonNode conf, ArrayNode arr, int idx) {
        propertyPane.getChildren().clear();
        HBox head = new HBox(new Label("EDIT " + name.toUpperCase()) {{ setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: #007acc;"); }}, new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }}, new Button("X") {{ setOnAction(e -> closePropertyPane()); }});
        
        com.routebuilder.ui.components.MonacoEditorPane editor = new com.routebuilder.ui.components.MonacoEditorPane("yaml");
        editor.setPrefHeight(300);
        editor.setText(extractDetails(conf));

        Button save = new Button("Save & Close") {{ setOnAction(e -> { 
            try { 
                JsonNode p = yamlMapper.readTree(editor.getText()); ObjectNode n = yamlMapper.createObjectNode(); n.set(name, p); arr.set(idx, n); renderFromRoot(); updateYamlFromRoot(); closePropertyPane(); 
            } catch (Exception ex) { 
                ObjectNode n = yamlMapper.createObjectNode(); n.put(name, editor.getText()); arr.set(idx, n); renderFromRoot(); updateYamlFromRoot(); closePropertyPane(); 
            } 
        }); }};
        
        VBox actions = new VBox(10, new Label("Quick Actions:"), new HBox(10, 
            new Button("Add Before") {{ setOnAction(e -> { ContextMenu cm = new ContextMenu(); cm.getItems().addAll(createInsertMenu("Add Before", arr, idx).getItems()); cm.show(this, javafx.geometry.Side.BOTTOM, 0, 0); }); }}, 
            new Button("Add After") {{ setOnAction(e -> { ContextMenu cm = new ContextMenu(); cm.getItems().addAll(createInsertMenu("Add After", arr, idx + 1).getItems()); cm.show(this, javafx.geometry.Side.BOTTOM, 0, 0); }); }}, 
            new Button("Delete") {{ setStyle("-fx-text-fill: #f44336;"); setOnAction(e -> { arr.remove(idx); renderFromRoot(); updateYamlFromRoot(); closePropertyPane(); }); }}));
        actions.setStyle("-fx-border-color: #333; -fx-border-width: 1 0 0 0; -fx-padding: 10 0 0 0;");
        
        if (name.equalsIgnoreCase("setBody")) {
            Label tl = new Label("Quick Templates:"); FlowPane fp = new FlowPane(5, 5);
            String[][] bt = { {"body.xml", "${body.xml}"}, {"body.json", "${body.json}"}, {"headers", "${headers}"} };
            for(String[] b : bt) { Button btn = new Button(b[0]); btn.setOnAction(e -> editor.setText(b[1])); fp.getChildren().add(btn); }
            propertyPane.getChildren().addAll(head, new Label("Configuration:"), editor, tl, fp, save, actions);
        } else {
            propertyPane.getChildren().addAll(head, new Label("Configuration:"), editor, save, actions);
        }
    }

    private void renderFromPropertyForm(JsonNode route, String uri) {
        propertyPane.getChildren().clear();
        HBox head = new HBox(new Label("EDIT FROM") {{ setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: #007acc;"); }}, new Region() {{ HBox.setHgrow(this, Priority.ALWAYS); }}, new Button("X") {{ setOnAction(e -> closePropertyPane()); }});
        TextField txt = new TextField(uri); Button save = new Button("Save & Close") {{ setOnAction(e -> { ((ObjectNode)route.get("from")).put("uri", txt.getText()); renderFromRoot(); updateYamlFromRoot(); closePropertyPane(); }); }};
        VBox actions = new VBox(10, new Label("Quick Actions:"), new Button("Add Step After") {{ setOnAction(e -> { ContextMenu cm = new ContextMenu(); cm.getItems().addAll(createInsertMenu("Add After", (ArrayNode)route.get("from").get("steps"), 0).getItems()); cm.show(this, javafx.geometry.Side.BOTTOM, 0, 0); }); }});
        actions.setStyle("-fx-border-color: #333; -fx-border-width: 1 0 0 0; -fx-padding: 10 0 0 0;");
        propertyPane.getChildren().addAll(head, new Label("Endpoint URI:"), txt, save, actions);
    }

    private String extractDetails(JsonNode c) { if(c==null) return ""; try { return yamlMapper.writeValueAsString(c).trim(); } catch (Exception e) { return c.asText(); } }
    private String limitString(String v, int m) { if(v==null) return ""; v = v.replace("\n", " "); return v.length()<=m ? v : v.substring(0, m-3)+"..."; }
    private void updateYamlFromRoot() { if(rootNode!=null && yamlUpdater!=null) { try { String y = yamlMapper.writeValueAsString(rootNode); yamlUpdater.accept(y); } catch (Exception ignored){} } }
    private void openPropertyPane() { if(!contentStack.getChildren().contains(propScroll)) contentStack.getChildren().add(propScroll); }
    private void closePropertyPane() { contentStack.getChildren().remove(propScroll); }

    private String determineIcon(String uri, String def) {
        if(uri==null) return def; String l = uri.toLowerCase();
        if(l.startsWith("timer")) return "fas-clock";
        if(l.startsWith("jms")||l.startsWith("ibmmq")) return "fas-envelope";
        if(l.startsWith("kafka")) return "fas-share-alt";
        return def;
    }

    private void renderOnExceptionNode(JsonNode exc, int idx, ArrayNode arr) {
        Pane w = createBaseContainer(0);
        StackPane node = createEipNode("EXCEPTION", "Global", "fas-exclamation-triangle", "node-nested");
        node.setOnMouseClicked(e -> selectNode(node, "onException", exc, arr, idx));
        
        ContextMenu m = new ContextMenu();
        m.getItems().addAll(
            new MenuItem("Edit Exception", new FontIcon("fas-edit")) {{ setOnAction(e -> selectNode(node, "onException", exc, arr, idx)); }},
            new SeparatorMenuItem(),
            new MenuItem("Delete Exception Handler", new FontIcon("fas-trash")) {{ setOnAction(e -> { arr.remove(idx); renderFromRoot(); updateYamlFromRoot(); closePropertyPane(); }); }}
        );
        node.setOnContextMenuRequested(e -> { if(activeContextMenu!=null) activeContextMenu.hide(); m.show(node, e.getScreenX(), e.getScreenY()); activeContextMenu=m; e.consume(); });

        w.getChildren().add(node);
        if (exc.has("steps")) renderSteps(exc.get("steps"), w); diagramContainer.getChildren().add(w);
    }

    private void renderRestNode(JsonNode rest, int idx, ArrayNode arr) {
        Pane w = createBaseContainer(0);
        StackPane node = createEipNode("REST", rest.has("path")?rest.get("path").asText():"/", "fas-server", "node-from");
        node.setOnMouseClicked(e -> selectNode(node, "rest", rest, arr, idx));
        
        ContextMenu m = new ContextMenu();
        m.getItems().addAll(
            new MenuItem("Edit REST", new FontIcon("fas-edit")) {{ setOnAction(e -> selectNode(node, "rest", rest, arr, idx)); }},
            new SeparatorMenuItem(),
            new MenuItem("Delete REST Config", new FontIcon("fas-trash")) {{ setOnAction(e -> { arr.remove(idx); renderFromRoot(); updateYamlFromRoot(); closePropertyPane(); }); }}
        );
        node.setOnContextMenuRequested(e -> { if(activeContextMenu!=null) activeContextMenu.hide(); m.show(node, e.getScreenX(), e.getScreenY()); activeContextMenu=m; e.consume(); });

        w.getChildren().add(node);
        diagramContainer.getChildren().add(w);
    }

    private javafx.scene.image.WritableImage snapshotDiagram() { if(diagramContainer==null) return null; return diagramContainer.snapshot(null, null); }

    public static class BeanData {
        public String name;
        public String type;
        public boolean isLocal;
        public java.util.Map<String, String> properties = new java.util.HashMap<>();
    }

    public List<BeanData> parseYamlBeans(JsonNode beansNode) {
        List<BeanData> result = new java.util.ArrayList<>();
        if (beansNode != null && beansNode.isArray()) {
            for (JsonNode bean : beansNode) {
                BeanData data = new BeanData();
                data.name = bean.has("name") ? bean.get("name").asText() : "Unnamed";
                String type = bean.has("type") ? bean.get("type").asText() : "Object";
                if (type.startsWith("#class:")) type = type.substring(7);
                data.type = type;
                data.isLocal = true;
                if (bean.has("properties") && bean.get("properties").isObject()) {
                    bean.get("properties").fields().forEachRemaining(entry -> {
                        data.properties.put(entry.getKey(), entry.getValue().asText());
                    });
                }
                result.add(data);
            }
            
            List<BeanData> externals = new java.util.ArrayList<>();
            for (BeanData data : result) {
                data.properties.values().forEach(val -> {
                    if (val != null && val.startsWith("#")) {
                        String rawRef = val.substring(1);
                        if (rawRef.startsWith("bean:")) rawRef = rawRef.substring(5);
                        final String finalRef = rawRef;
                        if (result.stream().noneMatch(b -> b.name.equals(finalRef)) && 
                            externals.stream().noneMatch(b -> b.name.equals(finalRef))) {
                            BeanData ext = new BeanData();
                            ext.name = finalRef;
                            ext.type = "External Ref";
                            ext.isLocal = false;
                            externals.add(ext);
                        }
                    }
                });
            }
            result.addAll(externals);
        }
        return result;
    }

    public List<BeanData> parseJavaBeans(String javaContent, File file) {
        List<BeanData> result = new java.util.ArrayList<>();
        Pattern clsPattern = Pattern.compile("public\\s+class\\s+([A-Za-z0-9_]+)");
        Matcher clsMatcher = clsPattern.matcher(javaContent);
        if (clsMatcher.find()) {
            BeanData data = new BeanData();
            data.name = clsMatcher.group(1);
            data.type = data.name;
            data.isLocal = true;
            
            Pattern fieldPattern = Pattern.compile("private\\s+([A-Za-z0-9_]+)\\s+([A-Za-z0-9_]+)\\s*;");
            Matcher fieldMatcher = fieldPattern.matcher(javaContent);
            while (fieldMatcher.find()) {
                data.properties.put(fieldMatcher.group(2), fieldMatcher.group(1));
            }
            result.add(data);
            
            if (file != null && file.getParentFile() != null) {
                for (String type : new java.util.HashSet<>(data.properties.values())) {
                    File other = new File(file.getParentFile(), type + ".java");
                    if (other.exists() && result.stream().noneMatch(b -> b.name.equals(type))) {
                        try {
                            result.addAll(parseJavaBeans(Files.readString(other.toPath()), other));
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        return result;
    }
}
