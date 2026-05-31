#!/bin/bash
set -e

# Create directories
mkdir -p certs/ibmmq certs/confluent-kafka certs/apache-kafka certs/mongodb certs/kerberos

echo "=== Generating shared Root CA ==="
openssl req -new -x509 -keyout certs/ca.key -out certs/ca.crt -days 365 -nodes \
  -subj "/C=US/ST=State/L=City/O=Enterprise/OU=IT/CN=EnterpriseCA"

echo "=== Generating MongoDB Certificates ==="
openssl genrsa -out certs/mongodb/mongodb.key 2048
openssl req -new -key certs/mongodb/mongodb.key -out certs/mongodb/mongodb.csr -subj "/CN=localhost"
openssl x509 -req -in certs/mongodb/mongodb.csr -CA certs/ca.crt -CAkey certs/ca.key \
  -CAcreateserial -out certs/mongodb/mongodb.crt -days 365
# Combine key and cert for MongoDB's required PEM file
cat certs/mongodb/mongodb.crt certs/mongodb/mongodb.key > certs/mongodb/mongodb.pem
cp certs/ca.crt certs/mongodb/ca.crt

# Generate PKCS12 Keystore for Client Cert and Key
openssl pkcs12 -export -in certs/mongodb/mongodb.crt -inkey certs/mongodb/mongodb.key \
  -out certs/mongodb/mongodb.keystore.p12 -name "mongodbclient" -passout pass:mongopassword -certfile certs/ca.crt

# Convert PKCS12 Keystore to JKS Keystore
keytool -importkeystore -srckeystore certs/mongodb/mongodb.keystore.p12 -srcstoretype PKCS12 \
  -srcstorepass mongopassword -destkeystore certs/mongodb/mongodb.keystore.jks -deststoretype JKS -deststorepass mongopassword -noprompt

# Generate JKS Truststore containing the Root CA Cert
keytool -importcert -trustcacerts -noprompt -alias mongoca -file certs/ca.crt \
  -keystore certs/mongodb/cacert.jks -storepass mongopassword



echo "=== Generating IBM MQ Certificates ==="
# MQ Server key and cert
openssl genrsa -out certs/ibmmq/mqserver.key 2048
openssl req -new -key certs/ibmmq/mqserver.key -out certs/ibmmq/mqserver.csr -subj "/CN=QM1"
openssl x509 -req -in certs/ibmmq/mqserver.csr -CA certs/ca.crt -CAkey certs/ca.key \
  -CAcreateserial -out certs/ibmmq/mqserver.crt -days 365

# Server PKCS12 Keystore containing Server Cert and CA Cert (IBM MQ expects label named 'ibmwebspheremq<qmgr_name_lowercase>')
openssl pkcs12 -export -out certs/ibmmq/key.p12 -inkey certs/ibmmq/mqserver.key \
  -in certs/ibmmq/mqserver.crt -certfile certs/ca.crt -passout pass:mqpassword -name "ibmwebspheremqqm1"

# Client Cert and Keystore
openssl genrsa -out certs/ibmmq/mqclient.key 2048
openssl req -new -key certs/ibmmq/mqclient.key -out certs/ibmmq/mqclient.csr -subj "/CN=mqclient"
openssl x509 -req -in certs/ibmmq/mqclient.csr -CA certs/ca.crt -CAkey certs/ca.key \
  -CAcreateserial -out certs/ibmmq/mqclient.crt -days 365

openssl pkcs12 -export -out certs/ibmmq/clientkey.p12 -inkey certs/ibmmq/mqclient.key \
  -in certs/ibmmq/mqclient.crt -certfile certs/ca.crt -passout pass:clientpassword -name "clientkey"

# Client Truststore (contains CA certificate)
keytool -importcert -trustcacerts -noprompt -alias CARoot -file certs/ca.crt -keystore certs/ibmmq/clienttrust.p12 -storetype PKCS12 -storepass mqpassword

echo "=== Generating Confluent Kafka Certificates ==="
# Create Broker Keystore (PKCS12)
keytool -keystore certs/confluent-kafka/kafka.broker.keystore.p12 -alias localhost -validity 365 -genkey \
  -keyalg RSA -ext SAN=dns:localhost,ip:127.0.0.1 -storetype PKCS12 -storepass kafkapassword -keypass kafkapassword \
  -dname "CN=localhost,OU=Dev,O=Enterprise,L=City,C=US"

# Export CSR
keytool -keystore certs/confluent-kafka/kafka.broker.keystore.p12 -alias localhost -certreq \
  -file certs/confluent-kafka/broker.csr -storepass kafkapassword -storetype PKCS12

# Sign Broker CSR with CA
openssl x509 -req -CA certs/ca.crt -CAkey certs/ca.key -in certs/confluent-kafka/broker.csr \
  -out certs/confluent-kafka/broker-signed.crt -days 365 -CAcreateserial

# Import CA cert into broker keystore
keytool -keystore certs/confluent-kafka/kafka.broker.keystore.p12 -alias CARoot -import -noprompt \
  -file certs/ca.crt -storepass kafkapassword -storetype PKCS12

# Import signed certificate into broker keystore
keytool -keystore certs/confluent-kafka/kafka.broker.keystore.p12 -alias localhost -import \
  -file certs/confluent-kafka/broker-signed.crt -storepass kafkapassword -storetype PKCS12

# Create Broker Truststore
keytool -keystore certs/confluent-kafka/kafka.broker.truststore.p12 -alias CARoot -import -noprompt \
  -file certs/ca.crt -storepass kafkapassword -storetype PKCS12

# Create Client Keystore (for mTLS client)
keytool -keystore certs/confluent-kafka/kafka.client.keystore.p12 -alias client -validity 365 -genkey \
  -keyalg RSA -storetype PKCS12 -storepass clientpassword -keypass clientpassword \
  -dname "CN=client,OU=Dev,O=Enterprise,L=City,C=US"

# Export Client CSR
keytool -keystore certs/confluent-kafka/kafka.client.keystore.p12 -alias client -certreq \
  -file certs/confluent-kafka/client.csr -storepass clientpassword -storetype PKCS12

# Sign client CSR
openssl x509 -req -CA certs/ca.crt -CAkey certs/ca.key -in certs/confluent-kafka/client.csr \
  -out certs/confluent-kafka/client-signed.crt -days 365 -CAcreateserial

# Import CA cert into client keystore
keytool -keystore certs/confluent-kafka/kafka.client.keystore.p12 -alias CARoot -import -noprompt \
  -file certs/ca.crt -storepass clientpassword -storetype PKCS12

# Import signed client cert into client keystore
keytool -keystore certs/confluent-kafka/kafka.client.keystore.p12 -alias client -import \
  -file certs/confluent-kafka/client-signed.crt -storepass clientpassword -storetype PKCS12

# Copy CA certificate to confluent-kafka
cp certs/ca.crt certs/confluent-kafka/ca.crt

echo "=== All Certificates Generated Successfully! ==="
