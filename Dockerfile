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

# Usuario no-root (UID/GID 10001 para no chocar con grupos existentes de la base)
RUN groupadd -g 10001 spring \
 && useradd -u 10001 -g spring -s /usr/sbin/nologin -M spring
COPY --from=build /workspace/app.jar /app/app.jar
USER 10001:10001

EXPOSE 8081
# Zona horaria del negocio (Perú). Así LocalDateTime.now() del server es hora
# local Lima y el front muestra las horas correctas (inicio de jornada, agenda,
# bitácora, etc.) sin desfase GMT.
ENV TZ="America/Lima"
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Duser.timezone=America/Lima"
# El perfil/secretos llegan por variables de entorno (ConfigMap/Secret en k8s).
ENTRYPOINT ["sh","-c","exec java $JAVA_OPTS -jar /app/app.jar"]
