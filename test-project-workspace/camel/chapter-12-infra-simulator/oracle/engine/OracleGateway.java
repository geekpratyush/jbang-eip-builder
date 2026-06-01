package oracle.engine;

import org.apache.camel.BindToRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.File;
import java.util.Iterator;

@BindToRegistry("oracle.engine.OracleGateway")
public class OracleGateway {

    private static final ObjectMapper mapper = new ObjectMapper();

    private static File getTableFile(String table) {
        String baseDir = System.getProperty("user.dir");
        File dataDir = new File(baseDir + "/infra-simulator/oracle/data");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        return new File(dataDir, table + ".json");
    }

    public static String execute(String requestJson) {
        try {
            JsonNode request = mapper.readTree(requestJson);
            String queryType = request.get("queryType").asText();
            String table = request.get("table").asText();
            
            File file = getTableFile(table);
            ArrayNode list;
            if (file.exists() && file.length() > 0) {
                JsonNode tree = mapper.readTree(file);
                if (tree.isArray()) {
                    list = (ArrayNode) tree;
                } else {
                    list = mapper.createArrayNode();
                    list.add(tree);
                }
            } else {
                list = mapper.createArrayNode();
            }

            if ("insert".equalsIgnoreCase(queryType)) {
                JsonNode row = request.get("row");
                String newId = row.get("id").asText();
                Iterator<JsonNode> it = list.elements();
                while (it.hasNext()) {
                    if (it.next().get("id").asText().equals(newId)) {
                        return "{\"status\": \"error\", \"message\": \"Duplicate record ID '" + newId + "'\"}";
                    }
                }
                list.add(row);
                mapper.writerWithDefaultPrettyPrinter().writeValue(file, list);
                return "{\"status\": \"row_inserted\", \"id\": \"" + newId + "\"}";
            } 
            else if ("select".equalsIgnoreCase(queryType)) {
                String column = request.get("column").asText();
                String val = request.get("value").asText();
                ArrayNode results = mapper.createArrayNode();
                for (JsonNode row : list) {
                    if (row.has(column) && row.get(column).asText().equals(val)) {
                        results.add(row);
                    }
                }
                return mapper.writeValueAsString(results);
            } 
            else if ("selectAll".equalsIgnoreCase(queryType)) {
                return mapper.writeValueAsString(list);
            }
            return "{\"error\": \"Unsupported SQL query type\"}";
        } catch (Exception e) {
            return "{\"status\": \"error\", \"message\": \"" + e.getMessage() + "\"}";
        }
    }
}
