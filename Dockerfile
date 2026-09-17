FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY dotani-monolith/pom.xml dotani-monolith/pom.xml
RUN mvn -pl dotani-monolith -am dependency:go-offline -DskipTests

COPY dotani-monolith/src dotani-monolith/src
RUN mvn -pl dotani-monolith -am package spring-boot:repackage -DskipTests

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app
COPY --from=build /workspace/dotani-monolith/target/dotani-monolith-*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
