package com.routebuilder.test;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import com.routebuilder.ui.TransformationBackend;
import java.io.File;
import java.nio.file.Files;

public class JoorTransformationTest {

    @Test
    public void testJoorSnippetExample1() throws Exception {
        System.out.println("=== RUNNING JOOR SNIPPET EXAMPLE 1 TEST ===");
        File folder = new File("../test-mapping/joor-java-mapper/example1");
        File logicFile = new File(folder, "Transform.java");
        File sourceFile = new File(folder, "source.xml");
        
        assertTrue(logicFile.exists(), "Transform.java should exist");
        assertTrue(sourceFile.exists(), "source.xml should exist");
        
        String input = Files.readString(sourceFile.toPath());
        String code = Files.readString(logicFile.toPath());
        
        String result = TransformationBackend.transform(input, code, "joor", folder);
        
        System.out.println("jOOR Snippet Result:\n" + result);
        assertNotNull(result);
        assertFalse(result.contains("Error"), "Should not contain Error: " + result);
        assertTrue(result.contains("Java Processed:"));
    }

    @Test
    public void testJoorClassExample2() throws Exception {
        System.out.println("=== RUNNING JOOR CLASS EXAMPLE 2 TEST ===");
        File folder = new File("../test-mapping/joor-java-mapper/example2");
        File logicFile = new File(folder, "Transform.java");
        File sourceFile = new File(folder, "source.xml");
        
        assertTrue(logicFile.exists(), "Transform.java should exist");
        assertTrue(sourceFile.exists(), "source.xml should exist");
        
        String input = Files.readString(sourceFile.toPath());
        String code = Files.readString(logicFile.toPath());
        
        String result = TransformationBackend.transform(input, code, "joor", folder);
        
        System.out.println("jOOR Class Result:\n" + result);
        assertNotNull(result);
        assertFalse(result.contains("Error"), "Should not contain Error: " + result);
        assertTrue(result.contains("store"));
        assertTrue(result.contains("catalog"));
    }
}
