# Camel gRPC Examples

This folder contains a fully defined gRPC Server and Client using the `camel-grpc` component.

## Important Note Regarding JBang

Unlike REST HTTP APIs, **gRPC** uses a strict binary format called Protocol Buffers. It relies on a contract file (`ping.proto`). 

In the Java ecosystem, gRPC **requires** that the `.proto` file is compiled into native Java Classes (Stubs) before the application runs. 

Because `camel-jbang` executes raw YAML scripts without a compilation step, **you cannot run these gRPC routes directly in the Builder by clicking Play**. If you try to run them, Camel will crash complaining that `com.tessera.grpc.ping.PingService` Class is missing!

### How to use these files in the Real World:
1. Create a standard Java Gradle/Maven project (like `tessera-app`).
2. Add the `protobuf-gradle-plugin` to compile the `ping.proto` file into Java classes during the build.
3. Include the compiled JAR in the classpath when running Camel, or just run the Camel application directly inside the Spring Boot/Quarkus container.

These YAML files are provided as an **architectural reference** to see how elegant a gRPC Server and Client look inside Camel!
