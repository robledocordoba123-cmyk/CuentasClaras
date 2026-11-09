// Cliente HTTP de la API. Siempre llama a /api en la misma dirección de la
// página: en desarrollo la redirige Vite y al publicar la reenvía Vercel. Así
// la cookie de sesión (HttpOnly) es del mismo sitio y el navegador la manda
// sola; el JavaScript nunca ve el token.
// La API responde siempre con ProblemDetail (RFC 9457): el mensaje está en
// "detail" y los errores por campo en "errores".
const URL_BASE = "/api";

export class ErrorDeApi extends Error {
  constructor(estado, mensaje, errores = {}) {
    super(mensaje);
    this.estado = estado;
    this.errores = errores;
  }
}

let alExpirarSesion = () => {};
export function configurarAlExpirarSesion(funcion) {
  alExpirarSesion = funcion;
}

// Rutas donde un 401 significa "datos incorrectos", no "la sesión venció".
const RUTAS_DE_ACCESO = ["/auth/login", "/auth/registro", "/auth/yo"];

async function solicitar(ruta, { metodo = "GET", cuerpo } = {}) {
  let respuesta;
  try {
    respuesta = await fetch(`${URL_BASE}${ruta}`, {
      method: metodo,
      headers: cuerpo !== undefined ? { "Content-Type": "application/json" } : {},
      body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined,
    });
  } catch {
    throw new ErrorDeApi(0, "No se pudo conectar con el servidor. Revisa tu conexión.");
  }

  if (respuesta.status === 204) return null;
  const datos = await respuesta.json().catch(() => null);

  if (!respuesta.ok) {
    if (respuesta.status === 401 && !RUTAS_DE_ACCESO.includes(ruta)) alExpirarSesion();
    throw new ErrorDeApi(respuesta.status, datos?.detail || "Ocurrió un error inesperado.", datos?.errores || {});
  }
  return datos;
}

/**
 * El plan gratuito apaga la API tras un rato sin uso y tarda hasta un par de
 * minutos en encender. Antes de lo demás se pregunta por su salud, con paciencia.
 */
export async function esperarApi({ intentos = 30, esperaMs = 5000 } = {}) {
  for (let i = 0; i < intentos; i += 1) {
    try {
      const respuesta = await fetch(`${URL_BASE}/salud`);
      if (respuesta.ok) return true;
    } catch {
      // Sin conexión o todavía encendiendo: se vuelve a intentar.
    }
    await new Promise((listo) => setTimeout(listo, esperaMs));
  }
  return false;
}

/** Descarga un archivo (CSV o JSON) con la sesión actual. */
async function descargar(ruta, nombreArchivo) {
  const respuesta = await fetch(`${URL_BASE}${ruta}`);
  if (!respuesta.ok) throw new ErrorDeApi(respuesta.status, "No se pudo descargar el archivo.");
  const enlace = document.createElement("a");
  enlace.href = URL.createObjectURL(await respuesta.blob());
  enlace.download = nombreArchivo;
  enlace.click();
  URL.revokeObjectURL(enlace.href);
}

export const api = {
  get: (ruta) => solicitar(ruta),
  post: (ruta, cuerpo) => solicitar(ruta, { metodo: "POST", cuerpo }),
  put: (ruta, cuerpo) => solicitar(ruta, { metodo: "PUT", cuerpo }),
  patch: (ruta, cuerpo) => solicitar(ruta, { metodo: "PATCH", cuerpo }),
  delete: (ruta, cuerpo) => solicitar(ruta, { metodo: "DELETE", cuerpo }),
  descargar,
};
