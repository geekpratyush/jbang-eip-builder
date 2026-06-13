//DEPS com.schibsted.spt.data:jslt:0.1.14
//DEPS com.fasterxml.jackson.core:jackson-databind:2.14.2
//DEPS com.fasterxml.jackson.core:jackson-core:2.14.2
//DEPS org.json:json:20240303

import com.schibsted.spt.data.jslt.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.XML;
import org.json.JSONObject;

public class TestJslt {
    public static void main(String[] args) throws Exception {
        String originalXml = "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.14\"><FIToFICstmrCdtTrf><CdtTrfTxInf><PmtId>1</PmtId></CdtTrfTxInf><CdtTrfTxInf><PmtId>2</PmtId></CdtTrfTxInf></FIToFICstmrCdtTrf></Document>";
        String truncatedXml = "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.14\"><FIToFICstmrCdtTrf><CdtTrfTxInf><PmtId>3</PmtId></CdtTrfTxInf><CdtTrfTxInf><PmtId>4</PmtId></CdtTrfTxInf></FIToFICstmrCdtTrf></Document>";
        
        String inputXml = "<envelope><original>" + originalXml + "</original><truncated>" + truncatedXml + "</truncated></envelope>";
        JSONObject jsonObj = XML.toJSONObject(inputXml);
        
        String jslt = "def merge(orig, trunc)\n" +
            "  if (is-object($orig))\n" +
            "    {for ($orig) \n" +
            "       let k = .key \n" +
            "       let v = .value\n" +
            "       let t = $trunc[$k]\n" +
            "       $k : merge($v, $t)\n" +
            "    }\n" +
            "  else if (is-array($orig))\n" +
            "    [for (array-zip($orig, $trunc)) \n" +
            "       merge(.orig, .trunc)\n" +
            "    ]\n" +
            "  else\n" +
            "    if ($trunc != null and string($trunc) != \"\")\n" +
            "      $trunc\n" +
            "    else\n" +
            "      $orig\n" +
            "\n" +
            "let env = if (.envelope) .envelope else .\n" +
            "let origDoc = $env.original.Document\n" +
            "let truncDoc = $env.truncated.Document\n" +
            "merge($origDoc, $truncDoc)";

        try {
            com.schibsted.spt.data.jslt.Function arrayZipFunc = new com.schibsted.spt.data.jslt.Function() {
                @Override public String getName() { return "array-zip"; }
                @Override public int getMinArguments() { return 2; }
                @Override public int getMaxArguments() { return 2; }
                @Override public com.fasterxml.jackson.databind.JsonNode call(com.fasterxml.jackson.databind.JsonNode inputNode, com.fasterxml.jackson.databind.JsonNode[] arguments) {
                    if (!(arguments[0] instanceof com.fasterxml.jackson.databind.node.ArrayNode)) {
                        return com.fasterxml.jackson.databind.node.NullNode.getInstance();
                    }
                    ObjectMapper mapper = new ObjectMapper();
                    com.fasterxml.jackson.databind.node.ArrayNode arr1 = (com.fasterxml.jackson.databind.node.ArrayNode) arguments[0];
                    com.fasterxml.jackson.databind.node.ArrayNode arr2 = arguments[1] instanceof com.fasterxml.jackson.databind.node.ArrayNode ? (com.fasterxml.jackson.databind.node.ArrayNode) arguments[1] : null;
                    com.fasterxml.jackson.databind.node.ArrayNode result = mapper.createArrayNode();
                    for (int i = 0; i < arr1.size(); i++) {
                        com.fasterxml.jackson.databind.node.ObjectNode obj = mapper.createObjectNode();
                        obj.set("orig", arr1.get(i));
                        obj.set("trunc", (arr2 != null && i < arr2.size()) ? arr2.get(i) : com.fasterxml.jackson.databind.node.NullNode.getInstance());
                        result.add(obj);
                    }
                    return result;
                }
            };
            java.util.Collection<com.schibsted.spt.data.jslt.Function> funcs = java.util.Collections.singletonList(arrayZipFunc);
            Expression expr = Parser.compileString(jslt, funcs);
            com.fasterxml.jackson.databind.JsonNode outputNode = expr.apply(new ObjectMapper().readTree(jsonObj.toString()));
            String jsonResult = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(outputNode);
            String xmlOut = XML.toString(new JSONObject(jsonResult), "Document");
            System.out.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
                   xmlOut.replace("<Document>", "<Document xmlns=\"urn:iso:std:iso:20022:tech:xsd:pacs.008.001.14\">"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
