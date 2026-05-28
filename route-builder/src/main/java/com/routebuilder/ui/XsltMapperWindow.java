package com.routebuilder.ui;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.util.*;

public class XsltMapperWindow {

    private static final DataFormat TREE_ITEM_FORMAT = new DataFormat("application/x-java-tree-item");

    private static final String XSLT_PACS008_ULTIMATE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<xsl:stylesheet version=\"2.0\" \n" +
            "    xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"\n" +
            "    xmlns:pacs=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.07\"\n" +
            "    xmlns:fn=\"http://www.w3.org/2005/xpath-functions\">\n" +
            "\n" +
            "    <!-- Production-Grade ISO 20022 to SWIFT MT103 Transformation -->\n" +
            "    <xsl:output method=\"text\" indent=\"no\" encoding=\"UTF-8\"/>\n" +
            "\n" +
            "    <xsl:template match=\"/pacs:Document/pacs:FIToFICstmrCdtTrf\">\n" +
            "        <xsl:variable name=\"hdr\" select=\"pacs:GrpHdr\"/>\n" +
            "        <xsl:variable name=\"tx\" select=\"pacs:CdtTrfTxInf\"/>\n" +
            "        \n" +
            "        <!-- SWIFT Block 1 & 2 -->\n" +
            "        {1:F01<xsl:value-of select=\"substring($hdr/pacs:InstgAgt/pacs:FinInstnId/pacs:BICFI, 1, 12)\"/>0000000000}\n" +
            "        {2:I103<xsl:value-of select=\"substring($hdr/pacs:InstdAgt/pacs:FinInstnId/pacs:BICFI, 1, 12)\"/>N}\n" +
            "        \n" +
            "        <!-- SWIFT Block 4 -->\n" +
            "        {4:\n" +
            "        :20:<xsl:value-of select=\"substring($tx/pacs:PmtId/pacs:EndToEndId, 1, 16)\"/>\n" +
            "        :23B:CRED\n" +
            "        \n" +
            "        <!-- Value Date, Currency, Interbank Settled Amount -->\n" +
            "        :32A:<xsl:value-of select=\"format-date($tx/pacs:IntrBkSttlmDt, '[YY][MN][D]')\"/>\n" +
            "             <xsl:value-of select=\"$tx/pacs:IntrBkSttlmAmt/@Ccy\"/>\n" +
            "             <xsl:value-of select=\"translate(format-number($tx/pacs:IntrBkSttlmAmt, '0.00'), '.', ',')\"/>\n" +
            "        \n" +
            "        <!-- Instructed Amount -->\n" +
            "        :33B:<xsl:value-of select=\"$tx/pacs:InstdAmt/@Ccy\"/><xsl:value-of select=\"translate(format-number($tx/pacs:InstdAmt, '0.00'), '.', ',')\"/>\n" +
            "        \n" +
            "        <!-- Ordering Customer (50K) -->\n" +
            "        :50K:/<xsl:value-of select=\"$tx/pacs:DbtrAcct/pacs:Id/pacs:IBAN\"/>\n" +
            "             <xsl:value-of select=\"substring($tx/pacs:Dbtr/pacs:Nm, 1, 35)\"/>\n" +
            "             <xsl:value-of select=\"substring(concat($tx/pacs:Dbtr/pacs:PstlAdr/pacs:StrtNm, ' ', $tx/pacs:Dbtr/pacs:PstlAdr/pacs:BldgNb), 1, 35)\"/>\n" +
            "             <xsl:value-of select=\"substring(concat($tx/pacs:Dbtr/pacs:PstCd, ' ', $tx/pacs:Dbtr/pacs:PstlAdr/pacs:TwnNm), 1, 35)\"/>\n" +
            "             <xsl:value-of select=\"$tx/pacs:Dbtr/pacs:PstlAdr/pacs:Ctry\"/>\n" +
            "        \n" +
            "        <!-- Ordering Institution (52A) -->\n" +
            "        :52A:<xsl:value-of select=\"$tx/pacs:InstgAgt/pacs:FinInstnId/pacs:BICFI\"/>\n" +
            "        \n" +
            "        <!-- Account Servicing Institution (57A) -->\n" +
            "        :57A:<xsl:value-of select=\"$tx/pacs:CdtrAgt/pacs:FinInstnId/pacs:BICFI\"/>\n" +
            "        \n" +
            "        <!-- Beneficiary Customer (59) -->\n" +
            "        :59:/<xsl:value-of select=\"$tx/pacs:CdtrAcct/pacs:Id/pacs:IBAN\"/>\n" +
            "             <xsl:value-of select=\"substring($tx/pacs:Cdtr/pacs:Nm, 1, 35)\"/>\n" +
            "             <xsl:value-of select=\"substring(concat($tx/pacs:Cdtr/pacs:PstlAdr/pacs:StrtNm, ' ', $tx/pacs:Cdtr/pacs:PstlAdr/pacs:TwnNm), 1, 35)\"/>\n" +
            "        \n" +
            "        <!-- Remittance Information (70) -->\n" +
            "        :70:<xsl:value-of select=\"substring($tx/pacs:RmtInf/pacs:Ustrd, 1, 140)\"/>\n" +
            "        \n" +
            "        <!-- Details of Charges (71A) -->\n" +
            "        :71A:<xsl:value-of select=\"$tx/pacs:ChrgBr\"/>\n" +
            "        \n" +
            "        <xsl:if test=\"$tx/pacs:ChrgsInf\">\n" +
            "            <xsl:for-each select=\"$tx/pacs:ChrgsInf\">\n" +
            "                :71F:<xsl:value-of select=\"pacs:Amt/@Ccy\"/><xsl:value-of select=\"translate(format-number(pacs:Amt, '0.00'), '.', ',')\"/>\n" +
            "            </xsl:for-each>\n" +
            "        </xsl:if>\n" +
            "        \n" +
            "        <!-- Regulatory Reporting (77B) -->\n" +
            "        <xsl:if test=\"$tx/pacs:RgltryRptg\">\n" +
            "            :77B:<xsl:value-of select=\"substring($tx/pacs:RgltryRptg/pacs:Dtls/pacs:Inf, 1, 105)\"/>\n" +
            "        </xsl:if>\n" +
            "        -}\n" +
            "    </xsl:template>\n" +
            "</xsl:stylesheet>";

