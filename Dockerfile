FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src
COPY frontend/public/assets ./frontend/public/assets

RUN mvn -B -DskipTests package \
    && jar tf target/fashion-0.0.1-SNAPSHOT.jar > /tmp/fashion-jar-contents.txt \
    && grep -Fx 'BOOT-INF/classes/review-migration.sql' /tmp/fashion-jar-contents.txt

FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /workspace/target/fashion-0.0.1-SNAPSHOT.jar app.jar

USER spring:spring

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
