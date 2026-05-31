# SRE Audit Shaper Sample Project

This sample demonstrates how to use the `AuditShaperProcessor` in various scenarios.

## Files
- `AuditShaperDemo.java`: A Camel route using JBang to run the processor.
- `run-test.sh`: Script to trigger the demo.

## How to Run Locally

1. **Direct JBang Run:**
   ```bash
   jbang AuditShaperDemo.java
   ```

2. **Using Camel JBang (Development Mode):**
   ```bash
   camel run AuditShaperDemo.java --dev
   ```

## Scenarios Demonstrated

### Scenario A: Simple Enveloping
Input: `{"message":"Hello SRE"}`
Output: A full JSON envelope with `metadata` and the original message in `payload`.

### Scenario B: Complex Transformation
Input:
```json
{
  "user": "admin",
  "details": { "role": "manager", "pin": 1234 },
  "tags": ["internal"]
}
```
Headers:
- `CamelAuditExcludeFields`: `$.details.pin` (Removes the PIN)
- `CamelAuditModifyFields`: `$.details.role=SRE_ADMIN` (Updates role)
- `CamelAuditEncryptFields`: `$.user` (Masks the username)

Result:
A secure audit record ready for Kibana or MongoDB.

## Post-Deployment Compatibility
Once deployed, this processor ensures that every message hitting your `audit` collection has the exact same schema, regardless of whether the original source was a CSV file, a REST API, or a Kafka topic.
