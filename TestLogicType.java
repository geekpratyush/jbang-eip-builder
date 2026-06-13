//DEPS org.json:json:20240303
import org.json.JSONObject;

public class TestLogicType {
    public static void main(String[] args) throws Exception {
        String json = "{\n" +
                      "  \"logic\": {\n" +
                      "    \"file\": \"enrich.groovy\",\n" +
                      "    \"type\": \"groovy\"\n" +
                      "  }\n" +
                      "}";
        JSONObject currentConfig = new JSONObject(json);
        String logicType = "xslt";
        Object logicObj = currentConfig.get("logic");
        if (logicObj instanceof org.json.JSONArray) {
            logicType = ((org.json.JSONArray) logicObj).getJSONObject(0).optString("type", "xslt");
        } else if (logicObj instanceof JSONObject) {
            logicType = ((JSONObject) logicObj).optString("type", "xslt");
        }
        System.out.println("logicType: " + logicType);
    }
}
