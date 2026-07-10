FROM gradle:8.14.3-jdk21-alpine AS build

WORKDIR /workspace
COPY --chown=gradle:gradle gradle/ gradle/
COPY --chown=gradle:gradle gradlew settings.gradle.kts build.gradle.kts ./
COPY --chown=gradle:gradle src/ src/
RUN chown gradle:gradle /workspace
USER gradle
RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre-alpine

RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=build /workspace/build/libs/opspulse-ai.jar app.jar

USER spring:spring
EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:MaxDirectMemorySize=32m"

HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=6 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
