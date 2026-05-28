package com.routebuilder.faker.generators;

@FunctionalInterface
public interface DataGenerator {
    String generate(String... parameters);
}
