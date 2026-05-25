package com.routebuilder.ui;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Slider;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Scale;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Dialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Alert;

public class DiagramPane extends VBox {

    private Pane diagramContainer;
    private Pane scrollPane;
    private Group zoomGroup;
    private ObjectMapper yamlMapper;
    private Scale zoomScale;
    
    private boolean isHorizontal = false;
    private String currentYaml = "";
    private java.io.File currentFile;
    public void setCurrentFile(java.io.File file) {
        this.currentFile = file;
    }

    public static class BeanData {
        public String name;
        public String type;
        public boolean isLocal;
        public Map<String, String> properties = new java.util.LinkedHashMap<>();
    }
    private Consumer<String> themeChanger;
    private Consumer<String> yamlUpdater;
    private JsonNode rootNode;
    private VBox propertyPane;
    private Pane selectedNodeUi;
    private javafx.scene.control.SplitPane splitPane;
    private ScrollPane propScroll;
    private Runnable onClose;
    private Runnable onMaximize;
    private javafx.scene.control.ContextMenu activeContextMenu;
    private ClassDiagramPane classDiagramPane;

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public void setOnMaximize(Runnable onMaximize) {
        this.onMaximize = onMaximize;
    }

    public DiagramPane(Consumer<String> themeChanger, Consumer<String> yamlUpdater) {
        this.themeChanger = themeChanger;
        this.yamlUpdater = yamlUpdater;
        getStyleClass().add("diagram-pane");
        yamlMapper = new ObjectMapper(new YAMLFactory());
        yamlMapper.enable(com.fasterxml.jackson.databind.SerializationFeature.INDENT_OUTPUT);

        Label title = new Label("VISUAL: EIP DIAGRAM");
        title.getStyleClass().add("pane-title");

        // Toolbar
        HBox toolbar = new HBox(15);
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(10));
        toolbar.getStyleClass().add("diagram-toolbar");

        // Toolbar buttons
        Button layoutBtn = new Button();
        layoutBtn.setGraphic(new FontIcon("fas-exchange-alt"));
        layoutBtn.setTooltip(new javafx.scene.control.Tooltip("Toggle Layout (T->B / L->R)"));
        layoutBtn.getStyleClass().addAll("editor-btn", "btn-layout");
        layoutBtn.setOnAction(e -> {
            isHorizontal = !isHorizontal;
            rebuildContainer();
            renderDiagram(currentYaml);
        });

        zoomScale = new Scale(1.0, 1.0);
        
        Button zoomOutBtn = new Button();
        zoomOutBtn.setGraphic(new FontIcon("fas-search-minus"));
        zoomOutBtn.setTooltip(new javafx.scene.control.Tooltip("Zoom Out"));
        zoomOutBtn.getStyleClass().addAll("editor-btn", "btn-zoom");
        zoomOutBtn.setOnAction(e -> {
            double z = Math.max(0.5, zoomScale.getX() - 0.1);
            zoomScale.setX(z);
            zoomScale.setY(z);
        });
        
        Button zoomInBtn = new Button();
        zoomInBtn.setGraphic(new FontIcon("fas-search-plus"));
        zoomInBtn.setTooltip(new javafx.scene.control.Tooltip("Zoom In"));
        zoomInBtn.getStyleClass().addAll("editor-btn", "btn-zoom");
        zoomInBtn.setOnAction(e -> {
            double z = Math.min(2.0, zoomScale.getX() + 0.1);
            zoomScale.setX(z);
            zoomScale.setY(z);
        });

        Button fitBtn = new Button();
        fitBtn.setGraphic(new FontIcon("fas-compress"));
        fitBtn.setTooltip(new javafx.scene.control.Tooltip("Fit to Screen"));
        fitBtn.getStyleClass().addAll("editor-btn", "btn-fit");
        fitBtn.setOnAction(e -> {
            zoomScale.setX(1.0);
            zoomScale.setY(1.0);
            zoomGroup.setTranslateX(0);
            zoomGroup.setTranslateY(0);
        });

        Button closeDiagramBtn = new Button();
        closeDiagramBtn.setGraphic(new FontIcon("fas-times"));
        closeDiagramBtn.setTooltip(new javafx.scene.control.Tooltip("Close Diagram"));
        closeDiagramBtn.getStyleClass().addAll("editor-btn");
        closeDiagramBtn.setOnAction(e -> {
            if (onClose != null) onClose.run();
        });

        Button maximizeBtn = new Button();
        maximizeBtn.setGraphic(new FontIcon("fas-expand-arrows-alt"));
        maximizeBtn.setTooltip(new javafx.scene.control.Tooltip("Maximize/Restore Diagram"));
        maximizeBtn.getStyleClass().addAll("editor-btn");
        maximizeBtn.setOnAction(e -> {
            if (onMaximize != null) onMaximize.run();
        });

