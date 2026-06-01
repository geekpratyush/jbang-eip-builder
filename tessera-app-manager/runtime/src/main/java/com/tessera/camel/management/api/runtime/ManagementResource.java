package com.tessera.camel.management.api.runtime;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.inject.Inject;

import org.apache.camel.CamelContext;
import org.apache.camel.spi.Resource;
import org.apache.camel.support.ResourceHelper;
import org.apache.camel.support.PluginHelper;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import org.bson.Document;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Collectors;

@Path("/admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ManagementResource {

    @Inject
    CamelContext camelContext;

    @Inject
    DynamicRouteLoader loader;

    @Inject
    jakarta.enterprise.inject.Instance<DataSource> dataSourceInstance;

    @Inject
    jakarta.enterprise.inject.Instance<MongoClient> mongoClientInstance;

    private static final Queue<String> logQueue = new ConcurrentLinkedQueue<>();
    private static boolean handlerAdded = false;

    private synchronized void registerLogHandler() {
        if (handlerAdded) return;
        java.util.logging.Logger rootLogger = java.util.logging.Logger.getLogger("");
        rootLogger.addHandler(new java.util.logging.Handler() {
            @Override
            public void publish(java.util.logging.LogRecord record) {
                if (record == null) return;
                
                // Exclude management resource requests to prevent logging loop
                if (record.getLoggerName() != null && 
                    (record.getLoggerName().contains("ManagementResource") || 
                     record.getLoggerName().contains("io.quarkus.rest"))) {
                    return;
                }
                
                String message = record.getMessage();
                if (message == null) return;
                
                if (record.getParameters() != null && record.getParameters().length > 0) {
                    try {
                        message = java.text.MessageFormat.format(message, record.getParameters());
                    } catch (Exception ignored) {}
                }
                
                String level = record.getLevel().getName();
                String formatted = String.format("[%s] %s", level, message);
                logQueue.add(formatted);
                while (logQueue.size() > 500) {
                    logQueue.poll();
                }
            }

            @Override
            public void flush() {}

            @Override
            public void close() throws SecurityException {}
        });
        handlerAdded = true;
    }

    @GET
    @Path("/heartbeat")
    public Response heartbeat() {
        registerLogHandler();
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("message", "Camel Quarkus Management API is active");
        
        // Return running route IDs and their source locations
        if (camelContext != null) {
            java.util.List<Map<String, Object>> routeDetails = camelContext.getRoutes().stream()
                .map(r -> {
                    Map<String, Object> details = new HashMap<>();
                    details.put("id", r.getRouteId());
                    details.put("source", r.getSourceLocation());
                    details.put("uptime", r.getUptime());
                    return details;
                })
                .collect(Collectors.toList());
            status.put("routes", routeDetails);
            status.put("components", camelContext.getComponentNames());
        }
        
        return Response.ok(status).build();
    }

    @GET
    @Path("/logs")
    public Response getLogs() {
        registerLogHandler();
        return Response.ok(new java.util.ArrayList<>(logQueue)).build();
    }

    @POST
    @Path("/routes/upload")
    public Response uploadRoute(Map<String, Object> payload) {
        System.out.println("Persistent upload requested: " + payload);
        try {
            String routeId = (String) payload.get("routeId");
            String format = (String) payload.get("format");
            String content = (String) payload.get("content");
            Object versionObj = payload.get("version");
            double version = 1.0;
            if (versionObj instanceof Number) {
                version = ((Number) versionObj).doubleValue();
            } else if (versionObj instanceof String) {
                try {
                    version = Double.parseDouble((String) versionObj);
                } catch (Exception ignored) {}
            }
            Boolean enabledObj = (Boolean) payload.get("enabled");
            boolean enabled = enabledObj == null || enabledObj;

            if (routeId == null || content == null || content.trim().isEmpty()) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("status", "error", "message", "routeId and content are required"))
                    .build();
            }

            if (format == null) {
                format = "yaml";
            }

            // Persist to database
            System.out.println("Loader DB configs: sqlEnabled=" + loader.isSqlEnabled() + ", mongoEnabled=" + loader.isMongoEnabled());
            System.out.println("Bean resolutions: dataSourceResolvable=" + dataSourceInstance.isResolvable() + ", mongoClientResolvable=" + mongoClientInstance.isResolvable());
            if (loader.isSqlEnabled() && dataSourceInstance.isResolvable()) {
                upsertRouteSql(routeId, content, format, version, enabled);
            } else if (loader.isMongoEnabled() && mongoClientInstance.isResolvable()) {
                upsertRouteMongo(routeId, content, format, version, enabled);
            } else {
                System.out.println("No active database storage configured for writes. Loading route in-memory only.");
            }

            // Sync with Camel Engine immediately
            loader.reloadRoutes();

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Route " + routeId + " version " + version + " successfully uploaded and loaded");
            response.put("persistent", true);
            return Response.ok(response).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("status", "error", "message", "Failed to upload persistent route: " + e.getMessage()))
                .build();
        }
    }

    @POST
    @Path("/routes/hotload-temp")
    public Response hotloadRouteTemp(Map<String, Object> payload) {
        System.out.println("Temporary hotload requested: " + payload);
        return loadRoute(payload, false);
    }

    @POST
    @Path("/routes/stop")
    public Response stopRoute(Map<String, Object> payload) {
        Object target = payload.get("routeId");
        System.out.println("[MGMT] Stop requested for: " + target);
        
        try {
            if (target == null) {
                return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("status", "error", "message", "routeId is required")).build();
            }

            final java.util.Set<String> targetIds = new java.util.HashSet<>();
            if (target instanceof java.util.List) {
                for (Object o : (java.util.List<?>) target) {
                    targetIds.add(o.toString());
                }
            } else {
                targetIds.add(target.toString());
            }

            // Find all routes that match
            java.util.List<org.apache.camel.Route> routesToStop = camelContext.getRoutes().stream()
                .filter(r -> {
                    String rid = r.getRouteId();
                    String src = r.getSourceLocation();
                    
                    // 1. Direct ID match (check if route ID is in our target list)
                    if (targetIds.contains(rid)) {
                        System.out.println("[MGMT] Match found by explicit ID: " + rid);
                        return true;
                    }
                    
                    // 2. Resource/Source match (fallback for logical IDs)
                    for (String targetId : targetIds) {
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

                            if (normalizedSrc.equals(targetId) || 
                                normalizedSrc.equals(targetId + ".yaml") || 
                                normalizedSrc.equals(targetId + ".yml") ||
                                normalizedSrc.endsWith("/" + targetId + ".yaml") ||
                                normalizedSrc.endsWith("/" + targetId + ".yml") ||
                                normalizedSrc.endsWith("\\" + targetId + ".yaml") ||
                                normalizedSrc.endsWith("\\" + targetId + ".yml") ||
                                normalizedSrc.startsWith(targetId + ".")) {
                                System.out.println("[MGMT] Match found by source location (" + normalizedSrc + ") for target: " + targetId);
                                return true;
                            }
                        }
                    }
                    
                    return false;
                })
                .collect(Collectors.toList());

            if (routesToStop.isEmpty()) {
                System.out.println("[MGMT] No matching routes found for: " + target);
                return Response.status(Response.Status.NOT_FOUND).entity(Map.of("status", "error", "message", "Route not found")).build();
            }

            System.out.println("[MGMT] Found " + routesToStop.size() + " routes to stop.");
            
            // First stop all
            for (org.apache.camel.Route route : routesToStop) {
                String id = route.getRouteId();
                System.out.println("[MGMT] Stopping route: " + id);
                camelContext.getRouteController().stopRoute(id);
            }
            
            // Then remove all
            for (org.apache.camel.Route route : routesToStop) {
                String id = route.getRouteId();
                System.out.println("[MGMT] Removing route: " + id);
                camelContext.removeRoute(id);
            }

            return Response.ok(Map.of("status", "success", "message", "Stopped and removed " + routesToStop.size() + " routes")).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(Map.of("status", "error", "message", e.getMessage())).build();
        }
    }

    @POST
    @Path("/files/upload")
    public Response uploadFile(Map<String, Object> payload) {
        String fileName = (String) payload.get("fileName");
        String content = (String) payload.get("content");
        String targetDir = (String) payload.get("targetDir");

        if (fileName == null || content == null || targetDir == null) {
            Map<String, Object> err = new HashMap<>();
            err.put("status", "error");
            err.put("message", "fileName, content, and targetDir are required");
            return Response.status(Response.Status.BAD_REQUEST).entity(err).build();
        }

        try {
            java.io.File dir = new java.io.File(targetDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created && !dir.exists()) {
                    throw new java.io.IOException("Permission denied or invalid path: failed to create target directory: " + targetDir);
                }
            }
            java.io.File targetFile = new java.io.File(dir, fileName);
            java.nio.file.Files.writeString(targetFile.toPath(), content);

            System.out.println("File successfully written to server path: " + targetFile.getAbsolutePath());

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "File written successfully");
            response.put("copiedPath", targetFile.getAbsolutePath());
            return Response.ok(response).build();
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> err = new HashMap<>();
            err.put("status", "error");
            err.put("message", "Failed to write file: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(err).build();
        }
    }

    @POST
    @Path("/transformers/upload")
    public Response uploadTransformer(Map<String, Object> payload) {
        System.out.println("Persistent transformer upload requested: " + payload);
        try {
            String id = (String) payload.get("id");
            String type = (String) payload.get("type");
            String fileName = (String) payload.get("fileName");
            String content = (String) payload.get("content");
            Object versionObj = payload.get("version");
            double version = 1.0;
            if (versionObj instanceof Number) {
                version = ((Number) versionObj).doubleValue();
            } else if (versionObj instanceof String) {
                try {
                    version = Double.parseDouble((String) versionObj);
                } catch (Exception ignored) {}
            }
            Boolean enabledObj = (Boolean) payload.get("enabled");
            boolean enabled = enabledObj == null || enabledObj;

            if (id == null || fileName == null || content == null) {
                return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("status", "error", "message", "id, fileName, and content are required"))
                    .build();
            }

            if (loader.isSqlEnabled() && dataSourceInstance.isResolvable()) {
                upsertTransformerSql(id, type, fileName, content, version, enabled);
            } else if (loader.isMongoEnabled() && mongoClientInstance.isResolvable()) {
                upsertTransformerMongo(id, type, fileName, content, version, enabled);
            } else {
                System.out.println("No active database storage configured for writes.");
            }

            if (loader.isTransformerEnabled()) {
                loader.extractTransformers();
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Transformer file " + fileName + " version " + version + " successfully uploaded");
            response.put("persistent", true);
            return Response.ok(response).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                .entity(Map.of("status", "error", "message", "Failed to upload transformer: " + e.getMessage()))
                .build();
        }
    }

    @POST
    @Path("/transformers/hotload-temp")
    public Response hotloadTransformerTemp(Map<String, Object> payload) {
        System.out.println("Temporary transformer hotload requested: " + payload);
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Transformer hot-loaded temporarily");
        response.put("persistent", false);
        return Response.ok(response).build();
    }

    private Response loadRoute(Map<String, Object> payload, boolean persistent) {
        try {
            String routeId = (String) payload.get("routeId");
            String format = (String) payload.get("format");
            String content = (String) payload.get("content");

            if (format == null) {
                format = "yaml";
            }
            if (routeId == null) {
                routeId = "temp-route-" + System.currentTimeMillis();
            }

            if (content == null || content.trim().isEmpty()) {
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Route content cannot be empty");
                return Response.status(Response.Status.BAD_REQUEST).entity(errorResponse).build();
            }

            // Unload existing route version if it exists
            try {
                if (camelContext.getRoute(routeId) != null) {
                    System.out.println("Stopping and removing existing route: " + routeId);
                    camelContext.getRouteController().stopRoute(routeId);
                    camelContext.removeRoute(routeId);
                }
            } catch (Exception e) {
                System.out.println("Ignoring exception while stopping existing route " + routeId + ": " + e.getMessage());
            }

            // Load and mount the route resource using PluginHelper
            Resource resource = ResourceHelper.fromString(routeId + "." + format, content);
            PluginHelper.getRoutesLoader(camelContext).loadRoutes(resource);

            System.out.println("Successfully loaded route: " + routeId);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Route " + routeId + " loaded successfully");
            response.put("persistent", persistent);
            response.put("status", "success");
            return Response.ok(response).build();
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to load route: " + e.getMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorResponse).build();
        }
    }

    private void upsertRouteSql(String routeId, String content, String format, double version, boolean enabled) throws Exception {
        try (Connection conn = dataSourceInstance.get().getConnection()) {
            String checkQuery = "SELECT count(*) FROM " + loader.getSqlTable() + " WHERE route_id = ? AND version = ?";
            boolean exists = false;
            try (PreparedStatement checkPs = conn.prepareStatement(checkQuery)) {
                checkPs.setString(1, routeId);
                checkPs.setDouble(2, version);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        exists = true;
                    }
                }
            }

            if (exists) {
                String update = "UPDATE " + loader.getSqlTable() + 
                                " SET route_content = ?, route_format = ?, enabled = ? WHERE route_id = ? AND version = ?";
                try (PreparedStatement ps = conn.prepareStatement(update)) {
                    ps.setString(1, content);
                    ps.setString(2, format);
                    ps.setBoolean(3, enabled);
                    ps.setString(4, routeId);
                    ps.setDouble(5, version);
                    ps.executeUpdate();
                }
            } else {
                String insert = "INSERT INTO " + loader.getSqlTable() + 
                                " (route_id, version, route_content, route_format, enabled, created_by) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insert)) {
                    ps.setString(1, routeId);
                    ps.setDouble(2, version);
                    ps.setString(3, content);
                    ps.setString(4, format);
                    ps.setBoolean(5, enabled);
                    ps.setString(6, "rest-api");
                    ps.executeUpdate();
                }
            }
        }
    }

    private void upsertRouteMongo(String routeId, String content, String format, double version, boolean enabled) {
        MongoClient mongoClient = mongoClientInstance.get();
        String dbName = loader.getMongoDatabaseName();
        MongoCollection<Document> collection = mongoClient.getDatabase(dbName).getCollection(loader.getMongoCollectionName());

        Document docId = new Document("route_id", routeId).append("version", version);
        Document doc = new Document("_id", docId)
            .append("route_id", routeId)
            .append("version", version)
            .append("content", content)
            .append("route_format", format)
            .append("enabled", enabled)
            .append("updated_at", new java.util.Date().toString());

        collection.replaceOne(new Document("_id", docId), doc, new ReplaceOptions().upsert(true));
    }

    private void upsertTransformerSql(String id, String type, String fileName, String content, double version, boolean enabled) throws Exception {
        try (Connection conn = dataSourceInstance.get().getConnection()) {
            String checkQuery = "SELECT count(*) FROM " + loader.getTransformerSqlTable() + " WHERE id = ? AND version = ?";
            boolean exists = false;
            try (PreparedStatement checkPs = conn.prepareStatement(checkQuery)) {
                checkPs.setString(1, id);
                checkPs.setDouble(2, version);
                try (ResultSet rs = checkPs.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) {
                        exists = true;
                    }
                }
            }

            if (exists) {
                String update = "UPDATE " + loader.getTransformerSqlTable() + 
                                " SET type = ?, file_name = ?, file_content = ?, enabled = ? WHERE id = ? AND version = ?";
                try (PreparedStatement ps = conn.prepareStatement(update)) {
                    ps.setString(1, type);
                    ps.setString(2, fileName);
                    ps.setString(3, content);
                    ps.setBoolean(4, enabled);
                    ps.setString(5, id);
                    ps.setDouble(6, version);
                    ps.executeUpdate();
                }
            } else {
                String insert = "INSERT INTO " + loader.getTransformerSqlTable() + 
                                " (id, type, version, file_name, file_content, enabled) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(insert)) {
                    ps.setString(1, id);
                    ps.setString(2, type);
                    ps.setDouble(3, version);
                    ps.setString(4, fileName);
                    ps.setString(5, content);
                    ps.setBoolean(6, enabled);
                    ps.executeUpdate();
                }
            }
        }
    }

    private void upsertTransformerMongo(String id, String type, String fileName, String content, double version, boolean enabled) {
        MongoClient mongoClient = mongoClientInstance.get();
        String dbName = loader.getMongoDatabaseName();
        MongoCollection<Document> collection = mongoClient.getDatabase(dbName).getCollection(loader.getTransformerMongoCollection());

        Document docId = new Document("id", id).append("version", version);
        Document doc = new Document("_id", docId)
            .append("id", id)
            .append("type", type)
            .append("version", version)
            .append("file_name", fileName)
            .append("content", content)
            .append("enabled", enabled)
            .append("updated_at", new java.util.Date().toString());

        collection.replaceOne(new Document("_id", docId), doc, new ReplaceOptions().upsert(true));
    }
}
