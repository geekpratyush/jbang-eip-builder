package com.tessera.faker;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TemplateDiscovery {
    private final Path templatesDir;
    private final Map<String, String> templateCache = new HashMap<>();

    public TemplateDiscovery(Path templatesDir) {
        this.templatesDir = templatesDir;
    }

    public void scanTemplates() {
        templateCache.clear();
        if (!Files.exists(templatesDir)) return;
        
        try {
            Files.walk(templatesDir)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    String fileName = path.getFileName().toString();
                    if (fileName.endsWith(".xml") || fileName.endsWith(".txt") || fileName.endsWith(".template")) {
                        String key = fileName.replace(".xml", "").replace(".txt", "").replace(".template", "");
                        try {
                            templateCache.put(key, Files.readString(path));
                        } catch (Exception ignored) {}
                    }
                });
        } catch (Exception ignored) {}
    }

    public String getTemplate(String messageType) {
        return templateCache.get(messageType);
    }

    public Path getTemplatesDir() {
        return templatesDir;
    }

    public List<String> getTemplateNames() {
        return new ArrayList<>(templateCache.keySet());
    }
}
