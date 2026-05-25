package com.routebuilder.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.routebuilder.ui.DiagramPane.BeanData;

import java.io.File;
import java.nio.file.Files;
import java.util.List;
import javafx.application.Platform;

public class BeanDiagramTest {

    static {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException e) {
            // Already started
        }
    }

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());

    @Test
    public void testParseYamlBeans() throws Exception {
        String yaml = "- beans:\n" +
                      "    - name: \"orderService\"\n" +
                      "      type: \"com.enterprise.services.OrderService\"\n" +
                      "      properties:\n" +
                      "        auditService: \"#auditService\"\n" +
                      "        maxRetries: 5\n" +
                      "    - name: \"auditService\"\n" +
                      "      type: \"com.enterprise.services.AuditService\"\n" +
                      "      properties:\n" +
                      "        dbSource: \"#dataSource\"\n";

        JsonNode root = yamlMapper.readTree(yaml);
        JsonNode beansNode = root.get(0).get("beans");

        DiagramPane diagramPane = new DiagramPane(theme -> {}, updatedYaml -> {});
        List<BeanData> beans = diagramPane.parseYamlBeans(beansNode);

        assertNotNull(beans);
        assertEquals(3, beans.size());

        BeanData order = beans.stream().filter(b -> b.name.equals("orderService")).findFirst().orElse(null);
        assertNotNull(order);
        assertTrue(order.isLocal);
        assertEquals("com.enterprise.services.OrderService", order.type);
        assertEquals("#auditService", order.properties.get("auditService"));

        BeanData dataSource = beans.stream().filter(b -> b.name.equals("dataSource")).findFirst().orElse(null);
        assertNotNull(dataSource);
        assertFalse(dataSource.isLocal);
        assertEquals("External Ref", dataSource.type);
    }

    @Test
    public void testParseJavaBeans() throws Exception {
        String javaContent = "package com.enterprise.services;\n" +
                             "public class OrderService {\n" +
                             "    private AuditService auditService;\n" +
                             "    private String env = \"PROD\";\n" +
                             "    public void processOrder() {}\n" +
                             "}\n";

        File tempDir = Files.createTempDirectory("routebuilder-test").toFile();
        try {
            File mainFile = new File(tempDir, "OrderService.java");
            Files.writeString(mainFile.toPath(), javaContent);

            String auditContent = "package com.enterprise.services;\n" +
                                  "public class AuditService {\n" +
                                  "    private Object dbSource;\n" +
                                  "}\n";
            File auditFile = new File(tempDir, "AuditService.java");
            Files.writeString(auditFile.toPath(), auditContent);

            DiagramPane diagramPane = new DiagramPane(theme -> {}, updatedYaml -> {});
            List<BeanData> beans = diagramPane.parseJavaBeans(javaContent, mainFile);

            assertNotNull(beans);
            assertEquals(2, beans.size());

            BeanData order = beans.stream().filter(b -> b.name.equals("OrderService")).findFirst().orElse(null);
            assertNotNull(order);
            assertTrue(order.isLocal);
            assertEquals("AuditService", order.properties.get("auditService"));

            BeanData audit = beans.stream().filter(b -> b.name.equals("AuditService")).findFirst().orElse(null);
            assertNotNull(audit);
            assertTrue(audit.isLocal);
        } finally {
            deleteDir(tempDir);
        }
    }

    @Test
    public void testYamlAndJavaCrossFileReferences() throws Exception {
        String yaml = "- beans:\n" +
                      "    - name: \"orderService\"\n" +
                      "      type: \"com.enterprise.services.OrderService\"\n" +
                      "      properties:\n" +
                      "        auditService: \"#auditService\"\n";

        File tempDir = Files.createTempDirectory("routebuilder-cross-test").toFile();
        try {
            File mainYamlFile = new File(tempDir, "01-yaml.yaml");
            Files.writeString(mainYamlFile.toPath(), yaml);

            String javaContent = "package com.enterprise.services;\n" +
                                 "public class OrderService {\n" +
                                 "    private AuditService auditService;\n" +
                                 "}\n";
            File orderServiceFile = new File(tempDir, "OrderService.java");
            Files.writeString(orderServiceFile.toPath(), javaContent);

            String auditContent = "package com.enterprise.services;\n" +
                                  "public class AuditService {\n" +
                                  "    private Object dbSource;\n" +
                                  "}\n";
            File auditFile = new File(tempDir, "AuditService.java");
            Files.writeString(auditFile.toPath(), auditContent);

            java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            final String[] resultMermaid = new String[1];
            final Throwable[] error = new Throwable[1];

            Platform.runLater(() -> {
                try {
                    ClassDiagramPane classDiagramPane = new ClassDiagramPane();
                    resultMermaid[0] = classDiagramPane.parseYamlToMermaid(yaml, mainYamlFile);
                } catch (Throwable t) {
                    error[0] = t;
                } finally {
                    latch.countDown();
                }
            });

            latch.await(5, java.util.concurrent.TimeUnit.SECONDS);

            if (error[0] != null) {
                throw new RuntimeException(error[0]);
            }

            String mermaid = resultMermaid[0];
            assertNotNull(mermaid);
            assertTrue(mermaid.contains("classDiagram"));
            assertTrue(mermaid.contains("orderService ..> OrderService : instantiates"));
            assertTrue(mermaid.contains("OrderService --> AuditService : refers"));
            assertTrue(mermaid.contains("class OrderService"));
            assertTrue(mermaid.contains("class AuditService"));
        } finally {
            deleteDir(tempDir);
        }
    }

    private void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();
    }
}
