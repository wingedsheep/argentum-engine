FROM eclipse-temurin:21-jdk AS build
WORKDIR /app
COPY . .
RUN ./gradlew :game-server:bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/game-server/build/libs/game-server.jar app.jar
# Stamped onto every recorded replay (see EngineVersion) so a replay that stops re-simulating can be
# traced back to the build that recorded it. Mirrors the frontend's COMMIT_HASH build arg.
ARG COMMIT_HASH=unknown
ENV APP_VERSION=$COMMIT_HASH
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
