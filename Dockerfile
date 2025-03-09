
# Stage 1: Build the React frontend
FROM node:18 AS frontend-build
WORKDIR /frontend
COPY ./frontend/package*.json ./
RUN npm install
COPY ./frontend .
RUN npm run build

# Stage 2: Prepare Spring Boot with React files
FROM maven:3.9.2-eclipse-temurin-17 AS springboot-handler
WORKDIR /app
COPY ./pom.xml .
RUN mvn dependency:go-offline
COPY ./ .
COPY --from=frontend-build /frontend/build ./src/main/resources/static/
RUN mvn clean package -DskipTests

# Stage 3: Create a lightweight runtime image for the backend
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app
COPY --from=springboot-handler /app/target/*.jar app.jar
EXPOSE 8443

# Command to run the Spring Boot application
CMD ["java", "-jar", "app.jar"]


# # Step 1: Remove the existing container (if it exists)
# docker rm -f my-app-container 2>/dev/null || true
#
# # Step 2: Build the Docker image
# docker build -t my-app:latest .
#
# # Step 3: Run the new container
# docker run -p 8443:8443 --name my-app-container my-app