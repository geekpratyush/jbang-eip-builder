# Workspace Variables Editor

Managing environment-specific configurations and property placeholders in Camel routes is simplified via the **Variables Editor**.

---

## 1. Environment Configurations

Instead of hardcoding URLs, passwords, and port configurations within route scripts, developers reference properties via placeholders:

```yaml
uri: "jms:queue:{{queuename}}"
parameters:
  connectionFactory: "#bean:{{mqPoolFactory}}"
```

---

## 2. Variables Panel features

The editor manages these placeholders within the active workspace:
- **Key-Value Grid**: Lists all active environment keys, resolved values, and formats.
- **Dynamic Override Profile**: Modify variable values locally for target run configurations (e.g. swap database URLs from Dev to Test profiles).
- **Target Export**: Automatically saves and writes variables to `application.properties` before running JBang route executions, ensuring JBang reads all placeholders correctly.
