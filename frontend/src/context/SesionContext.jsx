import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { api, configurarAlExpirarSesion, esperarApi } from "../api/cliente";

const SesionContext = createContext(null);

// Marca, sin ningún dato, de que en este navegador hubo una sesión. Si no la hay,
// lo público se muestra de una vez y la sesión se comprueba por detrás: la página
// de inicio no espera a que despierte la API.
const MARCA = "cc_hubo_sesion";
function leerMarca() {
  try {
    return localStorage.getItem(MARCA) === "1";
  } catch {
    return false;
  }
}
function guardarMarca(hay) {
  try {
    if (hay) localStorage.setItem(MARCA, "1");
    else localStorage.removeItem(MARCA);
  } catch {
    // Sin almacenamiento: siempre se comprueba por detrás.
  }
}

// La sesión vive en una cookie HttpOnly que pone la API: aquí solo se guarda
// quién es la persona. Al abrir la app se pregunta a la API si hay sesión.
export function SesionProvider({ children }) {
  const [estado, setEstado] = useState(() => (leerMarca() ? "comprobando" : "lista")); // comprobando | lista
  const [usuario, setUsuario] = useState(null);

  useEffect(() => {
    // Versiones anteriores guardaban el token en localStorage: se borra.
    try {
      localStorage.removeItem("cuentasclaras_sesion");
    } catch {
      // Sin almacenamiento no hay nada que borrar.
    }
    let vigente = true;
    if (!leerMarca()) {
      // Sin sesión previa no hay nada que comprobar: solo se despierta la API en
      // segundo plano, para que entrar a la demo o iniciar sesión responda rápido.
      esperarApi().catch(() => {});
      return () => {
        vigente = false;
      };
    }
    esperarApi()
      .then(() => api.get("/auth/yo"))
      .then((yo) => vigente && setUsuario(yo))
      .catch(() => {
        if (!vigente) return;
        setUsuario(null);
        guardarMarca(false);
      })
      .finally(() => vigente && setEstado("lista"));
    return () => {
      vigente = false;
    };
  }, []);

  const cerrarSesion = useCallback(async () => {
    setUsuario(null);
    guardarMarca(false);
    await api.post("/auth/logout").catch(() => {});
  }, []);

  useEffect(() => {
    configurarAlExpirarSesion(() => setUsuario(null));
  }, []);

  const valor = useMemo(
    () => ({
      estado,
      usuario,
      iniciarSesion: async (email, password) => {
        setUsuario((await api.post("/auth/login", { email, password })).usuario);
        guardarMarca(true);
      },
      registrarse: async (datos) => {
        setUsuario((await api.post("/auth/registro", datos)).usuario);
        guardarMarca(true);
      },
      cerrarSesion,
      olvidarSesion: () => {
        setUsuario(null);
        guardarMarca(false);
      },
    }),
    [estado, usuario, cerrarSesion]
  );

  return <SesionContext.Provider value={valor}>{children}</SesionContext.Provider>;
}

export function useSesion() {
  return useContext(SesionContext);
}
