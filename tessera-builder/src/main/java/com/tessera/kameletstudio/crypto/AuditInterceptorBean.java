package com.tessera.kameletstudio.crypto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.net.InetAddress;
import java.util.*;

/**
 * Audit Interceptor Bean for managing field-level audit controls.
 * Handles masking, exclusion, and asynchronous audit document generation.
 */
public class AuditInterceptorBean {

    private static final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);
    
    private final FieldCryptoProcessor cryptoProcessor = new FieldCryptoProcessor();

    public Object processAudit(Object body, Map<String, Object> headers, Map<String, Object> properties) {
        try {
            AuditDocument audit = new AuditDocument();
            audit.routeId = (String) headers.get("CamelRouteId");
            audit.correlationId = (String) headers.get("X-Correlation-ID");
            audit.operation = (String) headers.get("AuditOperation");
            audit.source = (String) headers.get("AuditSource");
            audit.destination = (String) headers.get("AuditDestination");
            audit.status = (String) headers.get("AuditStatus");
            if (audit.status == null) audit.status = "SUCCESS";

            // Metadata
            audit.metadata = new AuditDocument.Metadata();
            audit.metadata.userId = (String) headers.get("X-User-ID");
            audit.metadata.environment = System.getenv("APP_ENV");
            audit.metadata.appVersion = System.getenv("APP_VERSION");
            try {
                InetAddress localHost = InetAddress.getLocalHost();
                audit.metadata.hostname = localHost.getHostName();
                audit.metadata.ip = localHost.getHostAddress();
            } catch (Exception ignored) {}

            // Payload processing
            boolean excludePayload = Boolean.parseBoolean((String) properties.get("audit.exclude.payload"));
            if (!excludePayload) {
                audit.payload = new AuditDocument.Payload();
                Object clonedBody = cloneBody(body);
                applyAuditControls(clonedBody, properties);
                audit.payload.after = clonedBody;
            }

            // Convert to JSON
            String auditJson = mapper.writeValueAsString(audit);
            
            // In a real Camel environment, we would send this to a "direct:audit" or similar
            // For now, we'll log it or simulate the async send
            System.out.println("AUDIT RECORD GENERATED:\n" + auditJson);
            
            // If there's a ProducerTemplate available, we'd use it here:
            // producerTemplate.asyncSendBody("kamelet:kamelet-studio-mongodb-sink?operation=insert", auditJson);

        } catch (Exception e) {
            System.err.println("Failed to process audit: " + e.getMessage());
            e.printStackTrace();
        }
        return body;
    }

    private void applyAuditControls(Object body, Map<String, Object> properties) {
        if (!(body instanceof Map)) return;
        Map<String, Object> map = (Map<String, Object>) body;

        // Mask fields
        String maskFields = (String) properties.get("audit.mask.fields");
        if (maskFields != null) {
            for (String f : maskFields.split(",")) {
                f = f.trim();
                if (map.containsKey(f)) map.put(f, "****");
            }
        }

        // Delete fields
        String deleteFields = (String) properties.get("audit.delete.fields");
        if (deleteFields != null) {
            for (String f : deleteFields.split(",")) {
                map.remove(f.trim());
            }
        }

        // Encrypt fields in audit
        String encryptFields = (String) properties.get("audit.encrypt.fields");
        if (encryptFields != null) {
            String secret = System.getenv("AES_SECRET_KEY");
            if (secret != null) {
                for (String f : encryptFields.split(",")) {
                    f = f.trim();
                    Object val = map.get(f);
                    if (val instanceof String) {
                        try {
                            map.put(f, cryptoProcessor.encrypt((String) val, secret));
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
    }

    private Object cloneBody(Object body) {
        if (body instanceof Map) {
            return new HashMap<>((Map<?, ?>) body);
        }
        // Simplified clone for this prototype
        return body;
    }
}
