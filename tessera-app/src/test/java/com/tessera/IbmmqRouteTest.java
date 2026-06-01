package com.tessera;

import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
public class IbmmqRouteTest {

    static {
        System.setProperty("com.ibm.mq.cfg.useIBMCipherMappings", "false");
        // System.setProperty("javax.net.debug", "ssl,handshake");
    }

    @Inject
    CamelContext context;

    @Inject
    ProducerTemplate producerTemplate;

    @Test
    public void testIbmMqKamelets() throws Exception {
        assertNotNull(context);

        // Define routes using Java DSL referencing the Kamelets
        context.addRoutes(new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // Route 1: Send to MQ
                from("direct:sendToMq")
                    .to("kamelet:ibmmq-sink?" +
                        "queuename=IBMMQ.INBOUND.Q" +
                        "&hostname=localhost" +
                        "&port=1414" +
                        "&queuemanager=QM1" +
                        "&channel=DEV.ADMIN.SVRCONN" +
                        "&sslciphersuite=TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384" +
                        "&truststorepath=/home/pratyush/software/jbang-eip-builder/infra-setup/certs/ibmmq/clienttrust.p12" +
                        "&truststorepassword=mqpassword" +
                        "&keystorepath=/home/pratyush/software/jbang-eip-builder/infra-setup/certs/ibmmq/clientkey.p12" +
                        "&keystorepassword=clientpassword");

                // Route 2: Consume from MQ
                from("kamelet:ibmmq-source?" +
                        "queuename=IBMMQ.INBOUND.Q" +
                        "&hostname=localhost" +
                        "&port=1414" +
                        "&queuemanager=QM1" +
                        "&channel=DEV.ADMIN.SVRCONN" +
                        "&sslciphersuite=TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384" +
                        "&truststorepath=/home/pratyush/software/jbang-eip-builder/infra-setup/certs/ibmmq/clienttrust.p12" +
                        "&truststorepassword=mqpassword" +
                        "&keystorepath=/home/pratyush/software/jbang-eip-builder/infra-setup/certs/ibmmq/clientkey.p12" +
                        "&keystorepassword=clientpassword")
                    .to("mock:result");
            }
        });

        MockEndpoint mock = context.getEndpoint("mock:result", MockEndpoint.class);
        mock.expectedBodiesReceived("Hello IBM MQ over SSL!");

        producerTemplate.sendBody("direct:sendToMq", "Hello IBM MQ over SSL!");

        mock.assertIsSatisfied(15, TimeUnit.SECONDS);
    }
}
