# quarkus-app

This project uses Quarkus, the Supersonic Subatomic Java Framework.

If you want to learn more about Quarkus, please visit its website: https://quarkus.io/ .

## Configuration & Environment Setup

This project externalizes configuration values (such as connection URLs, passwords, and TLS paths) using environment variables.

### Local Development Setup
1. Create a `.env` file in the `quarkus-app` directory (this file is automatically ignored by `.gitignore` to keep credentials secure):
   ```env
   # MongoDB Connection
   QUARKUS_MONGODB_CONNECTION_STRING=mongodb://dbuser:dbpassword@localhost:27017/testdb?tls=true&authSource=admin
   QUARKUS_MONGODB_TLS=true
   QUARKUS_MONGODB_TLS_CONFIGURATION_NAME=mongodb

   # Truststore Configuration
   QUARKUS_TLS_MONGODB_TRUST_STORE_JKS_PATH=/home/pratyush/software/jbang-eip-builder/infra-setup/certs/mongodb/cacert.jks
   QUARKUS_TLS_MONGODB_TRUST_STORE_JKS_PASSWORD=mongopassword
   QUARKUS_TLS_MONGODB_HOSTNAME_VERIFICATION_ALGORITHM=NONE
   ```
2. When starting dev mode (`./gradlew quarkusDev`), Quarkus will automatically detect this `.env` file and load its variables.

### Deploying to Higher Environments (UAT / Production)
When moving the application to higher environments:
* **Do not deploy the `.env` file.**
* Declare the **same environment variables** in your Kubernetes/OpenShift deployment manifest using **ConfigMaps** (for non-sensitive configuration) and **Secrets** (for connection strings and truststore passwords). Quarkus automatically maps these environment variables to the correct properties during container startup.

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:
```shell script
./gradlew quarkusDev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:
```shell script
./gradlew build
```
It produces the `quarkus-run.jar` file in the `build/quarkus-app/` directory.
Be aware that it’s not an _über-jar_ as the dependencies are copied into the `build/quarkus-app/lib/` directory.

The application is now runnable using `java -jar build/quarkus-app/quarkus-run.jar`.

If you want to build an _über-jar_, execute the following command:
```shell script
./gradlew build -Dquarkus.package.type=uber-jar
```

The application, packaged as an _über-jar_, is now runnable using `java -jar build/*-runner.jar`.

## Creating a native executable

You can create a native executable using: 
```shell script
./gradlew build -Dquarkus.package.type=native
```

Or, if you don't have GraalVM installed, you can run the native executable build in a container using: 
```shell script
./gradlew build -Dquarkus.package.type=native -Dquarkus.native.container-build=true
```

You can then execute your native executable with: `./build/quarkus-app-1.0.0-SNAPSHOT-runner`

If you want to learn more about building native executables, please consult https://quarkus.io/guides/gradle-tooling.

## Related Guides

- REST ([guide](https://quarkus.io/guides/rest)): Build RESTful web services and APIs using Jakarta REST (formerly JAX-RS)

## Provided Code

### REST

Easily start your REST Web Services

[Related guide section...](https://quarkus.io/guides/getting-started-reactive#reactive-jax-rs-resources)
