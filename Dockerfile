# Stage 1: Build the Spring Boot + React app
FROM maven:3.9.2-eclipse-temurin-17 AS build

# Set working directory for the build stage
WORKDIR /app

# Copy the entire project into the container
COPY . .

# Build the application without tests
RUN mvn clean package -DskipTests

# Stage 2: Create a lightweight runtime image
FROM eclipse-temurin:17-jre

# Set working directory for the runtime stage
WORKDIR /app

# Copy the packaged JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the port for the application
EXPOSE 8443

# Command to run the application
CMD ["java", "-jar", "app.jar"]