import { expect, test } from "@playwright/test";
import AxeBuilder from "@axe-core/playwright";

// Revisión automática con axe (WCAG 2.1 A y AA) de cada pantalla. No reemplaza
// probar con lector de pantalla, pero atrapa contraste, nombres y etiquetas.
// Espera a que la pantalla haya terminado de traer sus datos.
async function listo(page) {
  await expect(page.locator('[aria-busy="true"]')).toHaveCount(0);
  await page.waitForTimeout(300);
}

async function sinProblemas(page, contexto) {
  const { violations } = await new AxeBuilder({ page }).withTags(["wcag2a", "wcag2aa", "wcag21a", "wcag21aa"]).analyze();
  const resumen = violations.map((v) => `${v.id} (${v.impact}): ${v.help} → ${v.nodes.slice(0, 3).map((n) => n.target.join(" ")).join(" | ")}`);
  expect(resumen, contexto).toEqual([]);
}

test("pantallas públicas", async ({ page }) => {
  for (const ruta of ["/acceso", "/privacidad", "/no-existe"]) {
    await page.goto(ruta);
    await listo(page);
    await sinProblemas(page, ruta);
  }
});

test("cada pantalla con sesión", async ({ page }) => {
  await page.goto("/acceso");
  await page.getByRole("button", { name: /Entrar a la demo/ }).click();
  await expect(page.getByRole("heading", { name: "Resumen", level: 1 })).toBeVisible();
  for (const ruta of ["/", "/movimientos", "/pagos-fijos", "/categorias", "/cuentas", "/presupuestos", "/metas", "/personas", "/perfil"]) {
    await page.goto(ruta);
    await expect(page.getByRole("heading", { level: 1 })).toBeVisible();
    await listo(page);
    await sinProblemas(page, ruta);
  }
});
