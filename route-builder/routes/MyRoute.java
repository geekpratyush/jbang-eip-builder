package com.example;

import org.apache.camel.builder.RouteBuilder;

public class MyRoute extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("direct:java")
            .log("Java Route Triggered");
    }
}
