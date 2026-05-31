package com.routebuilder.camel.management.api.runtime;

import io.quarkus.runtime.StartupEvent;
import com.mongodb.client.MongoClient;
import org.bson.Document;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import javax.sql.DataSource;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.Resource;
import org.apache.camel.support.ResourceHelper;
import org.apache.camel.support.PluginHelper;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.File;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

@ApplicationScoped
public class DynamicRouteLoader {

    @Inject
    CamelContext camelContext;

    @Inject
    jakarta.enterprise.inject.Instance<DataSource> dataSourceInstance;

    @Inject
    jakarta.enterprise.inject.Instance<MongoClient> mongoClientInstance;

    @ConfigProperty(name = "loader.file.enabled", defaultValue = "false")
    boolean fileEnabled;

    @ConfigProperty(name = "loader.file.directory", defaultValue = "src/main/resources/routes/")
    String fileDir;

    @ConfigProperty(name = "loader.sql.enabled", defaultValue = "false")
    boolean sqlEnabled;

    @ConfigProperty(name = "loader.sql.table", defaultValue = "camel_routes")
    String sqlTable;

    @ConfigProperty(name = "loader.mongodb.enabled", defaultValue = "false")
    boolean mongoEnabled;

    @ConfigProperty(name = "loader.mongodb.collection", defaultValue = "camel_routes_collection")
    String mongoCollectionName;

    @ConfigProperty(name = "loader.mongodb.database", defaultValue = "testdb")
    String mongoDatabaseName;

    // Transformer Properties
    @ConfigProperty(name = "loader.transformer.enabled", defaultValue = "false")
    boolean transformerEnabled;

    @ConfigProperty(name = "loader.transformer.directory", defaultValue = "src/main/resources/transformers/")
    String transformerDir;

    @ConfigProperty(name = "loader.transformer.categorize-by-type", defaultValue = "true")
    boolean transformerCategorize;

    @ConfigProperty(name = "loader.transformer.sql.table", defaultValue = "camel_transformers")
    String transformerSqlTable;

    @ConfigProperty(name = "loader.transformer.mongodb.collection", defaultValue = "camel_transformers_collection")
    String transformerMongoCollection;

    public boolean isFileEnabled() {
        return fileEnabled;
    }

    public String getFileDir() {
        return fileDir;
    }

    public boolean isSqlEnabled() {
        return sqlEnabled;
    }

    public String getSqlTable() {
        return sqlTable;
    }

    public boolean isMongoEnabled() {
        return mongoEnabled;
    }

    public String getMongoCollectionName() {
        return mongoCollectionName;
    }

    public String getMongoDatabaseName() {
        return mongoDatabaseName;
    }

    public boolean isTransformerEnabled() {
        return transformerEnabled;
    }

    public String getTransformerDir() {
        return transformerDir;
    }

    public boolean isTransformerCategorize() {
        return transformerCategorize;
    }

    public String getTransformerSqlTable() {
        return transformerSqlTable;
    }

    public String getTransformerMongoCollection() {
        return transformerMongoCollection;
    }

    private final Set<String> registeredRouteResources = new HashSet<>();

    public void onStart(@Observes StartupEvent ev) {
        reloadRoutes();
    }

    /**
     * Clears previously loaded dynamic routes and scans databases/filesystem
     * to hot-reload the latest active versions.
     */
    public synchronized void reloadRoutes() {
        System.out.println("Starting dynamic route loading sequence...");
        
        // 0. Extract transformer files to disk first so they are present when routes are loaded
        if (transformerEnabled) {
            extractTransformers();
        }
        
        // 1. Unload old routes to prevent duplicate route execution
        unloadPreviousRoutes();

        Map<String, RoutePayload> routeMap = new HashMap<>();

        // 2. Fetch from filesystem
        if (fileEnabled && !fileDir.isEmpty()) {
            loadFromFilesystem(routeMap);
        }

        // 3. Fetch from SQL Database
        if (sqlEnabled && !sqlTable.isEmpty()) {
            loadFromSql(routeMap);
        }

        // 4. Fetch from MongoDB
        if (mongoEnabled) {
            loadFromMongo(routeMap);
        }

        // 5. Build and Load Resources into Camel
        if (routeMap.isEmpty()) {
            System.out.println("No dynamic routes found to load.");
            return;
        }

        for (Map.Entry<String, RoutePayload> entry : routeMap.entrySet()) {
            String routeId = entry.getKey();
            RoutePayload payload = entry.getValue();
            
            System.out.println("Selecting latest route: " + routeId + " (Version: " + payload.version + ", Format: " + payload.format + ")");
            try {
                Resource resource = ResourceHelper.fromString(routeId + "." + payload.format, payload.content);
                PluginHelper.getRoutesLoader(camelContext).loadRoutes(resource);
                registeredRouteResources.add(routeId);
            } catch (Exception e) {
                System.err.println("Error mounting dynamic route " + routeId + " into Camel: " + e.getMessage());
            }
        }
        System.out.println("Successfully registered " + registeredRouteResources.size() + " latest dynamic routes.");
    }

