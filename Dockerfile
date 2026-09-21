# syntax=docker/dockerfile:1

########################################
# Stage 1: deps - resolve Maven dependencies only, nothing else.
# Only pom.xml + the Maven wrapper are copied in this stage - NOT src/. Docker caches a layer by
# its inputs, so as long as pom.xml doesn't change, this whole stage (and the dependency download)
# is skipped on every rebuild, even if you edit Java files constantly.
########################################
FROM eclipse-temurin:21-jdk-jammy AS deps
WORKDIR /build

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw

# go-offline pulls every dependency AND plugin needed for a build into the local ~/.m2 cache
# without compiling anything yet - this is the expensive step the layer cache above protects.
RUN ./mvnw -B dependency:go-offline


########################################
# Stage 2: builder - compile and package the jar, reusing the dependency cache from "deps".
# Source code is copied ONLY here, so editing Java files never invalidates the deps layer above.
########################################
FROM deps AS builder
WORKDIR /build

COPY src/ src/
RUN ./mvnw -B clean package -DskipTests


########################################
# Stage 3: runner - the ONLY stage that actually ships. JRE-only base image (no JDK, no Maven,
# no ~/.m2 cache, no source code) holding just the JVM and the single fat jar built above - this
# is the "only important library gets carried into the container" part: everything Maven/JDK/
# source-only from stages 1-2 is discarded, none of it exists in the final image's layers.
########################################
FROM eclipse-temurin:21-jre-jammy AS runner
WORKDIR /app

# Run as a non-root user - Temurin base images run as root by default.
RUN addgroup --system app && adduser --system --ingroup app app
USER app

COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080

# Secrets (DB/Redis/mail passwords, Firebase service-account file) are deliberately NOT baked
# into any layer above - see .dockerignore. Pass them at `docker run` time instead:
#   docker run --env-file .env \
#     -v "$(pwd)/firebase-service-account.json:/app/firebase-service-account.json:ro" \
#     -p 8080:8080 saku-ku
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
