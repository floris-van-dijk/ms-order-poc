FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build

COPY pom.xml .
RUN mvn dependency:go-offline --batch-mode --no-transfer-progress

COPY ./src ./src
RUN mvn package -DskipTests --batch-mode --no-transfer-progress



FROM eclipse-temurin:21-jre-alpine AS application
WORKDIR /app

COPY --from=build /build/target/*.jar ms-order.jar

EXPOSE 8083
ENTRYPOINT ["java", "-jar", "ms-order.jar"]
