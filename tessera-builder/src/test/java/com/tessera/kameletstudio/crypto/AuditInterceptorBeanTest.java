package com.tessera.kameletstudio.crypto;

import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

public class AuditInterceptorBeanTest {

    @Test
    public void testProcessAudit() {
        AuditInterceptorBean auditor = new AuditInterceptorBean();
        
        Map<String, Object> body = new HashMap<>();
        body.put("cardNumber", "1111-2222-3333-4444");
        body.put("amount", "100.00");
        body.put("ssn", "999-00-1111");

        Map<String, Object> headers = new HashMap<>();
        headers.put("CamelRouteId", "test-route");
        headers.put("AuditOperation", "PAYMENT_TEST");
        headers.put("X-User-ID", "user123");

        Map<String, Object> properties = new HashMap<>();
        properties.put("audit.mask.fields", "cardNumber");
        properties.put("audit.delete.fields", "ssn");

        Object result = auditor.processAudit(body, headers, properties);
        
        // Body itself should not be modified by the audit process (since we clone it)
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals("1111-2222-3333-4444", resultMap.get("cardNumber"));
        assertEquals("999-00-1111", resultMap.get("ssn"));
    }
}
