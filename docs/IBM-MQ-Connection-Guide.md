# IBM MQ mTLS Connection Guide

This guide details the complete configuration, infrastructure setup, and Apache Camel route integration for establishing a secure, Mutual TLS (mTLS) connection to IBM MQ.

---

## 1. Infrastructure & Certificate Configuration

The local development environment uses a dockerized IBM MQ container (version `9.3.2.0-r1`) configured with a custom Queue Manager `QM1` and channel `DEV.ADMIN.SVRCONN`.

### Key & Trust Store Generation
Certificates are generated using the `./generate-certs.sh` script, producing the following files under `infra-setup/certs/ibmmq/`:
- **Server Key Store (`key.p12`)**: PKCS12 file containing the server certificate and CA certificate. Placed inside the container at `/etc/mqm/pki/key.p12`.
- **Client Key Store (`clientkey.p12`)**: Contains the client private key and certificate with `CN=mqclient` (Password: `clientpassword`).
- **Client Trust Store (`clienttrust.p12`)**: Contains the Root CA certificate to verify the IBM MQ server certificate (Password: `mqpassword`).

### Auto-Tuning and Self-Healing Container Startup
During container startup in `start-infra.sh`, the following steps are automatically performed to ensure the container is configured correctly for mTLS:
1. **Stash File Alignment**: When the MQ image initializes `key.p12`, it stashes the password into `key.sth`. However, because the Queue Manager is configured to look for `key.p12`, GSKit requires the stash file to be named `key.p12.sth`. The script copies `/var/mqm/qmgrs/QM1/ssl/key.sth` to `/var/mqm/qmgrs/QM1/ssl/key.p12.sth`.
2. **Channel Mapping & Authentication Tuning**:
   - Maps clients presenting certificates with `CN=mqclient` to the administrative user `mqm`.
   - Alters `DEV.AUTHINFO` connection authentication to `CHCKCLNT(NONE)` and refreshes security. This disables password validation for client connections, which is required for passwordless mTLS connections mapping to administrative accounts.

```bash
# Executed automatically via start-infra.sh:
docker exec ibmmq cp /var/mqm/qmgrs/QM1/ssl/key.sth /var/mqm/qmgrs/QM1/ssl/key.p12.sth
echo "ALTER QMGR SSLKEYR('/var/mqm/qmgrs/QM1/ssl/key.p12') KEYRPWD(' ')
ALTER AUTHINFO(DEV.AUTHINFO) AUTHTYPE(IDPWOS) CHCKCLNT(NONE)
REFRESH SECURITY TYPE(ALL)
END" | docker exec -i -u 1001 ibmmq runmqsc QM1
```

---

## 2. Quarkus Application Configuration

Instead of declaring connection details inside `application.properties` or `application.yaml` or passing them explicitly in the route URIs, you can pass them as environment variables. 

Quarkus' MicroProfile Config automatically converts environment variables using the `CAMEL_KAMELET_...` naming convention directly into Camel's scoped Kamelet property registry (e.g. mapping `CAMEL_KAMELET_KAMELET_STUDIO_IBMMQ_SINK_HOSTNAME` to `camel.kamelet.kamelet-studio-ibmmq-sink.hostname`).

### 1. Environment Configuration (`.env`)
The local `.env` file defines the environment variables for both the IBM MQ source and sink Kamelets:
```env
# Camel Kamelet Studio IBM MQ Sink Properties
CAMEL_KAMELET_KAMELET_STUDIO_IBMMQ_SINK_HOSTNAME=localhost
CAMEL_KAMELET_KAMELET_STUDIO_IBMMQ_SINK_PORT=1414
CAMEL_KAMELET_KAMELET_STUDIO_IBMMQ_SINK_QUEUEMANAGER=QM1
CAMEL_KAMELET_KAMELET_STUDIO_IBMMQ_SINK_CHANNEL=DEV.ADMIN.SVRCONN
CAMEL_KAMELET_KAMELET_STUDIO_IBMMQ_SINK_SSLCIPHERSUITE=TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
CAMEL_KAMELET_KAMELET_STUDIO_IBMMQ_SINK_TRUSTSTOREPATH=/home/pratyush/software/jbang-eip-builder/infra-setup/certs/ibmmq/clienttrust.p12
CAMEL_KAMELET_KAMELET_STUDIO_IBMMQ_SINK_TRUSTSTOREPASSWORD=mqpassword
CAMEL_KAMELET_KAMELET_STUDIO_IBMMQ_SINK_KEYSTOREPATH=/home/pratyush/software/jbang-eip-builder/infra-setup/certs/ibmmq/clientkey.p12
CAMEL_KAMELET_KAMELET_STUDIO_IBMMQ_SINK_KEYSTOREPASSWORD=clientpassword

# Camel Kamelet Studio IBM MQ Source Properties
CAMEL_KAMELET_KAMELET_STUDIO_IBMMQ_SOURCE_HOSTNAME=localhost
CAMEL_KAMELET_KAMELET_STUDIO_IBMMQ_SOURCE_PORT=1414
CAMEL_KAMELET_KAMELET_STUDIO_IBMMQ_SOURCE_QUEUEMANAGER=QM1
CAMEL_KAMELET_KAMELET_STUDIO_IBMMQ_SOURCE_CHANNEL=DEV.ADMIN.SVRCONN
CAMEL_KAMELET_KAMELET_STUDIO_IBMMQ_SOURCE_SSLCIPHERSUITE=TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384
CAMEL_KAMELET_KAMELET_STUDIO_IBMMQ_SOURCE_TRUSTSTOREPATH=/home/pratyush/software/jbang-eip-builder/infra-setup/certs/ibmmq/clienttrust.p12
CAMEL_KAMELET_KAMELET_STUDIO_IBMMQ_SOURCE_TRUSTSTOREPASSWORD=mqpassword
CAMEL_KAMELET_KAMELET_STUDIO_IBMMQ_SOURCE_KEYSTOREPATH=/home/pratyush/software/jbang-eip-builder/infra-setup/certs/ibmmq/clientkey.p12
CAMEL_KAMELET_KAMELET_STUDIO_IBMMQ_SOURCE_KEYSTOREPASSWORD=clientpassword
```

