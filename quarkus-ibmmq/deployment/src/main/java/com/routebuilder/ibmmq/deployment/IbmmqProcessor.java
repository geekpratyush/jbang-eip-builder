package com.routebuilder.ibmmq.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;

class IbmmqProcessor {

    private static final String FEATURE = "ibmmq";

    @BuildStep
    io.quarkus.deployment.builditem.FeatureBuildItem feature() {
        return new io.quarkus.deployment.builditem.FeatureBuildItem(FEATURE);
    }

    @BuildStep
    io.quarkus.arc.deployment.AdditionalBeanBuildItem registerBeans() {
        return io.quarkus.arc.deployment.AdditionalBeanBuildItem.builder()
                .addBeanClass("com.routebuilder.ibmmq.runtime.TransactionManagerBinder")
                .setUnremovable()
                .build();
    }

    @BuildStep
    ReflectiveClassBuildItem registerReflectiveClasses() {
        return ReflectiveClassBuildItem.builder(
                "com.routebuilder.kameletstudio.core.lib.crypto.KameletStudioSslSocketFactory",
                "com.ibm.mq.jakarta.jms.MQConnectionFactory",
                "com.ibm.mq.jakarta.jms.MQXAConnectionFactory",
                "org.messaginghub.pooled.jms.JmsPoolConnectionFactory",
                "org.messaginghub.pooled.jms.JmsPoolXAConnectionFactory"
        ).methods().fields().build();
    }
}
