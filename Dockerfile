FROM maven:3.9.6-eclipse-temurin-21 AS build
COPY . .
RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jdk
COPY --from=build target/la-panchina-0.0.1-SNAPSHOT.jar la-panchina.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "la-panchina.jar"]