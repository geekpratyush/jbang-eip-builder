package com.tessera.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class RouteTreeTestFXAutomatorTest {

    @Test
    public void testFindChapter1Directory(@TempDir Path tempDir) throws Exception {
        RouteBuilderApp app = new RouteBuilderApp();
        File baseDir = tempDir.toFile();

        // Should return null if no directories exist
        assertNull(app.findChapter1Directory(baseDir));

        // Create some directories
        File ch2 = new File(baseDir, "chapter-02-routing");
        assertTrue(ch2.mkdirs());
        
        File ch1 = new File(baseDir, "chapter-01-basics");
        assertTrue(ch1.mkdirs());

        // Should find chapter-01-basics
        File found = app.findChapter1Directory(baseDir);
        assertNotNull(found);
        assertEquals("chapter-01-basics", found.getName());

        // Test nested search
        File nestedBase = new File(baseDir, "nested-base");
        File camelDir = new File(nestedBase, "camel");
        File nestedCh1 = new File(camelDir, "chapter-01-basics");
        assertTrue(nestedCh1.mkdirs());
        
        File foundNested = app.findChapter1Directory(nestedBase);
        assertNotNull(foundNested);
        assertEquals(nestedCh1.getAbsolutePath(), foundNested.getAbsolutePath());
    }

    @Test
    public void testGetChapter1Routes(@TempDir Path tempDir) throws Exception {
        RouteBuilderApp app = new RouteBuilderApp();
        File ch1Dir = tempDir.toFile();

        // Create some files
        File f1 = new File(ch1Dir, "01-hello-timer.camel.yaml");
        File f2 = new File(ch1Dir, "02-set-body-header.camel.yaml");
        File f3 = new File(ch1Dir, "03-simple-expression.camel.yaml");
        File txt = new File(ch1Dir, "notes.txt"); // should be ignored

        assertTrue(f1.createNewFile());
        assertTrue(f2.createNewFile());
        assertTrue(f3.createNewFile());
        assertTrue(txt.createNewFile());

        List<File> routes = app.getChapter1Routes(ch1Dir);
        assertEquals(3, routes.size());
        
        // Assert alphabetical sorting
        assertEquals("01-hello-timer.camel.yaml", routes.get(0).getName());
        assertEquals("02-set-body-header.camel.yaml", routes.get(1).getName());
        assertEquals("03-simple-expression.camel.yaml", routes.get(2).getName());
    }
}
