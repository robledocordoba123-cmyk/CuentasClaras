import { vi } from "vitest";

// Reemplaza fetch por respuestas fijas según método y ruta, y guarda cada petición
// para revisar qué envió la pantalla. Una ruta sin respuesta hace fallar la prueba.
// Una respuesta puede ser { estado, cuerpo } o directamente el cuerpo (200).
export function apiFalsa(rutas) {
  const peticiones = [];
  vi.spyOn(globalThis, "fetch").mockImplementation(async (url, opciones = {}) => {
    const metodo = opciones.method || "GET";
    const ruta = String(url);
    peticiones.push({ metodo, ruta, cuerpo: opciones.body ? JSON.parse(opciones.body) : undefined });
    const clave = Object.keys(rutas)
      .filter((k) => {
        const [m, prefijo] = k.split(" ");
        return m === metodo && ruta.startsWith(prefijo);
      })
      .sort((a, b) => b.length - a.length)[0];
    if (!clave) throw new Error(`Petición sin respuesta en la prueba: ${metodo} ${ruta}`);
    const valor = typeof rutas[clave] === "function" ? rutas[clave](ruta, opciones) : rutas[clave];
    const { estado, cuerpo } = valor && typeof valor === "object" && "estado" in valor ? valor : { estado: 200, cuerpo: valor };
    if (estado === 204) return new Response(null, { status: 204 });
    return new Response(JSON.stringify(cuerpo ?? {}), { status: estado, headers: { "Content-Type": "application/json" } });
  });
  return peticiones;
}
