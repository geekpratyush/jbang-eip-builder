package com.tessera.kameletstudio.crypto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class AuditDocument {
    public String auditId;
    public String timestamp;
    public String routeId;
    public String correlationId;
    public String operation;
    public String source;
    public String destination;
    public String status;
    public Long durationMs;
    public Payload payload;
    public Metadata metadata;
    public String errors;

    public static class Payload {
        public Object before;
        public Object after;
    }

    public static class Metadata {
        public String userId;
        public String environment;
        public String appVersion;
        public String hostname;
        public String ip;
        public Map<String, Object> additional;
    }

    public AuditDocument() {
        this.auditId = "AUD-" + UUID.randomUUID().toString();
        this.timestamp = Instant.now().toString();
    }
}
