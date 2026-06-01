package com.tessera.faker.generators;

@FunctionalInterface
public interface DataGenerator {
    String generate(String... parameters);
}
