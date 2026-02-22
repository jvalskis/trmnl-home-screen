# === Build stage ===
FROM eclipse-temurin:21-jdk-jammy AS build

# Install sbt
RUN apt-get update && \
    apt-get install -y --no-install-recommends apt-transport-https curl gnupg && \
    echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" > /etc/apt/sources.list.d/sbt.list && \
    curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | gpg --dearmor -o /etc/apt/trusted.gpg.d/sbt.gpg && \
    apt-get update && \
    apt-get install -y --no-install-recommends sbt && \
    rm -rf /var/lib/apt/lists/*

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

# Copy templates
COPY weather.liquid /app/templates/weather.liquid
COPY calendar.liquid /app/templates/calendar.liquid

# Set ownership
RUN chown -R appuser:appgroup /app

USER appuser

ENV WEATHER_TEMPLATE_FILE=/app/templates/weather.liquid
ENV CALENDAR_TEMPLATE_FILE=/app/templates/calendar.liquid

EXPOSE 8080

ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-Djava.security.egd=file:/dev/./urandom", \
    "-jar", "/app/app.jar"]
