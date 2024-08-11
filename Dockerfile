# Stage 1: Build the application
FROM maven:3.8.5-openjdk-17 AS build

WORKDIR /work

COPY . .

RUN mvn clean package -DskipTests

# Stage 2: Create the runtime image
FROM openjdk:17-jdk-slim

WORKDIR /work

COPY --from=build /work/target/quarkus-app /work/quarkus-app

EXPOSE 8080

CMD ["java", "-jar", "/work/quarkus-app/quarkus-run.jar"]
