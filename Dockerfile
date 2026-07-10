# syntax=docker/dockerfile:1

# ---- Build stage -------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Resolve dependencies first so they are cached independently of source changes.
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline

# Build the executable Spring Boot jar.
# Tests are skipped here: SassedoApplicationTests is a full @SpringBootTest that needs a live
# MySQL, which is not available in the isolated build stage. Run tests in CI with a DB service.
COPY src ./src
RUN mvn -B clean package -DskipTests

# ---- Runtime stage -----------------------------------------------------------
FROM eclipse-temurin:17-jre AS runtime

# curl is used by the container health check against the actuator endpoint.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl tini \
    && rm -rf /var/lib/apt/lists/*

# Run as an unprivileged user.
RUN groupadd --system --gid 1001 spring \
    && useradd --system --uid 1001 --gid spring --home-dir /app --shell /usr/sbin/nologin spring

WORKDIR /app

# Writable location for rolling log files (see log4j2.xml -Dlog-path).
RUN mkdir -p /app/logs && chown -R spring:spring /app

COPY --from=build /workspace/target/sassedo-be-*.jar /app/app.jar

ENV TZ=UTC \
    JAVA_OPTS="" \
    SPRING_PROFILES_ACTIVE=prod \
    LOG_PATH=/app/logs

EXPOSE 8080 8081

USER spring

ENTRYPOINT ["/usr/bin/tini", "--", "sh", "-c", "exec java $JAVA_OPTS -Dfile.encoding=UTF-8 -Dlog-path=\"$LOG_PATH\" -jar /app/app.jar"]
