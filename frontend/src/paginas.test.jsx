import { describe, expect, test } from "vitest";
import { render, screen, within } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import PersonasPage from "./pages/PersonasPage";
import PagosFijosPage from "./pages/PagosFijosPage";
import AccesoPage from "./pages/AccesoPage";
import { SesionProvider } from "./context/SesionContext";
import { apiFalsa } from "./pruebas/apiFalsa";

const PERSONAS = [
  { id: "a", nombre: "Andrés", teDebe: 0, leDebes: 150000, neto: -150000, deudasPendientes: 1 },
  { id: "m", nombre: "Mamá", teDebe: 0, leDebes: 0, neto: 0, deudasPendientes: 0 },
  { id: "v", nombre: "Valentina", teDebe: 40000, leDebes: 0, neto: 40000, deudasPendientes: 1 },
];
const DEUDAS = [
  { id: "d1", personaId: "v", persona: "Valentina", direccion: "ME_DEBEN", monto: 60000, abonado: 20000, pendiente: 40000,
    descripcion: "Almuerzo", fecha: "2026-11-01", saldada: false, abonos: [{ id: "ab1", monto: 20000, fecha: "2026-11-05" }] },
];

const conRouter = (ui) => render(<MemoryRouter>{ui}</MemoryRouter>);

describe("Personas", () => {
  test("cada saldo se dice con palabras, no solo con color", async () => {
    apiFalsa({ "GET /api/personas": PERSONAS, "GET /api/deudas": DEUDAS });
    conRouter(<PersonasPage />);
    expect(await screen.findByText("Le debes $ 150.000")).toBeInTheDocument();
    expect(screen.getByText("A paz y salvo")).toBeInTheDocument();
    expect(screen.getByText("Te debe $ 40.000")).toBeInTheDocument();
  });

  test("un abono de más muestra el motivo que da la API", async () => {
    const peticiones = apiFalsa({
      "GET /api/personas": PERSONAS,
      "GET /api/deudas": DEUDAS,
      "POST /api/deudas/d1/abonos": { estado: 422, cuerpo: { detail: "El abono supera lo pendiente ($ 40.000)." } },
    });
    const usuario = userEvent.setup();
    conRouter(<PersonasPage />);
    await usuario.click(await screen.findByRole("button", { name: /Valentina/ }));
    await usuario.click(screen.getByRole("button", { name: "Abonar" }));
    const dialogo = screen.getByRole("dialog");
    const monto = within(dialogo).getByLabelText(/Monto del abono/);
    await usuario.clear(monto);
    await usuario.type(monto, "50000");
    await usuario.click(within(dialogo).getByRole("button", { name: "Abonar" }));
    expect(await within(dialogo).findByRole("alert")).toHaveTextContent("El abono supera lo pendiente");
    expect(peticiones.find((p) => p.metodo === "POST").cuerpo.monto).toBe(50000);
  });
});

describe("Pagos fijos", () => {
  test("dice cuánto falta pagar y el estado de cada pago", async () => {
    const categoria = { id: "c", nombre: "Arriendo", color: "#6a4fc2" };
    const cuenta = { id: "b", nombre: "Banco" };
    apiFalsa({
      "GET /api/pagos-fijos": [
        { id: "1", nombre: "Arriendo", monto: 950000, diaDelMes: 3, categoria, cuenta, activo: true, vence: "2026-11-03", estado: "PAGADO", movimientoId: "m", montoPagado: 950000 },
        { id: "2", nombre: "Internet", monto: 89900, diaDelMes: 20, categoria, cuenta, activo: true, vence: "2026-11-20", estado: "VENCIDO", movimientoId: null, montoPagado: null },
      ],
      "GET /api/categorias": [],
      "GET /api/cuentas": [cuenta],
    });
    conRouter(<PagosFijosPage />);
    expect(await screen.findByText("Pagado")).toBeInTheDocument();
    expect(screen.getByText("Vencido")).toBeInTheDocument();
    expect(screen.getByText(/Te falta pagar/)).toHaveTextContent("$ 89.900");
    expect(screen.getByRole("button", { name: "Pagar Internet" })).toBeInTheDocument();
  });
});

describe("Registro", () => {
  test("envía la autorización de datos que marca la persona", async () => {
    const peticiones = apiFalsa({
      "GET /api/salud": { status: "UP" },
      "GET /api/auth/yo": { estado: 401, cuerpo: {} },
      "POST /api/auth/registro": { estado: 201, cuerpo: { usuario: { id: "u", nombre: "Ana", email: "ana@correo.com" } } },
    });
    const usuario = userEvent.setup();
    render(<MemoryRouter><SesionProvider><AccesoPage /></SesionProvider></MemoryRouter>);
    await usuario.click(await screen.findByRole("button", { name: "Regístrate" }));
    await usuario.type(screen.getByLabelText("Nombre"), "Ana");
    await usuario.type(screen.getByLabelText("Correo"), "ana@correo.com");
    await usuario.type(screen.getByLabelText("Contraseña"), "clave-segura-123");
    await usuario.click(screen.getByRole("checkbox"));
    await usuario.click(screen.getByRole("button", { name: /Crear cuenta/ }));
    await screen.findByRole("button", { name: /Crear cuenta/ });
    const envio = peticiones.find((p) => p.metodo === "POST");
    expect(envio.cuerpo).toMatchObject({ nombre: "Ana", email: "ana@correo.com", aceptaPolitica: true });
  });
});
