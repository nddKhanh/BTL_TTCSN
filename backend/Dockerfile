FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -DskipTests dependency:go-offline
COPY src src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/* \
    && addgroup --system app \
    && adduser --system --ingroup app app
COPY --from=build /workspace/target/order-*.jar app.jar
USER app
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s CMD wget -qO- http://127.0.0.1:8080/v3/api-docs || exit 1
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