    private void unloadPreviousRoutes() {
        for (String resourceId : registeredRouteResources) {
            try {
                System.out.println("[LOADER] Unloading routes for resource: " + resourceId);
                // Find all active routes in the context that match this resource ID
                java.util.List<org.apache.camel.Route> routes = camelContext.getRoutes().stream()
                    .filter(r -> {
                        String rid = r.getRouteId();
                        String src = r.getSourceLocation();
                        
                        if (rid.equals(resourceId)) return true;
                        if (src != null) {
                            String normalizedSrc = src;
                            if (src.contains(":")) {
                                String[] parts = src.split(":");
                                int index = (parts[0].equals("file") || parts[0].equals("classpath")) ? 1 : 0;
                                normalizedSrc = parts[index];
                                if (normalizedSrc.contains(".")) {
                                    int lastColon = src.lastIndexOf(':');
                                    if (lastColon > src.indexOf('.')) {
                                        normalizedSrc = src.substring(src.contains(":") && src.indexOf(":") < src.indexOf(".") ? src.indexOf(":") + 1 : 0, lastColon);
                                    }
                                }
                            }
                            if (normalizedSrc.startsWith("/") || normalizedSrc.startsWith("\\")) normalizedSrc = normalizedSrc.substring(1);
                            
                            return normalizedSrc.equals(resourceId) || 
                                   normalizedSrc.equals(resourceId + ".yaml") || 
                                   normalizedSrc.equals(resourceId + ".yml") ||
                                   normalizedSrc.startsWith(resourceId + ".");
                        }
                        return false;
                    })
                    .collect(java.util.stream.Collectors.toList());

                System.out.println("[LOADER] Found " + routes.size() + " routes to stop for resource " + resourceId);

                // First stop all
                for (org.apache.camel.Route route : routes) {
                    String id = route.getRouteId();
                    System.out.println("[LOADER] Stopping route: " + id);
                    camelContext.getRouteController().stopRoute(id);
                }
                
                // Then remove all
                for (org.apache.camel.Route route : routes) {
                    String id = route.getRouteId();
                    System.out.println("[LOADER] Removing route: " + id);
                    camelContext.removeRoute(id);
                }
            } catch (Exception e) {
                System.err.println("[LOADER] Error unloading routes for resource " + resourceId + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
        registeredRouteResources.clear();
    }

    private void loadFromFilesystem(Map<String, RoutePayload> routeMap) {
        File folder = new File(fileDir);
        if (!folder.exists() || !folder.isDirectory()) return;

        File[] files = folder.listFiles((dir, name) -> name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".xml"));
        if (files == null) return;

        for (File file : files) {
            try {
                String content = Files.readString(file.toPath());
                String baseName = file.getName().substring(0, file.getName().lastIndexOf('.'));
                String ext = file.getName().substring(file.getName().lastIndexOf('.') + 1);
                
                // Filesystem routes defaults to version 1.00 unless custom parsed
                double version = 1.00;
                resolveAndMerge(routeMap, baseName, content, ext, version);
            } catch (Exception e) {
                System.err.println("Error reading local route " + file.getName() + ": " + e.getMessage());
            }
        }
    }

    private void loadFromSql(Map<String, RoutePayload> routeMap) {
        if (!dataSourceInstance.isResolvable()) {
            System.err.println("SQL Loader enabled but DataSource bean is not active.");
            return;
        }

        String query = "SELECT route_id, route_content, route_format, version FROM " + sqlTable + 
                       " WHERE enabled = true ORDER BY route_id ASC, version DESC";
        try (Connection conn = dataSourceInstance.get().getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String routeId = rs.getString("route_id");
                String content = rs.getString("route_content");
                String format = rs.getString("route_format");
                double version = rs.getDouble("version");

                resolveAndMerge(routeMap, routeId, content, format, version);
            }
        } catch (Exception e) {
            System.err.println("Error querying SQL Route Loader table " + sqlTable + ": " + e.getMessage());
        }
    }

    private void loadFromMongo(Map<String, RoutePayload> routeMap) {
        if (!mongoClientInstance.isResolvable()) {
            System.err.println("MongoDB Loader enabled but MongoClient bean is not active.");
            return;
        }

        try {
            MongoClient mongoClient = mongoClientInstance.get();
            String dbName = mongoDatabaseName;
            System.out.println("Connecting to MongoDB database: '" + dbName + "' to load routes from collection: '" + mongoCollectionName + "'");
            MongoCollection<Document> collection = mongoClient.getDatabase(dbName).getCollection(mongoCollectionName);

            // Filter for enabled routes, sort by route_id ascending and version descending
            Document query = new Document("enabled", true);
            Document sort = new Document("route_id", 1).append("version", -1);

            try (MongoCursor<Document> cursor = collection.find(query).sort(sort).iterator()) {
                while (cursor.hasNext()) {
                    Document doc = cursor.next();
                    String routeId = doc.getString("route_id");
                    String content = doc.getString("content");
                    String format = doc.getString("route_format");
                    if (format == null) {
                        format = "yaml"; // fallback default
                    }
                    
                    Object verObj = doc.get("version");
                    double version = 1.0;
                    if (verObj instanceof Number) {
                        version = ((Number) verObj).doubleValue();
                    }

                    resolveAndMerge(routeMap, routeId, content, format, version);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading routes from MongoDB client: " + e.getMessage());
        }
    }

    private void resolveAndMerge(Map<String, RoutePayload> routeMap, String routeId, String content, String format, double version) {
        if (routeMap.containsKey(routeId)) {
            // Pick the latest version
            if (version > routeMap.get(routeId).version) {
                routeMap.put(routeId, new RoutePayload(content, format, version));
            }
        } else {
            routeMap.put(routeId, new RoutePayload(content, format, version));
        }
    }

    public void extractTransformers() {
        System.out.println("Initiating transformer file extraction...");
        Map<String, TransformerPayload> transMap = new HashMap<>();

        // 1. Fetch from SQL Table
        if (sqlEnabled && dataSourceInstance.isResolvable()) {
            loadTransformersFromSql(transMap);
        }

        // 2. Fetch from MongoDB Collection
        if (mongoEnabled && mongoClientInstance.isResolvable()) {
            loadTransformersFromMongo(transMap);
        }

        if (transMap.isEmpty()) {
            return;
        }

        // 3. Write deduplicated files to the filesystem
        File baseDir = new File(transformerDir);
        if (!baseDir.exists()) {
            baseDir.mkdirs();
        }

        for (TransformerPayload payload : transMap.values()) {
            try {
                File targetFile;
                if (transformerCategorize && payload.type != null && !payload.type.trim().isEmpty()) {
                    File typeDir = new File(baseDir, payload.type.trim().toLowerCase());
                    if (!typeDir.exists()) {
                        typeDir.mkdirs();
                    }
                    targetFile = new File(typeDir, payload.fileName);
                } else {
                    targetFile = new File(baseDir, payload.fileName);
                }

                Files.writeString(targetFile.toPath(), payload.content);
                System.out.println("Written file: " + targetFile.getAbsolutePath() + " (Version: " + payload.version + ")");
            } catch (Exception e) {
                System.err.println("Error writing file " + payload.fileName + " to filesystem: " + e.getMessage());
            }
        }
    }

    private void loadTransformersFromSql(Map<String, TransformerPayload> transMap) {
        String query = "SELECT type, version, file_name, file_content FROM " + transformerSqlTable +
                       " WHERE enabled = true ORDER BY file_name ASC, version DESC";
        try (Connection conn = dataSourceInstance.get().getConnection();
             PreparedStatement ps = conn.prepareStatement(query);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String type = rs.getString("type");
                double version = rs.getDouble("version");
                String fileName = rs.getString("file_name");
                String content = rs.getString("file_content");

                resolveAndMergeTransformer(transMap, fileName, content, type, version);
            }
        } catch (Exception e) {
            System.err.println("SQL Transformer loading failed: " + e.getMessage());
        }
    }

    private void loadTransformersFromMongo(Map<String, TransformerPayload> transMap) {
        try {
            MongoClient mongoClient = mongoClientInstance.get();
            String dbName = mongoDatabaseName;
            System.out.println("Connecting to MongoDB database: '" + dbName + "' to load transformers from collection: '" + transformerMongoCollection + "'");
            MongoCollection<Document> collection = mongoClient.getDatabase(dbName).getCollection(transformerMongoCollection);

            Document query = new Document("enabled", true);
            Document sort = new Document("file_name", 1).append("version", -1);

            try (MongoCursor<Document> cursor = collection.find(query).sort(sort).iterator()) {
                while (cursor.hasNext()) {
                    Document doc = cursor.next();
                    String type = doc.getString("type");
                    String fileName = doc.getString("file_name");
                    String content = doc.getString("content");

                    Object verObj = doc.get("version");
                    double version = 1.0;
                    if (verObj instanceof Number) {
                        version = ((Number) verObj).doubleValue();
                    }

                    resolveAndMergeTransformer(transMap, fileName, content, type, version);
                }
            }
        } catch (Exception e) {
            System.err.println("MongoDB Transformer loading failed: " + e.getMessage());
        }
    }

    private void resolveAndMergeTransformer(Map<String, TransformerPayload> transMap, 
                                            String fileName, String content, String type, double version) {
        if (transMap.containsKey(fileName)) {
            // Pick the latest version
            if (version > transMap.get(fileName).version) {
                transMap.put(fileName, new TransformerPayload(content, type, version, fileName));
            }
        } else {
            transMap.put(fileName, new TransformerPayload(content, type, version, fileName));
        }
    }

    private static class RoutePayload {
        final String content;
        final String format;
        final double version;

        RoutePayload(String content, String format, double version) {
            this.content = content;
            this.format = format;
            this.version = version;
        }
    }

    private static class TransformerPayload {
        final String content;
        final String type;
        final double version;
        final String fileName;

        TransformerPayload(String content, String type, double version, String fileName) {
            this.content = content;
            this.type = type;
            this.version = version;
            this.fileName = fileName;
        }
    }
}
