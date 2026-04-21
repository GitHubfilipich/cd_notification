# ЭТАП 1: Сборка
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Кешируем зависимости
COPY pom.xml .
RUN mvn dependency:go-offline

# Сборка jar
COPY src ./src
RUN mvn clean package -DskipTests -Dcheckstyle.skip

# ЭТАП 2: Запуск
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Копируем jar из этапа сборки
COPY --from=build /app/target/*.jar app.jar

EXPOSE 9900
ENTRYPOINT ["java", "-jar", "app.jar"]