package com.routebuilder.faker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class DatabaseService {
    private final Path dbDir;
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, JsonNode> loadedFiles = new HashMap<>();
    private final Random random = new Random();
    private final Map<String, String> aliases = new HashMap<>();

    public DatabaseService(Path dbDir) {
        this.dbDir = dbDir;
        // Register default compatibility aliases to map flat template keys to structured DB nodes
        aliases.put("bic", "banks.bic");
        aliases.put("company", "companies.name");
        aliases.put("streetAddress", "streets");
        aliases.put("street", "streets");
        aliases.put("town", "towns");
        aliases.put("country", "countries");
        aliases.put("purpose", "purposes");
        aliases.put("categoryPurpose", "categoryPurposes");
        aliases.put("serviceLevel", "serviceLevels");
        aliases.put("localInstrument", "localInstruments");
        aliases.put("chargeBearer", "chargeBearers");
        aliases.put("currency", "currencies");
        aliases.put("bankOperationCode", "bankOperationCodes");
        aliases.put("instructionCode", "instructionCodes");
        aliases.put("detailsOfCharges", "detailsOfCharges");
        aliases.put("clearingChannel", "clearingChannels");
        aliases.put("senderToReceiverInfo", "senderToReceiverInfos");
    }

    public Path getDbDir() {
        return dbDir;
    }

    public synchronized void reloadDatabase() {
        loadedFiles.clear();
    }

    private String resolveAliasDataset(String datasetName) {
        if (aliases.containsKey(datasetName)) {
            String resolved = aliases.get(datasetName);
            if (resolved.contains(".")) {
                return resolved.split("\\.")[0];
            }
            return resolved;
        }
        return datasetName;
    }

    public synchronized JsonNode getDataset(String datasetName) {
        String resolvedName = resolveAliasDataset(datasetName);
        if (loadedFiles.containsKey(resolvedName)) {
            return loadedFiles.get(resolvedName);
        }
        
        if (dbDir == null) return null;

        // Find resolvedName.json or resolvedNamePlural.json in dbDir
        Path p = dbDir.resolve(resolvedName + ".json");
        if (!Files.exists(p)) {
            p = dbDir.resolve(resolvedName.toLowerCase() + ".json");
            if (!Files.exists(p)) {
                String plural = resolvedName.endsWith("y") ? resolvedName.substring(0, resolvedName.length() - 1) + "ies" : resolvedName + "s";
                p = dbDir.resolve(plural + ".json");
                if (!Files.exists(p)) {
                    p = dbDir.resolve(plural.toLowerCase() + ".json");
                    if (!Files.exists(p)) {
                        return null;
                    }
                }
            }
        }
        
        try {
            JsonNode node = mapper.readTree(p.toFile());
            loadedFiles.put(resolvedName, node);
            return node;
        } catch (IOException e) {
            System.err.println("[DatabaseService] Error loading dataset " + resolvedName + ": " + e.getMessage());
            return null;
        }
    }

    public String getRandomValue(String datasetName, String propertyName, GenerationContext context, String role) {
        if (("country".equals(datasetName) || "countries".equals(datasetName)) && role != null && !role.isEmpty()) {
            Object cachedCompany = context.getCachedObject("companies:" + role);
            if (cachedCompany instanceof JsonNode) {
                JsonNode countryNode = ((JsonNode) cachedCompany).get("country");
                if (countryNode != null) return countryNode.asText();
            }
            Object cachedBank = context.getCachedObject("banks:" + role);
            if (cachedBank instanceof JsonNode) {
                JsonNode countryNode = ((JsonNode) cachedBank).get("country");
                if (countryNode != null) return countryNode.asText();
            }
        }
        
        if ("bic".equals(datasetName) && role != null && !role.isEmpty()) {
            Object cachedBank = context.getCachedObject("banks:" + role);
            if (cachedBank instanceof JsonNode) {
                JsonNode bicNode = ((JsonNode) cachedBank).get("bic");
                if (bicNode != null) return bicNode.asText();
            }
        }

        if (aliases.containsKey(datasetName) && (propertyName == null || propertyName.isEmpty())) {
            String resolved = aliases.get(datasetName);
            if (resolved.contains(".")) {
                String[] parts = resolved.split("\\.");
                datasetName = parts[0];
                propertyName = parts[1];
            } else {
                datasetName = resolved;
            }
        }

        JsonNode dataset = getDataset(datasetName);
        if (dataset == null) {
            return null;
        }
        if (dataset.isArray()) {
            if (dataset.size() == 0) return "";
            
            JsonNode selectedObject = null;
            if (role != null && !role.isEmpty()) {
                Object cached = context.getCachedObject(datasetName + ":" + role);
                if (cached instanceof JsonNode) {
                    selectedObject = (JsonNode) cached;
                }
            }

            if (selectedObject == null) {
                int index = random.nextInt(dataset.size());
                selectedObject = dataset.get(index);
                if (role != null && !role.isEmpty()) {
                    context.cacheObject(datasetName + ":" + role, selectedObject);
                }
            }

            if (propertyName != null && !propertyName.isEmpty()) {
                JsonNode propNode = selectedObject.get(propertyName);
                return propNode != null ? propNode.asText() : "";
            } else {
                return selectedObject.asText();
            }
        } else {
            if (propertyName != null && !propertyName.isEmpty()) {
                JsonNode propNode = dataset.get(propertyName);
                return propNode != null ? propNode.asText() : "";
            } else {
                return dataset.asText();
            }
        }
    }
}
