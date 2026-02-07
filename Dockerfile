FROM eclipse-temurin:17-jdk AS builder
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN useradd -m appuser
USER appuser

COPY --from=builder /app/build/libs/*.jar /app/app.jar

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
