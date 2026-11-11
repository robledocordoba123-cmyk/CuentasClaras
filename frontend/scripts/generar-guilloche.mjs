// Genera los guilloches como archivos SVG en public/. Se dibujan una sola vez aquí:
// dentro de la página como SVG en línea, sus miles de puntos se volvían a calcular
// en cada diseño y bloqueaban la página (Lighthouse). Como imagen cuestan muy poco.
// Uso: node scripts/generar-guilloche.mjs
import { writeFileSync } from "node:fs";

const PASOS = 360;

function curva(cx, cy, radio, amplitud, ondas, fase) {
  const puntos = [];
  for (let i = 0; i <= PASOS; i += 1) {
    const t = (i / PASOS) * Math.PI * 2;
    const r = radio + amplitud * Math.sin(ondas * t + fase);
    puntos.push(`${(cx + r * Math.cos(t)).toFixed(1)},${(cy + r * Math.sin(t)).toFixed(1)}`);
  }
  return `M${puntos.join("L")}Z`;
}

function guilloche(color, lineas, ondas) {
  const trazos = Array.from({ length: lineas }, (_, i) => curva(200, 200, 120 + i * 4, 26 + (i % 4) * 3, ondas, (i * Math.PI) / lineas));
  return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 400 400"><g fill="none" stroke="${color}" stroke-width="0.8">${trazos
    .map((d) => `<path d="${d}"/>`)
    .join("")}</g></svg>\n`;
}

writeFileSync(new URL("../public/guilloche-turquesa.svg", import.meta.url), guilloche("#3ec3d4", 14, 18));
writeFileSync(new URL("../public/guilloche-ocre.svg", import.meta.url), guilloche("#b7791f", 10, 12));
console.log("Guilloches generados en public/");
