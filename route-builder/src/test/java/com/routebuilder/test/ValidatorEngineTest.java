package com.routebuilder.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import com.routebuilder.ui.ValidatorStudioWindow;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ValidatorEngineTest {

    private static File tempWorkspace;

    @BeforeAll
    public static void setUp() throws IOException {
        Path tempPath = Files.createTempDirectory("validator-test-workspace-");
        tempWorkspace = tempPath.toFile();
        ValidatorStudioWindow.initializeWorkspace(tempWorkspace);
    }

    @AfterAll
    public static void tearDown() throws IOException {
        if (tempWorkspace != null && tempWorkspace.exists()) {
            Files.walk(tempWorkspace.toPath())
                 .sorted(Comparator.reverseOrder())
                 .map(Path::toFile)
                 .forEach(File::delete);
        }
    }

    @Test
    public void testXmlValidation() throws IOException {
        // Valid XML
        File validXmlFile = new File(tempWorkspace, "messages/xml/invoice-valid.xml");
        assertTrue(validXmlFile.exists());
        String validContent = Files.readString(validXmlFile.toPath());

        File schemaFile = new File(tempWorkspace, "schemas/xsd/invoice-schema.xsd");
        assertTrue(schemaFile.exists());
        String schemaContent = Files.readString(schemaFile.toPath());

        List<String> errors = new ArrayList<>();
        ValidatorStudioWindow.validateXmlAndXsd(validContent, schemaContent, errors);
        assertTrue(errors.isEmpty(), "Valid XML should have no errors: " + errors);

        // Invalid XML
        File invalidXmlFile = new File(tempWorkspace, "messages/xml/invoice-invalid.xml");
        assertTrue(invalidXmlFile.exists());
        String invalidContent = Files.readString(invalidXmlFile.toPath());
        errors.clear();
        ValidatorStudioWindow.validateXmlAndXsd(invalidContent, schemaContent, errors);
        assertFalse(errors.isEmpty(), "Invalid XML should report errors");
        assertTrue(errors.stream().anyMatch(e -> e.contains("Line")), "Errors should specify line number");
    }

    @Test
    public void testJsonValidation() throws IOException {
        // Valid JSON
        File validJsonFile = new File(tempWorkspace, "messages/json/customer-valid.json");
        assertTrue(validJsonFile.exists());
        String validContent = Files.readString(validJsonFile.toPath());

        File schemaFile = new File(tempWorkspace, "schemas/json-schema/customer-schema.json");
        assertTrue(schemaFile.exists());
        String schemaContent = Files.readString(schemaFile.toPath());

        List<String> errors = new ArrayList<>();
        ValidatorStudioWindow.validateJsonWithSchema(validContent, schemaContent, errors);
        assertTrue(errors.isEmpty(), "Valid JSON should have no errors: " + errors);

        // Invalid JSON
        File invalidJsonFile = new File(tempWorkspace, "messages/json/customer-invalid.json");
        assertTrue(invalidJsonFile.exists());
        String invalidContent = Files.readString(invalidJsonFile.toPath());
        errors.clear();
        ValidatorStudioWindow.validateJsonWithSchema(invalidContent, schemaContent, errors);
        assertFalse(errors.isEmpty(), "Invalid JSON should report errors");
        assertTrue(errors.stream().anyMatch(e -> e.contains("email") || e.contains("pattern")), "Errors should report validation constraint violations");
    }

    @Test
    public void testYamlValidation() throws IOException {
        // Valid YAML
        File validYamlFile = new File(tempWorkspace, "messages/yaml/config-valid.yaml");
        assertTrue(validYamlFile.exists());
        String validContent = Files.readString(validYamlFile.toPath());

        File schemaFile = new File(tempWorkspace, "schemas/json-schema/config-schema.json");
        assertTrue(schemaFile.exists());
        String schemaContent = Files.readString(schemaFile.toPath());

        List<String> errors = new ArrayList<>();
        ValidatorStudioWindow.validateYamlWithSchema(validContent, schemaContent, errors);
        assertTrue(errors.isEmpty(), "Valid YAML should have no errors: " + errors);

        // Invalid YAML
        File invalidYamlFile = new File(tempWorkspace, "messages/yaml/config-invalid.yaml");
        assertTrue(invalidYamlFile.exists());
        String invalidContent = Files.readString(invalidYamlFile.toPath());
        errors.clear();
        ValidatorStudioWindow.validateYamlWithSchema(invalidContent, schemaContent, errors);
        assertFalse(errors.isEmpty(), "Invalid YAML should report errors");
    }

    @Test
    public void testSwiftMtValidation() throws IOException {
        // Valid MT103 (Standard)
        File validMtFile = new File(tempWorkspace, "messages/mt/standard/mt103-valid.txt");
        assertTrue(validMtFile.exists());
        String validContent = Files.readString(validMtFile.toPath());
        List<String> errors = new ArrayList<>();
        ValidatorStudioWindow.validateSwiftMt(validContent, false, null, errors);
        assertTrue(errors.isEmpty(), "Valid MT103 should have no standard errors: " + errors);

        // Invalid MT103 (Standard)
        File invalidMtFile = new File(tempWorkspace, "messages/mt/standard/mt103-invalid.txt");
        assertTrue(invalidMtFile.exists());
        String invalidContent = Files.readString(invalidMtFile.toPath());
        errors.clear();
        ValidatorStudioWindow.validateSwiftMt(invalidContent, false, null, errors);
        assertFalse(errors.isEmpty(), "Invalid MT103 should report standard errors");

        // Enhanced Mode: Valid Enhanced
        File validEnhMtFile = new File(tempWorkspace, "messages/mt/enhanced/mt103-valid-enhanced.txt");
        assertTrue(validEnhMtFile.exists());
        String validEnhContent = Files.readString(validEnhMtFile.toPath());

        File rulesFile = new File(tempWorkspace, "validators/custom-mt-rules.json");
        String rulesContent = rulesFile.exists() ? Files.readString(rulesFile.toPath()) : null;

        errors.clear();
        ValidatorStudioWindow.validateSwiftMt(validEnhContent, true, rulesContent, errors);
        assertTrue(errors.isEmpty(), "Valid Enhanced MT103 should have no errors under enhanced mode: " + errors);

        // Enhanced Mode: Invalid Enhanced (Iran jurisdiction block, amount > 10M, etc.)
        File invalidEnhMtFile = new File(tempWorkspace, "messages/mt/enhanced/mt103-invalid-enhanced.txt");
        assertTrue(invalidEnhMtFile.exists());
        String invalidEnhContent = Files.readString(invalidEnhMtFile.toPath());
        errors.clear();
        ValidatorStudioWindow.validateSwiftMt(invalidEnhContent, true, rulesContent, errors);
        System.out.println("DEBUG Swift MT errors: " + errors);
        assertFalse(errors.isEmpty(), "Invalid Enhanced MT103 should report enhanced rules violations");
        assertTrue(errors.stream().anyMatch(e -> e.contains("Iran") || e.contains("exceeds limit")), 
                   "Should report custom-rule violations");
    }

    @Test
    public void testIso20022MxValidation() throws IOException {
        // Valid pacs.008
        File validIsoFile = new File(tempWorkspace, "messages/iso20022/pacs008-valid.xml");
        assertTrue(validIsoFile.exists());
        String validContent = Files.readString(validIsoFile.toPath());

        File schemaFile = new File(tempWorkspace, "schemas/iso20022/pacs008-schema.xsd");
        assertTrue(schemaFile.exists());
        String schemaContent = Files.readString(schemaFile.toPath());

        List<String> errors = new ArrayList<>();
        ValidatorStudioWindow.validateIso20022Mx(validContent, schemaContent, errors);
        assertTrue(errors.isEmpty(), "Valid MX should have no errors: " + errors);

        // Invalid pacs.008
        File invalidIsoFile = new File(tempWorkspace, "messages/iso20022/pacs008-invalid.xml");
        assertTrue(invalidIsoFile.exists());
        String invalidContent = Files.readString(invalidIsoFile.toPath());
        errors.clear();
        ValidatorStudioWindow.validateIso20022Mx(invalidContent, schemaContent, errors);
        assertFalse(errors.isEmpty(), "Invalid MX should report errors");
    }

    @Test
    public void testCsvWValidation() throws IOException {
        // Valid CSV
        File validCsvFile = new File(tempWorkspace, "messages/csv/transactions-valid.csv");
        assertTrue(validCsvFile.exists());
        String validContent = Files.readString(validCsvFile.toPath());

        File schemaFile = new File(tempWorkspace, "schemas/csv/transactions-metadata.json");
        assertTrue(schemaFile.exists());
        String schemaContent = Files.readString(schemaFile.toPath());

        List<String> errors = new ArrayList<>();
        ValidatorStudioWindow.validateCsvW(validContent, schemaContent, errors);
        assertTrue(errors.isEmpty(), "Valid CSV should have no errors: " + errors);

        // Invalid CSV
        File invalidCsvFile = new File(tempWorkspace, "messages/csv/transactions-invalid.csv");
        assertTrue(invalidCsvFile.exists());
        String invalidContent = Files.readString(invalidCsvFile.toPath());
        errors.clear();
        ValidatorStudioWindow.validateCsvW(invalidContent, schemaContent, errors);
        assertFalse(errors.isEmpty(), "Invalid CSV should report errors");
    }

    @Test
    public void testFlatFileValidation() throws IOException {
        // Valid Flat File
        File validFlatFile = new File(tempWorkspace, "messages/flatfile/fixedwidth-valid.txt");
        assertTrue(validFlatFile.exists());
        String validContent = Files.readString(validFlatFile.toPath());

        File schemaFile = new File(tempWorkspace, "schemas/flatfile/fixedwidth-schema.json");
        assertTrue(schemaFile.exists());
        String schemaContent = Files.readString(schemaFile.toPath());

        List<String> errors = new ArrayList<>();
        ValidatorStudioWindow.validateFlatFile(validContent, schemaContent, errors);
        assertTrue(errors.isEmpty(), "Valid Flat File should have no errors: " + errors);
    }
}
