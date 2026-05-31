# 🔌 Enterprise Service Connection Guide

Use the connection strings, credentials, and configuration commands below to connect external client tools (like MongoDB Compass, IBM MQ Explorer, and Kafka CLI tools) to the running infrastructure.

---

## 🍃 MongoDB

*   **Username**: `dbuser`
*   **Password**: `dbpassword`
*   **Auth Database**: `admin`

### 1. MongoDB Compass Connection URI (Username/Password + TLS CA Verification)
```text
mongodb://dbuser:dbpassword@localhost:27017/testdb?tls=true&tlsCAFile=/home/pratyush/software/jbang-eip-builder/infra-setup/certs/mongodb/ca.crt&tlsAllowInvalidHostnames=true&authSource=admin
```

### 2. MongoDB Shell (`mongosh`) Command
```bash
mongosh "mongodb://dbuser:dbpassword@localhost:27017/testdb?tls=true&tlsCAFile=/home/pratyush/software/jbang-eip-builder/infra-setup/certs/mongodb/ca.crt&tlsAllowInvalidHostnames=true&authSource=admin"
```

### 3. Java Truststore (JKS) Configuration (for Java/Quarkus client)
*   **Truststore (JKS)**: `/home/pratyush/software/jbang-eip-builder/infra-setup/certs/mongodb/cacert.jks`
*   **Truststore Password**: `mongopassword`



---

## ✉️ IBM MQ (mTLS)

To connect via **IBM MQ Explorer** or another client:

*   **Queue Manager**: `QM1`
*   **Host**: `localhost`
*   **Port**: `1414`
*   **Channel**: `DEV.ADMIN.SVRCONN`
*   **CipherSpec**: `TLS_RSA_WITH_AES_256_CBC_SHA256`
*   **SSL Keystore (PKCS12)**: `certs/ibmmq/clientkey.p12` (Password: `clientpassword`)
*   **SSL Truststore (PKCS12)**: `certs/ibmmq/clienttrust.p12` (Password: `mqpassword`)

---

## ⚙️ Confluent Kafka (mTLS)

To connect via standard clients or GUI dashboards (e.g. Offset Explorer, AKHQ, Conduktor):

*   **Bootstrap Server**: `localhost:19093`
*   **Security Protocol**: `SSL` (mTLS)
*   **Key Manager Keystore (PKCS12)**: `certs/confluent-kafka/kafka.client.keystore.p12` (Password: `clientpassword`)
*   **Trust Manager Truststore (PKCS12)**: `certs/confluent-kafka/kafka.broker.truststore.p12` (Password: `kafkapassword`)

---

## 🛡️ Apache Kafka (Kerberos / SASL_PLAINTEXT)

To connect via CLI clients or `kcat` (formerly `kafkacat`):

*   **Bootstrap Server**: `localhost:9094`
*   **Security Protocol**: `SASL_PLAINTEXT`
*   **SASL Mechanism**: `GSSAPI`
*   **Kerberos Service Name**: `kafka`

### 1. Verification with `kcat`
```bash
kcat -b localhost:9094 \
  -X security.protocol=SASL_PLAINTEXT \
  -X sasl.mechanisms=GSSAPI \
  -X sasl.kerberos.service.name=kafka \
  -X sasl.kerberos.keytab=/home/pratyush/software/jbang-eip-builder/infra-setup/certs/apache-kafka/client.keytab \
  -X sasl.kerberos.principal=client@EXAMPLE.COM \
  -L
```

### 2. JVM Client Configuration (Java/Quarkus)
Add to system properties or startup command:
```text
-Djava.security.auth.login.config=/home/pratyush/software/jbang-eip-builder/infra-setup/certs/apache-kafka/kafka_server_jaas.conf
-Djava.security.krb5.conf=/home/pratyush/software/jbang-eip-builder/infra-setup/krb5.conf
```
And use the JAAS Client section principal `client@EXAMPLE.COM` referencing `client.keytab`.
