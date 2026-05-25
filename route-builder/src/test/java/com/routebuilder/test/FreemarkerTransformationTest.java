package com.routebuilder.test;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.routebuilder.ui.TransformationBackend;
import java.io.File;
import java.nio.file.Files;

public class FreemarkerTransformationTest {

    @Test
    public void testFreemarkerJsonExample1() throws Exception {
        System.out.println("=== RUNNING FREEMARKER JSON EXAMPLE 1 TEST ===");
        File folder = new File("../test-mapping/freemarker-ftl/example1");
        File logicFile = new File(folder, "template.ftl");
        File sourceFile = new File(folder, "source.json");
        
        assertTrue(logicFile.exists(), "template.ftl should exist");
        assertTrue(sourceFile.exists(), "source.json should exist");
        
        String input = Files.readString(sourceFile.toPath());
        String code = Files.readString(logicFile.toPath());
        
        String result = TransformationBackend.transform(input, code, "freemarker", folder);
        
        System.out.println("FreeMarker Example 1 Result:\n" + result);
        assertNotNull(result);
        assertFalse(result.contains("Error"), "Should not contain Error: " + result);
        assertTrue(result.contains("Order Confirmation: ORD-12345"));
        assertTrue(result.contains("Laptop: $999.99"));
        assertTrue(result.contains("Mouse: $49.99"));
    }

    @Test
    public void testFreemarkerXmlExample2() throws Exception {
        System.out.println("=== RUNNING FREEMARKER XML EXAMPLE 2 TEST ===");
        File folder = new File("../test-mapping/freemarker-ftl/example2");
        File logicFile = new File(folder, "template.ftl");
        File sourceFile = new File(folder, "source.xml");
        
        assertTrue(logicFile.exists(), "template.ftl should exist");
        assertTrue(sourceFile.exists(), "source.xml should exist");
        
        String input = Files.readString(sourceFile.toPath());
        String code = Files.readString(logicFile.toPath());
        
        String result = TransformationBackend.transform(input, code, "freemarker", folder);
        
        System.out.println("FreeMarker Example 2 Result:\n" + result);
        assertNotNull(result);
        assertFalse(result.contains("Error"), "Should not contain Error: " + result);
        assertTrue(result.contains("User Status: <user>"));
    }
}
