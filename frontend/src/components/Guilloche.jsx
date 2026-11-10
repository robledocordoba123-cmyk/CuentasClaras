// Guilloche: las líneas finas entrelazadas de los billetes y los cheques. Aquí se
// dibujan con matemática (una onda alrededor de un círculo), así son propias y no
// copian ningún billete. Es decorativo: los lectores de pantalla lo ignoran.
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

export default function Guilloche({ color = "currentColor", lineas = 14, ondas = 18, className = "", opacidad = 0.35 }) {
  const trazos = Array.from({ length: lineas }, (_, i) => curva(200, 200, 120 + i * 4, 26 + (i % 4) * 3, ondas, (i * Math.PI) / lineas));
  return (
    <svg viewBox="0 0 400 400" className={className} aria-hidden="true" focusable="false">
      <g fill="none" stroke={color} strokeWidth="0.8" strokeOpacity={opacidad}>
        {trazos.map((d, i) => <path key={i} d={d} />)}
      </g>
    </svg>
  );
}
