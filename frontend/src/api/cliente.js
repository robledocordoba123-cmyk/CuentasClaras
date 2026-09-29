// Cliente HTTP de la API. Centraliza la URL base, el token y el formato de
// errores: la API responde siempre con ProblemDetail (RFC 9457), así que el
// mensaje está en "detail" y los errores por campo en "errores".
const URL_BASE = import.meta.env.VITE_API_URL || "/api";

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

async function solicitar(ruta, { metodo = "GET", cuerpo, token } = {}) {
  const cabeceras = {};
  if (cuerpo !== undefined) cabeceras["Content-Type"] = "application/json";
  if (token) cabeceras.Authorization = `Bearer ${token}`;

  let respuesta;
  try {
    respuesta = await fetch(`${URL_BASE}${ruta}`, {
      method: metodo,
      headers: cabeceras,
      body: cuerpo !== undefined ? JSON.stringify(cuerpo) : undefined,
    });
  } catch {
    throw new ErrorDeApi(0, "No se pudo conectar con el servidor. Revisa tu conexión.");
  }

  if (respuesta.status === 204) return null;
  const datos = await respuesta.json().catch(() => null);

  if (!respuesta.ok) {
    // Token vencido o inválido en una ruta protegida: se cierra la sesión.
    if (respuesta.status === 401 && token) alExpirarSesion();
    throw new ErrorDeApi(
      respuesta.status,
      datos?.detail || "Ocurrió un error inesperado.",
      datos?.errores || {}
    );
  }
  return datos;
}

/** Descarga un archivo (el CSV) respetando el token. */
async function descargar(ruta, token, nombreArchivo) {
  const respuesta = await fetch(`${URL_BASE}${ruta}`, { headers: { Authorization: `Bearer ${token}` } });
  if (!respuesta.ok) throw new ErrorDeApi(respuesta.status, "No se pudo descargar el archivo.");
  const enlace = document.createElement("a");
  enlace.href = URL.createObjectURL(await respuesta.blob());
  enlace.download = nombreArchivo;
  enlace.click();
  URL.revokeObjectURL(enlace.href);
}

export const api = {
  get: (ruta, token) => solicitar(ruta, { token }),
  post: (ruta, cuerpo, token) => solicitar(ruta, { metodo: "POST", cuerpo, token }),
  put: (ruta, cuerpo, token) => solicitar(ruta, { metodo: "PUT", cuerpo, token }),
  patch: (ruta, cuerpo, token) => solicitar(ruta, { metodo: "PATCH", cuerpo, token }),
  delete: (ruta, token) => solicitar(ruta, { metodo: "DELETE", token }),
  descargar,
};
