import { describe, expect, test } from "vitest";
import { formatearPesos, formatearPorcentaje, sumarMeses } from "./formato";
import { textoSobre } from "./colores";

describe("formatos colombianos", () => {
  test("pesos sin centavos y con punto de miles", () => {
    expect(formatearPesos(1250000).replace(/\s/g, " ")).toBe("$ 1.250.000");
    expect(formatearPesos("999.6").replace(/\s/g, " ")).toBe("$ 1.000");
  });

  test("porcentajes con coma decimal", () => {
    expect(formatearPorcentaje(86.12)).toBe("86,1 %");
  });

  test("sumar meses cruza el año", () => {
    expect(sumarMeses("2026-12", 1)).toBe("2027-01");
    expect(sumarMeses("2026-01", -1)).toBe("2025-12");
  });
});


describe("texto sobre un color", () => {
  test("blanco sobre colores oscuros y grafito sobre claros", () => {
    expect(textoSobre("#16181d")).toBe("#ffffff");
    expect(textoSobre("#6A4FC2")).toBe("#ffffff");
    expect(textoSobre("#B7791F")).toBe("#16181d");
    expect(textoSobre("#f3f4ef")).toBe("#16181d");
  });
});
