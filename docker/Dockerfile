# syntax=docker/dockerfile:1

# Build stage — compiles the app and produces the production Vaadin frontend
FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app

# Cache Gradle wrapper and dependency resolution separately from source code
COPY gradlew ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
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

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
