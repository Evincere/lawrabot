# Etapa de compilación
FROM mirror.gcr.io/library/maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copiar POM y descargar dependencias
COPY pom.xml .
RUN mvn dependency:go-offline

# Copiar código fuente y compilar
COPY src ./src
RUN mvn clean package -DskipTests

# Etapa de ejecución (Usa Eclipse Temurin en Noble para evitar dependencias de mcr.microsoft.com)
FROM mirror.gcr.io/library/eclipse-temurin:21-jre-noble
WORKDIR /app

# Instalar dependencias del sistema y navegadores de Playwright
RUN apt-get update && apt-get install -y --no-install-recommends nodejs npm \
    && npx playwright install-deps \
    && npx playwright install firefox chromium \
    && apt-get purge -y nodejs npm \
    && apt-get autoremove -y \
    && rm -rf /var/lib/apt/lists/*

# Copiar el JAR generado
COPY --from=builder /app/target/divorce-mcp-server-0.0.1-SNAPSHOT.jar app.jar

# Exponer puerto del servidor
EXPOSE 8080

# Comando de inicio
CMD ["java", "-jar", "app.jar"]
