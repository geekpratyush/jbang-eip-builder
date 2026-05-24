package com.enterprise.services;

import org.apache.camel.CamelContext;

/**
 * Service utilizing external Camel dependencies.
 */
public class ConnectivityService {
    private CamelContext context;

    public void checkStatus() {
        System.out.println("Context status: " + context.getStatus());
    }
}
