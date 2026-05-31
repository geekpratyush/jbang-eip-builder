// camel-k: language=java
// DEPS org.apache.camel:camel-bom:4.4.0@pom
// DEPS org.apache.camel:camel-endpointdsl
// DEPS org.apache.camel:camel-jackson
// DEPS com.jayway.jsonpath:json-path:2.9.0
// DEPS com.fasterxml.jackson.core:jackson-databind:2.16.1
// SOURCES ../../../../src/main/java/com/sre/engine/audit/AuditShaperProcessor.java

import org.apache.camel.builder.RouteBuilder;
import com.sre.engine.audit.AuditShaperProcessor;

/**
 * Demo route for SRE Audit Shaper.
 * Shows progression from simple to complex transformations.
 */
public class AuditShaperDemo extends RouteBuilder {

    @Override
    public void configure() throws Exception {

        // 1. Simple Audit: No transformations, just enveloping
        from("direct:simple-audit")
            .routeId("simple-route")
            .process(new AuditShaperProcessor())
            .log("SIMPLE OUTPUT:\n${body}");

        // 2. Complex Audit: Masking, Modifying, and Excluding
        from("direct:complex-audit")
            .routeId("complex-route")
            // Set input payload
            .setBody().constant("{\"user\":\"admin\", \"details\":{\"role\":\"manager\", \"pin\":1234}, \"tags\":[\"internal\"]}")
            // Configuration Headers
            .setHeader("CamelAuditExcludeFields", constant("$.details.pin"))
            .setHeader("CamelAuditModifyFields", constant("$.details.role=SRE_ADMIN,$.system=PROD"))
            .setHeader("CamelAuditEncryptFields", constant("$.user,$.tags"))
            .process(new AuditShaperProcessor())
            .log("COMPLEX OUTPUT:\n${body}");

        // Test Trigger
        from("timer:test?delay=1000&repeatCount=1")
            .setBody().constant("{\"message\":\"Hello SRE\"}")
            .to("direct:simple-audit")
            .to("direct:complex-audit");
    }
}
