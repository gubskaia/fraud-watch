FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY libs libs
COPY services services

RUN mvn -pl services/transaction-service -am -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends wget \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /workspace/services/transaction-service/target/transaction-service-0.1.0-SNAPSHOT.jar app.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "/app/app.jar", "--spring.profiles.active=docker"]
