import { describe, expect, test, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import { MemoryRouter } from "react-router-dom";
import Sobre from "./components/Sobre";
import LimiteDeError from "./components/LimiteDeError";
import NoEncontradaPage from "./pages/NoEncontradaPage";
import { esperarApi } from "./api/cliente";
import { apiFalsa } from "./pruebas/apiFalsa";

describe("Sobre", () => {
  const billetes = (contenedor) => contenedor.querySelectorAll("g[transform]").length;

  test("lleno muestra los billetes y vacío ninguno", () => {
    const { container: lleno } = render(<Sobre restante={1} />);
    expect(billetes(lleno)).toBe(3);
    const { container: vacio } = render(<Sobre restante={0} excedido />);
    expect(billetes(vacio)).toBe(0);
    // Excedido: la solapa en carmín.
    expect(vacio.querySelector('path[stroke="#c23b5a"]')).not.toBeNull();
  });

  test("es decorativo: los lectores de pantalla lo ignoran", () => {
    const { container } = render(<Sobre restante={0.5} />);
    expect(container.querySelector("svg")).toHaveAttribute("aria-hidden", "true");
  });
});

describe("Esperar a que la API despierte", () => {
  test("un 401 también significa que el servidor ya respondió", async () => {
    apiFalsa({ "GET /api/salud": { estado: 401, cuerpo: {} } });
    expect(await esperarApi({ intentos: 1, esperaMs: 0 })).toBe(true);
  });

  test("reintenta mientras el proxy responde 503", async () => {
    let llamadas = 0;
    apiFalsa({ "GET /api/salud": () => (++llamadas < 3 ? { estado: 503, cuerpo: {} } : { estado: 200, cuerpo: { status: "UP" } }) });
    expect(await esperarApi({ intentos: 5, esperaMs: 0 })).toBe(true);
    expect(llamadas).toBe(3);
  });

  test("se rinde después de los intentos", async () => {
    apiFalsa({ "GET /api/salud": { estado: 504, cuerpo: {} } });
    expect(await esperarApi({ intentos: 2, esperaMs: 0 })).toBe(false);
  });
});

describe("Pantallas de aviso", () => {
  function Rompe() {
    throw new Error("falla de prueba");
  }

  test("un error al dibujar muestra una salida en vez de la pantalla en blanco", () => {
    vi.spyOn(console, "error").mockImplementation(() => {});
    render(<LimiteDeError><Rompe /></LimiteDeError>);
    expect(screen.getByRole("heading", { name: "Algo se nos descuadró." })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: "Recargar" })).toBeInTheDocument();
  });

  test("la 404 pide no indexarse y ofrece volver", () => {
    render(<MemoryRouter><NoEncontradaPage /></MemoryRouter>);
    expect(screen.getByRole("heading", { name: "Este sobre está vacío." })).toBeInTheDocument();
    expect(document.querySelector('meta[name="robots"]')).toHaveAttribute("content", "noindex");
  });
});
