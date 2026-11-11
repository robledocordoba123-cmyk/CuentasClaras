import { expect, test } from "@playwright/test";

async function entrarALaDemo(page) {
  await page.goto("/acceso");
  await page.getByRole("button", { name: /Entrar a la demo/ }).click();
  await expect(page.getByRole("heading", { name: "Resumen", level: 1 })).toBeVisible();
}

test("una cuenta con una persona: se registra, se abona y el saldo cuadra", async ({ page }, info) => {
  const nombre = `Prueba ${info.project.name} ${Date.now() % 100000}`;
  await entrarALaDemo(page);
  await page.goto("/personas");

  await page.getByRole("button", { name: "Persona" }).click();
  await page.getByRole("dialog").getByLabel("Nombre").fill(nombre);
  await page.getByRole("dialog").getByRole("button", { name: "Agregar" }).click();
  await expect(page.getByRole("button", { name: new RegExp(nombre) })).toBeVisible();

  await page.getByRole("button", { name: "Registrar" }).click();
  const registro = page.getByRole("dialog");
  await registro.getByLabel("Persona").selectOption({ label: nombre });
  await registro.getByText("Me debe a mí").click();
  await registro.getByLabel("Monto (COP)").fill("30000");
  await registro.getByLabel("¿De qué es?").fill("Taxi compartido");
  await registro.getByRole("button", { name: "Registrar" }).click();

  const persona = page.locator("article").filter({ hasText: nombre });
  await expect(persona.getByText("Te debe $ 30.000")).toBeVisible();
  await persona.getByRole("button", { name: new RegExp(nombre) }).click();
  await persona.getByRole("button", { name: "Abonar" }).click();
  const abono = page.getByRole("dialog");
  await abono.getByLabel(/Monto del abono/).fill("10000");
  await abono.getByRole("button", { name: "Abonar" }).click();
  await expect(persona.getByText("Te debe $ 20.000")).toBeVisible();
});

test("un pago fijo nuevo se paga y queda marcado", async ({ page }, info) => {
  const nombre = `Gimnasio ${info.project.name} ${Date.now() % 100000}`;
  await entrarALaDemo(page);
  await page.goto("/pagos-fijos");
  await page.getByRole("button", { name: "Nuevo" }).click();
  const formulario = page.getByRole("dialog");
  await formulario.getByLabel("Nombre").fill(nombre);
  await formulario.getByLabel("Monto (COP)").fill("70000");
  await formulario.getByLabel("Día del mes").fill("1");
  await formulario.getByRole("button", { name: "Guardar" }).click();

  const fila = page.locator("li").filter({ hasText: nombre });
  await fila.getByRole("button", { name: `Pagar ${nombre}` }).click();
  await page.getByRole("dialog").getByRole("button", { name: /Registrar el pago/ }).click();
  await expect(fila.getByText("Pagado")).toBeVisible();
});

test("crear una cuenta exige autorizar los datos, y luego se puede borrar", async ({ page }, info) => {
  const correo = `e2e-${info.project.name}-${Date.now()}@ejemplo.test`;
  const clave = `clave-${Math.random().toString(36).slice(2, 12)}`;
  await page.goto("/acceso");
  await page.getByRole("button", { name: "Regístrate" }).click();
  await page.getByLabel("Nombre").fill("Persona de Prueba");
  await page.getByLabel("Correo").fill(correo);
  await page.getByLabel("Contraseña").fill(clave);
  await page.getByRole("button", { name: /Crear cuenta/ }).click();
  await expect(page.getByText("Debes autorizar el tratamiento de tus datos")).toBeVisible();

  await page.getByRole("checkbox").check();
  await page.getByRole("button", { name: /Crear cuenta/ }).click();
  await expect(page.getByRole("heading", { name: "Resumen", level: 1 })).toBeVisible();

  await page.goto("/perfil");
  await page.getByRole("button", { name: "Borrar mi cuenta" }).click();
  await page.getByLabel("Escribe tu contraseña para confirmar").fill(clave);
  await page.getByRole("button", { name: /Sí, borrar todo/ }).click();
  await expect(page.getByRole("heading", { name: "Inicia sesión" })).toBeVisible();
});

test("una dirección que no existe muestra la 404", async ({ page }) => {
  await page.goto("/esto-no-existe");
  await expect(page.getByRole("heading", { name: "Este sobre está vacío." })).toBeVisible();
});

test("la página de inicio deja probar la demo con un clic", async ({ page }) => {
  await page.goto("/");
  await expect(page.getByRole("heading", { name: "Reparte la plata del mes en sobres.", level: 1 })).toBeVisible();
  await page.getByRole("button", { name: /Probar la demo/ }).click();
  await expect(page.getByRole("heading", { name: "Resumen", level: 1 })).toBeVisible();
  await expect(page).toHaveTitle("Resumen · CuentasClaras");
});
