# ---------- Build stage ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Cache de dependencias: primero el wrapper y el pom
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -q dependency:go-offline

# Código y empaquetado (tests corren en el pipeline, no en la imagen)
COPY src/ src/
RUN ./mvnw -B -q -DskipTests clean package \
 && cp target/*.jar app.jar

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre AS runtime
WORKDIR /app

# Usuario no-root
RUN groupadd -g 1000 spring && useradd -u 1000 -g spring -s /usr/sbin/nologin spring
COPY --from=build /workspace/app.jar /app/app.jar
USER 1000:1000

EXPOSE 8081
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"
# El perfil/secretos llegan por variables de entorno (ConfigMap/Secret en k8s).
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
