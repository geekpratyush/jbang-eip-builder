package com.routebuilder.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class SampleProjectGenerationTest {

    @Test
    public void testGenerateSampleProject(@TempDir Path tempDir) throws Exception {
        RouteBuilderApp app = new RouteBuilderApp();
        File baseDir = tempDir.toFile();

        // Trigger generation
        app.dumpSamplesToResources(); // This executes with nulls to test it doesn't crash on null UI
        
        // Let's call the actual generation on our temp directory
        // Using a reflection or helper to access the private generateChapterSamples method
        java.lang.reflect.Method method = RouteBuilderApp.class.getDeclaredMethod("generateChapterSamples", RouteTreePane.class, File.class);
        method.setAccessible(true);
        method.invoke(app, null, baseDir);

        // Assert files exist as expected
        File appProps = new File(baseDir, "application.properties");
        assertTrue(appProps.exists(), "application.properties should exist");

        File mongoBean = new File(baseDir, "infra-simulator/mongodb/engine/MongoGateway.java");
        assertTrue(mongoBean.exists(), "MongoGateway.java should exist in simulator engine");

        File chapter1 = new File(baseDir, "chapter-01-basics/01-hello-timer.camel.yaml");
        assertTrue(chapter1.exists(), "Chapter 1 timer example should exist");

        // Verify that the number of files generated matches the index files.txt
        String filesIndex = appProps.getParentFile().toPath().resolve("application.properties").toString(); // just getting resource
        java.io.InputStream is = RouteBuilderApp.class.getResourceAsStream("/sampleproject/files.txt");
        assertNotNull(is, "files.txt should be in resources classpath");
        
        String indexContent = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        String[] lines = indexContent.split("\\r?\\n");
        int expectedFileCount = 0;
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                expectedFileCount++;
                File generatedFile = new File(baseDir, line.trim());
                assertTrue(generatedFile.exists(), "Generated file should exist: " + line);
            }
        }

        System.out.println("Successfully validated " + expectedFileCount + " generated files.");
    }

    @Test
    public void testGenerateSampleMappings(@TempDir Path tempDir) throws Exception {
        TransformationStudioWindow window = new TransformationStudioWindow();
        File baseDir = tempDir.toFile();

        // Using reflection to call generateSampleMappings on our temp directory
        java.lang.reflect.Method method = TransformationStudioWindow.class.getDeclaredMethod("generateSampleMappings", File.class);
        method.setAccessible(true);
        method.invoke(window, baseDir);

        // Read files.txt to verify all listed files were copied
        java.io.InputStream is = TransformationStudioWindow.class.getResourceAsStream("/samplemapping/files.txt");
        assertNotNull(is, "samplemapping/files.txt should be in resources classpath");

        String indexContent = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        String[] lines = indexContent.split("\\r?\\n");
        int expectedFileCount = 0;
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                expectedFileCount++;
                File generatedFile = new File(baseDir, line.trim());
                assertTrue(generatedFile.exists(), "Generated sample mapping file should exist: " + line);
            }
        }

        System.out.println("Successfully validated " + expectedFileCount + " generated sample mapping files.");
    }
}
