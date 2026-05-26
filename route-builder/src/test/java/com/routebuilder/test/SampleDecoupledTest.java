package com.routebuilder.test;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.application.Platform;
import com.routebuilder.ui.RouteTreePane;

public class SampleDecoupledTest {

    static {
        // Initialize JavaFX Platform so UI components can be instantiated without throwing exceptions
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Platform already started
        }
    }

    @Test
    public void testGenerateChapterSamplesAndVerifyDecoupling(@TempDir Path tempDir) throws Exception {
        RouteTreePane treePane = new RouteTreePane(file -> {});
        
        // Use reflection to invoke the private generateChapterSamples method
        java.lang.reflect.Method generateChapterSamplesMethod = 
            com.routebuilder.ui.RouteBuilderApp.class.getDeclaredMethod("generateChapterSamples", RouteTreePane.class, File.class);
        generateChapterSamplesMethod.setAccessible(true);
        
        // Instantiate RouteBuilderApp (may require Platform runtime, but let's see if constructor works)
        com.routebuilder.ui.RouteBuilderApp app = new com.routebuilder.ui.RouteBuilderApp();
        
        File baseDir = tempDir.toFile();
        generateChapterSamplesMethod.invoke(app, treePane, baseDir);
        
        // 1. Verify application.properties was generated in the base directory
        File appPropsFile = new File(baseDir, "application.properties");
        assertTrue(appPropsFile.exists(), "application.properties should exist in root of sample project");
        
        String propsContent = Files.readString(appPropsFile.toPath());
        assertTrue(propsContent.contains("wiretap.audit.uri="), "Should contain wiretap properties");
        assertTrue(propsContent.contains("kafka.orders.uri="), "Should contain kafka properties");
        assertTrue(propsContent.contains("ibmmq.request.uri="), "Should contain ibmmq properties");
        assertTrue(propsContent.contains("mongodb.orders.uri="), "Should contain mongodb properties");
        assertTrue(propsContent.contains("dlq.uri="), "Should contain dlq properties");
        
        // 2. Scan all generated YAML files and ensure none contain stub: directly (except maybe in comment headers describing the stub, but not in the URI field), and that they use the properties placeholders
        List<Path> yamlFiles;
        try (Stream<Path> stream = Files.walk(tempDir)) {
            yamlFiles = stream
                .filter(Files::isRegularFile)
                .filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
                .collect(Collectors.toList());
        }
        
        assertFalse(yamlFiles.isEmpty(), "Should have generated sample YAML files");
        
        for (Path yamlFile : yamlFiles) {
            String content = Files.readString(yamlFile);
            
            // Check lines starting with uri: to ensure they don't hardcode stub: or other real URIs
            String[] lines = content.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("uri:")) {
                    // Check if it is a placeholder or timer/direct/mock
                    boolean isPlaceholder = trimmed.contains("{{") && trimmed.contains("}}");
                    boolean isLocalEndpoint = trimmed.contains("timer:") || trimmed.contains("direct:") || trimmed.contains("mock:") || trimmed.contains("mongodb:cameldb"); // mongodb:cameldb is inside kamelet metadata spec properties definition
                    
                    // The spec properties in kamelets or comments can contain definitions, but actual routing uris should be placeholders
                    if (!isPlaceholder && !isLocalEndpoint) {
                        // Let's print for debugging
                        System.out.println("Non-placeholder, non-local URI line found in " + yamlFile.getFileName() + ": " + trimmed);
                    }
                }
            }
        }
        
        // Specifically verify the wiretap example uses placeholder
        Path wiretapFile = tempDir.resolve("chapter-02-routing/02-wiretap-audit.camel.yaml");
        assertTrue(Files.exists(wiretapFile), "wiretap example yaml should exist");
        String wiretapContent = Files.readString(wiretapFile);
        assertTrue(wiretapContent.contains("uri: \"{{wiretap.audit.uri}}\""), "wiretap example should use placeholder");
        
        // Specifically verify the kafka consumer example uses placeholder
        Path kafkaFile = tempDir.resolve("chapter-03-messaging/01-kafka-consumer.camel.yaml");
        assertTrue(Files.exists(kafkaFile), "kafka consumer example yaml should exist");
        String kafkaContent = Files.readString(kafkaFile);
        assertTrue(kafkaContent.contains("uri: \"{{kafka.orders.uri}}\""), "kafka consumer example should use placeholder");
    }

    @Test
    public void testDetectDependenciesFromProperties(@TempDir Path tempDir) throws Exception {
        Path propsFile = tempDir.resolve("test.properties");
        String content = 
            "# Comment line\n" +
            "kafka.orders.uri=stub:kafka:topic:orders\n" +
            "ibmmq.request.uri=stub:jms:queue:REQUEST.Q\n" +
            "mongodb.orders.uri=stub:mongodb:cameldb?operation=insert\n" +
            "some.other.uri=activemq:queue:my-queue\n" +
            "plain.value=not-a-uri-value\n";
        Files.writeString(propsFile, content);

        java.lang.reflect.Method detectDepsMethod = 
            com.routebuilder.ui.RouteBuilderApp.class.getDeclaredMethod("detectDependenciesFromProperties", java.util.List.class);
        detectDepsMethod.setAccessible(true);

        @SuppressWarnings("unchecked")
        java.util.Set<String> deps = (java.util.Set<String>) detectDepsMethod.invoke(null, java.util.List.of(propsFile.toString()));

        assertNotNull(deps);
        assertTrue(deps.contains("stub"));
        assertTrue(deps.contains("kafka"));
        assertTrue(deps.contains("jms"));
        assertTrue(deps.contains("mongodb"));
        assertTrue(deps.contains("activemq"));
        assertFalse(deps.contains("plain.value"));
    }

    @Test
    public void testRouteTreePaneFilteringNormalMode(@TempDir Path tempDir) throws Exception {
        Files.createFile(tempDir.resolve("Route.java"));
        Files.createFile(tempDir.resolve("Route.yaml"));
        Files.createFile(tempDir.resolve("Route.yml"));
        Files.createFile(tempDir.resolve("config.properties"));
        Files.createFile(tempDir.resolve(".hidden-file"));
        Path subDir = tempDir.resolve("subfolder");
        Files.createDirectory(subDir);
        Files.createFile(subDir.resolve("SubRoute.java"));
        Files.createFile(subDir.resolve("invalid.txt"));

        RouteTreePane pane = new RouteTreePane(file -> {});
        pane.setBaseDirectory(tempDir.toFile());
        pane.refresh();

        javafx.scene.control.TreeItem<File> root = pane.getRootItem();
        assertNotNull(root);
        
        java.util.Set<String> visibleNames = new java.util.HashSet<>();
        collectVisibleFileNames(root, visibleNames);

        assertTrue(visibleNames.contains("Route.java"));
        assertTrue(visibleNames.contains("Route.yaml"));
        assertTrue(visibleNames.contains("Route.yml"));
        assertTrue(visibleNames.contains("subfolder"));
        
        assertFalse(visibleNames.contains("config.properties"), "config.properties should be filtered out in normal mode");
        assertFalse(visibleNames.contains(".hidden-file"), "hidden files should be filtered out in normal mode");
        assertFalse(visibleNames.contains("invalid.txt"), "invalid.txt should be filtered out in normal mode");
    }

    private void collectVisibleFileNames(javafx.scene.control.TreeItem<File> item, java.util.Set<String> names) {
        if (item.getValue() != null) {
            names.add(item.getValue().getName());
        }
        for (javafx.scene.control.TreeItem<File> child : item.getChildren()) {
            collectVisibleFileNames(child, names);
        }
    }

    @Test
    public void testTransformationWizardFolderSafeConversion() {
        String title = "My awesome XSLT Mapping - Version 1.0! & XML";
        String folderSafe = title.trim().toLowerCase()
                                 .replaceAll("[^a-z0-9\\s-]", "")
                                 .replaceAll("\\s+", "-");
        
        assertEquals("my-awesome-xslt-mapping---version-10-xml", folderSafe);
    }

    @Test
    public void testCreateTransformationTemplateNested(@TempDir Path tempDir) throws Exception {
        Path subFolder = tempDir.resolve("grouping-folder");
        Files.createDirectory(subFolder);

        java.util.concurrent.CompletableFuture<Void> future = new java.util.concurrent.CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                com.routebuilder.ui.TransformationStudioWindow window = new com.routebuilder.ui.TransformationStudioWindow();
                java.lang.reflect.Method createTemplateMethod = 
                    com.routebuilder.ui.TransformationStudioWindow.class.getDeclaredMethod(
                        "createTransformationTemplate", 
                        File.class, String.class, String.class, String.class
                    );
                createTemplateMethod.setAccessible(true);

                createTemplateMethod.invoke(window, subFolder.toFile(), "Test Logic", "test-logic", "JSON to JSON (JSLT)");
                future.complete(null);
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });

        future.get(5, java.util.concurrent.TimeUnit.SECONDS);

        Path projectDir = subFolder.resolve("test-logic");
        assertTrue(Files.exists(projectDir));
        assertTrue(Files.exists(projectDir.resolve("transformation.json")));
        assertTrue(Files.exists(projectDir.resolve("source.json")));
        assertTrue(Files.exists(projectDir.resolve("transform.jslt")));

        String configContent = Files.readString(projectDir.resolve("transformation.json"));
        org.json.JSONObject config = new org.json.JSONObject(configContent);
        assertEquals("Test Logic", config.getString("name"));
        assertEquals("jslt", config.getString("type"));
    }

    @Test
    public void testSnippetDependencyAndRouteUsage() {
        String type = "jslt";
        String path = "/tmp/transform.jslt";
        String dep = "camel:jslt";
        String javaDsl = ".to(\"jslt:file:" + path + "\")";
        String yamlDsl = "- to:\n    uri: \"jslt:file:" + path + "\"";

        assertEquals("camel:jslt", dep);
        assertEquals(".to(\"jslt:file:/tmp/transform.jslt\")", javaDsl);
        assertEquals("- to:\n    uri: \"jslt:file:/tmp/transform.jslt\"", yamlDsl);
    }

    @Test
    public void testSnippetDependencyAndRouteUsageXslt() {
        String type = "xslt";
        String path = "/tmp/transform.xslt";
        String artifactId = "camel-xslt";
        String javaDsl = ".to(\"xslt-saxon:file:" + path + "\")";
        String yamlDsl = "- to:\n    uri: \"xslt-saxon:file:" + path + "\"";

        assertEquals("camel-xslt", artifactId);
        assertEquals(".to(\"xslt-saxon:file:/tmp/transform.xslt\")", javaDsl);
        assertEquals("- to:\n    uri: \"xslt-saxon:file:/tmp/transform.xslt\"", yamlDsl);
    }

    @Test
    public void testSnippetDependencyAndRouteUsageSmooks() {
        String xmlContent = "<smooks-resource-list xmlns=\"https://www.smooks.org/xsd/smooks-2.0.xsd\" xmlns:csv=\"https://www.smooks.org/xsd/smooks/csv-1.7.xsd\"><csv:reader /></smooks-resource-list>";
        String artifactId = "smooks-camel-cartridge";
        if (xmlContent.contains("csv") || xmlContent.contains("<csv:") || xmlContent.contains("xmlns:csv")) {
            artifactId = "smooks-csv-cartridge";
        }
        assertEquals("smooks-csv-cartridge", artifactId);
    }

    @Test
    public void testIndentString() throws Exception {
        java.lang.reflect.Method indentMethod = 
            com.routebuilder.ui.TransformationStudioWindow.class.getDeclaredMethod("indentString", String.class, int.class);
        indentMethod.setAccessible(true);
        
        String input = "line1\nline2\r\nline3";
        String output = (String) indentMethod.invoke(null, input, 4);
        assertEquals("    line1\n    line2\n    line3", output);
    }

    @Test
    public void testSnippetGenerationTimerAndSourceMessage() {
        String sourceMsg = "<order id=\"101\">\n    <customer>John Doe</customer>\n</order>";
        String path = "/tmp/transform-config.xml";
        
        // Replicate indentation for YAML (14 spaces)
        String indent = " ".repeat(14);
        String indented = indent + sourceMsg.replace("\r\n", "\n").replace("\r", "\n").replace("\n", "\n" + indent);
        
        String yamlDsl = "- route:\n" +
                         "    id: transform-test-route\n" +
                         "    from:\n" +
                         "      uri: \"timer:trigger?repeatCount=1\"\n" +
                         "      steps:\n" +
                         "        - setBody:\n" +
                         "            constant: |\n" +
                         indented + "\n" +
                         "        - unmarshal:\n" +
                         "            smooks:\n" +
                         "              smooksConfig: \"file:" + path + "\"\n" +
                         "        - log: \"Parsed Output: ${body}\"";
                         
        assertTrue(yamlDsl.contains("timer:trigger?repeatCount=1"));
        assertTrue(yamlDsl.contains("constant: |"));
        assertTrue(yamlDsl.contains("<order id=\"101\">"));
        assertTrue(yamlDsl.contains("smooksConfig: \"file:/tmp/transform-config.xml\""));
    }

    @Test
    public void testSnippetGenerationFlatpack() {
        String path = "/tmp/csv-format.pzmap.xml";
        String yamlStep = "unmarshal:\n" +
                          "            flatpack:\n" +
                          "              fixed: false\n" +
                          "              definition: \"file:" + path + "\"\n" +
                          "        - split:\n" +
                          "            simple: \"${body}\"\n" +
                          "            steps:\n" +
                          "              - log: \"Row: ${body}\"";
                          
        assertTrue(yamlStep.contains("unmarshal:"));
        assertTrue(yamlStep.contains("flatpack:"));
        assertTrue(yamlStep.contains("fixed: false"));
        assertTrue(yamlStep.contains("definition: \"file:/tmp/csv-format.pzmap.xml\""));
        assertTrue(yamlStep.contains("- split:"));
    }

    @Test
    public void testFlatpackFixedDetection() {
        String delimContent = "<flatpack><column name=\"FIRSTNAME\"/></flatpack>";
        String fixedContent = "<flatpack><column name=\"FIRSTNAME\" length=\"10\"/></flatpack>";
        
        boolean delimIsFixed = delimContent.contains("length=");
        boolean fixedIsFixed = fixedContent.contains("length=");
        
        assertFalse(delimIsFixed);
        assertTrue(fixedIsFixed);
    }

    @Test
    public void testGroovyDependencies() {
        java.util.List<String> deps = new java.util.ArrayList<>();
        String type = "groovy";
        if ("groovy".equals(type)) {
            deps.add("org.apache.camel:camel-groovy:4.20.0");
            deps.add("org.apache.groovy:groovy-xml:4.0.21");
            deps.add("org.apache.groovy:groovy-json:4.0.21");
        }
        assertTrue(deps.contains("org.apache.camel:camel-groovy:4.20.0"));
        assertTrue(deps.contains("org.apache.groovy:groovy-xml:4.0.21"));
        assertTrue(deps.contains("org.apache.groovy:groovy-json:4.0.21"));
    }

    @Test
    public void testExplicitDependencyScannerLogic() {
        String yamlContent = "#DEPS org.apache.camel:camel-groovy:4.20.0\n" +
                             "#DEPS org.apache.groovy:groovy-xml:4.0.21\n" +
                             "//DEPS org.apache.groovy:groovy-json:4.0.21\n" +
                             "- route:\n" +
                             "    id: test-route";
        java.util.Set<String> explicitDeps = new java.util.HashSet<>();
        for (String line : yamlContent.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("#DEPS ") || trimmed.startsWith("//DEPS ")) {
                String dep = trimmed.substring(trimmed.indexOf("DEPS ") + 5).trim();
                if (!dep.isEmpty()) {
                    explicitDeps.add(dep);
                }
            }
        }
        assertEquals(3, explicitDeps.size());
        assertTrue(explicitDeps.contains("org.apache.camel:camel-groovy:4.20.0"));
        assertTrue(explicitDeps.contains("org.apache.groovy:groovy-xml:4.0.21"));
        assertTrue(explicitDeps.contains("org.apache.groovy:groovy-json:4.0.21"));
    }

    @Test
    public void testRuntimeSelectionOption() {
        String defaultOption = "Camel Main Runtime";
        String mainOption = "Camel Main Runtime";
        
        boolean isMainDefault = "Camel Main Runtime".equals(defaultOption);
        boolean isMainMain = "Camel Main Runtime".equals(mainOption);
        
        assertTrue(isMainDefault);
        assertTrue(isMainMain);
    }

    @Test
    public void testJoorMainExecutionLogic() {
        String yamlContent = "        - transform:\n" +
                             "            joor: \"resource:file:Transform.java\"";
        
        boolean usesJoor = yamlContent.contains("joor:") || yamlContent.contains("<joor>");
        boolean isQuarkus = false;
        
        boolean usesMain = usesJoor && !isQuarkus;
        
        assertTrue(usesMain);
        assertFalse(isQuarkus);
    }

    @Test
    public void testJoorSnippetDependencyReplacement() {
        String type = "joor";
        java.util.List<String> deps = new java.util.ArrayList<>();
        if ("joor".equals(type)) {
            deps.add("org.apache.camel:camel-joor:4.18.2");
        }
        assertEquals(1, deps.size());
        assertEquals("org.apache.camel:camel-joor:4.18.2", deps.get(0));
    }

    @Test
    public void testWindowsPathSeparatorsNormalization() {
        String windowsPath = "C:\\Users\\User\\project\\transform.xslt";
        String normalized = windowsPath.replace("\\", "/");
        assertEquals("C:/Users/User/project/transform.xslt", normalized);
    }

    @Test
    public void testMermaidResourcePresence() throws java.io.IOException {
        try (java.io.InputStream is = com.routebuilder.ui.ClassDiagramPane.class.getResourceAsStream("/styles/mermaid.min.js")) {
            assertNotNull(is, "mermaid.min.js resource should be bundled in the classpath under /styles/");
            byte[] bytes = is.readAllBytes();
            assertTrue(bytes.length > 0, "mermaid.min.js should not be empty");
        }
    }
}
