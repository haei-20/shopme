# Use an official OpenJDK runtime as a parent image
FROM eclipse-temurin:17-jdk

# Set the working directory inside the container
WORKDIR /app


# Copy the Maven wrapper và pom.xml
COPY mvnw* pom.xml ./
COPY .mvn .mvn

# Cấp quyền thực thi cho mvnw (fix lỗi permission denied)
RUN chmod +x mvnw

# Copy the source code
COPY src ./src
COPY src/main/resources ./src/main/resources

# Package the application (skip tests for faster build, can remove if you want tests)
RUN ./mvnw clean package -DskipTests

# Copy the built jar to the container
RUN cp target/*.jar app.jar

# Expose the port (change if your app uses a different port)
EXPOSE 8181

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
