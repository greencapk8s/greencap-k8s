import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    id("org.springframework.boot") version "3.3.0"
    id("io.spring.dependency-management") version "1.1.5"
    id("com.vaadin") version "24.4.4"
}

group = "io.greencap"
version = "0.1.76-rc"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.vaadin.com/vaadin-addons") }
}

// BOM do Vaadin — gerencia versões dos sub-módulos
dependencyManagement {
    imports {
        mavenBom("com.vaadin:vaadin-bom:24.4.4")
    }
}

dependencies {
    // ── Spring Boot core ─────────────────────────────────────────
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // ── Vaadin Flow ───────────────────────────────────────────────
    implementation("com.vaadin:vaadin-spring-boot-starter")

    // ── Banco de dados ────────────────────────────────────────────
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")

    // ── Kubernetes / Fabric8 ──────────────────────────────────────
    implementation("io.fabric8:kubernetes-client:6.13.1")
    implementation("io.fabric8:openshift-client:6.13.1")   // OKD + OpenShift

    // ── Segurança (encriptação do kubeconfig) ─────────────────────
    implementation("org.springframework.security:spring-security-crypto")

    // ── Utilitários ───────────────────────────────────────────────
    implementation("org.apache.commons:commons-lang3:3.14.0")
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // ── Dev tools ─────────────────────────────────────────────────
    developmentOnly("org.springframework.boot:spring-boot-devtools")

    // ── Testes ────────────────────────────────────────────────────
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.fabric8:kubernetes-server-mock:6.13.1")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("com.github.mvysny.kaributesting:karibu-testing-v24:2.1.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Vaadin produção: minifica e empacota assets no JAR
tasks.named<BootJar>("bootJar") {
    dependsOn("vaadinBuildFrontend")
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
    systemProperty("vaadin.devmode.devTools.enabled", "false")
}

vaadin {
    productionMode = System.getenv("VAADIN_PRODUCTION") == "true"
}

// Generates META-INF/build-info.properties — makes BuildProperties bean available at runtime
springBoot {
    buildInfo()
}
