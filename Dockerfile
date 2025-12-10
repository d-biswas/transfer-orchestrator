# Build stage
FROM gradle:9.2.1-jdk25 AS builder
WORKDIR /app

# Copy source code and build the boot jar
COPY . /app/
RUN gradle bootJar --no-daemon

# Runtime stage
FROM eclipse-temurin:25-jre
WORKDIR /app

# Copy the built jar from the builder
COPY --from=builder /app/build/libs/*.jar app.jar

# Set up JVM GC logs directory
ENV GC_LOG_PATH=/var/log/jvm
RUN mkdir -p ${GC_LOG_PATH}

# Expose application port
EXPOSE 8080

# Entry point with JVM options
ENTRYPOINT ["java", \
    "-XX:+UseContainerSupport", \
    "-XX:InitialRAMPercentage=60.0", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-XX:MaxGCPauseMillis=200", \
    "-XX:+UseStringDeduplication", \
    "-Xlog:gc*:file=/var/log/jvm/gc.log:time,level,tags:filecount=5,filesize=10M", \
    "-XX:+HeapDumpOnOutOfMemoryError", \
    "-XX:HeapDumpPath=/var/log/jvm/heapdump.hprof", \
    "-XX:+ExitOnOutOfMemoryError", \
    "-jar", "app.jar"]
