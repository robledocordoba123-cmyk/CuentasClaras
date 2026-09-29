import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { api, configurarAlExpirarSesion } from "../api/cliente";

const SesionContext = createContext(null);
const CLAVE = "cuentasclaras_sesion";

// La sesión (token + datos básicos) se guarda en localStorage para que un
// recargo de página no la cierre. Si el token ya venció, se descarta al leerla.
function leerSesion() {
  try {
    const sesion = JSON.parse(localStorage.getItem(CLAVE));
    if (sesion && new Date(sesion.expiraEn) > new Date()) return sesion;
  } catch {
    // localStorage puede no estar disponible (modo privado): se sigue sin sesión.
  }
  return null;
}

export function SesionProvider({ children }) {
  const [sesion, setSesion] = useState(leerSesion);

  useEffect(() => {
    try {
      if (sesion) localStorage.setItem(CLAVE, JSON.stringify(sesion));
      else localStorage.removeItem(CLAVE);
    } catch {
      // Sin almacenamiento: la sesión dura mientras la pestaña esté abierta.
    }
  }, [sesion]);

  const cerrarSesion = useCallback(() => setSesion(null), []);

  useEffect(() => {
    configurarAlExpirarSesion(cerrarSesion);
  }, [cerrarSesion]);

  const valor = useMemo(() => {
    const guardar = (respuesta) =>
      setSesion({ token: respuesta.token, expiraEn: respuesta.expiraEn, usuario: respuesta.usuario });
    return {
      token: sesion?.token,
      usuario: sesion?.usuario,
      iniciarSesion: async (email, password) => guardar(await api.post("/auth/login", { email, password })),
      registrarse: async (datos) => guardar(await api.post("/auth/registro", datos)),
      cerrarSesion,
    };
  }, [sesion, cerrarSesion]);

  return <SesionContext.Provider value={valor}>{children}</SesionContext.Provider>;
}

export function useSesion() {
  return useContext(SesionContext);
}
