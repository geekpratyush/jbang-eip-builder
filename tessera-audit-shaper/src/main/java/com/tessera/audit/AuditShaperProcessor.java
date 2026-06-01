package com.tessera.audit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Set;

/**
 * SRE Audit & Crypto Processor.
 * Centralized security backbone for the Sovereign Resilience Engine.
 */
public class AuditShaperProcessor implements Processor {

    private static final String HEADER_EXCLUDE = "CamelAuditExcludeFields";
    private static final String HEADER_MODIFY = "CamelAuditModifyFields";
    private static final String HEADER_ENCRYPT = "CamelAuditEncryptFields";
    private static final String HEADER_DECRYPT = "CamelAuditDecryptFields";
    private static final String HEADER_CRYPTO_KEY_OVERRIDE = "CamelAuditCryptoKey";
    private static final String HEADER_DEBUG_LOG = "CamelAuditDebugLog";
    private static final String HEADER_PRETTY_PRINT = "CamelAuditPrettyPrint";
    
    private static final String PROP_GLOBAL_KEY = "sre.audit.crypto.key";
    
    private static final Set<String> SENSITIVE_KEYS = Set.of("password", "secret", "cvv", "pin", "apikey");

    private final ObjectMapper mapper;
    private final Configuration jsonPathConfig;
    private String hostname;
    private String ipAddress;

    public AuditShaperProcessor() {
        this.mapper = new ObjectMapper();
        this.jsonPathConfig = Configuration.builder()
                .jsonProvider(new JacksonJsonNodeJsonProvider(mapper))
                .mappingProvider(new JacksonMappingProvider(mapper))
                .options(Option.DEFAULT_PATH_LEAF_TO_NULL)
                .build();

        try {
            InetAddress localHost = InetAddress.getLocalHost();
            this.hostname = localHost.getHostName();
            this.ipAddress = localHost.getHostAddress();
        } catch (UnknownHostException e) {
            this.hostname = "unknown";
            this.ipAddress = "127.0.0.1";
        }
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        String body = exchange.getIn().getBody(String.class);
        if (body == null || body.isBlank()) {
            body = "{}";
        }

        DocumentContext ctx = JsonPath.using(jsonPathConfig).parse(body);

        // 1. Data Modification (Add/Update/Delete)
        handleExclusions(exchange, ctx);
        handleModifications(exchange, ctx);

        // 2. Cryptography (Encrypt/Decrypt)
        handleCrypto(exchange, ctx, HEADER_ENCRYPT, true);
        handleCrypto(exchange, ctx, HEADER_DECRYPT, false);

        // 3. Default Masking for Log Protection
        boolean debugMode = "true".equalsIgnoreCase(exchange.getIn().getHeader(HEADER_DEBUG_LOG, String.class));
        if (!debugMode) {
            applySafeMasking(ctx);
        }

        // 4. Build Canonical Envelope
        ObjectNode envelope = mapper.createObjectNode();

        // Traceability Metadata
        ObjectNode metadata = envelope.putObject("metadata");
        metadata.put("exchangeId", exchange.getExchangeId());
        metadata.put("routeId", exchange.getFromRouteId());
        metadata.put("timestamp", Instant.now().toString());
        metadata.put("source_host", hostname);
        metadata.put("source_ip", ipAddress);
        
        String corrId = exchange.getProperty(Exchange.CORRELATION_ID, String.class);
        if (corrId == null) corrId = exchange.getIn().getHeader("CamelCorrelationId", String.class);
        if (corrId == null) corrId = exchange.getIn().getHeader("breadcrumbId", String.class);
        metadata.put("correlationId", corrId);

        // Filter Context Headers
        ObjectNode headersNode = envelope.putObject("contextHeaders");
        for (Map.Entry<String, Object> entry : exchange.getIn().getHeaders().entrySet()) {
            if (isSerializable(entry.getValue()) && !isSensitiveKey(entry.getKey())) {
                headersNode.put(entry.getKey(), entry.getValue().toString());
            } else if (isSensitiveKey(entry.getKey())) {
                headersNode.put(entry.getKey(), "******");
            }
        }

        envelope.set("payload", (JsonNode) ctx.json());
        
        boolean pretty = "true".equalsIgnoreCase(exchange.getIn().getHeader(HEADER_PRETTY_PRINT, String.class));
        String resultBody = pretty ? 
            mapper.writerWithDefaultPrettyPrinter().writeValueAsString(envelope) : 
            mapper.writeValueAsString(envelope);

        exchange.getIn().setBody(resultBody);
    }

