//DEPS org.apache.camel:camel-core:4.18.0
//DEPS org.apache.camel:camel-jslt:4.18.0

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.component.jslt.JsltComponent;
import java.util.Collections;

public class TestJsltCamel {
    public static void main(String[] args) throws Exception {
        CamelContext context = new DefaultCamelContext();
        JsltComponent component = new JsltComponent();
        
        com.schibsted.spt.data.jslt.Function arrayZipFunc = new com.schibsted.spt.data.jslt.Function() {
            @Override public String getName() { return "array-zip"; }
            @Override public int getMinArguments() { return 2; }
            @Override public int getMaxArguments() { return 2; }
            @Override public com.fasterxml.jackson.databind.JsonNode call(com.fasterxml.jackson.databind.JsonNode inputNode, com.fasterxml.jackson.databind.JsonNode[] arguments) {
                return com.fasterxml.jackson.databind.node.NullNode.getInstance();
            }
        };
        
        component.setFunctions(Collections.singletonList(arrayZipFunc));
        context.addComponent("jslt", component);
        System.out.println("Success");
    }
}
