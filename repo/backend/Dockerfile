FROM eclipse-temurin:23-jdk AS build

WORKDIR /app

# Copy Maven wrapper and pom first for dependency caching
COPY .mvn/ .mvn/
COPY mvnw mvnw
COPY pom.xml pom.xml

RUN chmod +x mvnw

# Download dependencies (cached layer)
RUN ./mvnw dependency:go-offline -B -q || true

# Copy source
COPY src/ src/

# Build the application (skip tests — tests run separately)
RUN ./mvnw package -DskipTests -q

FROM eclipse-temurin:23-jre

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

# Create cache directory for audio segments
RUN mkdir -p /app/cache /app/exports/reconciliation

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