        Button saveAsSvgBtn = new Button();
        saveAsSvgBtn.setGraphic(new FontIcon("fas-file-export"));
        saveAsSvgBtn.setTooltip(new javafx.scene.control.Tooltip("Save as SVG"));
        saveAsSvgBtn.getStyleClass().addAll("editor-btn");
        saveAsSvgBtn.setOnAction(e -> {
            javafx.scene.image.WritableImage image = snapshotDiagram();
            if (image == null) return;
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setTitle("Save Diagram as SVG");
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("SVG Files", "*.svg"));
            fc.setInitialFileName("diagram.svg");
            java.io.File file = fc.showSaveDialog(getScene() != null ? getScene().getWindow() : null);
            if (file != null) {
                try {
                    int w = (int) image.getWidth();
                    int h = (int) image.getHeight();
                    java.io.ByteArrayOutputStream pngOut = new java.io.ByteArrayOutputStream();
                    javax.imageio.ImageIO.write(javafx.embed.swing.SwingFXUtils.fromFXImage(image, null), "png", pngOut);
                    String b64 = java.util.Base64.getEncoder().encodeToString(pngOut.toByteArray());
                    String svg = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                        "<svg xmlns=\"http://www.w3.org/2000/svg\" " +
                        "xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
                        "width=\"" + w + "\" height=\"" + h + "\">\n" +
                        "  <image xlink:href=\"data:image/png;base64," + b64 + "\"" +
                        " width=\"" + w + "\" height=\"" + h + "\"/>\n" +
                        "</svg>";
                    java.nio.file.Files.writeString(file.toPath(), svg);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    javafx.scene.control.Alert errAlert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, "Failed to save SVG: " + ex.getMessage());
                    RouteBuilderApp.themeDialog(errAlert);
                    errAlert.show();
                }
            }
        });

        Button copyImageBtn = new Button();
        copyImageBtn.setGraphic(new FontIcon("fas-copy"));
        copyImageBtn.setTooltip(new javafx.scene.control.Tooltip("Copy Diagram to Clipboard"));
        copyImageBtn.getStyleClass().addAll("editor-btn");
        copyImageBtn.setOnAction(e -> {
            javafx.scene.image.WritableImage image = snapshotDiagram();
            if (image == null) return;
            javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
            content.putImage(image);
            javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
        });

        toolbar.getChildren().addAll(layoutBtn, zoomOutBtn, zoomInBtn, fitBtn,
            new javafx.scene.control.Separator(), saveAsSvgBtn, copyImageBtn,
            new javafx.scene.control.Separator(), maximizeBtn, closeDiagramBtn);

        rebuildContainer();

        zoomGroup = new Group(diagramContainer);

        Group scrollContent = new Group(zoomGroup);
        scrollPane = new Pane(scrollContent);
        scrollPane.getStyleClass().add("diagram-scroll");
        
        javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle();
        clip.widthProperty().bind(scrollPane.widthProperty());
        clip.heightProperty().bind(scrollPane.heightProperty());
        scrollPane.setClip(clip);
        
        scrollPane.addEventFilter(javafx.scene.input.ScrollEvent.ANY, event -> {
            if (event.getDeltaY() != 0) {
                double zoomFactor = event.getDeltaY() > 0 ? 1.05 : 1 / 1.05;
                double newScale = zoomScale.getX() * zoomFactor;
                if (newScale >= 0.2 && newScale <= 5.0) {
                    zoomScale.setX(newScale);
                    zoomScale.setY(newScale);
                }
                event.consume();
            }
        });

        scrollPane.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_PRESSED, event -> {
            if (activeContextMenu != null && activeContextMenu.isShowing()) {
                activeContextMenu.hide();
                activeContextMenu = null;
            }
            if (event.isPrimaryButtonDown()) {
                scrollPane.getProperties().put("mouseX", event.getScreenX());
                scrollPane.getProperties().put("mouseY", event.getScreenY());
            }
        });

        scrollPane.addEventFilter(javafx.scene.input.MouseEvent.MOUSE_DRAGGED, event -> {
            if (event.isPrimaryButtonDown()) {
                Double lastX = (Double) scrollPane.getProperties().get("mouseX");
                Double lastY = (Double) scrollPane.getProperties().get("mouseY");
                if (lastX != null && lastY != null) {
                    double deltaX = event.getScreenX() - lastX;
                    double deltaY = event.getScreenY() - lastY;
                    zoomGroup.setTranslateX(zoomGroup.getTranslateX() + deltaX);
                    zoomGroup.setTranslateY(zoomGroup.getTranslateY() + deltaY);
                    scrollPane.getProperties().put("mouseX", event.getScreenX());
                    scrollPane.getProperties().put("mouseY", event.getScreenY());
                }
                event.consume();
            }
        });

        javafx.scene.control.ContextMenu canvasMenu = new javafx.scene.control.ContextMenu();
        javafx.scene.control.MenuItem addRouteItem = new javafx.scene.control.MenuItem("New Route", new FontIcon("fas-route"));
        addRouteItem.setOnAction(e -> {
            try {
                com.fasterxml.jackson.databind.JsonNode newRoute = yamlMapper.readTree("- route:\n    from:\n      uri: direct:start\n      steps:\n        - log: New Route started");
                if (rootNode == null || !rootNode.isArray()) {
                    rootNode = yamlMapper.createArrayNode();
                }
                ((com.fasterxml.jackson.databind.node.ArrayNode)rootNode).add(newRoute.get(0));
                updateYamlFromRoot();
            } catch (Exception ex) {}
        });
        javafx.scene.control.MenuItem addExceptionItem = new javafx.scene.control.MenuItem("New Global Exception (onException)", new FontIcon("fas-exclamation-triangle"));
        addExceptionItem.setOnAction(e -> {
            try {
                com.fasterxml.jackson.databind.JsonNode newExc = yamlMapper.readTree("- onException:\n    exception: [\"java.lang.Exception\"]\n    steps:\n        - log: Caught Exception");
                if (rootNode != null && rootNode.isArray()) {
                    ((com.fasterxml.jackson.databind.node.ArrayNode)rootNode).add(newExc.get(0));
                    updateYamlFromRoot();
                }
            } catch (Exception ex) {}
        });
        javafx.scene.control.MenuItem addRestItem = new javafx.scene.control.MenuItem("New REST Configuration", new FontIcon("fas-server"));
        addRestItem.setOnAction(e -> {
            try {
                com.fasterxml.jackson.databind.JsonNode newRest = yamlMapper.readTree("- rest:\n    path: /api\n    get:\n      - path: /hello\n        to: direct:hello");
                if (rootNode != null && rootNode.isArray()) {
                    ((com.fasterxml.jackson.databind.node.ArrayNode)rootNode).add(newRest.get(0));
                    updateYamlFromRoot();
                }
            } catch (Exception ex) {}
        });
        canvasMenu.getItems().addAll(addRouteItem, addExceptionItem, addRestItem);

        scrollPane.setOnContextMenuRequested(e -> {
            if (activeContextMenu != null) activeContextMenu.hide();
            canvasMenu.show(scrollPane, e.getScreenX(), e.getScreenY());
            activeContextMenu = canvasMenu;
        });

        propertyPane = new VBox(15);
        propertyPane.setPadding(new Insets(15));
        propertyPane.setMinWidth(350);
        propertyPane.getStyleClass().add("property-pane");

        splitPane = new javafx.scene.control.SplitPane();
        splitPane.setOrientation(javafx.geometry.Orientation.HORIZONTAL);
        
        propScroll = new ScrollPane(propertyPane);
        propScroll.getStyleClass().add("property-scroll");
        propScroll.setFitToWidth(true);
        propScroll.setMinWidth(450);
        propScroll.setPrefWidth(450);
        propScroll.setMaxWidth(600);
        javafx.scene.control.SplitPane.setResizableWithParent(propScroll, false);
        
        splitPane.getItems().add(scrollPane);
        VBox.setVgrow(splitPane, Priority.ALWAYS);

        getChildren().addAll(title, toolbar, splitPane);
        }
    private void closePropertyPane() {
        if (splitPane.getItems().contains(propScroll)) splitPane.getItems().remove(propScroll);
    }

    private void openPropertyPane() {
        if (!splitPane.getItems().contains(propScroll)) {
            splitPane.getItems().add(propScroll);
            double w = splitPane.getWidth();
            if (w > 10) {
                splitPane.setDividerPositions(Math.max(0.1, (w - 460) / w));
            } else {
                splitPane.setDividerPositions(0.5);
            }
        }
    }

    private void rebuildContainer() {
        if (isHorizontal) {
            // L->R routes should be stacked vertically (so they don't overlap horizontally)
            VBox box = new VBox(80);
            box.setAlignment(Pos.TOP_LEFT);
            diagramContainer = box;
        } else {
            // T->B routes should be placed side-by-side horizontally
            HBox box = new HBox(80);
            box.setAlignment(Pos.TOP_CENTER);
            diagramContainer = box;
        }
        diagramContainer.setPadding(new Insets(30));
        diagramContainer.getTransforms().clear();
        diagramContainer.getTransforms().add(zoomScale);
        
        if (zoomGroup != null) {
            zoomGroup.getChildren().clear();
            zoomGroup.getChildren().add(diagramContainer);
        }
    }

    private Pane createBaseContainer(double spacing) {
        if (isHorizontal) {
            HBox box = new HBox(spacing);
            box.setAlignment(Pos.CENTER_LEFT);
            return box;
        } else {
            VBox box = new VBox(spacing);
            box.setAlignment(Pos.TOP_CENTER);
            return box;
        }
    }
    
    private Pane createBranchContainer(double spacing) {
        if (isHorizontal) {
            VBox box = new VBox(spacing); 
            box.setAlignment(Pos.CENTER_LEFT);
            return box;
        } else {
            HBox box = new HBox(spacing); 
            box.setAlignment(Pos.TOP_CENTER);
            return box;
        }
    }

    public void renderDiagram(String yamlText) {
        this.currentYaml = yamlText;
        diagramContainer.getChildren().clear();

        boolean hasBeans = false;
        try {
            JsonNode parsed = yamlMapper.readTree(yamlText);
            if (parsed != null && parsed.isArray()) {
                for (JsonNode item : parsed) {
                    if (item.has("beans")) {
                        hasBeans = true;
                        break;
                    }
                }
            }
        } catch (Exception e) {}

        // 1. Detect if it's Java
        boolean isJava = false;
        if (currentFile != null && currentFile.getName().endsWith(".java")) {
            isJava = true;
        } else if (yamlText != null && (yamlText.trim().startsWith("package") || yamlText.trim().startsWith("import") || yamlText.contains("public class") || yamlText.contains("class "))) {
            isJava = true;
        }

        boolean isUmlDiagram = isJava || hasBeans;

        if (isUmlDiagram) {
            if (classDiagramPane == null) {
                classDiagramPane = new ClassDiagramPane();
            }
            if (splitPane.getItems().contains(scrollPane)) {
                splitPane.getItems().remove(scrollPane);
            }
            if (!splitPane.getItems().contains(classDiagramPane)) {
                splitPane.getItems().add(0, classDiagramPane);
            }
            
            if (isJava) {
                classDiagramPane.renderClassDiagram(yamlText);
            } else {
                classDiagramPane.renderYamlBeansDiagram(yamlText, currentFile);
            }
            return;
        }

        // Standard route EIP view
        if (classDiagramPane != null && splitPane.getItems().contains(classDiagramPane)) {
            splitPane.getItems().remove(classDiagramPane);
        }
        if (!splitPane.getItems().contains(scrollPane)) {
            splitPane.getItems().add(0, scrollPane);
        }

        // YAML / JSON processing
        try {
            this.rootNode = yamlMapper.readTree(yamlText);
            if (this.rootNode != null && this.rootNode.isArray()) {
                com.fasterxml.jackson.databind.node.ArrayNode rootArray = (com.fasterxml.jackson.databind.node.ArrayNode) this.rootNode;
                for (int i = 0; i < rootArray.size(); i++) {
                    JsonNode routeItem = rootArray.get(i);
                    if (routeItem.has("route")) {
                        Pane routeContainer = createBaseContainer(0); // VBox if T->B, HBox if L->R
                        renderRouteNode(routeItem.get("route"), routeContainer, i, rootArray);
                        diagramContainer.getChildren().add(routeContainer);
                    } else if (routeItem.has("onException")) {
                        Pane excContainer = createBaseContainer(0);
                        renderOnExceptionNode(routeItem.get("onException"), excContainer, i, rootArray);
                        diagramContainer.getChildren().add(excContainer);
                    } else if (routeItem.has("rest")) {
                        Pane restContainer = createBaseContainer(0);
                        renderRestNode(routeItem.get("rest"), restContainer, i, rootArray);
                        diagramContainer.getChildren().add(restContainer);
                    } else if (routeItem.has("beans")) {
                        Pane beansContainer = createBaseContainer(0);
                        List<BeanData> beansList = parseYamlBeans(routeItem.get("beans"));
                        renderStunningBeanDiagram(beansList, beansContainer);
                        diagramContainer.getChildren().add(beansContainer);
                    }
                }
            }
        } catch (Exception e) {
            Label errorLabel = new Label("Waiting for valid YAML...");
            errorLabel.setTextFill(Color.GRAY);
            diagramContainer.getChildren().add(errorLabel);
        }
    }

    List<BeanData> parseJavaBeans(String javaContent, java.io.File currentFile) {
        List<BeanData> beans = new java.util.ArrayList<>();
        java.util.Set<String> parsedTypes = new java.util.HashSet<>();
        
        // Let's parse the main file first
        parseSingleJavaFile(javaContent, currentFile != null ? currentFile.getName() : "Main.java", beans, parsedTypes, currentFile);
        
        // Find referenced types that haven't been parsed
        java.io.File dir = currentFile != null ? currentFile.getParentFile() : null;
        boolean foundNew = true;
        while (foundNew) {
            foundNew = false;
            List<String> toParse = new java.util.ArrayList<>();
            for (BeanData b : beans) {
                for (Map.Entry<String, String> prop : b.properties.entrySet()) {
                    String type = prop.getValue();
                    if (isUserDefinedType(type) && !parsedTypes.contains(type)) {
                        toParse.add(type);
                    }
                }
            }
            
            for (String type : toParse) {
                if (dir != null) {
                    java.io.File file = new java.io.File(dir, type + ".java");
                    if (file.exists()) {
                        try {
                            String content = java.nio.file.Files.readString(file.toPath());
                            parseSingleJavaFile(content, file.getName(), beans, parsedTypes, currentFile);
                            foundNew = true;
                            continue;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
                
                // If we couldn't parse it locally, create a placeholder external ref bean!
                BeanData refBean = new BeanData();
                refBean.name = type;
                refBean.type = type;
                refBean.isLocal = false;
                beans.add(refBean);
                parsedTypes.add(type);
                foundNew = true;
            }
        }
        
        return beans;
    }

    private void parseSingleJavaFile(String content, String fileName, List<BeanData> beans, java.util.Set<String> parsedTypes, java.io.File currentFile) {
        // Extract class name
        String className = null;
        java.util.regex.Matcher classMatcher = java.util.regex.Pattern.compile("(?:public\\s+)?class\\s+(\\w+)").matcher(content);
        if (classMatcher.find()) {
            className = classMatcher.group(1);
        } else {
            int dot = fileName.lastIndexOf('.');
            className = dot > 0 ? fileName.substring(0, dot) : fileName;
        }
        
        if (parsedTypes.contains(className)) {
            return;
        }
        
        BeanData b = new BeanData();
        b.name = className;
        b.type = className;
        b.isLocal = true;
        parsedTypes.add(className);
        
        // Extract package name
        String pkg = "";
        java.util.regex.Matcher pkgMatcher = java.util.regex.Pattern.compile("package\\s+([\\w\\.]+);").matcher(content);
        if (pkgMatcher.find()) {
            pkg = pkgMatcher.group(1);
            b.type = pkg + "." + className;
        }
        
        // Extract fields
        java.util.regex.Matcher fieldMatcher = java.util.regex.Pattern.compile("(?m)^\\s*(?:private|protected|public|static|final|transient|volatile|\\s+)*\\s+([a-zA-Z0-9_<>\\[\\]]+)\\s+(\\w+)\\s*(?:=\\s*[^;]+)?;").matcher(content);
        while (fieldMatcher.find()) {
            String type = fieldMatcher.group(1);
            String name = fieldMatcher.group(2);
            
            // Filter out keywords
            if (isKeyword(type) || isKeyword(name)) continue;
            
            b.properties.put(name, type);
        }
        
        beans.add(b);
    }
    
    private boolean isKeyword(String s) {
        String[] keywords = {"return", "package", "import", "throw", "new", "class", "interface", "enum", "extends", "implements", "static", "final", "transient", "volatile", "public", "private", "protected", "void"};
        for (String kw : keywords) {
            if (kw.equals(s)) return true;
        }
        return false;
    }
    
    private boolean isUserDefinedType(String type) {
        if (type == null) return false;
        String[] standardTypes = {"String", "int", "double", "float", "long", "boolean", "char", "byte", "short", "Integer", "Double", "Float", "Long", "Boolean", "Character", "Byte", "Short", "Object", "List", "Map", "Set", "CamelContext"};
        for (String st : standardTypes) {
            if (st.equals(type) || type.startsWith(st + "<")) return false;
        }
        // Must start with uppercase letter
        if (type.isEmpty() || !Character.isUpperCase(type.charAt(0))) return false;
        return true;
    }

    List<BeanData> parseYamlBeans(JsonNode beansNode) {
        List<BeanData> beans = new java.util.ArrayList<>();
        if (beansNode.isArray()) {
            for (JsonNode beanNode : beansNode) {
                BeanData b = new BeanData();
                b.name = beanNode.has("name") ? beanNode.get("name").asText() : "Unnamed";
                b.type = beanNode.has("type") ? beanNode.get("type").asText() : "Object";
                b.isLocal = true;
                if (beanNode.has("properties") && beanNode.get("properties").isObject()) {
                    beanNode.get("properties").fields().forEachRemaining(entry -> {
                        b.properties.put(entry.getKey(), entry.getValue().asText());
                    });
                }
                beans.add(b);
            }
            
            // Check for external references
            List<BeanData> refs = new java.util.ArrayList<>();
            for (BeanData b : beans) {
                for (Map.Entry<String, String> prop : b.properties.entrySet()) {
                    String val = prop.getValue();
                    if (val != null && val.startsWith("#")) {
                        String targetName = val.substring(1);
                        boolean exists = beans.stream().anyMatch(x -> x.name.equals(targetName));
                        if (!exists) {
                            BeanData refBean = new BeanData();
                            refBean.name = targetName;
                            refBean.type = "External Ref";
                            refBean.isLocal = false;
                            refs.add(refBean);
                        }
                    }
                }
            }
            beans.addAll(refs);
        }
        return beans;
    }

    private void renderStunningBeanDiagram(List<BeanData> beans, Pane parentContainer) {
        if (beans.isEmpty()) return;

        // Calculate dependency levels
        Map<String, Integer> levels = new java.util.HashMap<>();
        for (BeanData b : beans) {
            levels.put(b.name, 0);
        }

        // Propagate levels to lay out in columns (run up to 10 iterations to settle)
        for (int iter = 0; iter < 10; iter++) {
            for (BeanData b : beans) {
                int levelA = levels.getOrDefault(b.name, 0);
                for (Map.Entry<String, String> prop : b.properties.entrySet()) {
                    String val = prop.getValue();
                    String targetName = null;
                    if (val != null && val.startsWith("#")) {
                        targetName = val.substring(1);
                    } else if (isUserDefinedType(val)) {
                        targetName = val;
                    }
                    if (targetName != null && levels.containsKey(targetName)) {
                        int levelB = levels.get(targetName);
                        if (levelB <= levelA) {
                            levels.put(targetName, levelA + 1);
                        }
                    }
                }
            }
        }

        // Group by level
        Map<Integer, List<BeanData>> columns = new java.util.TreeMap<>();
        for (BeanData b : beans) {
            int lvl = levels.getOrDefault(b.name, 0);
            columns.computeIfAbsent(lvl, k -> new java.util.ArrayList<>()).add(b);
        }

        // Calculate positions
        double cardWidth = 240;
        double cardHeight = 90;
        double xSpacing = 340;
        double ySpacing = 160;

        double maxColHeight = 0;
        for (Map.Entry<Integer, List<BeanData>> col : columns.entrySet()) {
            double h = col.getValue().size() * ySpacing;
            if (h > maxColHeight) maxColHeight = h;
        }

        Pane canvas = new Pane();
        canvas.setStyle("-fx-background-color: transparent;");

        Map<String, double[]> nodeCenters = new java.util.HashMap<>();

        // Render cards
        for (Map.Entry<Integer, List<BeanData>> col : columns.entrySet()) {
            int lvl = col.getKey();
            List<BeanData> colBeans = col.getValue();
            double colHeight = colBeans.size() * ySpacing;
            double yOffset = (maxColHeight - colHeight) / 2.0;

            for (int i = 0; i < colBeans.size(); i++) {
                BeanData b = colBeans.get(i);
                double x = lvl * xSpacing + 20;
                double y = i * ySpacing + yOffset + 20;

                VBox card = new VBox(5);
                card.setPrefSize(cardWidth, cardHeight);
                card.setMinSize(cardWidth, cardHeight);
                card.setMaxSize(cardWidth, cardHeight);

                card.getStyleClass().add("eip-node");
                if (b.isLocal) {
                    card.getStyleClass().add("node-bean-local");
                    card.setStyle("-fx-background-color: linear-gradient(to bottom right, #2b3e50, #1e2b37); " +
                                 "-fx-padding: 10; -fx-border-radius: 8; -fx-background-radius: 8; " +
                                 "-fx-border-color: #3498db; -fx-border-width: 1.5;");
                } else {
                    card.getStyleClass().add("node-bean-ref");
                    card.setStyle("-fx-background-color: linear-gradient(to bottom right, #2d2d30, #252526); " +
                                 "-fx-padding: 10; -fx-border-radius: 8; -fx-background-radius: 8; " +
                                 "-fx-border-color: #7a7a7a; -fx-border-width: 1.5; -fx-border-style: dashed;");
                }

                Label lblHeader = new Label(b.name);
                lblHeader.setStyle("-fx-font-weight: bold; -fx-text-fill: #ffffff; -fx-font-size: 13px;");

                String cleanType = b.type;
                if (cleanType.contains(".")) {
                    cleanType = cleanType.substring(cleanType.lastIndexOf('.') + 1);
                }
                Label lblType = new Label(b.isLocal ? "<<" + cleanType + ">>" : "<<ref: " + cleanType + ">>");
                lblType.setStyle("-fx-font-style: italic; -fx-text-fill: " + (b.isLocal ? "#3498db" : "#aaaaaa") + "; -fx-font-size: 11px;");

                card.getChildren().addAll(lblHeader, lblType);

                if (!b.properties.isEmpty()) {
                    VBox propsBox = new VBox(1);
                    propsBox.setStyle("-fx-padding: 5 0 0 0; -fx-border-color: #3f3f46; -fx-border-width: 0.5 0 0 0;");
                    int count = 0;
                    for (Map.Entry<String, String> entry : b.properties.entrySet()) {
                        if (count >= 2) {
                            Label lblMore = new Label("... and " + (b.properties.size() - 2) + " more");
                            lblMore.setStyle("-fx-text-fill: #71717a; -fx-font-size: 10px;");
                            propsBox.getChildren().add(lblMore);
                            break;
                        }
                        String propType = entry.getValue();
                        if (propType.contains(".")) {
                            propType = propType.substring(propType.lastIndexOf('.') + 1);
                        }
                        Label lblProp = new Label(entry.getKey() + ": " + propType);
                        lblProp.setStyle("-fx-text-fill: #cccccc; -fx-font-size: 10px;");
                        propsBox.getChildren().add(lblProp);
                        count++;
                    }
                    card.getChildren().add(propsBox);
                }

                card.setLayoutX(x);
                card.setLayoutY(y);
                canvas.getChildren().add(card);

                nodeCenters.put(b.name, new double[]{x, y, cardWidth, cardHeight});
            }
        }

        // Render relation lines
        for (BeanData b : beans) {
            double[] coordsA = nodeCenters.get(b.name);
            if (coordsA == null) continue;

            for (Map.Entry<String, String> prop : b.properties.entrySet()) {
                String val = prop.getValue();
                String targetName = null;
                if (val != null && val.startsWith("#")) {
                    targetName = val.substring(1);
                } else if (isUserDefinedType(val)) {
                    targetName = val;
                }

                if (targetName != null && nodeCenters.containsKey(targetName)) {
                    double[] coordsB = nodeCenters.get(targetName);
                    
                    double startX, startY, endX, endY;
                    
                    if (coordsA[0] + coordsA[2] < coordsB[0]) {
                        startX = coordsA[0] + coordsA[2];
                        startY = coordsA[1] + coordsA[3]/2.0;
                        endX = coordsB[0];
                        endY = coordsB[1] + coordsB[3]/2.0;
                    } else if (coordsB[0] + coordsB[2] < coordsA[0]) {
                        startX = coordsA[0];
                        startY = coordsA[1] + coordsA[3]/2.0;
                        endX = coordsB[0] + coordsB[2];
                        endY = coordsB[1] + coordsB[3]/2.0;
                    } else {
                        startX = coordsA[0] + coordsA[2]/2.0;
                        startY = coordsA[1] + coordsA[3];
                        endX = coordsB[0] + coordsB[2]/2.0;
                        endY = coordsB[1];
                    }

                    Line line = new Line(startX, startY, endX, endY);
                    line.setStroke(Color.web(b.isLocal ? "#3498db" : "#aaaaaa"));
                    line.setStrokeWidth(2.0);
                    
                    boolean isTargetLocal = beans.stream().anyMatch(x -> x.name.equals(val != null && val.startsWith("#") ? val.substring(1) : val) && x.isLocal);
                    if (!isTargetLocal) {
                        line.getStrokeDashArray().addAll(5.0, 5.0);
                    }
                    
                    canvas.getChildren().add(line);

                    double angle = Math.atan2(endY - startY, endX - startX);
                    double arrowSize = 8;
                    javafx.scene.shape.Polygon arrow = new javafx.scene.shape.Polygon(
                        endX, endY,
                        endX - arrowSize * Math.cos(angle - Math.PI/6), endY - arrowSize * Math.sin(angle - Math.PI/6),
                        endX - arrowSize * Math.cos(angle + Math.PI/6), endY - arrowSize * Math.sin(angle + Math.PI/6)
                    );
                    arrow.setFill(Color.web(b.isLocal ? "#3498db" : "#aaaaaa"));
                    canvas.getChildren().add(arrow);

                    double midX = (startX + endX) / 2.0;
                    double midY = (startY + endY) / 2.0;
                    Label lblProp = new Label(prop.getKey());
                    lblProp.setStyle("-fx-font-size: 9px; -fx-text-fill: #ffffff; -fx-background-color: #1e1e1e; -fx-padding: 1 3; -fx-background-radius: 3; -fx-border-color: #3f3f46; -fx-border-width: 0.5; -fx-border-radius: 3;");
                    lblProp.setLayoutX(midX - 25);
                    lblProp.setLayoutY(midY - 7);
                    canvas.getChildren().add(lblProp);
                }
            }
        }

        double totalWidth = columns.size() * xSpacing + 40;
        double totalHeight = maxColHeight + 40;
        canvas.setPrefSize(totalWidth, totalHeight);
        canvas.setMinSize(totalWidth, totalHeight);

        parentContainer.getChildren().add(canvas);
    }

    private void renderBeansNode(JsonNode beansNode, Pane parentContainer, int index, com.fasterxml.jackson.databind.node.ArrayNode rootArray) {
        List<BeanData> beansList = parseYamlBeans(beansNode);
        renderStunningBeanDiagram(beansList, parentContainer);
    }

    private void renderOnExceptionNode(JsonNode excNode, Pane parentContainer, int index, com.fasterxml.jackson.databind.node.ArrayNode rootArray) {
        String excName = "Exception";
        if (excNode.has("exception") && excNode.get("exception").isArray() && excNode.get("exception").size() > 0) {
            excName = excNode.get("exception").get(0).asText();
        }
        
        StackPane nodePane = createEipNode("GLOBAL EXCEPTION", excName, "fas-exclamation-triangle", "node-nested");
        parentContainer.getChildren().add(nodePane);
        
        ContextMenu menu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Delete Global Exception", new FontIcon("fas-trash"));
        deleteItem.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete Global Exception?", ButtonType.YES, ButtonType.NO);
            RouteBuilderApp.themeDialog(confirm);
            confirm.showAndWait().ifPresent(res -> {
                if (res == ButtonType.YES) {
                    rootArray.remove(index);
                    updateYamlFromRoot();
                    if (activeContextMenu != null) activeContextMenu.hide();
                }
            });
        });
        menu.getItems().add(deleteItem);
        nodePane.setOnContextMenuRequested(e -> {
            if (activeContextMenu != null) activeContextMenu.hide();
            menu.show(nodePane, e.getScreenX(), e.getScreenY());
            activeContextMenu = menu;
            e.consume();
        });
        
        if (excNode.has("steps")) {
            renderSteps(excNode.get("steps"), parentContainer);
        }
    }

    private void renderRestNode(JsonNode restNode, Pane parentContainer, int index, com.fasterxml.jackson.databind.node.ArrayNode rootArray) {
        String path = restNode.has("path") ? restNode.get("path").asText() : "/";
        StackPane nodePane = createEipNode("REST CONFIG", path, "fas-server", "node-from");
        parentContainer.getChildren().add(nodePane);
        
        ContextMenu menu = new ContextMenu();
        MenuItem deleteItem = new MenuItem("Delete REST Config", new FontIcon("fas-trash"));
        deleteItem.setOnAction(e -> {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Delete REST Config?", ButtonType.YES, ButtonType.NO);
            RouteBuilderApp.themeDialog(confirm);
            confirm.showAndWait().ifPresent(res -> {
                if (res == ButtonType.YES) {
                    rootArray.remove(index);
                    updateYamlFromRoot();
                    if (activeContextMenu != null) activeContextMenu.hide();
                }
            });
        });
        menu.getItems().add(deleteItem);
        nodePane.setOnContextMenuRequested(e -> {
            if (activeContextMenu != null) activeContextMenu.hide();
            menu.show(nodePane, e.getScreenX(), e.getScreenY());
            activeContextMenu = menu;
            e.consume();
        });

        Pane verbsBox = createBranchContainer(40);
        String[] verbs = {"get", "post", "put", "delete", "patch"};
        for (String verb : verbs) {
            if (restNode.has(verb)) {
                JsonNode verbNodes = restNode.get(verb);
                if (verbNodes.isArray()) {
                    for (JsonNode vNode : verbNodes) {
                        String vPath = vNode.has("path") ? vNode.get("path").asText() : "";
                        String vTo = vNode.has("to") ? vNode.get("to").asText() : "";
                        
                        Pane branchBox = createBaseContainer(0);
                        Label titleLabel = new Label(verb.toUpperCase() + " " + vPath);
                        titleLabel.getStyleClass().add("branch-title");
                        branchBox.getChildren().add(titleLabel);
                        
                        branchBox.getChildren().add(createConnector("standard"));
                        branchBox.getChildren().add(createEipNode("TO", vTo, determineIcon(vTo, "fas-paper-plane"), "node-to"));
                        verbsBox.getChildren().add(branchBox);
                    }
                }
            }
        }
        
        if (!verbsBox.getChildren().isEmpty()) {
            parentContainer.getChildren().add(createConnector("split"));
            parentContainer.getChildren().add(verbsBox);
        }
    }

    private void renderRouteNode(JsonNode routeNode, Pane parentContainer, int routeIndex, com.fasterxml.jackson.databind.node.ArrayNode rootArray) {
        if (routeNode.has("from")) {
            JsonNode fromNode = routeNode.get("from");
            String uri = fromNode.has("uri") ? fromNode.get("uri").asText() : (fromNode.isTextual() ? fromNode.asText() : "Unknown Source");
            
            StackPane fromPane = createEipNode("FROM", uri, determineIcon(uri, "fas-play-circle"), "node-from");
            parentContainer.getChildren().add(fromPane);
            
            fromPane.setOnMouseClicked(e -> {
                if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                    selectFromNode(fromPane, routeNode, uri, true);
                    e.consume();
                }
            });
            
            ContextMenu fromMenu = new ContextMenu();
            MenuItem editFrom = new MenuItem("Edit Source URI...", new FontIcon("fas-edit"));
            editFrom.setOnAction(e -> {
                selectFromNode(fromPane, routeNode, uri, true);
            });
            fromMenu.getItems().add(editFrom);
            
            MenuItem deleteRoute = new MenuItem("Delete Route", new FontIcon("fas-trash"));
            deleteRoute.setOnAction(e -> {
                Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this entire route?", ButtonType.YES, ButtonType.NO);
                RouteBuilderApp.themeDialog(confirm);
                confirm.showAndWait().ifPresent(res -> {
                    if (res == ButtonType.YES) {
                        rootArray.remove(routeIndex);
                        updateYamlFromRoot();
                        if (activeContextMenu != null) activeContextMenu.hide();
                    }
                });
            });
            fromMenu.getItems().addAll(new javafx.scene.control.SeparatorMenuItem(), deleteRoute);
            
            fromPane.setOnContextMenuRequested(e -> {
                if (activeContextMenu != null) activeContextMenu.hide();
                fromMenu.show(fromPane, e.getScreenX(), e.getScreenY());
                activeContextMenu = fromMenu;
                e.consume();
            });

            if (fromNode.has("steps")) {
                renderSteps(fromNode.get("steps"), parentContainer);
            }
        }
    }

    private void renderSteps(JsonNode steps, Pane parentContainer) {
        if (steps != null && steps.isArray()) {
            com.fasterxml.jackson.databind.node.ArrayNode arrayNode = (com.fasterxml.jackson.databind.node.ArrayNode) steps;
            for (int i = 0; i < arrayNode.size(); i++) {
                final int index = i;
                JsonNode step = arrayNode.get(i);
                
                parentContainer.getChildren().add(createConnector("standard"));
                
                Pane stepContainer = createBaseContainer(0);
                renderSingleStep(step, stepContainer);
                
                stepContainer.setOnMouseClicked(e -> {
                    if (e.getButton() == javafx.scene.input.MouseButton.PRIMARY) {
                        Iterator<Map.Entry<String, JsonNode>> fields = step.fields();
                        if (fields.hasNext()) {
                            Map.Entry<String, JsonNode> field = fields.next();
                            selectNode(stepContainer, field.getKey(), field.getValue(), arrayNode, index, true);
                        }
                        e.consume();
                    }
                });
                
                ContextMenu menu = new ContextMenu();
                
                String insertBeforeStr = isHorizontal ? "Insert Left..." : "Insert Above...";
                String insertAfterStr = isHorizontal ? "Insert Right..." : "Insert Below...";
                String insertBeforeIcon = isHorizontal ? "fas-arrow-left" : "fas-arrow-up";
                String insertAfterIcon = isHorizontal ? "fas-arrow-right" : "fas-arrow-down";

                MenuItem editItem = new MenuItem("Edit Properties...", new FontIcon("fas-edit"));
                editItem.setOnAction(e -> {
                    Iterator<Map.Entry<String, JsonNode>> fields = step.fields();
                    if (fields.hasNext()) {
                        Map.Entry<String, JsonNode> field = fields.next();
                        selectNode(stepContainer, field.getKey(), field.getValue(), arrayNode, index, true);
                    }
                });

                javafx.scene.control.Menu insertAboveMenu = new javafx.scene.control.Menu(insertBeforeStr, new FontIcon(insertBeforeIcon));
                insertAboveMenu.getItems().addAll(
                    createInsertMenuItem("Log", "log", arrayNode, index),
                    createInsertMenuItem("SetBody", "setBody", arrayNode, index),
                    createInsertMenuItem("SetHeader", "setHeader", arrayNode, index),
                    createInsertMenuItem("To", "to", arrayNode, index),
                    createInsertMenuItem("To (XSLT)", "to_xslt", arrayNode, index),
                    createInsertMenuItem("To (JSLT)", "to_jslt", arrayNode, index),
                    createInsertMenuItem("To (Flatpack)", "to_flatpack", arrayNode, index),
                    createInsertMenuItem("Transform", "transform", arrayNode, index),
                    createInsertMenuItem("Process", "process", arrayNode, index),
                    createInsertMenuItem("To (Kamelet)", "kamelet", arrayNode, index),
                    createInsertMenuItem("Enrich", "enrich", arrayNode, index),
                    createInsertMenuItem("Delay", "delay", arrayNode, index),
                    createInsertMenuItem("WireTap", "wireTap", arrayNode, index),
                    new javafx.scene.control.SeparatorMenuItem(),
                    createInsertMenuItem("Choice", "choice", arrayNode, index),
                    createInsertMenuItem("DoTry", "doTry", arrayNode, index),
                    createInsertMenuItem("Circuit Breaker", "circuitBreaker", arrayNode, index),
                    createInsertMenuItem("Filter", "filter", arrayNode, index),
                    createInsertMenuItem("Multicast", "multicast", arrayNode, index),
                    createInsertMenuItem("Split", "split", arrayNode, index)
                );

                javafx.scene.control.Menu insertBelowMenu = new javafx.scene.control.Menu(insertAfterStr, new FontIcon(insertAfterIcon));
                insertBelowMenu.getItems().addAll(
                    createInsertMenuItem("Log", "log", arrayNode, index + 1),
                    createInsertMenuItem("SetBody", "setBody", arrayNode, index + 1),
                    createInsertMenuItem("SetHeader", "setHeader", arrayNode, index + 1),
                    createInsertMenuItem("To", "to", arrayNode, index + 1),
                    createInsertMenuItem("To (XSLT)", "to_xslt", arrayNode, index + 1),
                    createInsertMenuItem("To (JSLT)", "to_jslt", arrayNode, index + 1),
                    createInsertMenuItem("To (Flatpack)", "to_flatpack", arrayNode, index + 1),
                    createInsertMenuItem("Transform", "transform", arrayNode, index + 1),
                    createInsertMenuItem("Process", "process", arrayNode, index + 1),
                    createInsertMenuItem("To (Kamelet)", "kamelet", arrayNode, index + 1),
                    createInsertMenuItem("Enrich", "enrich", arrayNode, index + 1),
                    createInsertMenuItem("Delay", "delay", arrayNode, index + 1),
                    createInsertMenuItem("WireTap", "wireTap", arrayNode, index + 1),
                    new javafx.scene.control.SeparatorMenuItem(),
                    createInsertMenuItem("Choice", "choice", arrayNode, index + 1),
                    createInsertMenuItem("DoTry", "doTry", arrayNode, index + 1),
                    createInsertMenuItem("Circuit Breaker", "circuitBreaker", arrayNode, index + 1),
                    createInsertMenuItem("Filter", "filter", arrayNode, index + 1),
                    createInsertMenuItem("Multicast", "multicast", arrayNode, index + 1),
                    createInsertMenuItem("Split", "split", arrayNode, index + 1)
                );

                MenuItem deleteItem = new MenuItem("Delete Step", new FontIcon("fas-trash"));
                deleteItem.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Are you sure you want to delete this step?", ButtonType.YES, ButtonType.NO);
                    RouteBuilderApp.themeDialog(confirm);
                    confirm.showAndWait().ifPresent(res -> {
                        if (res == ButtonType.YES) {
                            arrayNode.remove(index);
                            updateYamlFromRoot();
                            if (activeContextMenu != null) activeContextMenu.hide();
                        }
                    });
                });
                
                menu.getItems().addAll(editItem, new javafx.scene.control.SeparatorMenuItem(), insertAboveMenu, insertBelowMenu, new javafx.scene.control.SeparatorMenuItem(), deleteItem);
                
                stepContainer.setOnContextMenuRequested(e -> {
                    if (activeContextMenu != null) activeContextMenu.hide();
                    menu.show(stepContainer, e.getScreenX(), e.getScreenY());
                    activeContextMenu = menu;
                    e.consume();
                });
                
                parentContainer.getChildren().add(stepContainer);
            }
        }
    }

    private org.fxmisc.richtext.CodeArea createHighlightedCodeArea(String text, int height) {
        org.fxmisc.richtext.CodeArea codeArea = new org.fxmisc.richtext.CodeArea();
        codeArea.appendText(text);
        codeArea.setPrefHeight(height);
        codeArea.setWrapText(true);
        codeArea.getStyleClass().add("syntax-editor");
        
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
            "(?<COMMENT>//[^\n]*)|" +
            "(?<KEYWORD>\\b(class|interface|public|private|protected|void|return|if|else|for|while|new|import|package|exchange|message|body|request|response|true|false|null|var|def)\\b)|" +
            "(?<STRING>\"[^\"]*\"|'[^']*')|" +
            "(?<NUMBER>\\b\\d+\\b)|" +
            "(?<XMLTAG></?[a-zA-Z0-9:-]+>)|" +
            "(?<XMLATTR>[a-zA-Z0-9:-]+=\"[^\"]*\")|" +
            "(?<KEY>\"[^\"]+\"(?=\\s*:))"
        );

        java.util.function.Consumer<String> applyHighlighting = (textToHighlight) -> {
            java.util.regex.Matcher matcher = pattern.matcher(textToHighlight);
            int lastKwEnd = 0;
            org.fxmisc.richtext.model.StyleSpansBuilder<java.util.Collection<String>> spansBuilder = new org.fxmisc.richtext.model.StyleSpansBuilder<>();
            while(matcher.find()) {
                String styleClass =
                    matcher.group("COMMENT") != null ? "syntax-comment" :
                    matcher.group("KEYWORD") != null ? "syntax-keyword" :
                    matcher.group("STRING") != null ? "syntax-string" :
                    matcher.group("NUMBER") != null ? "syntax-number" :
                    matcher.group("XMLTAG") != null ? "syntax-tag" :
                    matcher.group("XMLATTR") != null ? "syntax-attr" :
                    matcher.group("KEY") != null ? "syntax-key" : null;
                
                spansBuilder.add(java.util.Collections.emptyList(), matcher.start() - lastKwEnd);
                spansBuilder.add(java.util.Collections.singleton(styleClass), matcher.end() - matcher.start());
                lastKwEnd = matcher.end();
            }
            spansBuilder.add(java.util.Collections.emptyList(), textToHighlight.length() - lastKwEnd);
            codeArea.setStyleSpans(0, spansBuilder.create());
        };

        codeArea.textProperty().addListener((obs, oldText, newText) -> applyHighlighting.accept(newText));
        
        codeArea.setParagraphGraphicFactory(org.fxmisc.richtext.LineNumberFactory.get(codeArea));
        
        // Apply highlighting to initial text
        javafx.application.Platform.runLater(() -> applyHighlighting.accept(codeArea.getText()));
        
        GridPane.setHgrow(codeArea, Priority.ALWAYS);
        GridPane.setVgrow(codeArea, Priority.ALWAYS);
        
        return codeArea;
    }

    private String tryPrettyPrint(String input) {
        if (input == null || input.trim().isEmpty()) return input;
        String trimmed = input.trim();
        
        // Strip outer single/double quotes if they wrap the entire string
        if (trimmed.startsWith("'") && trimmed.endsWith("'") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        } else if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            trimmed = trimmed.substring(1, trimmed.length() - 1).trim();
        }
        
        // Unescape internal quotes if it looks like escaped JSON/XML
        if (trimmed.contains("\\\"")) {
            trimmed = trimmed.replace("\\\"", "\"");
        }
        if (trimmed.contains("\\n")) {
            trimmed = trimmed.replace("\\n", "\n");
        }
        if (trimmed.contains("\\t")) {
            trimmed = trimmed.replace("\\t", "\t");
        }
        
        if ((trimmed.startsWith("{") && trimmed.endsWith("}")) || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper jsonMapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Object json = jsonMapper.readValue(trimmed, Object.class);
                return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
            } catch (Exception e) { return input; }
        } else if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
            try {
                javax.xml.transform.Transformer transformer = javax.xml.transform.TransformerFactory.newInstance().newTransformer();
                transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
                transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
                // Remove standalone XML declaration to keep it clean if it wasn't there
                transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
                javax.xml.transform.stream.StreamResult result = new javax.xml.transform.stream.StreamResult(new java.io.StringWriter());
                javax.xml.transform.stream.StreamSource source = new javax.xml.transform.stream.StreamSource(new java.io.StringReader(trimmed));
                transformer.transform(source, result);
                return result.getWriter().toString();
            } catch (Exception e) { return input; }
        }
        return input;
    }

    private void updateYamlFromRoot() {
        if (rootNode != null && yamlUpdater != null) {
            try {
                String newYaml = yamlMapper.writeValueAsString(rootNode);
                yamlUpdater.accept(newYaml);
                renderDiagram(newYaml);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private MenuItem createInsertMenuItem(String label, String type, com.fasterxml.jackson.databind.node.ArrayNode arrayNode, int index) {
        MenuItem item = new MenuItem(label);
        item.setOnAction(e -> insertStep(arrayNode, index, type));
        return item;
    }

    private void insertStep(com.fasterxml.jackson.databind.node.ArrayNode arrayNode, int index, String type) {
        try {
            JsonNode newStep;
            if ("log".equals(type)) {
                newStep = yamlMapper.readTree("{ \"log\": { \"message\": \"New Log Step\" } }");
            } else if ("setBody".equals(type)) {
                newStep = yamlMapper.readTree("{ \"setBody\": { \"constant\": \"New Body\" } }");
            } else if ("setHeader".equals(type)) {
                newStep = yamlMapper.readTree("{ \"setHeader\": { \"name\": \"MyHeader\", \"constant\": \"MyValue\" } }");
            } else if ("to".equals(type)) {
                newStep = yamlMapper.readTree("{ \"to\": \"mock:destination\" }");
            } else if ("to_xslt".equals(type)) {
                newStep = yamlMapper.readTree("{ \"to\": \"xslt:stylesheet.xsl\" }");
            } else if ("to_jslt".equals(type)) {
                newStep = yamlMapper.readTree("{ \"to\": \"jslt:transform.json\" }");
            } else if ("to_flatpack".equals(type)) {
                newStep = yamlMapper.readTree("{ \"to\": \"flatpack:mapping.pzmap.xml\" }");
            } else if ("wireTap".equals(type)) {
                newStep = yamlMapper.readTree("{ \"wireTap\": \"mock:tap\" }");
            } else if ("transform".equals(type)) {
                newStep = yamlMapper.readTree("{ \"transform\": { \"simple\": \"${body.toUpperCase()}\" } }");
            } else if ("multicast".equals(type)) {
                newStep = yamlMapper.readTree("{ \"multicast\": { \"steps\": [ { \"to\": \"mock:a\" }, { \"to\": \"mock:b\" } ] } }");
            } else if ("split".equals(type)) {
                newStep = yamlMapper.readTree("{ \"split\": { \"simple\": \"${body}\", \"steps\": [ { \"log\": \"Split step\" } ] } }");
            } else if ("choice".equals(type)) {
                newStep = yamlMapper.readTree("{ \"choice\": { \"when\": [ { \"simple\": \"${body} != null\", \"steps\": [ { \"log\": { \"message\": \"Valid\" } } ] } ], \"otherwise\": { \"steps\": [ { \"log\": { \"message\": \"Invalid\" } } ] } } }");
            } else if ("doTry".equals(type)) {
                newStep = yamlMapper.readTree("{ \"doTry\": { \"steps\": [ { \"log\": \"Try block\" } ], \"doCatch\": [ { \"exception\": [ \"java.lang.Exception\" ], \"steps\": [ { \"log\": \"Caught exception\" } ] } ] } }");
            } else if ("circuitBreaker".equals(type)) {
                newStep = yamlMapper.readTree("{ \"circuitBreaker\": { \"steps\": [ { \"log\": \"Circuit breaker steps\" } ], \"onFallback\": { \"steps\": [ { \"log\": \"Fallback logic\" } ] } } }");
            } else if ("process".equals(type)) {
                newStep = yamlMapper.readTree("{ \"process\": { \"ref\": \"myCustomProcessorBean\" } }");
            } else if ("kamelet".equals(type)) {
                newStep = yamlMapper.readTree("{ \"to\": \"kamelet:my-kamelet\" }");
            } else if ("enrich".equals(type)) {
                newStep = yamlMapper.readTree("{ \"enrich\": { \"expression\": { \"constant\": \"direct:resource\" } } }");
            } else if ("delay".equals(type)) {
                newStep = yamlMapper.readTree("{ \"delay\": { \"constant\": 1000 } }");
            } else if ("filter".equals(type)) {
                newStep = yamlMapper.readTree("{ \"filter\": { \"simple\": \"${body} contains 'test'\", \"steps\": [ { \"log\": \"Filtered branch\" } ] } }");
            } else {
                newStep = yamlMapper.readTree("{ \"log\": { \"message\": \"Unknown\" } }");
            }
            arrayNode.insert(index, newStep);
            updateYamlFromRoot();
        } catch(Exception ex) { ex.printStackTrace(); }
    }



    private void selectNode(Pane nodePane, String title, JsonNode config, com.fasterxml.jackson.databind.node.ArrayNode arrayNode, int index, boolean openPanel) {
        if (selectedNodeUi != null) selectedNodeUi.setStyle("");
        selectedNodeUi = nodePane;
        selectedNodeUi.setStyle("-fx-border-color: #007acc; -fx-border-width: 3px; -fx-border-radius: 5px;");
        renderPropertyForm(title, config, arrayNode, index);
        if (openPanel) openPropertyPane();
    }

    private void selectFromNode(Pane nodePane, JsonNode routeNode, String uri, boolean openPanel) {
        if (selectedNodeUi != null) selectedNodeUi.setStyle("");
        selectedNodeUi = nodePane;
        selectedNodeUi.setStyle("-fx-border-color: #007acc; -fx-border-width: 3px; -fx-border-radius: 5px;");
        renderFromPropertyForm(routeNode, uri);
        if (openPanel) openPropertyPane();
    }

    private void renderPropertyForm(String stepName, JsonNode config, com.fasterxml.jackson.databind.node.ArrayNode arrayNode, int index) {
        propertyPane.getChildren().clear();
        
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label propTitle = new Label("Edit " + stepName.toUpperCase());
        propTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #007acc;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button closeBtn = new Button();
        closeBtn.getStyleClass().add("editor-btn");
        closeBtn.setGraphic(new FontIcon("fas-times"));
        closeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> closePropertyPane());
        header.getChildren().addAll(propTitle, spacer, closeBtn);
        propertyPane.getChildren().add(header);

        VBox formContainer = new VBox(10);

        final TextField[] txtFields = new TextField[3];
        final ComboBox<String>[] cbFields = new ComboBox[3];
        final org.fxmisc.richtext.CodeArea[] caFields = new org.fxmisc.richtext.CodeArea[3];

        if (stepName.equals("process") || stepName.equals("script")) {
            Label lbl1 = new Label("Execution Type:");
            lbl1.setStyle("-fx-font-weight: bold; -fx-padding: 5 0 0 0;");
            
            ComboBox<String> typeSelector = new ComboBox<>();
            typeSelector.getItems().addAll("Bean Reference", "Inline Java Script (jOOR)", "Inline Groovy Script");
            typeSelector.getStyleClass().add("combo-box");
            typeSelector.setMaxWidth(Double.MAX_VALUE);
            cbFields[0] = typeSelector;
            
            VBox dynamicForm = new VBox(10);
            
            // Sub-form 1: Bean Ref
            Label lblBean = new Label("Bean Ref:");
            lblBean.setStyle("-fx-font-weight: bold;");
            TextField txtBean = new TextField();
            txtFields[0] = txtBean;
            VBox beanBox = new VBox(5, lblBean, txtBean);
            
            // Sub-form 2: Java Code Area
            Label lblJava = new Label("Java (jOOR) Script:");
            lblJava.setStyle("-fx-font-weight: bold;");
            org.fxmisc.richtext.CodeArea caJava = createHighlightedCodeArea("", 200);
            caFields[0] = caJava;
            VBox javaBox = new VBox(5, lblJava, caJava);
            
            // Sub-form 3: Groovy Code Area
            Label lblGroovy = new Label("Groovy Script:");
            lblGroovy.setStyle("-fx-font-weight: bold;");
            org.fxmisc.richtext.CodeArea caGroovy = createHighlightedCodeArea("", 200);
            caFields[1] = caGroovy;
            VBox groovyBox = new VBox(5, lblGroovy, caGroovy);
            
            String initialType = "Bean Reference";
            if (stepName.equals("process")) {
                initialType = "Bean Reference";
                String r = "";
                if (config.isTextual()) r = config.asText();
                else if (config.has("ref")) r = config.get("ref").asText();
                txtBean.setText(r);
            } else if (stepName.equals("script")) {
                if (config.has("joor")) {
                    initialType = "Inline Java Script (jOOR)";
                    caJava.replaceText(config.get("joor").asText());
                } else if (config.has("groovy")) {
                    initialType = "Inline Groovy Script";
                    caGroovy.replaceText(config.get("groovy").asText());
                }
            }
            typeSelector.setValue(initialType);
            
            Runnable updateVis = () -> {
                dynamicForm.getChildren().clear();
                String sel = typeSelector.getValue();
                if ("Bean Reference".equals(sel)) dynamicForm.getChildren().add(beanBox);
                else if ("Inline Java Script (jOOR)".equals(sel)) dynamicForm.getChildren().add(javaBox);
                else if ("Inline Groovy Script".equals(sel)) dynamicForm.getChildren().add(groovyBox);
            };
            typeSelector.setOnAction(ev -> updateVis.run());
            updateVis.run();
            
            formContainer.getChildren().addAll(lbl1, typeSelector, dynamicForm);
            
        } else if (stepName.equals("transform") || stepName.equals("filter") || stepName.equals("split") || stepName.equals("setBody")) {
            Label lbl1 = new Label("Expression Language:");
            lbl1.setStyle("-fx-font-weight: bold; -fx-padding: 5 0 0 0;");
            
            ComboBox<String> langSelector = new ComboBox<>();
            langSelector.getItems().addAll("simple", "constant", "groovy", "joor", "jq", "jsonpath", "xpath");
            langSelector.getStyleClass().add("combo-box");
            langSelector.setMaxWidth(Double.MAX_VALUE);
            cbFields[0] = langSelector;
            
            Label lbl2 = new Label("Expression:");
            lbl2.setStyle("-fx-font-weight: bold; -fx-padding: 5 0 0 0;");
            
            String initialLang = "simple";
            String expressionText = "";
            
            if (config.isTextual()) {
                expressionText = config.asText();
            } else if (config.isObject()) {
                String[] langs = {"simple", "constant", "groovy", "joor", "jq", "jsonpath", "xpath"};
                boolean found = false;
                for (String l : langs) {
                    if (config.has(l)) {
                        initialLang = l;
                        expressionText = config.get(l).asText();
                        found = true;
                        break;
                    }
                }
                if (!found && config.has("expression")) {
                    JsonNode expNode = config.get("expression");
                    if (expNode.isTextual()) {
                        expressionText = expNode.asText();
                    } else {
                        for (String l : langs) {
                            if (expNode.has(l)) {
                                initialLang = l;
                                expressionText = expNode.get(l).asText();
                                break;
                            }
                        }
                    }
                }
            }
            langSelector.setValue(initialLang);
            
            org.fxmisc.richtext.CodeArea caExpr = createHighlightedCodeArea(tryPrettyPrint(expressionText), 160);
            caFields[0] = caExpr;
            
            formContainer.getChildren().addAll(lbl1, langSelector, lbl2, caExpr);
            
        } else if (stepName.equals("setHeader")) {
            Label lblName = new Label("Header Name:");
            lblName.setStyle("-fx-font-weight: bold; -fx-padding: 5 0 0 0;");
            TextField txtName = new TextField(config.has("name") ? config.get("name").asText() : "");
            txtFields[0] = txtName;
            
            Label lbl1 = new Label("Expression Language:");
            lbl1.setStyle("-fx-font-weight: bold; -fx-padding: 5 0 0 0;");
            
            ComboBox<String> langSelector = new ComboBox<>();
            langSelector.getItems().addAll("simple", "constant", "groovy", "joor", "jq", "jsonpath", "xpath");
            langSelector.getStyleClass().add("combo-box");
            langSelector.setMaxWidth(Double.MAX_VALUE);
            cbFields[0] = langSelector;
            
            Label lblExpr = new Label("Expression:");
            lblExpr.setStyle("-fx-font-weight: bold; -fx-padding: 5 0 0 0;");
            
            String initialLang = "simple";
            String expressionText = "";
            if (config.has("constant")) {
                initialLang = "constant";
                expressionText = config.get("constant").asText();
            } else if (config.has("simple")) {
                initialLang = "simple";
                expressionText = config.get("simple").asText();
            } else {
                String[] langs = {"simple", "constant", "groovy", "joor", "jq", "jsonpath", "xpath"};
                for (String l : langs) {
                    if (config.has(l)) {
                        initialLang = l;
                        expressionText = config.get(l).asText();
                        break;
                    }
                }
            }
            langSelector.setValue(initialLang);
            
            org.fxmisc.richtext.CodeArea caExpr = createHighlightedCodeArea(tryPrettyPrint(expressionText), 100);
            caFields[0] = caExpr;
            
            formContainer.getChildren().addAll(lblName, txtName, lbl1, langSelector, lblExpr, caExpr);
            
        } else if (stepName.equals("log")) {
            Label lbl1 = new Label("Message:");
            lbl1.setStyle("-fx-font-weight: bold; -fx-padding: 5 0 0 0;");
            String m = "";
            if (config.isTextual()) m = config.asText();
            else if (config.has("message")) m = config.get("message").asText();
            org.fxmisc.richtext.CodeArea ca = createHighlightedCodeArea(tryPrettyPrint(m), 60);
            caFields[0] = ca;
            formContainer.getChildren().addAll(lbl1, ca);
            
        } else if (stepName.equals("to") || stepName.equals("wireTap")) {
            Label lbl1 = new Label("URI:");
            lbl1.setStyle("-fx-font-weight: bold; -fx-padding: 5 0 0 0;");
            String u = "";
            if (config.isTextual()) u = config.asText();
            else if (config.has("uri")) u = config.get("uri").asText();
            TextField txt1 = new TextField(u);
            txtFields[0] = txt1;
            
            Label templatesLbl = new Label("Quick Templates:");
            templatesLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888; -fx-padding: 5 0 0 0;");
            javafx.scene.layout.FlowPane templatesBox = new javafx.scene.layout.FlowPane(5, 5);
            String[][] templates = {
                {"mock", "mock:result"},
                {"direct", "direct:start"},
                {"xslt", "xslt:stylesheet.xsl"},
                {"jslt", "jslt:transform.json"},
                {"flatpack", "flatpack:mapping.pzmap.xml"}
            };
            for (String[] t : templates) {
                Button btn = new Button(t[0]);
                btn.setStyle("-fx-font-size: 10px; -fx-padding: 3 6; -fx-background-color: #333333; -fx-text-fill: #cccccc; -fx-cursor: hand; -fx-border-color: #555555; -fx-border-radius: 2px;");
                btn.setOnAction(ev -> txt1.setText(t[1]));
                templatesBox.getChildren().add(btn);
            }
            formContainer.getChildren().addAll(lbl1, txt1, templatesLbl, templatesBox);
            
        } else if (stepName.equals("kamelet")) {
            Label lbl1 = new Label("Kamelet Name/URI:");
            lbl1.setStyle("-fx-font-weight: bold; -fx-padding: 5 0 0 0;");
            String k = "";
            if (config.isTextual()) k = config.asText();
            else if (config.has("name")) k = config.get("name").asText();
            TextField txt1 = new TextField(k);
            txtFields[0] = txt1;
            formContainer.getChildren().addAll(lbl1, txt1);
            
        } else if (stepName.equals("delay")) {
            Label lbl1 = new Label("Delay (Milliseconds):");
            lbl1.setStyle("-fx-font-weight: bold; -fx-padding: 5 0 0 0;");
            String d = "";
            if (config.isNumber() || config.isTextual()) d = config.asText();
            else if (config.has("constant")) d = config.get("constant").asText();
            TextField txt1 = new TextField(d);
            txtFields[0] = txt1;
            formContainer.getChildren().addAll(lbl1, txt1);
            
        } else if (stepName.equals("enrich")) {
            Label lbl1 = new Label("Enrich Resource URI:");
            lbl1.setStyle("-fx-font-weight: bold; -fx-padding: 5 0 0 0;");
            String e = "";
            if (config.isTextual()) e = config.asText();
            else if (config.has("expression") && config.get("expression").has("constant")) e = config.get("expression").get("constant").asText();
            else if (config.has("expression") && config.get("expression").isTextual()) e = config.get("expression").asText();
            TextField txt1 = new TextField(e);
            txtFields[0] = txt1;
            formContainer.getChildren().addAll(lbl1, txt1);
            
        } else {
            Label lbl1 = new Label("Raw JSON:");
            lbl1.setStyle("-fx-font-weight: bold; -fx-padding: 5 0 0 0;");
            String val = config.isTextual() ? config.asText() : config.toString();
            val = tryPrettyPrint(val);
            org.fxmisc.richtext.CodeArea ca = createHighlightedCodeArea(val, 240);
            caFields[0] = ca;
            formContainer.getChildren().addAll(lbl1, ca);
        }

        Button saveBtn = new Button("Save & Close");
        saveBtn.getStyleClass().add("editor-btn");
        saveBtn.setStyle("-fx-background-color: #007acc; -fx-text-fill: white; -fx-margin-top: 15px;");
        saveBtn.setOnAction(e -> {
            com.fasterxml.jackson.databind.node.ObjectNode finalNode;
            
            if (stepName.equals("process") || stepName.equals("script")) {
                String type = cbFields[0].getValue();
                if ("Bean Reference".equals(type)) {
                    com.fasterxml.jackson.databind.node.ObjectNode propsNode = yamlMapper.createObjectNode();
                    propsNode.put("ref", txtFields[0].getText());
                    finalNode = yamlMapper.createObjectNode();
                    finalNode.set("process", propsNode);
                } else if ("Inline Java Script (jOOR)".equals(type)) {
                    com.fasterxml.jackson.databind.node.ObjectNode propsNode = yamlMapper.createObjectNode();
                    propsNode.put("joor", caFields[0].getText());
                    finalNode = yamlMapper.createObjectNode();
                    finalNode.set("script", propsNode);
                } else {
                    com.fasterxml.jackson.databind.node.ObjectNode propsNode = yamlMapper.createObjectNode();
                    propsNode.put("groovy", caFields[1].getText());
                    finalNode = yamlMapper.createObjectNode();
                    finalNode.set("script", propsNode);
                }
            } else if (stepName.equals("transform") || stepName.equals("filter") || stepName.equals("split") || stepName.equals("setBody")) {
                com.fasterxml.jackson.databind.node.ObjectNode propsNode = yamlMapper.createObjectNode();
                propsNode.put(cbFields[0].getValue(), caFields[0].getText());
                if (config.isObject() && config.has("steps")) {
                    propsNode.set("steps", config.get("steps"));
                }
                finalNode = yamlMapper.createObjectNode();
                finalNode.set(stepName, propsNode);
                
            } else if (stepName.equals("setHeader")) {
                com.fasterxml.jackson.databind.node.ObjectNode propsNode = yamlMapper.createObjectNode();
                propsNode.put("name", txtFields[0].getText());
                propsNode.put(cbFields[0].getValue(), caFields[0].getText());
                if (config.isObject() && config.has("steps")) {
                    propsNode.set("steps", config.get("steps"));
                }
                finalNode = yamlMapper.createObjectNode();
                finalNode.set(stepName, propsNode);
                
            } else if (stepName.equals("to") || stepName.equals("wireTap")) {
                finalNode = yamlMapper.createObjectNode();
                finalNode.put(stepName, txtFields[0].getText());
                
            } else if (stepName.equals("kamelet")) {
                finalNode = yamlMapper.createObjectNode();
                finalNode.put(stepName, txtFields[0].getText());
                
            } else if (stepName.equals("enrich")) {
                com.fasterxml.jackson.databind.node.ObjectNode propsNode = yamlMapper.createObjectNode();
                com.fasterxml.jackson.databind.node.ObjectNode exprNode = yamlMapper.createObjectNode();
                exprNode.put("constant", txtFields[0].getText());
                propsNode.set("expression", exprNode);
                finalNode = yamlMapper.createObjectNode();
                finalNode.set(stepName, propsNode);
                
            } else if (stepName.equals("delay")) {
                com.fasterxml.jackson.databind.node.ObjectNode propsNode = yamlMapper.createObjectNode();
                propsNode.put("constant", txtFields[0].getText());
                finalNode = yamlMapper.createObjectNode();
                finalNode.set(stepName, propsNode);
                
            } else if (stepName.equals("log")) {
                com.fasterxml.jackson.databind.node.ObjectNode propsNode = yamlMapper.createObjectNode();
                propsNode.put("message", caFields[0].getText());
                if (config.isObject() && config.has("steps")) {
                    propsNode.set("steps", config.get("steps"));
                }
                finalNode = yamlMapper.createObjectNode();
                finalNode.set(stepName, propsNode);
                
            } else {
                try {
                    finalNode = (com.fasterxml.jackson.databind.node.ObjectNode) yamlMapper.createObjectNode().set(stepName, yamlMapper.readTree(caFields[0].getText()));
                } catch (Exception ignored) {
                    finalNode = yamlMapper.createObjectNode();
                }
            }
            
            arrayNode.set(index, finalNode);
            updateYamlFromRoot();
            closePropertyPane();
        });
        propertyPane.getChildren().addAll(formContainer, saveBtn);
    }

    private void renderFromPropertyForm(JsonNode routeNode, String uri) {
        propertyPane.getChildren().clear();
        
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        Label propTitle = new Label("Edit FROM");
        propTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 16px; -fx-text-fill: #007acc;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button closeBtn = new Button();
        closeBtn.getStyleClass().add("editor-btn");
        closeBtn.setGraphic(new FontIcon("fas-times"));
        closeBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
        closeBtn.setOnAction(e -> closePropertyPane());
        header.getChildren().addAll(propTitle, spacer, closeBtn);
        propertyPane.getChildren().add(header);

        VBox formContainer = new VBox(10);
        Label lbl1 = new Label("Source URI:");
        lbl1.setStyle("-fx-font-weight: bold; -fx-padding: 5 0 0 0;");
        TextField txt1 = new TextField(uri);
        formContainer.getChildren().addAll(lbl1, txt1);

        Button saveBtn = new Button("Save & Close");
        saveBtn.getStyleClass().add("editor-btn");
        saveBtn.setStyle("-fx-background-color: #007acc; -fx-text-fill: white; -fx-margin-top: 15px;");
        saveBtn.setOnAction(e -> {
            if (routeNode.isObject()) {
                com.fasterxml.jackson.databind.node.ObjectNode fromObj = yamlMapper.createObjectNode();
                fromObj.put("uri", txt1.getText());
                JsonNode fromNode = routeNode.get("from");
                if (fromNode.has("steps")) {
                    fromObj.set("steps", fromNode.get("steps"));
                }
                ((com.fasterxml.jackson.databind.node.ObjectNode)routeNode).set("from", fromObj);
                updateYamlFromRoot();
                closePropertyPane();
            }
        });
        propertyPane.getChildren().addAll(formContainer, saveBtn);
    }

    private void renderSingleStep(JsonNode stepNode, Pane parentContainer) {
        Iterator<Map.Entry<String, JsonNode>> fields = stepNode.fields();
        if (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String stepName = field.getKey();
            JsonNode config = field.getValue();

            switch (stepName.toLowerCase()) {
                case "choice":
                    renderChoice(config, parentContainer);
                    break;
                case "multicast":
                case "split":
                    // Render as parallel branches instead of sequential lines
                    renderParallelEip(stepName, config, "fas-code-branch", parentContainer);
                    break;
                case "dotry":
                    renderDoTry(config, parentContainer);
                    break;
                case "circuitbreaker":
                    renderCircuitBreaker(config, parentContainer);
                    break;
                case "docatch":
                case "dofinally":
                    renderNestedEip(stepName, config, "fas-shield-alt", parentContainer);
                    break;
                case "aggregate":
                    parentContainer.getChildren().add(createEipNode("AGGREGATE", extractDetails(config), "fas-compress-arrows-alt", "node-nested"));
                    break;
                case "filter":
                    renderNestedEip(stepName, config, "fas-filter", parentContainer);
                    break;
                case "log":
                    parentContainer.getChildren().add(createEipNode("LOG", extractDetails(config), "fas-terminal", "node-step"));
                    break;
                case "to":
                    parentContainer.getChildren().add(createEipNode("TO", extractDetails(config), determineIcon(extractDetails(config), "fas-paper-plane"), "node-to"));
                    break;
                case "process":
                    parentContainer.getChildren().add(createEipNode("PROCESS", extractDetails(config), "fas-microchip", "node-step"));
                    break;
                case "script":
                    parentContainer.getChildren().add(createEipNode("SCRIPT", extractDetails(config), "fas-code", "node-step"));
                    break;
                case "kamelet":
                    parentContainer.getChildren().add(createEipNode("KAMELET", extractDetails(config), "fas-puzzle-piece", "node-step"));
                    break;
                case "transform":
                    parentContainer.getChildren().add(createEipNode("TRANSFORM", extractDetails(config), "fas-exchange-alt", "node-step"));
                    break;
                case "enrich":
                case "pollenrich":
                    parentContainer.getChildren().add(createEipNode(stepName.toUpperCase(), extractDetails(config), "fas-link", "node-step"));
                    break;
                case "delay":
                    parentContainer.getChildren().add(createEipNode("DELAY", extractDetails(config), "fas-hourglass-half", "node-step"));
                    break;
                case "marshal":
                case "unmarshal":
                    parentContainer.getChildren().add(createEipNode(stepName.toUpperCase(), extractDetails(config), "fas-file-code", "node-step"));
                    break;
                case "setbody":
                case "setheader":
                case "removeheader":
                    parentContainer.getChildren().add(createEipNode(stepName.toUpperCase(), extractDetails(config), "fas-edit", "node-step"));
                    break;
                default:
                    parentContainer.getChildren().add(createEipNode(stepName.toUpperCase(), extractDetails(config), "fas-cog", "node-step"));
                    break;
            }
        }
    }

    private void renderChoice(JsonNode choiceConfig, Pane parentContainer) {
        parentContainer.getChildren().add(createEipNode("CHOICE", "Router", "fas-random", "node-choice"));

        Pane branchesBox = createBranchContainer(40);

        if (choiceConfig.has("when")) {
            JsonNode whenNodes = choiceConfig.get("when");
            if (whenNodes.isArray()) {
                for (JsonNode whenNode : whenNodes) {
                    branchesBox.getChildren().add(createBranch("WHEN", extractDetails(whenNode.get("simple")), whenNode.get("steps")));
                }
            }
        }
        
        if (choiceConfig.has("otherwise")) {
            JsonNode otherwiseNode = choiceConfig.get("otherwise");
            branchesBox.getChildren().add(createBranch("OTHERWISE", "", otherwiseNode.get("steps")));
        }

        if (!branchesBox.getChildren().isEmpty()) {
            parentContainer.getChildren().add(createConnector("split"));
            parentContainer.getChildren().add(branchesBox);
        }
    }

    private void renderDoTry(JsonNode tryConfig, Pane parentContainer) {
        parentContainer.getChildren().add(createEipNode("DOTRY", "Try Block", "fas-shield-alt", "node-nested"));
        
        // Render the Try steps
        if (tryConfig.has("steps")) {
            Pane nestedContainer = createBaseContainer(0);
            nestedContainer.getStyleClass().add("nested-container");
            renderSteps(tryConfig.get("steps"), nestedContainer);
            parentContainer.getChildren().add(nestedContainer);
        }

        // Render Catch blocks (can be an array)
        if (tryConfig.has("doCatch")) {
            JsonNode catches = tryConfig.get("doCatch");
            if (catches.isArray()) {
                for (JsonNode catchNode : catches) {
                    parentContainer.getChildren().add(createConnector("split"));
                    
                    // Extract exception name if available
                    String exceptionName = "Exception";
                    if (catchNode.has("exception") && catchNode.get("exception").isArray() && catchNode.get("exception").size() > 0) {
                        exceptionName = catchNode.get("exception").get(0).asText();
                    }
                    
                    parentContainer.getChildren().add(createEipNode("DOCATCH", exceptionName, "fas-exclamation-triangle", "node-nested"));
                    
                    if (catchNode.has("steps")) {
                        Pane catchContainer = createBaseContainer(0);
                        catchContainer.getStyleClass().add("nested-container");
                        renderSteps(catchNode.get("steps"), catchContainer);
                        parentContainer.getChildren().add(catchContainer);
                    }
                }
            }
        }
        
        // Render Finally block
        if (tryConfig.has("doFinally")) {
            JsonNode finallyNode = tryConfig.get("doFinally");
            parentContainer.getChildren().add(createConnector("standard"));
            parentContainer.getChildren().add(createEipNode("DOFINALLY", "Always executes", "fas-check-double", "node-nested"));
            
            if (finallyNode.has("steps")) {
                Pane finallyContainer = createBaseContainer(0);
                finallyContainer.getStyleClass().add("nested-container");
                renderSteps(finallyNode.get("steps"), finallyContainer);
                parentContainer.getChildren().add(finallyContainer);
            }
        }
    }

    private void renderCircuitBreaker(JsonNode cbConfig, Pane parentContainer) {
        parentContainer.getChildren().add(createEipNode("CIRCUITBREAKER", "Circuit Breaker", "fas-heartbeat", "node-nested"));
        
        // Render the main steps inside the circuit breaker
        if (cbConfig.has("steps")) {
            Pane nestedContainer = createBaseContainer(0);
            nestedContainer.getStyleClass().add("nested-container");
            renderSteps(cbConfig.get("steps"), nestedContainer);
            parentContainer.getChildren().add(nestedContainer);
        }

        // Render onFallback block
        if (cbConfig.has("onFallback")) {
            JsonNode fallbackNode = cbConfig.get("onFallback");
            parentContainer.getChildren().add(createConnector("split"));
            parentContainer.getChildren().add(createEipNode("ONFALLBACK", "Fallback Route", "fas-life-ring", "node-nested"));
            
            if (fallbackNode.has("steps")) {
                Pane fallbackContainer = createBaseContainer(0);
                fallbackContainer.getStyleClass().add("nested-container");
                renderSteps(fallbackNode.get("steps"), fallbackContainer);
                parentContainer.getChildren().add(fallbackContainer);
            } else if (fallbackNode.isArray()) {
                Pane fallbackContainer = createBaseContainer(0);
                fallbackContainer.getStyleClass().add("nested-container");
                renderSteps(fallbackNode, fallbackContainer);
                parentContainer.getChildren().add(fallbackContainer);
            }
        }
    }

    private void renderParallelEip(String name, JsonNode config, String iconLiteral, Pane parentContainer) {
        parentContainer.getChildren().add(createEipNode(name.toUpperCase(), extractDetails(config), iconLiteral, "node-nested"));
        
        if (config.has("steps")) {
            Pane branchesBox = createBranchContainer(40);
            branchesBox.getStyleClass().add("nested-container");
            
            JsonNode steps = config.get("steps");
            if (steps.isArray()) {
                for (JsonNode step : steps) {
                    Pane branchBox = createBaseContainer(0);
                    renderSingleStep(step, branchBox);
                    branchesBox.getChildren().add(branchBox);
                }
            }
            
            if (!branchesBox.getChildren().isEmpty()) {
                parentContainer.getChildren().add(createConnector("split"));
                parentContainer.getChildren().add(branchesBox);
            }
        }
    }

    private void renderNestedEip(String name, JsonNode config, String iconLiteral, Pane parentContainer) {
        parentContainer.getChildren().add(createEipNode(name.toUpperCase(), extractDetails(config), iconLiteral, "node-nested"));
        if (config.has("steps")) {
            Pane nestedContainer = createBaseContainer(0);
            nestedContainer.getStyleClass().add("nested-container");
            renderSteps(config.get("steps"), nestedContainer);
            parentContainer.getChildren().add(nestedContainer);
        }
    }

    private Pane createBranch(String title, String condition, JsonNode steps) {
        Pane branchBox = createBaseContainer(0);
        
        Label titleLabel = new Label(title + (condition.isEmpty() ? "" : ": " + condition));
        titleLabel.getStyleClass().add("branch-title");
        branchBox.getChildren().add(titleLabel);

        renderSteps(steps, branchBox);
        return branchBox;
    }

    private String extractDetails(JsonNode config) {
        if (config == null) return "";
        if (config.isTextual()) return config.asText();
        if (config.isObject()) {
            if (config.has("constant")) return config.get("constant").asText();
            if (config.has("simple")) return config.get("simple").asText();
            if (config.has("message")) return config.get("message").asText();
            if (config.has("uri")) return config.get("uri").asText();
            if (config.has("name")) return config.get("name").asText();
            if (config.has("ref")) return "ref: " + config.get("ref").asText();
            if (config.has("joor")) return "Java: " + limitString(config.get("joor").asText(), 25);
            if (config.has("groovy")) return "Groovy: " + limitString(config.get("groovy").asText(), 25);
            if (config.has("jq")) return "jq: " + limitString(config.get("jq").asText(), 25);
            if (config.has("jsonpath")) return "jsonpath: " + limitString(config.get("jsonpath").asText(), 25);
            if (config.has("xpath")) return "xpath: " + limitString(config.get("xpath").asText(), 25);
        }
        return "...";
    }

    private String limitString(String val, int max) {
        if (val == null) return "";
        val = val.trim().replace("\n", " ");
        if (val.length() <= max) return val;
        return val.substring(0, max - 3) + "...";
    }

    private String determineIcon(String uri, String defaultIcon) {
        if (uri == null) return defaultIcon;
        String lower = uri.toLowerCase();
        if (lower.startsWith("timer")) {
            return "svg:M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm.5-13H11v6l5.25 3.15.75-1.23-4.5-2.67z";
        } else if (lower.startsWith("kafka")) {
            return "svg:M16 11c1.66 0 2.99-1.34 2.99-3S17.66 5 16 5c-1.66 0-3 1.34-3 3v.53L8.04 6.04C8.42 5.17 8.1 4.15 7.25 3.65c-.88-.51-2.01-.21-2.52.67-.51.88-.21 2.01.67 2.52.48.27 1.05.29 1.54.08L11.5 9.38v5.24l-4.56 2.46c-.49-.21-1.06-.19-1.54.08-.88.51-1.18 1.64-.67 2.52.51.88 1.64 1.18 2.52.67.85-.5 1.17-1.52.79-2.39L13 15.53v.47c0 1.66 1.34 3 3 3 1.66 0 3-1.34 3-3 0-1.66-1.34-3-3-3-1.66 0-3 1.34-3 3v-2.02l-1.02-.68L13 11.66V11c0-.17.03-.33.07-.5l3.5-1.92c.32.26.74.42 1.2.42h.23z";
        } else if (lower.startsWith("ibmmq") || lower.startsWith("jms") || lower.startsWith("amqp")) {
            return "svg:M4 6h16v2H4zm0 5h16v2H4zm0 5h16v2H4z";
        } else if (lower.startsWith("mongodb")) {
            return "svg:M12 2c-3.3 0-6 2.7-6 6v8c0 3.3 2.7 6 6 6s6-2.7 6-6V8c0-3.3-2.7-6-6-6zm0 2c2.2 0 4 1.8 4 4s-1.8 4-4 4-4-1.8-4-4 1.8-4 4-4zm0 16c-2.2 0-4-1.8-4-4v-6.3c1 .9 2.4 1.3 4 1.3s3-.4 4-1.3V16c0 2.2-1.8 4-4 4z";
        }
        return defaultIcon;
    }

    private StackPane createEipNode(String title, String details, String iconLiteral, String styleClass) {
        VBox box = new VBox(5);
        box.setAlignment(Pos.CENTER);
        
        javafx.scene.Node iconNode;
        if (iconLiteral.startsWith("svg:")) {
            javafx.scene.shape.SVGPath path = new javafx.scene.shape.SVGPath();
            path.setContent(iconLiteral.substring(4));
            path.getStyleClass().add("node-svg-icon");
            path.setScaleX(1.3);
            path.setScaleY(1.3);
            
            javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
            glow.setColor(Color.web("#888888"));
            glow.setRadius(3);
            glow.setSpread(0.2);
            path.setEffect(glow);
            
            javafx.scene.layout.StackPane wrapper = new javafx.scene.layout.StackPane(path);
            wrapper.setMinSize(24, 24);
            wrapper.setMaxSize(24, 24);
            iconNode = wrapper;
        } else {
            FontIcon icon = new FontIcon(iconLiteral);
            icon.setIconSize(24);
            icon.getStyleClass().add("node-icon");
            iconNode = icon;
        }
        
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("node-title");
        
        Label detailsLabel = new Label(details);
        detailsLabel.getStyleClass().add("node-details");
        detailsLabel.setWrapText(true);
        detailsLabel.setMaxWidth(160);
        
        box.getChildren().addAll(iconNode, titleLabel, detailsLabel);
        
        StackPane wrapper = new StackPane(box);
        wrapper.setPadding(new Insets(10));
        return wrapper;
    }

    private Pane createConnector(String type) {
        Pane box = isHorizontal ? new HBox() : new VBox();
        if (box instanceof HBox) ((HBox)box).setAlignment(Pos.CENTER);
        else ((VBox)box).setAlignment(Pos.CENTER);
        
        double lineLength = 15;
        
        if ("standard".equals(type)) {
            Line line1 = isHorizontal ? new Line(0, 0, lineLength, 0) : new Line(0, 0, 0, lineLength);
            line1.setStroke(Color.web("#888888"));
            line1.setStrokeWidth(2.5);
            box.getChildren().add(line1);
            
            javafx.scene.shape.Polygon arrow = isHorizontal ? 
                new javafx.scene.shape.Polygon(0, -4, 0, 4, 6, 0) : 
                new javafx.scene.shape.Polygon(-4, 0, 4, 0, 0, 6);
            arrow.setFill(Color.web("#888888"));
            box.getChildren().add(arrow);
        } else if ("split".equals(type)) {
            javafx.scene.shape.SVGPath branch = new javafx.scene.shape.SVGPath();
            branch.setContent(isHorizontal ? 
                "M 0,15 L 10,15 Q 20,15 20,0 M 10,15 L 20,15 M 10,15 Q 20,15 20,30" : 
                "M 15,0 L 15,10 Q 15,20 0,20 M 15,10 L 15,20 M 15,10 Q 15,20 30,20");
            branch.setStroke(Color.web("#FF9800"));
            branch.setStrokeWidth(3);
            branch.setFill(Color.TRANSPARENT);
            
            javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
            glow.setColor(Color.web("#FF9800"));
            glow.setRadius(5);
            branch.setEffect(glow);
            
            box.getChildren().add(branch);
        } else if ("aggregate".equals(type)) {
            javafx.scene.shape.SVGPath merge = new javafx.scene.shape.SVGPath();
            merge.setContent(isHorizontal ? 
                "M 0,0 Q 0,15 10,15 M 0,15 L 20,15 M 0,30 Q 0,15 10,15" : 
                "M 0,0 Q 15,0 15,10 M 15,0 L 15,20 M 30,0 Q 15,0 15,10");
            merge.setStroke(Color.web("#9C27B0"));
            merge.setStrokeWidth(3);
            merge.setFill(Color.TRANSPARENT);
            
            javafx.scene.effect.DropShadow glow = new javafx.scene.effect.DropShadow();
            glow.setColor(Color.web("#9C27B0"));
            glow.setRadius(5);
            merge.setEffect(glow);
            
            box.getChildren().add(merge);
        }
        
        return box;
    }
    private javafx.scene.image.WritableImage snapshotDiagram() {
        if (scrollPane == null) return null;
        javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
        params.setFill(javafx.scene.paint.Color.TRANSPARENT);
        return scrollPane.snapshot(params, null);
    }
}
