plugins {
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDepMgmt)
    java
}

dependencyManagement {
    imports {
        mavenBom(libs.spring.boot.bom.get().toString())
        mavenBom(libs.spring.cloud.bom.get().toString())
        mavenBom(libs.testcontainers.bom.get().toString())
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

dependencies {
    // Lombok
    compileOnly(libs.lombok.lib)
    annotationProcessor(libs.lombok.lib)

    // Spring Cloud Gateway (pulls in WebFlux + Netty)
    implementation(libs.spring.cloud.gateway)

    // Security & Redis
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.data.redis.reactive)

    // Circuit breaker (reactive Resilience4j)
    implementation(libs.spring.cloud.circuitbreaker.reactor)

    // JWT
    implementation(libs.jjwt.api)
    runtimeOnly(libs.jjwt.impl)
    runtimeOnly(libs.jjwt.jackson)

    // Actuator / Observability
    implementation(libs.spring.boot.starter.actuator)
    runtimeOnly(libs.micrometer.prometheus)
    implementation(libs.logstash.logback.encoder)

    // Test
    testCompileOnly(libs.lombok.lib)
    testAnnotationProcessor(libs.lombok.lib)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.testcontainers.junit)
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading")
}
