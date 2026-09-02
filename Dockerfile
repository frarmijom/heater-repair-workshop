FROM maven:3.9.12-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN groupadd --system --gid 10001 spring \
    && useradd --system --uid 10001 --gid spring --no-create-home spring
COPY --from=build --chown=spring:spring /workspace/target/heater-repair-workshop-1.0-SNAPSHOT.jar app.jar

USER spring
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
