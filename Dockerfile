# syntax=docker/dockerfile:1

ARG SERVICE

FROM eclipse-temurin:25-jdk AS build
ARG SERVICE
WORKDIR /workspace

COPY ${SERVICE}/.mvn/ .mvn/
COPY ${SERVICE}/mvnw ${SERVICE}/pom.xml ./
RUN chmod +x mvnw && ./mvnw --batch-mode dependency:go-offline

COPY ${SERVICE}/src ./src/
RUN ./mvnw --batch-mode package -DskipTests

FROM eclipse-temurin:25-jre
WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
