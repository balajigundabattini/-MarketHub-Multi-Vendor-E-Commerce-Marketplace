FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

RUN chmod +x mvnw

COPY src/ src/

RUN ./mvnw -B clean package -DskipTests

RUN JAR_FILE="$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' | head -n 1)" \
    && test -n "$JAR_FILE" \
    && cp "$JAR_FILE" /app/app.jar

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build --chown=10001:10001 /app/app.jar app.jar

ENV SPRING_PROFILES_ACTIVE=docker

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