    private static class MappingLine {
        List<String> sourceIds = new ArrayList<>();
        String targetId;
        String transformationExpr = "";
        VBox logicNode;

        MappingLine(String sourceId, String targetId) {
            this.sourceIds.add(sourceId);
            this.targetId = targetId;
        }
    }

    private static final List<MappingLine> MAPPINGS = new ArrayList<>();
    private static Pane workspaceCanvas;
    private static TreeView<String> sourceTree;
    private static TreeView<String> targetTree;
    private static WebView webView;
    private static WebEngine engine;
    private static boolean editorInitialized = false;

    public static void show() {
        Stage stage = new Stage();
        stage.setTitle("Enterprise Data Mapper - ISO 20022 Financial Orchestrator");

        BorderPane root = new BorderPane();
        root.setStyle("-fx-background-color: #1e1e1e;");

        // --- Global Header ---
        HBox header = new HBox(15);
        header.setPadding(new Insets(10, 20, 10, 20));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: #252526; -fx-border-color: #333333; -fx-border-width: 0 0 1 0;");

        Label title = new Label("ISO 20022 (pacs.008) → SWIFT MT103 ORCHESTRATOR");
        title.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-font-size: 14px;");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button btnAddFunc = new Button("Add Logic Node", new FontIcon("fas-plus-circle"));
        btnAddFunc.getStyleClass().add("editor-btn");
        btnAddFunc.setOnAction(e -> createFloatingLogicNode("New Function", "value-of(...)"));

        Button btnSync = new Button("Sync XSLT", new FontIcon("fas-sync-alt"));
        btnSync.getStyleClass().add("editor-btn");
        btnSync.setStyle("-fx-text-fill: #2196F3;");
        btnSync.setOnAction(e -> syncCodeFromMap());

        Button btnDeploy = new Button("Deploy Route", new FontIcon("fas-rocket"));
        btnDeploy.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 6 15;");

        header.getChildren().addAll(new FontIcon("fas-project-diagram"), title, spacer, btnAddFunc, btnSync, btnDeploy);
        root.setTop(header);

        // --- Main Workspace ---
        SplitPane mainSplit = new SplitPane();
        mainSplit.setStyle("-fx-background-color: transparent;");

        // 1. Monaco Editor (Left)
        webView = new WebView();
        RouteBuilderApp.installClipboardShortcuts(webView);
        engine = webView.getEngine();
        loadMonacoInWebView();

        VBox codeBox = new VBox(webView);
        VBox.setVgrow(webView, Priority.ALWAYS);
        codeBox.setMinWidth(400);

        // 2. Mapping Workspace (Right)
        StackPane mapper = createMappingWorkspace();
        
        mainSplit.getItems().addAll(codeBox, mapper);
        mainSplit.setDividerPositions(0.35);
        root.setCenter(mainSplit);

        Scene scene = new Scene(root, 1600, 1000);
        scene.getStylesheets().add(XsltMapperWindow.class.getResource("/styles/main.css").toExternalForm());
        stage.setScene(scene);
        stage.show();

        Platform.runLater(XsltMapperWindow::autoMapComprehensive);
    }

