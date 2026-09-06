FROM gradle:8.14.3-jdk21-alpine AS build

WORKDIR /workspace
COPY --chown=gradle:gradle gradle/ gradle/
COPY --chown=gradle:gradle gradlew settings.gradle.kts build.gradle.kts ./
COPY --chown=gradle:gradle src/ src/
RUN chown gradle:gradle /workspace
USER gradle
RUN ./gradlew --no-daemon bootJar -x test

# Derive the JDK modules the application actually needs and build a minimal
# runtime; shipping the stock JRE pushes the slim image past the 250 MB gate.
RUN jdeps --ignore-missing-deps --print-module-deps --multi-release 21 -q \
      build/libs/opspulse-ai.jar > /tmp/app.modules
RUN jlink \
      --add-modules "$(cat /tmp/app.modules),java.management,java.naming,java.instrument,java.security.jgss,java.sql,java.desktop,jdk.unsupported,jdk.crypto.ec,jdk.crypto.cryptoki" \
      --strip-debug --no-man-pages --no-header-files --compress zip-6 \
      --output /workspace/javaruntime

FROM alpine:3.20

RUN addgroup -S spring && adduser -S spring -G spring
WORKDIR /app
COPY --from=build /workspace/javaruntime /opt/java
COPY --from=build /workspace/build/libs/opspulse-ai.jar app.jar

USER spring:spring
EXPOSE 8080

ENV JAVA_HOME="/opt/java" PATH="/opt/java/bin:${PATH}"
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:MaxDirectMemorySize=32m"

HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=6 \
  CMD wget --quiet --tries=1 --spider http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
