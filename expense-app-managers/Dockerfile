FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY expense-app-managers/pom.xml .
COPY expense-app-managers/src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/target/expense-app-managers-1.0-SNAPSHOT.jar app.jar
# Runtime database
COPY revExpenseData.db /app/revExpenseData.db

ENV TEST_DATABASE_PATH=/app/revExpenseData.db

EXPOSE 5001

ENTRYPOINT ["java", "-jar", "app.jar"]
# ENTRYPOINT ["java", "-jar", "app.jar", "/app/testDatabase.db"]
