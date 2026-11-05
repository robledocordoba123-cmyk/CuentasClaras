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
RUN useradd --system --create-home cuentasclaras && chown cuentasclaras /app
USER cuentasclaras
COPY --from=compilacion /app/target/*.jar app.jar
# El plan gratuito tiene 512 MB y 0,1 de CPU. La JVM usa como máximo el 75 % de
# la memoria, y TieredStopAtLevel=1 deja solo el compilador rápido: se pierde
# algo de rendimiento máximo, pero arranca mucho antes con tan poca CPU.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1"

# Arranque rápido con CDS: se separa el .jar y se hace un arranque de ensayo
# que guarda las clases ya cargadas en app.jsa. El contenedor las reutiliza en
# vez de volver a leerlas. El ensayo no necesita base de datos: Flyway se apaga
# y Hibernate no consulta el servidor; estos ajustes valen solo para este RUN.
RUN java -Djarmode=tools -jar app.jar extract --destination extraido     && java -XX:ArchiveClassesAtExit=app.jsa        -Dspring.context.exit=onRefresh        -Dspring.flyway.enabled=false        -Dspring.jpa.hibernate.ddl-auto=none        -Dspring.jpa.database-platform=org.hibernate.dialect.PostgreSQLDialect        -Dspring.jpa.properties.hibernate.boot.allow_jdbc_metadata_access=false        -Dspring.datasource.hikari.initialization-fail-timeout=-1        -jar extraido/app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-XX:SharedArchiveFile=app.jsa", "-Xshare:auto", "-jar", "extraido/app.jar"]
