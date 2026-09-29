// Formatos colombianos: $ 1.250.000 y "15 sept 2026".
const pesos = new Intl.NumberFormat("es-CO", {
  style: "currency",
  currency: "COP",
  maximumFractionDigits: 0,
});

// La API manda los montos como números con hasta 2 decimales; en pantalla se
// muestran sin centavos, como se usa normalmente con pesos.
export const formatearPesos = (valor) => pesos.format(Number(valor ?? 0));

// "2026-09-15" se interpreta como fecha local (no UTC) para que no se corra un día.
export function formatearFecha(texto, opciones = { day: "numeric", month: "short", year: "numeric" }) {
  const [a, m, d] = texto.split("-").map(Number);
  return new Date(a, m - 1, d).toLocaleDateString("es-CO", opciones);
}

export function mesActual() {
  const hoy = new Date();
  return `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, "0")}`;
}

export function hoyISO() {
  const hoy = new Date();
  return `${hoy.getFullYear()}-${String(hoy.getMonth() + 1).padStart(2, "0")}-${String(hoy.getDate()).padStart(2, "0")}`;
}

export function nombreMes(mes, largo = true) {
  const [a, m] = mes.split("-").map(Number);
  return new Date(a, m - 1, 1).toLocaleDateString("es-CO", { month: largo ? "long" : "short", year: largo ? "numeric" : undefined });
}

export function sumarMeses(mes, cantidad) {
  const [a, m] = mes.split("-").map(Number);
  const fecha = new Date(a, m - 1 + cantidad, 1);
  return `${fecha.getFullYear()}-${String(fecha.getMonth() + 1).padStart(2, "0")}`;
}

export const NOMBRES_TIPO_CUENTA = {
  EFECTIVO: "Efectivo",
  BILLETERA_DIGITAL: "Billetera digital",
  BANCO: "Banco",
};