    private static void loadMonacoInWebView() {
        try {
            com.sun.net.httpserver.HttpServer server = com.sun.net.httpserver.HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                if ("/".equals(path) || "/index.html".equals(path)) {
                    String html = getMonacoHtml();
                    byte[] response = html.getBytes(java.nio.charset.StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                } else {
                    java.io.InputStream is = XsltMapperWindow.class.getResourceAsStream("/monaco" + path);
                    if (is == null) {
                        exchange.sendResponseHeaders(404, -1);
                    } else {
                        byte[] data = is.readAllBytes();
                        if (path.endsWith(".js")) exchange.getResponseHeaders().add("Content-Type", "application/javascript");
                        else if (path.endsWith(".css")) exchange.getResponseHeaders().add("Content-Type", "text/css");
                        else if (path.endsWith(".ttf")) exchange.getResponseHeaders().add("Content-Type", "font/ttf");
                        exchange.sendResponseHeaders(200, data.length);
                        exchange.getResponseBody().write(data);
                    }
                }
                exchange.close();
            });
            server.setExecutor(java.util.concurrent.Executors.newCachedThreadPool());
            server.start();
            int port = server.getAddress().getPort();

            engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
                if (newState == javafx.concurrent.Worker.State.SUCCEEDED) {
                    editorInitialized = true;
                    setEditorText(XSLT_PACS008_ULTIMATE);
                }
            });

            engine.load("http://127.0.0.1:" + port + "/");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String getMonacoHtml() {
        return "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <meta charset=\"UTF-8\">\n" +
            "    <style>\n" +
            "        body { margin: 0; padding: 0; overflow: hidden; background-color: #1e1e1e; }\n" +
            "        #editor { width: 100vw; height: 100vh; }\n" +
            "    </style>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <div id=\"editor\"></div>\n" +
            "    <script src=\"/vs/loader.js\"></script>\n" +
            "    <script>\n" +
            "        require.config({ paths: { vs: '/vs' }});\n" +
            "        require(['vs/editor/editor.main'], function() {\n" +
            "            window.editor = monaco.editor.create(document.getElementById('editor'), {\n" +
            "                value: '',\n" +
            "                language: 'xml',\n" +
            "                theme: 'vs-dark',\n" +
            "                automaticLayout: true,\n" +
            "                minimap: { enabled: true },\n" +
            "                scrollBeyondLastLine: false,\n" +
            "                fontSize: 14,\n" +
            "                lineNumbers: 'on',\n" +
            "                renderWhitespace: 'all',\n" +
            "                folding: true\n" +
            "            });\n" +
            "        });\n" +
            "        window.setValue = function(val) {\n" +
            "            if(window.editor) window.editor.setValue(val);\n" +
            "        };\n" +
            "        window.getValue = function() {\n" +
            "            return window.editor ? window.editor.getValue() : '';\n" +
            "        };\n" +
            "        window.appendText = function(val) {\n" +
            "            if(window.editor) {\n" +
            "                var lineCount = window.editor.getModel().getLineCount();\n" +
            "                var lastLineLength = window.editor.getModel().getLineMaxColumn(lineCount);\n" +
            "                var range = new monaco.Range(lineCount, lastLineLength, lineCount, lastLineLength);\n" +
            "                window.editor.executeEdits('', [{ range: range, text: '\\n' + val }]);\n" +
            "            }\n" +
            "        };\n" +
            "    </script>\n" +
            "</body>\n" +
            "</html>";
    }

    private static void setEditorText(String text) {
        if (editorInitialized) {
            try {
                String encoded = java.net.URLEncoder.encode(text, "UTF-8").replace("+", "%20");
                engine.executeScript("window.setValue(decodeURIComponent('" + encoded + "'));");
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private static String getEditorText() {
        return editorInitialized ? (String) engine.executeScript("window.getValue();") : "";
    }

    private static void appendToEditor(String text) {
        if (editorInitialized) {
            try {
                String encoded = java.net.URLEncoder.encode(text, "UTF-8").replace("+", "%20");
                engine.executeScript("window.appendText(decodeURIComponent('" + encoded + "'));");
            } catch (Exception e) { e.printStackTrace(); }
        }
    }

    private static StackPane createMappingWorkspace() {
        StackPane workspace = new StackPane();
        workspace.setStyle("-fx-background-color: #121212;");

        HBox layout = new HBox(0);
        layout.setAlignment(Pos.CENTER);

        VBox sourceContainer = createTreePanel("SOURCE: ISO 20022", "#4CAF50");
        sourceTree = new TreeView<>(createPacs008HugeMock());
        setupTreeInteractivity(sourceTree, true);
        sourceContainer.getChildren().add(sourceTree);
        VBox.setVgrow(sourceTree, Priority.ALWAYS);

        VBox targetContainer = createTreePanel("TARGET: SWIFT MT103", "#2196F3");
        targetTree = new TreeView<>(createMt103HugeMock());
        setupTreeInteractivity(targetTree, false);
        targetContainer.getChildren().add(targetTree);
        VBox.setVgrow(targetTree, Priority.ALWAYS);

        workspaceCanvas = new Pane();
        workspaceCanvas.setPickOnBounds(false);
        
        Region centerSpacer = new Region();
        centerSpacer.setMinWidth(400);
        HBox.setHgrow(centerSpacer, Priority.ALWAYS);

        layout.getChildren().addAll(sourceContainer, centerSpacer, targetContainer);
        workspace.getChildren().addAll(layout, workspaceCanvas);

        sourceTree.expandedItemCountProperty().addListener((o, ov, nv) -> Platform.runLater(XsltMapperWindow::redrawMappings));
        targetTree.expandedItemCountProperty().addListener((o, ov, nv) -> Platform.runLater(XsltMapperWindow::redrawMappings));
        
        Platform.runLater(() -> {
            setupScrollListeners(sourceTree);
            setupScrollListeners(targetTree);
        });

        return workspace;
    }

    private static VBox createTreePanel(String title, String color) {
        VBox box = new VBox(0);
        box.setMinWidth(350);
        box.setPrefWidth(400);
        box.setStyle("-fx-background-color: #1e1e1e; -fx-border-color: #333333; -fx-border-width: 0 1 0 1;");
        
        HBox tb = new HBox(8);
        tb.getStyleClass().add("mapper-tree-toolbar");
        tb.setPadding(new Insets(8, 10, 8, 10));
        
        Label lbl = new Label(title);
        lbl.setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold; -fx-font-size: 10px;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        
        Button btnExp = new Button("", new FontIcon("fas-plus-square"));
        btnExp.getStyleClass().add("mapper-tool-btn");
        btnExp.setOnAction(e -> expandAll(title.contains("TARGET") ? targetTree : sourceTree));
        
        Button btnCol = new Button("", new FontIcon("fas-minus-square"));
        btnCol.getStyleClass().add("mapper-tool-btn");
        btnCol.setOnAction(e -> collapseAll(title.contains("TARGET") ? targetTree : sourceTree));
        
        tb.getChildren().addAll(lbl, sp, btnExp, btnCol);
        box.getChildren().add(tb);
        return box;
    }

    private static void setupTreeInteractivity(TreeView<String> tree, boolean isSource) {
        tree.setCellFactory(tv -> {
            TreeCell<String> cell = new TreeCell<>() {
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null); setGraphic(null); setStyle("-fx-background-color: transparent;");
                    } else {
                        setText(item);
                        setGraphic(getTreeItem().getGraphic());
                        String col = item.contains("string") ? "#ce9178" : (item.contains("date") ? "#b5cea8" : "#cccccc");
                        setStyle("-fx-background-color: transparent; -fx-text-fill: " + col + "; -fx-font-size: 14px;");
                    }
                }
            };

            if (isSource) {
                cell.setOnDragDetected(e -> {
                    if (!cell.isEmpty()) {
                        Dragboard db = cell.startDragAndDrop(TransferMode.LINK);
                        ClipboardContent content = new ClipboardContent();
                        content.put(TREE_ITEM_FORMAT, cell.getItem());
                        db.setContent(content);
                        e.consume();
                    }
                });
            } else {
                cell.setOnDragOver(e -> { if (e.getDragboard().hasContent(TREE_ITEM_FORMAT)) e.acceptTransferModes(TransferMode.LINK); e.consume(); });
                cell.setOnDragEntered(e -> { if (e.getDragboard().hasContent(TREE_ITEM_FORMAT)) cell.setStyle("-fx-background-color: #094771;"); });
                cell.setOnDragExited(e -> cell.setStyle("-fx-background-color: transparent;"));
                cell.setOnDragDropped(e -> {
                    Dragboard db = e.getDragboard();
                    if (db.hasContent(TREE_ITEM_FORMAT)) {
                        addOrUpdateMapping((String) db.getContent(TREE_ITEM_FORMAT), cell.getItem());
                        e.setDropCompleted(true);
                    }
                    e.consume();
                });
            }
            return cell;
        });
    }

    private static void addOrUpdateMapping(String sourceId, String targetId) {
        Optional<MappingLine> existing = MAPPINGS.stream().filter(m -> m.targetId.equals(targetId)).findFirst();
        if (existing.isPresent()) {
            if (!existing.get().sourceIds.contains(sourceId)) existing.get().sourceIds.add(sourceId);
        } else {
            MAPPINGS.add(new MappingLine(sourceId, targetId));
        }
        redrawMappings();
    }

    private static void redrawMappings() {
        workspaceCanvas.getChildren().clear();
        for (MappingLine mapping : MAPPINGS) {
            drawMappingGraph(mapping);
        }
    }

    private static void drawMappingGraph(MappingLine mapping) {
        List<Node> sNodes = new ArrayList<>();
        for (String sid : mapping.sourceIds) {
            Node n = findTreeCell(sourceTree, sid);
            if (n != null && n.isVisible()) sNodes.add(n);
        }
        Node tNode = findTreeCell(targetTree, mapping.targetId);

        if (sNodes.isEmpty() || tNode == null || !tNode.isVisible()) return;

        Point2D tPt = getAnchorPoint(tNode, false);
        Point2D tLoc = workspaceCanvas.sceneToLocal(tPt);

        if (mapping.sourceIds.size() > 1 || !mapping.transformationExpr.isEmpty()) {
            if (mapping.logicNode == null) mapping.logicNode = createLogicCard(mapping);
            double midX = workspaceCanvas.getWidth() / 2 - 65;
            double midY = tLoc.getY() - 25;
            mapping.logicNode.setLayoutX(midX);
            mapping.logicNode.setLayoutY(midY);
            if (!workspaceCanvas.getChildren().contains(mapping.logicNode)) workspaceCanvas.getChildren().add(mapping.logicNode);
            for (Node sn : sNodes) {
                Point2D sPt = getAnchorPoint(sn, true);
                Point2D sLoc = workspaceCanvas.sceneToLocal(sPt);
                workspaceCanvas.getChildren().add(createBezier(sLoc.getX(), sLoc.getY(), midX, midY + 25));
            }
            workspaceCanvas.getChildren().add(createBezier(midX + 130, midY + 25, tLoc.getX(), tLoc.getY()));
        } else {
            Point2D sPt = getAnchorPoint(sNodes.get(0), true);
            Point2D sLoc = workspaceCanvas.sceneToLocal(sPt);
            workspaceCanvas.getChildren().add(createBezier(sLoc.getX(), sLoc.getY(), tLoc.getX(), tLoc.getY()));
        }
    }

    private static VBox createLogicCard(MappingLine mapping) {
        VBox card = new VBox(2);
        card.getStyleClass().add("mapping-node");
        card.setPrefWidth(130);
        Label lblType = new Label(mapping.sourceIds.size() > 1 ? "AGGREGATION" : "TRANSFORM");
        lblType.getStyleClass().add("mapping-node-title");
        String expr = mapping.transformationExpr.isEmpty() ? "XSLT Script" : mapping.transformationExpr;
        Label lblExpr = new Label(expr);
        lblExpr.getStyleClass().add("mapping-node-expr");
        lblExpr.setWrapText(true);
        card.getChildren().addAll(lblType, lblExpr);
        card.setOnMouseClicked(e -> { if (e.getClickCount() == 2) showExpressionEditor(mapping); });
        return card;
    }

    private static void showExpressionEditor(MappingLine mapping) {
        TextInputDialog tid = new TextInputDialog(mapping.transformationExpr);
        RouteBuilderApp.themeDialog(tid);
        tid.setTitle("Logic Expression Editor");
        tid.setHeaderText("Mapping to " + mapping.targetId);
        tid.setContentText("Enter XSLT Function / XPath expression:");
        tid.showAndWait().ifPresent(v -> {
            mapping.transformationExpr = v;
            redrawMappings();
        });
    }

    private static void createFloatingLogicNode(String name, String initialExpr) {
        VBox card = new VBox(5);
        card.getStyleClass().add("mapping-node");
        card.setPrefWidth(150);
        card.setLayoutX(500); card.setLayoutY(300);
        Label title = new Label(name); title.getStyleClass().add("mapping-node-title");
        Label expr = new Label(initialExpr); expr.getStyleClass().add("mapping-node-expr");
        card.getChildren().addAll(title, expr);
        final double[] drag = new double[2];
        card.setOnMousePressed(e -> { drag[0] = e.getX(); drag[1] = e.getY(); card.setCursor(Cursor.MOVE); });
        card.setOnMouseDragged(e -> { card.setLayoutX(card.getLayoutX() + e.getX() - drag[0]); card.setLayoutY(card.getLayoutY() + e.getY() - drag[1]); redrawMappings(); });
        card.setOnMouseReleased(e -> card.setCursor(Cursor.DEFAULT));
        workspaceCanvas.getChildren().add(card);
    }

    private static CubicCurve createBezier(double x1, double y1, double x2, double y2) {
        CubicCurve c = new CubicCurve();
        c.setStartX(x1); c.setStartY(y1); c.setEndX(x2); c.setEndY(y2);
        double dist = Math.abs(x2 - x1);
        c.setControlX1(x1 + dist * 0.45); c.setControlY1(y1);
        c.setControlX2(x2 - dist * 0.45); c.setControlY2(y2);
        c.setStroke(Color.web("#007acc", 0.7)); c.setStrokeWidth(2.0); c.setFill(null); c.setStrokeLineCap(StrokeLineCap.ROUND);
        return c;
    }

    private static Point2D getAnchorPoint(Node n, boolean right) {
        double x = right ? n.getBoundsInLocal().getWidth() : 0;
        double y = n.getBoundsInLocal().getHeight() / 2;
        return n.localToScene(x, y);
    }

    private static Node findTreeCell(TreeView<String> tree, String text) {
        for (Node n : tree.lookupAll(".tree-cell")) {
            TreeCell<?> cell = (TreeCell<?>) n;
            if (text.equals(cell.getItem())) return cell;
        }
        return null;
    }

    private static void setupScrollListeners(TreeView<String> tree) {
        for (Node n : tree.lookupAll(".scroll-bar")) {
            ScrollBar sb = (ScrollBar) n;
            sb.valueProperty().addListener((o, ov, nv) -> redrawMappings());
        }
    }

    private static void expandAll(TreeView<String> tree) { expandRecursive(tree.getRoot(), true); Platform.runLater(XsltMapperWindow::redrawMappings); }
    private static void collapseAll(TreeView<String> tree) { expandRecursive(tree.getRoot(), false); tree.getRoot().setExpanded(true); Platform.runLater(XsltMapperWindow::redrawMappings); }
    private static void expandRecursive(TreeItem<String> item, boolean expand) {
        if (item == null) return;
        item.setExpanded(expand);
        for (TreeItem<String> child : item.getChildren()) expandRecursive(child, expand);
    }

    private static void clearMappings() { MAPPINGS.clear(); workspaceCanvas.getChildren().clear(); redrawMappings(); }

    private static void syncCodeFromMap() {
        StringBuilder sb = new StringBuilder();
        sb.append("<!-- Auto-Generated Mapping Logic -->\n");
        for (MappingLine m : MAPPINGS) {
            sb.append("<xsl:template match=\"").append(m.sourceIds.get(0)).append("\">\n");
            sb.append("    <").append(m.targetId.replaceAll("Tag \\w+: ", "").replaceAll(" ", "")).append(">\n");
            sb.append("        <xsl:value-of select=\"").append(m.transformationExpr.isEmpty() ? "." : m.transformationExpr).append("\"/>\n");
            sb.append("    </").append(m.targetId.replaceAll("Tag \\w+: ", "").replaceAll(" ", "")).append(">\n");
            sb.append("</xsl:template>\n");
        }
        appendToEditor(sb.toString());
    }

    private static void autoMapComprehensive() {
        addOrUpdateMapping("EndToEndId (string)", "Tag 20: Sender Reference");
        addOrUpdateMapping("IntrBkSttlmDt (date)", "Tag 32A: Value Date");
        addOrUpdateMapping("Ccy (string)", "Tag 32A: Currency");
        addOrUpdateMapping("IntrBkSttlmAmt (decimal)", "Tag 32A: Settlement Amount");
        addOrUpdateMapping("Nm (string)", "Tag 50K: Ordering Customer");
        addOrUpdateMapping("StrtNm (string)", "Tag 50K: Ordering Customer");
        addOrUpdateMapping("TwnNm (string)", "Tag 50K: Ordering Customer");
        
        MappingLine m = MAPPINGS.stream().filter(l -> l.targetId.contains("50K")).findFirst().get();
        m.transformationExpr = "concat(Nm, ', ', StrtNm, ', ', TwnNm)";
        
        addOrUpdateMapping("BICFI (string)", "Tag 52A: Ordering Institution BIC");
        addOrUpdateMapping("IBAN (string)", "Tag 59: Beneficiary Account");
        redrawMappings();
    }

    private static TreeItem<String> createPacs008HugeMock() {
        TreeItem<String> root = new TreeItem<>("pacs.008.001.07", new FontIcon("fas-file-invoice-dollar"));
        root.setExpanded(true);
        TreeItem<String> grpHdr = new TreeItem<>("GrpHdr");
        grpHdr.getChildren().addAll(new TreeItem<>("MsgId (string)"), new TreeItem<>("CreDtTm (datetime)"), new TreeItem<>("NbOfTxs (int)"));
        TreeItem<String> tx = new TreeItem<>("CdtTrfTxInf"); tx.setExpanded(true);
        TreeItem<String> pmtId = new TreeItem<>("PmtId");
        pmtId.getChildren().addAll(new TreeItem<>("InstrId (string)"), new TreeItem<>("EndToEndId (string)"), new TreeItem<>("TxId (string)"));
        TreeItem<String> amt = new TreeItem<>("IntrBkSttlmAmt (decimal)"); amt.getChildren().add(new TreeItem<>("Ccy (string)"));
        TreeItem<String> dt = new TreeItem<>("IntrBkSttlmDt (date)");
        TreeItem<String> dbtr = new TreeItem<>("Dbtr");
        dbtr.getChildren().add(new TreeItem<>("Nm (string)"));
        TreeItem<String> adr = new TreeItem<>("PstlAdr");
        adr.getChildren().addAll(new TreeItem<>("StrtNm (string)"), new TreeItem<>("BldgNb (string)"), new TreeItem<>("PstCd (string)"), new TreeItem<>("TwnNm (string)"), new TreeItem<>("Ctry (string)"));
        dbtr.getChildren().add(adr);
        TreeItem<String> dbtrAcct = new TreeItem<>("DbtrAcct");
        TreeItem<String> dId = new TreeItem<>("Id"); dId.getChildren().add(new TreeItem<>("IBAN (string)"));
        dbtrAcct.getChildren().add(dId);
        TreeItem<String> instgAgt = new TreeItem<>("InstgAgt");
        TreeItem<String> fiId = new TreeItem<>("FinInstnId"); fiId.getChildren().add(new TreeItem<>("BICFI (string)"));
        instgAgt.getChildren().add(fiId);
        tx.getChildren().addAll(pmtId, amt, dt, dbtr, dbtrAcct, instgAgt);
        root.getChildren().addAll(grpHdr, tx);
        return root;
    }

    private static TreeItem<String> createMt103HugeMock() {
        TreeItem<String> root = new TreeItem<>("SWIFT MT103", new FontIcon("fas-university"));
        root.setExpanded(true);
        TreeItem<String> b4 = new TreeItem<>("Text Block {4}"); b4.setExpanded(true);
        b4.getChildren().addAll(
            new TreeItem<>("Tag 20: Sender Reference"), new TreeItem<>("Tag 23B: Bank Operation Code"),
            new TreeItem<>("Tag 32A: Value Date"), new TreeItem<>("Tag 32A: Currency"), new TreeItem<>("Tag 32A: Settlement Amount"),
            new TreeItem<>("Tag 50K: Ordering Customer"), new TreeItem<>("Tag 52A: Ordering Institution BIC"),
            new TreeItem<>("Tag 59: Beneficiary Account"), new TreeItem<>("Tag 59: Beneficiary Name"),
            new TreeItem<>("Tag 71A: Details of Charges"), new TreeItem<>("Tag 77B: Regulatory Reporting")
        );
        root.getChildren().add(b4);
        return root;
    }
}
