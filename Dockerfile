# ---------- BUILD STAGE ----------
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Скопировать pom.xml и скачать зависимости заранее (кэш)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Скопировать исходники и собрать
COPY src ./src
RUN mvn clean package -DskipTests

# ---------- RUNTIME STAGE ----------
FROM eclipse-temurin:17-jre
WORKDIR /app


COPY --from=build /app/target/bankcards-*.jar app.jar

# Запускаем
ENTRYPOINT ["java","-jar","/app/app.jar"]