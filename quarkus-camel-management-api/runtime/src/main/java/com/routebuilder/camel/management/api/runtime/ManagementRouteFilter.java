package com.routebuilder.camel.management.api.runtime;

import io.quarkus.arc.Arc;
import io.quarkus.arc.ArcContainer;
import io.vertx.ext.web.Router;
import jakarta.enterprise.event.Observes;

public class ManagementRouteFilter {

    public void setupRouteFilter(@Observes Router router) {
        router.route().order(-100).handler(ctx -> {
            ArcContainer container = Arc.container();
            if (container == null || !container.isRunning()) {
                ctx.response().setStatusCode(503).end("Service is shutting down");
                return;
            }
            ctx.next();
        });
    }
}
