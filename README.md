# CuentasClaras

![CI](https://github.com/robledocordoba123-cmyk/CuentasClaras/actions/workflows/ci.yml/badge.svg)

> *"Cuentas claras, amistades largas."*

App de finanzas personales para quien maneja su plata repartida entre efectivo, Nequi, Daviplata y el banco. Registras lo que entra y sale de cada cuenta, te pones un presupuesto por categoría y la app te avisa antes de que te pases.

**Estado:** en construcción. El diseño completo (historias de usuario, reglas de negocio, modelo de datos y plan de trabajo) está en [`docs/01-diseno.md`](docs/01-diseno.md).

## Stack

| Capa | Tecnologías |
|---|---|
| API | Java 21, Spring Boot 4, Spring Security (OAuth2 Resource Server + JWT), Spring Data JPA, Bean Validation |
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

## Autenticación

| Método | Ruta | Descripción |
|---|---|---|
| POST | `/api/auth/registro` | Crea la cuenta y devuelve un JWT |
| POST | `/api/auth/login` | Devuelve un JWT |
| GET | `/api/auth/yo` | Datos de la persona autenticada (requiere `Authorization: Bearer <token>`) |

### Cuentas y categorías

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/cuentas?incluirArchivadas=false` | Cuentas propias con saldo actual calculado |
| GET / PUT | `/api/cuentas/{id}` | Ver / editar |
| POST | `/api/cuentas` | Crear (nombre, tipo, saldo inicial) |
| DELETE | `/api/cuentas/{id}` | Elimina si no tiene movimientos; si tiene, la archiva |
| PATCH | `/api/cuentas/{id}/restaurar` | Desarchivar |
| GET | `/api/categorias?tipo=GASTO` | Por defecto + propias |
| POST / PUT / DELETE | `/api/categorias[/{id}]` | Solo categorías propias; las por defecto dan 403 |

Cada persona solo ve y modifica lo suyo: pedir un recurso ajeno responde 404, igual que si no existiera.

### Movimientos

| Método | Ruta | Descripción |
|---|---|---|
| GET | `/api/movimientos?desde=&hasta=&cuentaId=&categoriaId=&tipo=&pagina=0&tamano=20` | Filtrado y paginado, lo más reciente primero |
| POST / PUT / DELETE | `/api/movimientos[/{id}]` | Ingresos y gastos |

Documentación interactiva: `http://localhost:8080/swagger-ui.html` (botón **Authorize** para pegar el token).

Los errores siguen el estándar RFC 9457 (`application/problem+json`): `{ "status", "title", "detail" }`, y en validaciones un objeto `errores` por campo.

## Pruebas

```bash
cd backend
./mvnw verify
```

No necesitan la base de docker compose: Testcontainers levanta su propio PostgreSQL 16 en Docker, aplica las migraciones y lo borra al terminar.

## Avance

- [x] Diseño
- [x] Esqueleto: Spring Boot, PostgreSQL, Flyway, seguridad base, CI
- [x] Registro e inicio de sesión (JWT)
- [x] Cuentas y categorías
- [x] Movimientos
- [ ] Transferencias
- [ ] Presupuestos y resumen mensual
- [ ] Frontend
- [ ] Despliegue y demo

---

Proyecto de portafolio de **Manuela Córdoba Robledo**, aprendiz de Análisis y Desarrollo de Software (SENA).
