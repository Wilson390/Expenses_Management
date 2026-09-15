# syntax=docker/dockerfile:1
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY BE/pom.xml pom.xml
RUN --mount=type=cache,target=/root/.m2 mvn -B dependency:go-offline
COPY BE/src src
RUN --mount=type=cache,target=/root/.m2 mvn -B verify

FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S app && adduser -S app -G app
WORKDIR /app
COPY --from=build --chown=app:app /build/target/giving-0.0.1-SNAPSHOT.jar app.jar
RUN mkdir -p /app/uploads && chown app:app /app/uploads && chmod 755 /app/uploads
USER app
ENV TZ=Asia/Kolkata JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0"
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=90s --retries=6 CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health/readiness || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
