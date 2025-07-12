# 1) Build stage: Gradle on Java 21
FROM gradle:jdk21 AS builder
WORKDIR /home/gradle/project

# Cache dependencies by copying only Gradle build scripts
COPY build.gradle settings.gradle ./
RUN gradle dependencies --no-daemon

# Copy full source code and build application
COPY . .
RUN gradle clean bootJar -x test --no-daemon

# 2) Runtime stage: OpenJDK 21 slim
FROM openjdk:21-jdk-slim
WORKDIR /app

# Copy executable JAR from builder stage
COPY --from=builder /home/gradle/project/build/libs/*.jar app.jar

# Activate prod profile at runtime
ENTRYPOINT ["java", "-jar", "app.jar", "--spring.profiles.active=${SPRING_PROFILES_ACTIVE}"]
