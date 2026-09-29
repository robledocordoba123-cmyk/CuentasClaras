# CuentasClaras

![CI](https://github.com/robledocordoba123-cmyk/CuentasClaras/actions/workflows/ci.yml/badge.svg)

> *"Cuentas claras, amistades largas."*

App de finanzas personales para quien maneja su plata repartida entre efectivo, Nequi, Daviplata y el banco. Registras lo que entra y sale de cada cuenta, te pones un presupuesto por categoría y la app te avisa antes de que te pases.

**Estado:** en construcción. El diseño completo (historias de usuario, reglas de negocio, modelo de datos y plan de trabajo) está en [`docs/01-diseno.md`](docs/01-diseno.md).

## Stack

| Capa | Tecnologías |
|---|---|
| API | Java 21, Spring Boot 4, Spring Security, Spring Data JPA, Bean Validation |
| Datos | PostgreSQL 16, Flyway (migraciones) |
| Pruebas | JUnit 5, MockMvc, Testcontainers (PostgreSQL real en Docker) |
| CI | GitHub Actions |

## Cómo correrlo en local

Requisitos: JDK 21 y Docker Desktop.

```bash
# 1. Base de datos (queda en el puerto 5433)
docker compose up -d

# 2. API en http://localhost:8080
cd backend
./mvnw spring-boot:run
```

Comprueba que está viva: `http://localhost:8080/actuator/health` → `{"status":"UP"}`

## Pruebas

```bash
cd backend
./mvnw verify
```

No necesitan la base de docker compose: Testcontainers levanta su propio PostgreSQL 16 en Docker, aplica las migraciones y lo borra al terminar.

## Avance

- [x] Diseño
- [x] Esqueleto: Spring Boot, PostgreSQL, Flyway, seguridad base, CI
- [ ] Registro e inicio de sesión (JWT)
- [ ] Cuentas y categorías
- [ ] Movimientos
- [ ] Transferencias
- [ ] Presupuestos y resumen mensual
- [ ] Frontend
- [ ] Despliegue y demo

---

Proyecto de portafolio de **Manuela Córdoba Robledo**, aprendiz de Análisis y Desarrollo de Software (SENA).