### 2. Application Properties Bridge (`application.yaml`)
We only need to map endpoint mappings and the destination queue name. No connection parameters are defined here:
```yaml
ibmmq:
  queue: "${IBMMQ_QUEUE:IBMMQ.INBOUND.Q}"

# Map endpoints to custom Kamelet definitions
ibmmq-source: "kamelet:kamelet-studio-ibmmq-source"
ibmmq-sink: "kamelet:kamelet-studio-ibmmq-sink"
```

---

## 3. Dynamic Camel Route & Kamelet Integration

### 1. Decluttered Route Configuration (`ibmmq-mtls-test.yaml`)
By offloading connection properties to the environment registry, the route definitions are clean and only require the queue name:
```yaml
- route:
    id: ibmmq-producer-route
    from:
      uri: timer:tick
      parameters:
        period: 5000
    steps:
      - setBody:
          constant: "Hello from dynamic IBM MQ mTLS Route!"
      - log: "Sending message to IBM MQ: ${body}"
      - to: "{{ibmmq-sink}}?queuename={{ibmmq.queue}}"

- route:
    id: ibmmq-consumer-route
    from:
      uri: "{{ibmmq-source}}?queuename={{ibmmq.queue}}"
    steps:
      - log: "Successfully received message from IBM MQ: ${body}"
```

### 2. Kamelet Connectors
Under the hood, `kamelet-studio-ibmmq-sink` and `kamelet-studio-ibmmq-source` bind a custom SSL Socket Factory (`KameletStudioSslSocketFactory`) which automatically resolves the environment-derived properties at runtime:

```yaml
# Inside the Kamelet definition:
template:
  beans:
    - name: sslFactory
      type: "#class:com.tessera.kameletstudio.core.lib.crypto.KameletStudioSslSocketFactory"
      properties:
        trustStorePath: '{{truststorepath}}'
        trustStorePassword: '{{truststorepassword}}'
        keyStorePath: '{{keystorepath}}'
        keyStorePassword: '{{keystorepassword}}'
    - name: mqConnectionFactory
      type: "#class:com.ibm.mq.jakarta.jms.MQConnectionFactory"
      properties:
        hostName: '{{hostname}}'
        port: '{{port}}'
        queueManager: '{{queuemanager}}'
        channel: '{{channel}}'
        sslCipherSuite: '{{sslciphersuite}}'
        sslSocketFactory: '#bean:{{sslFactory}}'
```

---

## 4. Key Troubleshooting Points

- **`GSK_ERROR_KEYRING_OPEN_FAILED` (gsk_environment_init / code 202)**: Ensure the Queue Manager has a valid `.p12.sth` stash file corresponding to the key repository file `.p12`. If `SSLKEYR` is `/path/to/key.p12`, the stash file **must** be `/path/to/key.p12.sth`.
- **`JmsSecurityException` / `AMQ5540E`**: When mTLS client certificates are mapped to administrative IDs, connection authentication checks must be disabled on the Queue Manager:
  ```text
  ALTER AUTHINFO(DEV.AUTHINFO) AUTHTYPE(IDPWOS) CHCKCLNT(NONE)
  REFRESH SECURITY TYPE(CONNAUTH)
  ```
- **Zero Configuration File Footprint**: In modern microservices layouts, connection parameters do not need to be registered in config files. Defining `CAMEL_KAMELET_<KAMELET_ID_IN_UPPERCASE>_<PROPERTY_NAME_IN_UPPERCASE>` environment variables allows Camel to configure the Kamelets implicitly.

