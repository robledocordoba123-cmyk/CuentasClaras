import { ChevronLeft, ChevronRight } from "lucide-react";
import { mesActual, nombreMes, sumarMeses } from "../utils/formato";

/** Flechas para moverse entre meses. No deja pasar del mes actual. */
export default function SelectorMes({ mes, alCambiar }) {
  const esActual = mes >= mesActual();
  return (
    <div className="inline-flex items-center gap-1 rounded-lg border border-slate-300 bg-white p-1">
      <button
        onClick={() => alCambiar(sumarMeses(mes, -1))}
        className="rounded-md p-1.5 text-slate-600 hover:bg-slate-100"
        aria-label="Mes anterior"
      >
        <ChevronLeft size={18} />
      </button>
      <span className="min-w-36 text-center text-sm font-medium capitalize text-slate-800">{nombreMes(mes)}</span>
      <button
        onClick={() => alCambiar(sumarMeses(mes, 1))}
        disabled={esActual}
        className="rounded-md p-1.5 text-slate-600 hover:bg-slate-100 disabled:opacity-30"
        aria-label="Mes siguiente"
      >
        <ChevronRight size={18} />
      </button>
    </div>
  );
}
