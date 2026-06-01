# Standalone Liquibase Migrator

This directory contains a standalone, zero-docker database migration utility using the Liquibase Java API. It compiles into a single "fat JAR" containing all dependencies, including database drivers and the Liquibase MongoDB extension.

---

## 1. How to Compile & Package

Compile and package the standalone fat JAR runner using Maven:

```bash
mvn clean package -DskipTests
```

This generates the executable JAR:
`target/liquibase-migrator-1.0.0-runner.jar`

---

## 2. Configuration Options (Environment Variables)

The migrator runner is configured strictly via environment variables.

| Environment Variable | Description | Default Value |
| :--- | :--- | :--- |
| `DB_URL` | Target database connection URI (JDBC for SQL or native MongoDB URI) | `jdbc:h2:mem:testdb` |
| `DB_USERNAME` | Username for JDBC connections (ignored for MongoDB) | `null` (falls back to `sa` for H2) |
| `DB_PASSWORD` | Password for JDBC connections (ignored for MongoDB) | `null` (falls back to `""` for H2) |
| `CHANGELOG_FILE` | Path to the master changelog file to execute | `changelog-master.xml` |
| `SEARCH_PATH` | The local/classpath directory where changelogs and files reside | `.` |

---

## 3. Local Run Examples

### A. MongoDB Migration
To connect to a TLS-enabled MongoDB instance locally:

```bash
export DB_URL="mongodb://dbuser:dbpassword@localhost:27017/testdb?authSource=admin&tls=true"
export CHANGELOG_FILE="changelog-export-mongodb.xml"
export SEARCH_PATH="."

# Pass target truststore configurations if using self-signed certs
java -Djavax.net.ssl.trustStore=/path/to/cacert.jks \
     -Djavax.net.ssl.trustStorePassword=mongopassword \
     -jar target/liquibase-migrator-1.0.0-runner.jar
```

### B. Relational Database Migration (PostgreSQL / Oracle)
To connect to a standard SQL database:

```bash
export DB_URL="jdbc:postgresql://localhost:5432/testdb"
export DB_USERNAME="dbuser"
export DB_PASSWORD="dbpassword"
export CHANGELOG_FILE="changelog-export-sql.xml"
export SEARCH_PATH="."

java -jar target/liquibase-migrator-1.0.0-runner.jar
```

---

## 4. Deploying to Artifactory

You can deploy this artifact directly to your internal company Artifactory repository:

```bash
mvn deploy
```
*(Ensure your global `~/.m2/settings.xml` has credentials configured for the server mapped in your deployment settings).*
