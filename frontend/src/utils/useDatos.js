import { useCallback, useEffect, useState } from "react";
import { api } from "../api/cliente";
import { useSesion } from "../context/SesionContext";

/**
 * Carga datos de la API y los recarga cuando cambia la ruta. Devuelve también
 * "recargar" para refrescar después de crear, editar o borrar algo.
 */
export function useDatos(ruta) {
  const { token } = useSesion();
  const [datos, setDatos] = useState(null);
  const [error, setError] = useState("");

  const recargar = useCallback(async () => {
    try {
      setError("");
      setDatos(await api.get(ruta, token));
    } catch (e) {
      setError(e.message);
    }
  }, [ruta, token]);

  useEffect(() => {
    let vigente = true;
    api
      .get(ruta, token)
      .then((respuesta) => vigente && setDatos(respuesta))
      .catch((e) => vigente && setError(e.message));
    // Si la ruta cambia antes de que llegue la respuesta, se ignora la vieja.
    return () => {
      vigente = false;
    };
  }, [ruta, token]);

  return { datos, error, recargar };
}
