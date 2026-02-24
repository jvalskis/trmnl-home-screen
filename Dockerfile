# === Build stage ===
FROM sbtscala/scala-sbt:eclipse-temurin-21.0.8_9_1.11.7_3.3.7 AS build

WORKDIR /build

# Cache dependency resolution
COPY project/build.properties project/plugins.sbt project/
COPY build.sbt .
RUN sbt update

# Copy source and build fat JAR
COPY src/ src/
RUN sbt assembly

# === Runtime stage ===
FROM eclipse-temurin:21-jre-alpine AS runtime

# Create non-root user with no shell access
RUN addgroup -g 1001 appgroup && \
    adduser -u 1001 -G appgroup -s /sbin/nologin -D appuser

WORKDIR /app

# Copy fat JAR from build stage
COPY --from=build /build/target/scala-3.3.7/trmnl-home-screen-app.jar /app/app.jar

# Copy template
COPY screen.liquid /app/templates/screen.liquid

# Set ownership
RUN chown -R appuser:appgroup /app

USER appuser

ENV SCREEN_TEMPLATE_FILE=/app/templates/screen.liquid

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "/app/app.jar"]
