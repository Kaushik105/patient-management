FROM maven:3.9.9-eclipse-temurin-17 AS builder
WORKDIR /app

# 1) Copy pom first for better dependency cache reuse
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests dependency:go-offline

# 2) Copy sources only after deps are cached
COPY src ./src
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app

# Prefer explicit jar name if stable
COPY --from=builder /app/target/patient-service-1.0-SNAPSHOT.jar app.jar

EXPOSE 4000
ENTRYPOINT ["java", "-jar", "app.jar"]