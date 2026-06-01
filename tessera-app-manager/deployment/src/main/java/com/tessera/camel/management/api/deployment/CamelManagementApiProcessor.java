package com.tessera.camel.management.api.deployment;

import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import com.tessera.camel.management.api.runtime.ManagementResource;

class CamelManagementApiProcessor {

    private static final String FEATURE = "camel-management-api";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem registerBeans() {
        return AdditionalBeanBuildItem.builder()
                .addBeanClass(ManagementResource.class)
                .addBeanClass("com.tessera.camel.management.api.runtime.DynamicRouteLoader")
                .addBeanClass("com.tessera.camel.management.api.runtime.ManagementRouteFilter")
                .setUnremovable()
                .build();
    }

    @BuildStep
    io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem registerReflectiveClasses() {
        return io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem.builder(
                "com.tessera.camel.management.api.runtime.ManagementResource"
        ).methods().fields().build();
    }
}
