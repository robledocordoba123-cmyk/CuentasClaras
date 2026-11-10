// Un presupuesto es un sobre: empieza el mes lleno y se vacía a medida que gastas.
// Los billetes que asoman son lo que queda; si te pasaste, el sobre queda vacío y
// su solapa se pone carmín. El dibujo es decorativo: el texto al lado dice las cifras.
export default function Sobre({ color = "#138a9c", restante = 1, excedido = false, className = "" }) {
  const queda = Math.max(0, Math.min(1, restante));
  // Cuánto asoman los billetes por encima de la boca del sobre (de 0 a 34 unidades).
  const asoma = Math.round(queda * 34);
  const billetes = queda > 0 ? [0, 1, 2] : [];
  return (
    <svg viewBox="0 0 120 96" className={className} aria-hidden="true" focusable="false">
      {/* Fondo del sobre */}
      <rect x="6" y="32" width="108" height="58" rx="7" fill={color} fillOpacity="0.14" stroke={color} strokeOpacity="0.5" />
      {/* Billetes: más altos cuanto más queda */}
      {billetes.map((i) => (
        <g key={i} transform={`translate(${18 + i * 6} ${40 - asoma + i * 3})`}>
          <rect width="72" height="40" rx="4" fill="#ffffff" stroke={color} strokeWidth="1.2" />
          <rect x="5" y="5" width="62" height="30" rx="2.5" fill={color} fillOpacity="0.16" />
          <circle cx="36" cy="20" r="8" fill="none" stroke={color} strokeOpacity="0.6" />
        </g>
      ))}
      {/* Bolsillo delantero, encima de los billetes */}
      <path d="M6 44 L60 70 L114 44 V83 a7 7 0 0 1 -7 7 H13 a7 7 0 0 1 -7 -7 Z" fill="#ffffff" stroke={excedido ? "#c23b5a" : color} strokeWidth="1.4" />
      <path d="M6 44 L60 70 L114 44" fill="none" stroke={excedido ? "#c23b5a" : color} strokeWidth={excedido ? 2.4 : 1.4} strokeLinejoin="round" />
    </svg>
  );
}
