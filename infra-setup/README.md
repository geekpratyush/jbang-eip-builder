# EIP Integration Testing Infrastructure

This folder contains a fully-dockerized local infrastructure sandbox containing IBM MQ (mTLS), Apache Kafka (Kerberos), Confluent Kafka (mTLS), and MongoDB (TLS CA-cert) with pre-generated certs and automatic keytab generation.

---

## 🛠️ Expose Services & Ports

| Service | Port | Security Protocol | Credentials / Key |
| :--- | :--- | :--- | :--- |
| **IBM MQ** | `1414` | mTLS (SSL) | Keystore: `certs/ibmmq/clientkey.p12` (pass: `clientpassword`) <br>Truststore: `certs/ibmmq/clienttrust.p12` (pass: `mqpassword`) |
| **Confluent Kafka** | `19093` | mTLS (SSL) | Keystore: `certs/confluent-kafka/kafka.client.keystore.p12` (pass: `clientpassword`) <br>Truststore: `certs/confluent-kafka/kafka.broker.truststore.p12` (pass: `kafkapassword`) |
| **Apache Kafka** | `9094` | SASL_PLAINTEXT (Kerberos) | Principal: `client@EXAMPLE.COM` <br>Keytab: `certs/apache-kafka/client.keytab` <br>Krb5 Config: `krb5.conf` |
| **MongoDB** | `27017` | TLS/SSL | CA Certificate: `certs/mongodb/ca.crt` |
| **Kerberos KDC** | `88`, `749` | Kerberos v5 KDC | Realm: `EXAMPLE.COM` |

---

## 🚀 How to Start the Infrastructure

You can choose to start individual services, multiple services, or all of them.

### On Linux / macOS (Bash)
Run the interactive startup script:
```bash
./start-infra.sh
```
*(Uses a terminal-based checkbox menu. Space to toggle, Enter to confirm.)*

### On Windows / Linux / macOS (PowerShell)
Run the interactive PowerShell script:
```powershell
.\start-infra.ps1
```
*(Lists numbered services and accepts comma-separated inputs, e.g. `1,3` to start IBM MQ and Confluent.)*

---

## 🛑 How to Stop & Clean Up Ports

Stopping the services tears down all running containers, removes Docker volumes (to clear out any database state), and terminates any remaining host processes clashing on the service ports.

### On Linux / macOS (Bash)
```bash
./stop-infra.sh
```

### On Windows / Linux / macOS (PowerShell)
```powershell
.\stop-infra.ps1
```

---

## 🔑 Regeneration of Certificates

To regenerate all keystores, truststores, and PEM certificates:
```bash
./generate-certs.sh
```
*(Note: This is automatically executed on the first setup and populates the `certs/` directories)*
