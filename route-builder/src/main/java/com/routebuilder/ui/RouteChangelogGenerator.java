package com.routebuilder.ui;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RouteChangelogGenerator {

    public static class RouteInfo {
        public String routeId;
        public String status = "ENABLED";
        public String format = "yaml";
        public String definition;
    }

    public static void generate(File baseDir, File outputDir) {
        if (baseDir == null || !baseDir.exists()) {
            return;
        }
        if (outputDir == null) {
            outputDir = baseDir;
        }
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        List<RouteInfo> routes = new ArrayList<>();
        collectRoutes(baseDir, routes, baseDir);

        if (routes.isEmpty()) {
            System.out.println("No routes found to generate changelogs.");
            return;
        }

        // 1. Generate MongoDB JSON Changelog
        generateMongoChangelog(routes, new File(outputDir, "mongodb-routes-changelog.json"));

        // 2. Generate Liquibase YAML Changelog
        generateLiquibaseChangelog(routes, new File(outputDir, "liquibase-routes-changelog.yaml"));
    }

    private static void collectRoutes(File current, List<RouteInfo> routes, File baseDir) {
        File[] files = current.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (f.isDirectory()) {
                // Skip output directories or export directories to prevent loops
                if (!f.getName().equals("export-dir") && !f.getName().equals("build") && !f.getName().equals(".git")) {
                    collectRoutes(f, routes, baseDir);
                }
            } else {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".java") || name.endsWith(".xml") || name.endsWith(".groovy")) {
                    try {
                        String content = Files.readString(f.toPath());
                        RouteInfo info = parseRouteInfo(f, content);
                        routes.add(info);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static RouteInfo parseRouteInfo(File file, String content) {
        RouteInfo info = new RouteInfo();
        info.definition = content;
        
        String filename = file.getName();
        int dot = filename.lastIndexOf('.');
        String baseName = dot > 0 ? filename.substring(0, dot) : filename;
        
        String ext = dot > 0 ? filename.substring(dot + 1).toLowerCase() : "yaml";
        if (ext.equals("yml")) ext = "yaml";
        info.format = ext;

        // 1. Look for # ID: <id>
        Pattern idCommentPattern = Pattern.compile("(?i)#\\s*ID:\\s*(\\S+)");
        Matcher m = idCommentPattern.matcher(content);
        if (m.find()) {
            info.routeId = m.group(1).trim();
        } else {
            // 2. Look for id: "something" in YAML
            Pattern idYamlPattern = Pattern.compile("(?m)^\\s*id:\\s*[\"']?([a-zA-Z0-9_-]+)[\"']?");
            m = idYamlPattern.matcher(content);
            if (m.find()) {
                info.routeId = m.group(1).trim();
            } else {
                // 3. Fallback to filename
                info.routeId = baseName;
            }
        }

        // Look for # ENABLED: <true|false>
        Pattern enabledPattern = Pattern.compile("(?i)#\\s*ENABLED:\\s*(\\S+)");
        m = enabledPattern.matcher(content);
        if (m.find()) {
            boolean enabled = Boolean.parseBoolean(m.group(1).trim());
            info.status = enabled ? "ENABLED" : "DISABLED";
        }

        return info;
    }

    private static void generateMongoChangelog(List<RouteInfo> routes, File outputFile) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Map<String, Object>> mongoList = new ArrayList<>();
            for (RouteInfo r : routes) {
                Map<String, Object> doc = new LinkedHashMap<>();
                doc.put("routeId", r.routeId);
                doc.put("status", r.status);
                doc.put("format", r.format);
                doc.put("definition", r.definition);
                mongoList.add(doc);
            }
            mapper.writerWithDefaultPrettyPrinter().writeValue(outputFile, mongoList);
            System.out.println("MongoDB routes changelog generated: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void generateLiquibaseChangelog(List<RouteInfo> routes, File outputFile) {
        StringBuilder sb = new StringBuilder();
        sb.append("databaseChangeLog:\n");
        sb.append("  - changeSet:\n");
        sb.append("      id: create-kamelet-studio-routes-table\n");
        sb.append("      author: kamelet-studio\n");
        sb.append("      preConditions:\n");
        sb.append("        - onFail: MARK_RAN\n");
        sb.append("        - not:\n");
        sb.append("            - tableExists:\n");
        sb.append("                tableName: KAMELET_STUDIO_ROUTES\n");
        sb.append("      changes:\n");
        sb.append("        - createTable:\n");
        sb.append("            tableName: KAMELET_STUDIO_ROUTES\n");
        sb.append("            columns:\n");
        sb.append("              - column:\n");
        sb.append("                  name: ROUTE_ID\n");
        sb.append("                  type: VARCHAR(255)\n");
        sb.append("                  constraints:\n");
        sb.append("                    primaryKey: true\n");
        sb.append("                    nullable: false\n");
        sb.append("              - column:\n");
        sb.append("                  name: STATUS\n");
        sb.append("                  type: VARCHAR(50)\n");
        sb.append("                  defaultValue: ENABLED\n");
        sb.append("                  constraints:\n");
        sb.append("                    nullable: false\n");
        sb.append("              - column:\n");
        sb.append("                  name: FORMAT\n");
        sb.append("                  type: VARCHAR(10)\n");
        sb.append("                  defaultValue: yaml\n");
        sb.append("                  constraints:\n");
        sb.append("                    nullable: false\n");
        sb.append("              - column:\n");
        sb.append("                  name: DEFINITION\n");
        sb.append("                  type: CLOB\n");
        sb.append("                  constraints:\n");
        sb.append("                    nullable: false\n");
        sb.append("              - column:\n");
        sb.append("                  name: UPDATED_AT\n");
        sb.append("                  type: TIMESTAMP\n");
        sb.append("                  defaultValueComputed: CURRENT_TIMESTAMP\n\n");

        for (RouteInfo r : routes) {
            sb.append("  - changeSet:\n");
            sb.append("      id: sync-route-").append(r.routeId).append("\n");
            sb.append("      author: kamelet-studio\n");
            sb.append("      runOnChange: true\n");
            sb.append("      changes:\n");
            sb.append("        - delete:\n");
            sb.append("            tableName: KAMELET_STUDIO_ROUTES\n");
            sb.append("            where: ROUTE_ID = '").append(r.routeId.replace("'", "''")).append("'\n");
            sb.append("        - insert:\n");
            sb.append("            tableName: KAMELET_STUDIO_ROUTES\n");
            sb.append("            columns:\n");
            sb.append("              - column:\n");
            sb.append("                  name: ROUTE_ID\n");
            sb.append("                  value: '").append(r.routeId.replace("'", "''")).append("'\n");
            sb.append("              - column:\n");
            sb.append("                  name: STATUS\n");
            sb.append("                  value: '").append(r.status).append("'\n");
            sb.append("              - column:\n");
            sb.append("                  name: FORMAT\n");
            sb.append("                  value: '").append(r.format).append("'\n");
            sb.append("              - column:\n");
            sb.append("                  name: DEFINITION\n");
            sb.append("                  value: ").append(formatYamlLiteral(r.definition, 20));
        }

        try {
            Files.writeString(outputFile.toPath(), sb.toString());
            System.out.println("Liquibase routes changelog generated: " + outputFile.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static String formatYamlLiteral(String text, int indentSpaces) {
        StringBuilder sb = new StringBuilder();
        String indent = " ".repeat(indentSpaces);
        sb.append("|-\n");
        String[] lines = text.split("\\r?\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            sb.append(indent).append(lines[i]);
            if (i < lines.length - 1) {
                sb.append("\n");
            }
        }
        sb.append("\n");
        return sb.toString();
    }
}
