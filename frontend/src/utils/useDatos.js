import { useCallback, useEffect, useState } from "react";
import { api } from "../api/cliente";

/**
 * Carga datos de la API y los recarga cuando cambia la ruta. Devuelve también
 * "recargar" para refrescar después de crear, editar o borrar algo.
 */
export function useDatos(ruta) {
  const [datos, setDatos] = useState(null);
  const [error, setError] = useState("");

  const recargar = useCallback(async () => {
    try {
      setError("");
      setDatos(await api.get(ruta));
    } catch (e) {
      setError(e.message);
    }
  }, [ruta]);

  useEffect(() => {
    let vigente = true;
    api
      .get(ruta)
      .then((respuesta) => vigente && setDatos(respuesta))
      .catch((e) => vigente && setError(e.message));
    // Si la ruta cambia antes de que llegue la respuesta, se ignora la vieja.
    return () => {
      vigente = false;
    };
  }, [ruta]);

  return { datos, error, recargar };
}