    private void applySafeMasking(DocumentContext ctx) {
        // Simple top-level masking for known sensitive keys
        if (ctx.json() instanceof ObjectNode) {
            ObjectNode node = (ObjectNode) ctx.json();
            for (String key : SENSITIVE_KEYS) {
                if (node.has(key)) node.put(key, "******");
            }
        }
    }

    private void handleCrypto(Exchange exchange, DocumentContext ctx, String header, boolean encrypt) {
        String fields = exchange.getIn().getHeader(header, String.class);
        if (fields == null) return;

        // Key resolution hierarchy: Header Override -> Global Property -> Environment Variable -> NULL
        String key = exchange.getIn().getHeader(HEADER_CRYPTO_KEY_OVERRIDE, String.class);
        if (key == null) {
            key = exchange.getContext().resolvePropertyPlaceholders("{{" + PROP_GLOBAL_KEY + ":}}");
        }
        if (key == null || key.isBlank()) {
            key = System.getenv("SRE_AUDIT_CRYPTO_KEY");
        }

        for (String path : fields.split(",")) {
            path = path.trim();
            Object val = ctx.read(path);
            if (val == null) continue;

            if (key == null || key.isBlank()) {
                if (encrypt) ctx.set(path, "[STUB_ENCRYPTED::" + Integer.toHexString(val.hashCode()) + "]");
            } else {
                try {
                    String result = encrypt ? aesEncrypt(val.toString(), key) : aesDecrypt(val.toString(), key);
                    ctx.set(path, result);
                } catch (Exception e) {
                    ctx.set(path, "[CRYPTO_ERROR]");
                }
            }
        }
    }

    private String aesEncrypt(String data, String key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(fixKey(key.getBytes(StandardCharsets.UTF_8)), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes(StandardCharsets.UTF_8)));
    }

    private String aesDecrypt(String encryptedData, String key) throws Exception {
        SecretKeySpec secretKey = new SecretKeySpec(fixKey(key.getBytes(StandardCharsets.UTF_8)), "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedData)), StandardCharsets.UTF_8);
    }

    private byte[] fixKey(byte[] key) {
        byte[] fixed = new byte[16];
        System.arraycopy(key, 0, fixed, 0, Math.min(key.length, 16));
        return fixed;
    }

    private void handleExclusions(Exchange exchange, DocumentContext ctx) {
        String fields = exchange.getIn().getHeader(HEADER_EXCLUDE, String.class);
        if (fields != null) {
            for (String path : fields.split(",")) {
                try { ctx.delete(path.trim()); } catch (Exception ignored) {}
            }
        }
    }

    private void handleModifications(Exchange exchange, DocumentContext ctx) {
        String fields = exchange.getIn().getHeader(HEADER_MODIFY, String.class);
        if (fields != null) {
            for (String pair : fields.split(",")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2) {
                    ctx.put(getParentPath(kv[0].trim()), getKey(kv[0].trim()), kv[1].trim());
                }
            }
        }
    }

    private String getParentPath(String p) { return (p.lastIndexOf(".") <= 1) ? "$" : p.substring(0, p.lastIndexOf(".")); }
    private String getKey(String p) { return (p.lastIndexOf(".") == -1) ? p.replace("$", "") : p.substring(p.lastIndexOf(".") + 1); }
    private boolean isSerializable(Object v) { return v instanceof String || v instanceof Number || v instanceof Boolean; }
    private boolean isSensitiveKey(String k) { return SENSITIVE_KEYS.contains(k.toLowerCase()); }
}
