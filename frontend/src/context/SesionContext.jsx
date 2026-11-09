import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { api, configurarAlExpirarSesion, esperarApi } from "../api/cliente";

const SesionContext = createContext(null);

// La sesión vive en una cookie HttpOnly que pone la API: aquí solo se guarda
// quién es la persona. Al abrir la app se pregunta a la API si hay sesión.
export function SesionProvider({ children }) {
  const [estado, setEstado] = useState("comprobando"); // comprobando | lista
  const [usuario, setUsuario] = useState(null);

  useEffect(() => {
    // Versiones anteriores guardaban el token en localStorage: se borra.
    try {
      localStorage.removeItem("cuentasclaras_sesion");
    } catch {
      // Sin almacenamiento no hay nada que borrar.
    }
    let vigente = true;
    esperarApi()
      .then(() => api.get("/auth/yo"))
      .then((yo) => vigente && setUsuario(yo))
      .catch(() => vigente && setUsuario(null))
      .finally(() => vigente && setEstado("lista"));
    return () => {
      vigente = false;
    };
  }, []);

  const cerrarSesion = useCallback(async () => {
    setUsuario(null);
    await api.post("/auth/logout").catch(() => {});
  }, []);

  useEffect(() => {
    configurarAlExpirarSesion(() => setUsuario(null));
  }, []);

  const valor = useMemo(
    () => ({
      estado,
      usuario,
      iniciarSesion: async (email, password) => setUsuario((await api.post("/auth/login", { email, password })).usuario),
      registrarse: async (datos) => setUsuario((await api.post("/auth/registro", datos)).usuario),
      cerrarSesion,
      olvidarSesion: () => setUsuario(null),
    }),
    [estado, usuario, cerrarSesion]
  );

  return <SesionContext.Provider value={valor}>{children}</SesionContext.Provider>;
}

export function useSesion() {
  return useContext(SesionContext);
}
