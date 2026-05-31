package mongodb.engine;

import org.apache.camel.BindToRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.File;
import java.util.Iterator;

@BindToRegistry("mongodb.engine.MongoGateway")
public class MongoGateway {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static File getCollectionFile(String collection) {
        String baseDir = System.getProperty("user.dir");
        File dataDir = new File(baseDir + "/infra-simulator/mongodb/data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        return new File(dataDir, collection + ".json");
    }

    public static String process(String inputJson) {
        try {
            JsonNode request = mapper.readTree(inputJson);
            String operation = request.get("operation").asText();
            String collection = request.get("collection").asText();
            
            File file = getCollectionFile(collection);
            ArrayNode list;
            if (file.exists() && file.length() > 0) {
                JsonNode existing = mapper.readTree(file);
                if (existing.isArray()) {
                    list = (ArrayNode) existing;
                } else {
                    list = mapper.createArrayNode();
                    list.add(existing);
                }
            } else {
                list = mapper.createArrayNode();
            }

            if ("insert".equalsIgnoreCase(operation)) {
                JsonNode doc = request.get("document");
                String newId = doc.get("id").asText();
                Iterator<JsonNode> it = list.elements();
                while (it.hasNext()) {
                    if (it.next().get("id").asText().equals(newId)) {
                        return "{\"status\": \"error\", \"message\": \"Duplicate document ID '" + newId + "'\"}";
                    }
                }
                list.add(doc);
                mapper.writerWithDefaultPrettyPrinter().writeValue(file, list);
                return "{\"status\": \"inserted\", \"id\": \""+ newId +"\"}";
            } 
            else if ("findOne".equalsIgnoreCase(operation)) {
                JsonNode query = request.get("query");
                String queryId = query.get("id").asText();
                for (JsonNode node : list) {
                    if (node.has("id") && node.get("id").asText().equals(queryId)) {
                        return mapper.writeValueAsString(node);
                    }
                }
                return "{\"error\": \"Document not found\", \"id\": \"" + queryId + "\"}";
            } 
            else if ("findAll".equalsIgnoreCase(operation)) {
                return mapper.writeValueAsString(list);
            } 
            else if ("delete".equalsIgnoreCase(operation)) {
                JsonNode query = request.get("query");
                String queryId = query.get("id").asText();
                boolean removed = false;
                Iterator<JsonNode> it = list.elements();
                while (it.hasNext()) {
                    JsonNode node = it.next();
                    if (node.has("id") && node.get("id").asText().equals(queryId)) {
                        it.remove();
                        removed = true;
                        break;
                    }
                }
                if (removed) {
                    mapper.writerWithDefaultPrettyPrinter().writeValue(file, list);
                    return "{\"status\": \"deleted\", \"id\": \"" + queryId + "\"}";
                } else {
                    return "{\"error\": \"Document not found\"}";
                }
            }
            return "{\"error\": \"Unsupported operation\"}";
        } catch (Exception e) {
            return "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}";
        }
    }
}
