# Etapa de compilación
FROM mirror.gcr.io/library/maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copiar POM y descargar dependencias
COPY pom.xml .
RUN mvn dependency:go-offline

# Copiar código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa de ejecución (Usa imagen oficial de Playwright con Java para asegurar dependencias de Chromium)
FROM mcr.microsoft.com/playwright/java:v1.49.0-noble
WORKDIR /app

# Copiar el JAR generado
COPY --from=builder /app/target/divorce-mcp-server-0.0.1-SNAPSHOT.jar app.jar

# Exponer puerto del servidor (WebFlux usa 8080 por defecto)
EXPOSE 8080

# Comando de inicio
CMD ["java", "-jar", "app.jar"]
