FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /app

COPY pom.xml .
COPY .mvn/ .mvn/
COPY mvnw .

RUN chmod +x mvnw

COPY src/ src/

RUN ./mvnw -B clean package

RUN JAR_FILE="$(find target -maxdepth 1 -type f -name '*.jar' ! -name '*.original' | head -n 1)" \
    && test -n "$JAR_FILE" \
    && cp "$JAR_FILE" /app/app.jar

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
