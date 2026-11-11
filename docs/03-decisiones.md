# CuentasClaras: decisiones técnicas

Cada decisión con lo que se eligió, por qué y lo que se dejó de lado. Formato corto de *Architecture Decision Record*.

---

### D-01. La plata en `NUMERIC(14,2)` y `BigDecimal`, nunca `double`

**Por qué:** con decimales binarios, `0.1 + 0.2` no da `0.3` exacto. En un resumen de miles de movimientos, el error se ve en los pesos. La clase `Pesos` centraliza el redondeo.
**Costo:** `BigDecimal` es más largo de escribir (`a.add(b)` en vez de `a + b`).

### D-02. El saldo se calcula, no se guarda

**Por qué:** un saldo guardado se puede desincronizar si falla una actualización a mitad de camino. Calcularlo con saldo inicial + ingresos − gastos ± transferencias siempre da el valor real.
**Costo:** cada consulta suma movimientos. Con índices por `(usuario_id, fecha)` y por `cuenta_id` sigue siendo rápida para el volumen de una persona.

### D-03. Sesión en cookie `HttpOnly`, no en `localStorage`

**Por qué:** un token en `localStorage` lo puede leer cualquier script que se cuele en la página (XSS). En una cookie `HttpOnly` ni el propio código de la app puede leerlo. `SameSite=Strict` bloquea el envío desde otros sitios (CSRF).
**Para que funcione:** Vercel reescribe `/api` hacia Render y así todo queda en el mismo dominio. Sin eso, la cookie sería de terceros y los navegadores la bloquearían.
**Se dejó de lado:** el encabezado `Authorization: Bearer` sigue funcionando para pruebas y clientes que no son navegador.

### D-04. Versión de sesión dentro del token

**Por qué:** un JWT no se puede "borrar" del lado del servidor. Si la persona cambia la clave (por ejemplo, porque alguien más la conocía), los tokens anteriores deben dejar de servir. El claim `v` se compara con `usuarios.version_sesion`, y al subir la versión todos los tokens viejos quedan inválidos.
**Costo:** una consulta pequeña por petición. A cambio, cambiar la clave cierra de verdad las sesiones de los otros dispositivos. Cerrar sesión normal borra la cookie de este navegador.

### D-05. JdbcTemplate en los módulos nuevos

**Por qué:** deudas, pagos fijos y metas son sobre todo consultas con sumas y agrupaciones (cuánto se ha abonado, cuánto falta, qué pagos están pendientes este mes). Con SQL a mano, la consulta que se lee es la que corre. Con JPA habría que adivinar qué genera Hibernate.
**Costo:** dos estilos en el mismo proyecto. Se aceptó porque los módulos originales con JPA ya estaban probados y no ganaban nada reescribiéndose.

### D-06. Bloqueo `FOR UPDATE` al abonar

**Por qué:** si llegan dos abonos a la vez (dos clics rápidos), ambos podrían leer "faltan $50.000" y pasarse del total. Bloquear la fila de la deuda mientras dura la transacción hace que el segundo abono espere y vea el valor actualizado. Hay una prueba que lanza abonos en paralelo para comprobarlo.

### D-07. Un pago fijo se paga una sola vez por mes: índice único parcial

**Por qué:** `CREATE UNIQUE INDEX ... ON movimientos (pago_fijo_id, periodo) WHERE pago_fijo_id IS NOT NULL`. La regla queda en la base de datos, no solo en el código, así que ni dos peticiones simultáneas la rompen. Pagar reutiliza `MovimientoService.crear`, para que un pago fijo cumpla las mismas validaciones que cualquier gasto.

### D-08. 404 en vez de 403 para datos ajenos

**Por qué:** responder 403 ("existe pero no es tuyo") confirma que ese id existe. Con 404, desde afuera no se puede distinguir.

### D-09. Ley 1581: descargar y borrar mis datos

**Por qué:** en Colombia, las personas tienen derecho a conocer y suprimir sus datos personales. `GET /auth/mis-datos` entrega todo en JSON y `DELETE /auth/cuenta` lo borra (pide la clave). `BorradoDeDatos` es el único lugar que conoce el orden de borrado según las llaves foráneas. Si se agrega una tabla, solo se toca ahí.

### D-10. Identidad visual "sobres" con colores de billetes

**Por qué:** el método del sobre (repartir la plata del mes por categoría) ya lo conoce la gente; dibujar el presupuesto como un sobre que se vacía se entiende sin explicar. Los colores salen de los billetes colombianos (2 mil turquesa, 5 mil ocre, 10 mil rojo…) y los guilloches se dibujan con una fórmula propia, sin copiar ningún billete. Así la app no se parece a una plantilla.
**Detalle técnico:** los guilloches se generan como SVG estáticos (`scripts/generar-guilloche.mjs`). Dibujados en línea, sus miles de puntos se recalculaban en cada diseño de la página y Lighthouse marcaba bloqueo.

### D-11. Página pública que no espera a la API

**Por qué:** la API gratuita se apaga sin uso y tarda cerca de un minuto en despertar. Si la web esperara a la API para mostrar algo, quien llega desde LinkedIn vería una pantalla de carga. Una marca sin datos (`cc_hubo_sesion`) indica si hubo sesión. Si no la hubo, la página aparece de inmediato y la API se despierta en segundo plano.

### D-12. Carga por partes

**Por qué:** Recharts y las pantallas internas pesan la mayor parte del paquete. Con `React.lazy`, quien solo mira la página de inicio descarga 75 KB comprimidos en lugar de 216 KB. Resultado en Lighthouse: inicio 99 / 100 / 100 / 100.

### D-13. Pruebas en tres niveles

| Nivel | Herramienta | Qué cubre |
|---|---|---|
| API | JUnit + Testcontainers (PostgreSQL real) | Reglas de negocio, seguridad, concurrencia (98 pruebas) |
| Componentes | Vitest + Testing Library | Pantallas y utilidades con una API falsa (15) |
| De punta a punta | Playwright + axe | Recorridos reales contra el jar y accesibilidad de cada pantalla pública (14) |

**Por qué Testcontainers y no H2:** H2 no tiene índices parciales, `FOR UPDATE` igual ni las mismas funciones de fecha. Las pruebas deben correr contra el mismo motor que producción.
