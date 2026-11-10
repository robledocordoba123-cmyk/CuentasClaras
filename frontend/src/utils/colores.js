// Cada persona elige el color de sus categorías, así que el texto encima no puede
// ser siempre blanco: se elige blanco o grafito según cuál contraste más (WCAG).
function luminancia(hex) {
  const limpio = hex.replace("#", "");
  const [r, g, b] = [0, 2, 4].map((i) => parseInt(limpio.slice(i, i + 2), 16) / 255)
    .map((c) => (c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4));
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

const contraste = (a, b) => (Math.max(a, b) + 0.05) / (Math.min(a, b) + 0.05);

const BLANCO = "#ffffff";
const TINTA = "#16181d";

export function textoSobre(colorDeFondo) {
  const fondo = luminancia(colorDeFondo);
  return contraste(fondo, luminancia(BLANCO)) >= contraste(fondo, luminancia(TINTA)) ? BLANCO : TINTA;
}
