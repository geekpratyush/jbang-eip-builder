package com.tessera.ui;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.tessera.ui.DiagramPane.BeanData;

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

    @Test
    public void testKameletIbmMqRendering() throws Exception {
        String yaml = "- beans:\n" +
                      "    - name: sslFactory\n" +
                      "      type: \"#class:com.tessera.kameletstudio.core.lib.crypto.KameletStudioSslSocketFactory\"\n" +
                      "      properties:\n" +
                      "        trustStorePath: '{{truststorepath}}'\n" +
                      "        trustStorePassword: '{{truststorepassword}}'\n" +
                      "        keyStorePath: '{{keystorepath}}'\n" +
                      "        keyStorePassword: '{{keystorepassword}}'\n" +
                      "    - name: mqConnectionFactory\n" +
                      "      type: \"#class:com.ibm.mq.jakarta.jms.MQConnectionFactory\"\n" +
                      "      properties:\n" +
                      "        hostName: '{{hostname}}'\n" +
                      "        port: '{{port}}'\n" +
                      "        queueManager: '{{queuemanager}}'\n" +
                      "        channel: '{{channel}}'\n" +
                      "        sslCipherSuite: '{{sslciphersuite}}'\n" +
                      "        sslSocketFactory: '#bean:{{sslFactory}}'\n" +
                      "    - name: mqPoolFactory\n" +
                      "      type: \"#class:org.messaginghub.pooled.jms.JmsPoolConnectionFactory\"\n" +
                      "      properties:\n" +
                      "        connectionFactory: '#bean:{{mqConnectionFactory}}'\n" +
                      "        maxConnections: 5\n" +
                      "- route:\n" +
                      "    from:\n" +
                      "      uri: \"kamelet:source\"\n" +
                      "      steps:\n" +
                      "        - to:\n" +
                      "            uri: \"jms:queue:{{queuename}}\"\n" +
                      "            parameters:\n" +
                      "              connectionFactory: \"#bean:{{mqPoolFactory}}\"\n";

        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
        final String[] resultMermaid = new String[1];
        final Throwable[] error = new Throwable[1];

        Platform.runLater(() -> {
            try {
                ClassDiagramPane classDiagramPane = new ClassDiagramPane();
                resultMermaid[0] = classDiagramPane.parseYamlToMermaid(yaml, null);
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
        
        // Assertions verifying that '#bean:' and '{{...}}' placeholders are cleaned up properly
        assertTrue(mermaid.contains("mqConnectionFactory --> sslFactory : depends"), "mqConnectionFactory depends on sslFactory relation not found");
        assertTrue(mermaid.contains("mqPoolFactory --> mqConnectionFactory : depends"), "mqPoolFactory depends on mqConnectionFactory relation not found");
        
        // Verify steps and endpoints
        assertTrue(mermaid.contains("Source_kamelet_source"), "Source node not found");
        assertTrue(mermaid.contains("Step_1_to"), "Step node not found");
        
        // Flow connections
        assertTrue(mermaid.contains("Source_kamelet_source --> Step_1_to : flow"), "Flow relation not found");
        
        // Step references to the pooled MQ factory bean
        assertTrue(mermaid.contains("Step_1_to ..> mqPoolFactory : references"), "Step references to mqPoolFactory relation not found");
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
