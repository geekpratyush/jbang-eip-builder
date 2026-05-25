package com.routebuilder.test;

import org.eclipse.lsp4j.*;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

import java.io.File;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class LspTest {
    public static void main(String[] args) throws Exception {
        File lspJar = new File("lsp/camel-lsp-server.jar");
        if (!lspJar.exists()) {
            System.err.println("LSP JAR not found");
            return;
        }
        
        ProcessBuilder pb = new ProcessBuilder("java", "-jar", lspJar.getAbsolutePath());
        Process process = pb.start();
        
        LanguageClient client = new LanguageClient() {
            @Override public void telemetryEvent(Object object) {}
            @Override public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
                System.out.println("DIAGNOSTICS RECEIVED:");
                System.out.println(diagnostics);
            }
            @Override public void showMessage(MessageParams messageParams) {}
            @Override public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams requestParams) { return CompletableFuture.completedFuture(null); }
            @Override public void logMessage(MessageParams message) {
                System.out.println("LSP LOG: " + message.getMessage());
            }
        };
        
        org.eclipse.lsp4j.jsonrpc.Launcher<LanguageServer> launcher = LSPLauncher.createClientLauncher(client, process.getInputStream(), process.getOutputStream());
        LanguageServer server = launcher.getRemoteProxy();
        launcher.startListening();
        
        InitializeParams initParams = new InitializeParams();
        initParams.setProcessId((int) ProcessHandle.current().pid());
        initParams.setRootUri(new File(".").toURI().toString());
        initParams.setCapabilities(new ClientCapabilities());
        
        java.util.Map<String, Object> camelConfig = new java.util.HashMap<>();
        camelConfig.put("camelCatalogProvider", "Quarkus");
        java.util.Map<String, Object> settings = new java.util.HashMap<>();
        settings.put("camel", camelConfig);
        initParams.setInitializationOptions(settings);
        
        System.out.println("Initializing...");
        server.initialize(initParams).get();
        server.initialized(new InitializedParams());
        System.out.println("Initialized!");
        
        String yaml = "- route:\n" +
                      "    id: \"test\"\n" +
                      "    from:\n" +
                      "      uri: \"timer:trigger\"\n" +
                      "      steps:\n" +
                      "        - choice:\n" +
                      "            when:\n" +
                      "              - simp: \"${body} == 'A'\"\n" +
                      "                steps:\n" +
                      "                  - log: \"Processing type A\"";
                      
        DidOpenTextDocumentParams params = new DidOpenTextDocumentParams(
            new TextDocumentItem("file:///route.camel.yaml", "yaml", 1, yaml)
        );
        
        System.out.println("Opening document...");
        server.getTextDocumentService().didOpen(params);
        
        Thread.sleep(5000);
        
        process.destroyForcibly();
    }
}
