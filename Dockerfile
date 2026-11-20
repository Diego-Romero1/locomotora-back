FROM maven:3.9.9-eclipse-temurin-21 AS build

WORKDIR /workspace

COPY pom.xml ./
COPY src ./src

RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/target/locomotora-backend-*.jar /app/app.jar

EXPOSE 8080

# Pensado para contenedores chicos (Render free: 512 MB y 0,1 CPU): heap al 60 % de la RAM del contenedor,
# GC serial (menos memoria y hilos), pila de hilos mas corta y solo el compilador C1 para arrancar mas rapido.
# Se puede sobrescribir con la variable JAVA_TOOL_OPTIONS al ejecutar el contenedor.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=60 -XX:+UseSerialGC -Xss512k -XX:TieredStopAtLevel=1"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]