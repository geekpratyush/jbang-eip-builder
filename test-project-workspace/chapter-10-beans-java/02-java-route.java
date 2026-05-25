// camel-k: language=java
//DEPS org.apache.camel:camel-timer:4.20.0

import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.BindToRegistry;

public class MyBeanRoute extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("timer:java-bean?period=4000&repeatCount=3")
            .setBody().constant("Message from Java DSL")
            .bean("myJavaFormatter", "format")
            .log("Result: ${body}");
    }

    @BindToRegistry("myJavaFormatter")
    public static class JavaFormatter {
        public String format(String input) {
            return "[JAVA-BEAN-LOG] " + input;
        }
    }
}
