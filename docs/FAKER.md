# FAKER Engine: Automated Financial Message Simulation

The **FAKER engine** is a high-performance, data-agnostic financial message simulator integrated within the Camel Quarkus Route Builder workspace. It allows developers to generate realistic, formatted message traffic for over **550+ MX (ISO 20022)** and **16+ legacy SWIFT MT** formats dynamically.

By coupling the faker engine to active Camel routes, you can continuously stream fake traffic into integration pipelines, message brokers (Kafka, IBM MQ, ActiveMQ), or databases to test resilience, throughput, and business logic without relying on production data feeds.

---

## 🛠️ Step-by-Step Setup Guide

Follow these steps to generate and run a faking simulation in your local environment.

### Step 1: Create a Sample Project in the IDE
1. Launch the **Route Builder IDE**.
2. Click **File -> Create Sample Camel Project**.
3. Choose a destination workspace folder.
4. The IDE will automatically unpack the project templates, including the `FAKER` directory with all beans, databases, and message templates.

---

### Step 2: Structure of the FAKER Directory
Once unpacked, the `FAKER` folder has the following layout:
```text
FAKER/
├── beans/
│   ├── CBPRFaker.java           # Main registry entry-point for Camel calls
│   ├── TemplateDiscovery.java   # Scans and resolves template paths
│   ├── TemplateEngine.java      # Processes placeholders and performs role correlation
│   ├── GenerationContext.java   # Stores run state and correlates entity attributes
│   └── DatabaseService.java     # Retrieves realistic datasets from the databases
├── faker-db/
│   ├── firstNames.json          # Mock database files supplying random
│   ├── lastNames.json           # names, addresses, companies, and
│   ├── banks.json               # SWIFT BICs/countries
│   └── streets.json
├── templates/
│   ├── pacs.008.xml             # ISO 20022 XML template files
│   ├── camt.053.xml             # (550+ MX templates pre-loaded)
│   └── mt103.txt                # SWIFT MT block text template files
├── faker-route.camel.yaml       # Single-type generation route
└── faker-stub-demo.camel.yaml   # Multi-type traffic simulator route
```

---

### Step 3: Run the Multi-Type Traffic Simulation
To start generating continuous traffic, run the `faker-stub-demo.camel.yaml` using JBang:

```bash
jbang camel run FAKER/faker-stub-demo.camel.yaml
```

This boots the Camel JBang runtime, compiles the helper Java beans on-the-fly, loads the dataset JSONs, and starts printing generated messages to the console log.

---

## 🔍 Understanding the Traffic Simulator Route

Here is the logic inside `faker-stub-demo.camel.yaml`:

```yaml
- beans:
    - name: "cbprFaker"
      type: "beans.CBPRFaker"
      properties:
        database: "financial"

- route:
    id: "faker-source"
    from:
      uri: "timer:fake-traffic?period=100"  # Triggers every 100 milliseconds
      steps:
        - setHeader:
            name: "destination"
            # List of possible target templates to randomly generate from
            simple: "mt103, mt202, mt940, pacs002, pacs004, pacs008, pacs009, camt053, pain001"
        - bean:
            ref: "cbprFaker"
            method: "generate(${header.destination})"
        - log: "Faker: Generated for [${header.destination}] and sending to direct:app-incoming"
        - to: "direct:app-incoming"

- route:
    id: "actual-logic-consumer"
    from:
      uri: "direct:app-incoming"
      steps:
        - log: "CONSUMER: Received Live Fake Message from [${header.destination}]:\n${body}"
```

### Breakdown of the Pipeline:
1. **Bean Registration (`cbprFaker`)**: Registers the simulator engine bean and tells it to load the `financial` dataset folder.
2. **Timer Trigger (`timer:fake-traffic?period=100`)**: Sends an exchange down the pipeline every **100ms** (10 messages per second).
3. **Random Selection (`setHeader:destination`)**: Sets a comma-separated list of MX/MT codes. The `CBPRFaker.generate(messageType)` method parses the comma and chooses one type at random for each execution.
4. **Data Faking & Rendering**: The bean processes the template associated with the selected message type (e.g. `pacs.008.xml` or `mt103.txt`), fills it with correlated random entities, and outputs the result as the exchange body.
5. **Consumption (`direct:app-incoming`)**: In this sandbox demo, messages are routed locally, but in a production simulation, you can easily bridge this to external brokers (see below).

---

## 🚀 Simulating Real Integrations (Brokers, Kafka, IBM MQ)

To feed the faked traffic into your real application pipelines, replace the final step `- to: "direct:app-incoming"` with your broker endpoints:

### Option A: Streaming to Apache Kafka
If your application consumes from a Kafka topic:
```yaml
        - to: "kafka:incoming-financial-traffic?brokers=localhost:9092"
```

### Option B: Streaming to IBM MQ
If your application processes messages via JMS/MQ queues:
```yaml
        - to: "ibmmq:queue:DEV.QUEUE.1?connectionFactory=#mqConnectionFactory"
```

### Option C: Simulating an API Client
If you are load-testing an HTTP REST gateway:
```yaml
        - setHeader:
            name: "CamelHttpMethod"
            constant: "POST"
        - setHeader:
            name: "Content-Type"
            constant: "application/xml"
        - to: "http://localhost:8080/api/v1/payments"
```
