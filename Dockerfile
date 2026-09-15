from maven:3.9.6-eclipse-temurin-22-alpine AS builder
workdir /app
copy pom.xml .
copy src ./src
RUN mvn package -DskipTests

FROM eclipse-temurin:22-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
CMD ["java", "-jar", "app.jar"]