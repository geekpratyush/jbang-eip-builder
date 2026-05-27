package com.routebuilder.lsp;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class LspManager {

    private LanguageServer server;
    private Process process;
    private int documentVersion = 1;
    private boolean isDocumentOpen = false;
    private String currentUri = "file:///route.camel.yaml";
    private final ObjectMapper mapper = new ObjectMapper();
    private java.util.function.Consumer<PublishDiagnosticsParams> diagnosticsConsumer;

    public void setDocumentUri(String uri) {
        // Normalise: LSP needs a real file:// URI for diagnostics to work.
        // If the uri is already a file:// URI keep it as-is; otherwise treat
        // as a path and convert.  Append .camel.yaml only when the file has no
        // recognised YAML extension so the Camel LSP activates its schema.
        if (!uri.toLowerCase().startsWith("file:")) {
            java.io.File f = new java.io.File(uri);
            uri = f.toURI().toString();
        }
        if (uri.startsWith("file:/") && !uri.startsWith("file:///")) {
            uri = "file:///" + uri.substring(6);
        }
        String lower = uri.toLowerCase();
        if (!lower.endsWith(".camel.yaml") && !lower.endsWith(".yaml") && !lower.endsWith(".yml")) {
            uri = uri + ".camel.yaml";
        }
        if (server != null && isDocumentOpen) {
            server.getTextDocumentService().didClose(new DidCloseTextDocumentParams(new TextDocumentIdentifier(currentUri)));
        }
        this.currentUri = uri;
        this.isDocumentOpen = false;
        this.documentVersion = 1;
    }

    public void setDiagnosticsConsumer(java.util.function.Consumer<PublishDiagnosticsParams> consumer) {
        this.diagnosticsConsumer = consumer;
    }

    public void start() {
        new Thread(() -> {
            try {
                File lspJar = null;
                try {
                    File jarFile = new File(LspManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
                    File installDir = jarFile.getParentFile().getParentFile();
                    lspJar = new File(installDir, "lsp/camel-lsp-server.jar");
                } catch (Exception ignored) {}

                if (lspJar == null || !lspJar.exists()) {
                    lspJar = new File("lsp/camel-lsp-server.jar");
                }

                if (!lspJar.exists()) {
                    System.err.println("LSP JAR not found at: " + lspJar.getAbsolutePath());
                    return;
                }

                ProcessBuilder pb = new ProcessBuilder("java", "-jar", lspJar.getAbsolutePath());
                pb.redirectError(ProcessBuilder.Redirect.INHERIT);
                process = pb.start();

                LanguageClient client = new LanguageClient() {
                    @Override public void telemetryEvent(Object object) {}
                    @Override public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
                        if (diagnosticsConsumer != null) {
                            diagnosticsConsumer.accept(diagnostics);
                        }
                    }
                    @Override public void showMessage(MessageParams messageParams) {}
                    @Override public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) { return CompletableFuture.completedFuture(null); }
                    @Override public void logMessage(MessageParams message) {}
                };

                org.eclipse.lsp4j.jsonrpc.Launcher<LanguageServer> launcher = LSPLauncher.createClientLauncher(client, process.getInputStream(), process.getOutputStream());
                server = launcher.getRemoteProxy();
                launcher.startListening();

                InitializeParams initParams = new InitializeParams();
                initParams.setProcessId((int) ProcessHandle.current().pid());
                initParams.setRootUri(new File(".").toURI().toString());
                initParams.setCapabilities(new ClientCapabilities());

                java.util.Map<String, Object> camelConfig = new java.util.HashMap<>();
                camelConfig.put("camelCatalogProvider", "Main");
                java.util.Map<String, Object> settings = new java.util.HashMap<>();
                settings.put("camel", camelConfig);
                initParams.setInitializationOptions(settings);

                server.initialize(initParams).get();
                server.initialized(new InitializedParams());
                
                Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
                
                System.out.println("Camel Language Server initialized successfully!");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "LspManagerThread").start();
    }

    public void openDocument(String text) {
        if (server == null) return;
        DidOpenTextDocumentParams params = new DidOpenTextDocumentParams(
            new TextDocumentItem(currentUri, "yaml", documentVersion, text)
        );
        server.getTextDocumentService().didOpen(params);
        isDocumentOpen = true;
    }

    public void updateDocument(String text) {
        if (server == null) return;
        if (!isDocumentOpen) {
            openDocument(text);
            return;
        }
        documentVersion++;
        DidChangeTextDocumentParams params = new DidChangeTextDocumentParams(
            new VersionedTextDocumentIdentifier(currentUri, documentVersion),
            Collections.singletonList(new TextDocumentContentChangeEvent(text))
        );
        server.getTextDocumentService().didChange(params);
    }

    public CompletableFuture<String> getCompletions(int line, int col) {
        if (server == null) return CompletableFuture.completedFuture("[]");
        CompletionParams params = new CompletionParams(new TextDocumentIdentifier(currentUri), new Position(line, col));
        return server.getTextDocumentService().completion(params).thenApply(result -> {
            ArrayNode array = mapper.createArrayNode();
            if (result == null) return "[]";
            
            if (result.isLeft()) {
                for (CompletionItem item : result.getLeft()) {
                    addCompletionNode(array, item);
                }
            } else if (result.isRight()) {
                for (CompletionItem item : result.getRight().getItems()) {
                    addCompletionNode(array, item);
                }
            }
            return array.toString();
        });
    }

    private void addCompletionNode(ArrayNode array, CompletionItem item) {
        ObjectNode obj = array.addObject();
        obj.put("label", item.getLabel());
        
        if (item.getTextEdit() != null) {
            obj.put("insertText", item.getTextEdit().getLeft().getNewText());
        } else {
            obj.put("insertText", item.getInsertText() != null ? item.getInsertText() : item.getLabel());
        }
        
        if (item.getInsertTextFormat() == InsertTextFormat.Snippet) {
            obj.put("insertTextRules", 4); // monaco.languages.CompletionItemInsertTextRule.InsertAsSnippet
        }
        
        obj.put("detail", item.getDetail() != null ? item.getDetail() : "");
        obj.put("documentation", item.getDocumentation() != null && item.getDocumentation().isLeft() ? item.getDocumentation().getLeft() : "");
        obj.put("kind", mapKind(item.getKind()));
    }

    private int mapKind(CompletionItemKind kind) {
        if (kind == null) return 18; // Monaco Text
        switch (kind) {
            case Method: return 0;
            case Function: return 1;
            case Constructor: return 2;
            case Field: return 3;
            case Variable: return 4;
            case Class: return 5;
            case Interface: return 7;
            case Module: return 8;
            case Property: return 9;
            case Value: return 13;
            case Enum: return 15;
            case Keyword: return 17;
            case Snippet: return 27;
            default: return 18;
        }
    }

    public void stop() {
        if (process != null && process.isAlive()) {
            process.destroyForcibly();
        }
    }
}
