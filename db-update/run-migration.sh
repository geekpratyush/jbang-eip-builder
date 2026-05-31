#!/usr/bin/env bash
set -e

# Resolve and copy the latest runner JAR from the Maven repository
LOCAL_JAR="./liquibase-migrator-runner.jar"
echo "--- Fetching/Copying latest runner JAR from Maven Repository ---"
mvn dependency:copy \
    -Dartifact=com.routebuilder:liquibase-migrator:1.0.0:jar:runner \
    -DoutputDirectory=. \
    -Dmdep.stripVersion=true \
    -q

if [ ! -f "$LOCAL_JAR" ]; then
    echo "ERROR: liquibase-migrator-runner.jar not found after fetching!"
    exit 1
fi

# Load environment variables from .env file
if [ -f .env ]; then
    echo "--- Loading configuration parameters from .env ---"
    while IFS= read -r line || [ -n "$line" ]; do
        # Strip comments and empty lines
        if [[ ! "$line" =~ ^# ]] && [[ ! -z "$line" ]]; then
            export "$line"
        fi
    done < .env
else
    echo "ERROR: .env configuration file not found!"
    exit 1
fi

echo "=================================================="
echo "Executing Standalone Database Update Script"
echo "=================================================="
echo "DB_URL: $DB_URL"
echo "CHANGELOG: $CHANGELOG_FILE"
echo "SEARCH_PATH: $SEARCH_PATH"
echo "=================================================="

# Run migration with truststore configuration
java -Djavax.net.ssl.trustStore="$SSL_TRUSTSTORE_PATH" \
     -Djavax.net.ssl.trustStorePassword="$SSL_TRUSTSTORE_PASSWORD" \
     -jar "$LOCAL_JAR"
