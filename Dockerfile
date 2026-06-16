# Multi-stage build for MathMentor AI
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 18080

ENV AI_MOCK_ENABLED=true
ENV SERVER_PORT=18080

ENTRYPOINT ["java", "-jar", "app.jar"]
