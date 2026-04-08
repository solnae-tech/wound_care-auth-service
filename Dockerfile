# ─────────────────────────────────────────────────────────────────────────────
# Stage 1 — Build
#   Uses the full Maven + JDK 21 image only during the build phase.
#   The resulting fat-JAR is copied to the slim runtime image below.
# ─────────────────────────────────────────────────────────────────────────────
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /app

# Copy only the POM first so Maven dependency layer is cached separately.
# Re-downloaded only when pom.xml changes, not on every source edit.
COPY pom.xml .
RUN mvn dependency:go-offline -B --no-transfer-progress

# Copy source and build the fat-JAR (skip tests — run them in CI instead)
COPY src ./src
RUN mvn clean package -DskipTests -B --no-transfer-progress


# ─────────────────────────────────────────────────────────────────────────────
# Stage 2 — Runtime
#   Slim Alpine JRE image (~90 MB vs ~600 MB for the builder).
#   Only the compiled JAR is copied across — no source, no Maven cache.
# ─────────────────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring

# Copy the fat-JAR from the builder stage
COPY --from=builder /app/target/*.jar app.jar

# Switch to non-root
USER spring

# Document the default port (Render overrides this with the PORT env var)
EXPOSE 8081

# Tune GC for containerised environments
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
