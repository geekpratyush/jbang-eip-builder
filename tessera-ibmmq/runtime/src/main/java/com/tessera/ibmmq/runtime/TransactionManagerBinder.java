package com.tessera.ibmmq.runtime;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import jakarta.transaction.TransactionManager;
import org.apache.camel.CamelContext;
import org.apache.camel.spi.CamelEvent.CamelContextStartingEvent;

/**
 * Binds the JTA TransactionManager into Camel Registry as 'jtaTxManager'
 * upon Camel context initialization.
 */
@ApplicationScoped
public class TransactionManagerBinder {

    @Inject
    TransactionManager transactionManager;

    public void onContextStarting(@Observes CamelContextStartingEvent event) {
        CamelContext context = event.getContext();
        context.getRegistry().bind("jtaTxManager", transactionManager);
        System.out.println("Successfully bound JTA TransactionManager into Camel Registry under the name 'jtaTxManager'");
    }
}
