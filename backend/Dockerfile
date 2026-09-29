# Imagen de la API en dos etapas: la primera compila con el JDK y Maven; la
# segunda solo lleva el JRE y el .jar, así la imagen final es más pequeña y no
# incluye el código fuente ni las herramientas de compilación.

# --- Etapa 1: compilar ---
FROM eclipse-temurin:21-jdk AS compilacion
WORKDIR /app
# Primero solo el pom y el wrapper: si no cambian, Docker reutiliza la capa
# con las dependencias descargadas y la siguiente compilación es más rápida.
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN chmod +x mvnw && ./mvnw -q -B dependency:go-offline
COPY src src
# Las pruebas ya corren en el CI (GitHub Actions); aquí solo se empaqueta.
RUN ./mvnw -q -B package -DskipTests

# --- Etapa 2: ejecutar ---
FROM eclipse-temurin:21-jre
WORKDIR /app
# Nunca correr como root dentro del contenedor.
RUN useradd --system --create-home cuentasclaras
USER cuentasclaras
COPY --from=compilacion /app/target/*.jar app.jar
# El plan gratuito tiene 512 MB: la JVM usa como máximo el 75 % de esa memoria.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
