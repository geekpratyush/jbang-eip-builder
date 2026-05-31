#!/bin/bash
set -e

# Change to the script's directory
cd "$(dirname "$0")"

# Ensure all cert permissions are readable by container users (e.g. mqm: 888, kafka: 1000)
chmod -R 755 certs/
find certs/ -type f -exec chmod 644 {} \;

# Prompt user via whiptail checklist
CHOICES=$(whiptail --title "Infrastructure Selector" --checklist \
"Choose which services to start (Space to toggle, Enter to select):" 16 65 4 \
"ibmmq" "IBM MQ with mTLS" ON \
"apache-kafka" "Apache Kafka with Kerberos" ON \
"confluent-kafka" "Confluent Kafka with mTLS" ON \
"mongodb" "MongoDB with TLS CA" ON 3>&1 1>&2 2>&3)

if [ -z "$CHOICES" ]; then
    echo "No services selected or operation cancelled."
    exit 0
fi

# Clean up quotes from whiptail output
CHOICES=$(echo "$CHOICES" | tr -d '"')

SERVICES_TO_START=""
START_APACHE_KAFKA=false

for choice in $CHOICES; do
    case "$choice" in
        ibmmq)
            SERVICES_TO_START="$SERVICES_TO_START ibmmq"
            ;;
        apache-kafka)
            SERVICES_TO_START="$SERVICES_TO_START zookeeper kerberos-kdc apache-kafka"
            START_APACHE_KAFKA=true
            ;;
        confluent-kafka)
            SERVICES_TO_START="$SERVICES_TO_START zookeeper confluent-kafka"
            ;;
        mongodb)
            SERVICES_TO_START="$SERVICES_TO_START mongodb"
            ;;
    esac
done

echo "Starting selected infrastructure: $SERVICES_TO_START..."
./docker-compose up -d $SERVICES_TO_START

if [[ "$SERVICES_TO_START" == *"ibmmq"* ]]; then
    echo "=== Tuning IBM MQ Security Configuration ==="
    # Give the container a moment to start
    sleep 5
    # Ensure security is refreshed to pick up the mq-init.mqsc settings
    echo "REFRESH SECURITY TYPE(ALL)
END" | docker exec -i -u 1001 ibmmq runmqsc QM1 2>/dev/null || true
fi

if [ "$START_APACHE_KAFKA" = true ]; then
    echo "=== Configuring Kerberos Principals and Keytabs ==="
    echo "Waiting for Kerberos KDC to boot..."
    sleep 6

    echo "Creating principal: kafka/localhost@EXAMPLE.COM"
    docker exec kerberos-kdc kadmin.local -q "addprinc -randkey kafka/localhost@EXAMPLE.COM" || true
    echo "Creating principal: client@EXAMPLE.COM"
    docker exec kerberos-kdc kadmin.local -q "addprinc -randkey client@EXAMPLE.COM" || true

    echo "Exporting keytabs..."
    docker exec kerberos-kdc kadmin.local -q "xst -k /var/keytabs/kafka.keytab kafka/localhost@EXAMPLE.COM" || true
    docker exec kerberos-kdc kadmin.local -q "xst -k /var/keytabs/client.keytab client@EXAMPLE.COM" || true

    # Fix permissions via the KDC container so they are readable on the host and by apache-kafka container
    docker exec kerberos-kdc chown -R 1000:1000 /var/keytabs || true
    docker exec kerberos-kdc chmod 644 /var/keytabs/kafka.keytab /var/keytabs/client.keytab || true

    # Copy the JAAS configuration into the mapped directory
    cp kafka_server_jaas.conf certs/apache-kafka/kafka_server_jaas.conf || true
    chmod 644 certs/apache-kafka/kafka_server_jaas.conf || true

    echo "Restarting apache-kafka to apply Kerberos keytabs..."
    ./docker-compose restart apache-kafka
fi

echo "=== Selected Infrastructure is Running! ==="
./docker-compose ps
