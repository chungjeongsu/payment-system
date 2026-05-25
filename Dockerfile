FROM eclipse-temurin:21-jdk AS builder
WORKDIR /build
COPY . .
RUN chmod +x gradlew && ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY --from=builder /build/build/libs/*.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
