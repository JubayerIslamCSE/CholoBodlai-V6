# CholoBodlai V6: reproducible Java 21 multi-stage Docker build for Render.
# Secrets are supplied ONLY at runtime through Render environment variables.
FROM maven:3.9.11-eclipse-temurin-21 AS builder
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --create-home --uid 10001 appuser
COPY --from=builder --chown=10001:10001 /workspace/target/*.jar /app/app.jar
USER 10001
ENV PORT=8080
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=60.0", "-jar", "/app/app.jar"]
