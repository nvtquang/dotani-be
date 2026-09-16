FROM maven:3.9.9-eclipse-temurin-21 AS build
ARG MODULE=audit-service
WORKDIR /workspace
COPY pom.xml ./
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY auth-service/pom.xml auth-service/pom.xml
COPY member-service/pom.xml member-service/pom.xml
COPY event-service/pom.xml event-service/pom.xml
COPY content-service/pom.xml content-service/pom.xml
COPY chat-service/pom.xml chat-service/pom.xml
COPY notification-service/pom.xml notification-service/pom.xml
COPY audit-service/pom.xml audit-service/pom.xml
COPY ${MODULE}/src ${MODULE}/src
RUN mvn -pl ${MODULE} package spring-boot:repackage -DskipTests

FROM eclipse-temurin:21-jre-jammy
ARG MODULE=audit-service
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/${MODULE}/target/${MODULE}-*.jar /app/app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
