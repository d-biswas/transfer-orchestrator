FROM eclipse-temurin:17-jre-alpine

# Install wget for healthchecks
RUN apk add --no-cache wget

# Create app directory
WORKDIR /app

# Copy the EDC connector JAR
COPY edc-connector.jar /app/edc-connector.jar

# Create directories for config, vault, and keystore
RUN mkdir -p /app/config /app/vault

# Expose ports
# Management API, DSP Protocol, Public API, Control API
EXPOSE 9191 9192 9193 9194

# Use exec form for proper signal handling
ENTRYPOINT ["sh", "-c", "exec java $JVM_ARGS -jar /app/edc-connector.jar"]