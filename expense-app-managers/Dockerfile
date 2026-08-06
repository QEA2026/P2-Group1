FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/expense-app-managers/target/expense-app-managers-1.0-SNAPSHOT.jar app.jar
COPY --from=build /workspace/revExpenseData.db /app/revExpenseData.db

ENV DATABASE_PATH=/app/revExpenseData.db

EXPOSE 5001

ENTRYPOINT ["java","-jar","app.jar"]