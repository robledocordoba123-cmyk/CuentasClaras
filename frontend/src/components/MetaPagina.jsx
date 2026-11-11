import { useEffect } from "react";

const SITIO = "https://cuentasclaras-demo.vercel.app";

// Título de la pestaña y lo que ven los buscadores. Las pantallas con sesión piden
// no aparecer en Google; las públicas dicen cuál es su dirección oficial.
export default function MetaPagina({ titulo, ruta, privada = false }) {
  useEffect(() => {
    document.title = titulo ? `${titulo} · CuentasClaras` : "CuentasClaras · Reparte la plata del mes en sobres";
  }, [titulo]);
  return privada ? <meta name="robots" content="noindex, nofollow" /> : <link rel="canonical" href={`${SITIO}${ruta}`} />;
}
