// Guilloche: las líneas finas entrelazadas de los billetes y los cheques, dibujadas
// con matemática (una onda alrededor de un círculo), así son propias y no copian
// ningún billete. Los archivos los genera scripts/generar-guilloche.mjs: como
// imagen, el navegador no los recalcula en cada diseño de la página.
// Es decorativo: los lectores de pantalla lo ignoran.
const ARCHIVOS = { turquesa: "/guilloche-turquesa.svg", ocre: "/guilloche-ocre.svg" };

export default function Guilloche({ variante = "turquesa", className = "", opacidad = 0.35 }) {
  return <img src={ARCHIVOS[variante]} alt="" aria-hidden="true" decoding="async" className={className} style={{ opacity: opacidad }} />;
}
