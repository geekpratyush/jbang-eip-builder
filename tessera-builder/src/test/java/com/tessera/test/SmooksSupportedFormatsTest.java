package com.tessera.test;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.tessera.ui.TransformationBackend;
import java.io.File;
import java.nio.file.Files;

public class SmooksSupportedFormatsTest {

    private File getTestFolder(String path) throws Exception {
        java.net.URL resource = getClass().getResource(path);
        if (resource == null) {
            throw new IllegalArgumentException("Resource not found: " + path);
        }
        return new File(resource.toURI());
    }

    @Test
    public void testCsvTransformation() throws Exception {
        System.out.println("=== RUNNING CSV TRANSFORMATION TEST ===");
        File folder = getTestFolder("/test-mapping/smooks/smooks-csv/example1");
        File configFile = new File(folder, "smooks-config.xml");
        File sourceFile = new File(folder, "source.csv");
        
        assertTrue(configFile.exists(), "smooks-config.xml should exist");
        assertTrue(sourceFile.exists(), "source.csv should exist");
        
        String input = Files.readString(sourceFile.toPath());
        String configXml = Files.readString(configFile.toPath());
        
        String result = TransformationBackend.transform(input, configXml, "smooks", folder);
        
        System.out.println("CSV Result:\n" + result);
        assertNotNull(result);
        assertFalse(result.contains("Error"), "Should not contain Error: " + result);
        assertTrue(result.contains("<csv-set>"));
        assertTrue(result.contains("<csv-record"));
    }

    @Test
    public void testCsvTransformationExample3() throws Exception {
        System.out.println("=== RUNNING CSV TRANSFORMATION TEST EXAMPLE 3 ===");
        File folder = getTestFolder("/test-mapping/smooks/smooks-csv/example3");
        File configFile = new File(folder, "smooks-config.xml");
        File sourceFile = new File(folder, "source.csv");
        
        assertTrue(configFile.exists(), "smooks-config.xml should exist");
        assertTrue(sourceFile.exists(), "source.csv should exist");
        
        String input = Files.readString(sourceFile.toPath());
        String configXml = Files.readString(configFile.toPath());
        
        String result = TransformationBackend.transform(input, configXml, "smooks", folder);
        
        System.out.println("CSV Example 3 Result:\n" + result);
        assertNotNull(result);
        assertFalse(result.contains("Error"), "Should not contain Error: " + result);
        assertTrue(result.contains("<csv-set>"));
        assertTrue(result.contains("<csv-record"));
        assertTrue(result.contains("<firstName>John</firstName>"));
    }

    @Test
    public void testJsonTransformation() throws Exception {
        System.out.println("=== RUNNING JSON TRANSFORMATION TEST ===");
        File folder = getTestFolder("/test-mapping/smooks/smooks-json/example1");
        File configFile = new File(folder, "smooks-config.xml");
        File sourceFile = new File(folder, "source.json");
        
        assertTrue(configFile.exists(), "smooks-config.xml should exist");
        assertTrue(sourceFile.exists(), "source.json should exist");
        
        String input = Files.readString(sourceFile.toPath());
        String configXml = Files.readString(configFile.toPath());
        
        String result = TransformationBackend.transform(input, configXml, "smooks", folder);
        
        System.out.println("JSON Result:\n" + result);
        assertNotNull(result);
        assertFalse(result.contains("Error"), "Should not contain Error: " + result);
        assertTrue(result.contains("<order>"));
        assertTrue(result.contains("<firstName>John</firstName>"));
    }

    @Test
    public void testYamlTransformation() throws Exception {
        System.out.println("=== RUNNING YAML TRANSFORMATION TEST ===");
        File folder = getTestFolder("/test-mapping/smooks/smooks-yaml/example1");
        File configFile = new File(folder, "smooks-config.xml");
        File sourceFile = new File(folder, "source.yaml");
        
        assertTrue(configFile.exists(), "smooks-config.xml should exist");
        assertTrue(sourceFile.exists(), "source.yaml should exist");
        
        String input = Files.readString(sourceFile.toPath());
        String configXml = Files.readString(configFile.toPath());
        
        String result = TransformationBackend.transform(input, configXml, "smooks", folder);
        
        System.out.println("YAML Result:\n" + result);
        assertNotNull(result);
        assertFalse(result.contains("Error"), "Should not contain Error: " + result);
        assertTrue(result.contains("<config>"));
    }

    @Test
    public void testFixedLengthTransformation() throws Exception {
        System.out.println("=== RUNNING FIXED-LENGTH TRANSFORMATION TEST ===");
        File folder = getTestFolder("/test-mapping/smooks/smooks-fixed-length/example1");
        File configFile = new File(folder, "smooks-config.xml");
        File sourceFile = new File(folder, "source.txt");
        
        assertTrue(configFile.exists(), "smooks-config.xml should exist");
        assertTrue(sourceFile.exists(), "source.txt should exist");
        
        String input = Files.readString(sourceFile.toPath());
        String configXml = Files.readString(configFile.toPath());
        
        String result = TransformationBackend.transform(input, configXml, "smooks", folder);
        
        System.out.println("Fixed-Length Result:\n" + result);
        assertNotNull(result);
        assertFalse(result.contains("Error"), "Should not contain Error: " + result);
        assertTrue(result.contains("<set>"));
        assertTrue(result.contains("<record"));
    }
}
