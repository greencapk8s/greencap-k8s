# syntax=docker/dockerfile:1

# Build stage — compiles the app and produces the production Vaadin frontend
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Download Helm CLI binary
ARG HELM_VERSION=v4.2.2
RUN curl -fsSL https://get.helm.sh/helm-${HELM_VERSION}-linux-amd64.tar.gz \
    | tar -xz --strip-components=1 -C /usr/local/bin linux-amd64/helm

# Cache Gradle wrapper and dependency resolution separately from source code
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
RUN ./gradlew --no-daemon dependencies

COPY . .
ENV VAADIN_PRODUCTION=true
RUN ./gradlew --no-daemon bootJar -x jar

# Runtime stage — only the JRE and the executable jar
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /usr/local/bin/helm /usr/local/bin/helm

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
