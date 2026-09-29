// Piezas visuales reutilizables. Un solo lugar define cómo se ve un botón,
// un campo o una tarjeta, así toda la app es consistente.
import { X } from "lucide-react";
import { useEffect } from "react";

const VARIANTES = {
  primario: "bg-emerald-600 text-white hover:bg-emerald-700 shadow-sm",
  secundario: "bg-white text-slate-700 border border-slate-300 hover:bg-slate-50",
  peligro: "bg-white text-red-600 border border-red-200 hover:bg-red-50",
  fantasma: "text-slate-600 hover:bg-slate-100",
};

export function Boton({ variante = "primario", icono: Icono, className = "", children, ...props }) {
  return (
    <button
      {...props}
      className={`inline-flex items-center justify-center gap-1.5 rounded-lg px-3.5 py-2 text-sm font-medium transition disabled:cursor-not-allowed disabled:opacity-50 ${VARIANTES[variante]} ${className}`}
    >
      {Icono && <Icono size={16} aria-hidden="true" />}
      {children}
    </button>
  );
}

/** Campo de formulario: input, o select si recibe opciones como children. */
export function Campo({ etiqueta, error, children, ...props }) {
  const clase = `mt-1 w-full rounded-lg border px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-emerald-500 ${
    error ? "border-red-400" : "border-slate-300"
  }`;
  return (
    <label className="block text-sm font-medium text-slate-700">
      {etiqueta}
      {children ? (
        <select {...props} className={`${clase} bg-white`}>
          {children}
        </select>
      ) : (
        <input {...props} className={clase} />
      )}
      {error && <span className="mt-1 block text-xs font-normal text-red-600">{error}</span>}
    </label>
  );
}

export function Tarjeta({ titulo, accion, className = "", children }) {
  return (
    <section className={`rounded-2xl border border-slate-200 bg-white p-5 shadow-sm ${className}`}>
      {(titulo || accion) && (
        <div className="mb-4 flex items-center justify-between gap-3">
          {titulo && <h2 className="font-semibold text-slate-900">{titulo}</h2>}
          {accion}
        </div>
      )}
      {children}
    </section>
  );
}

export function EncabezadoPagina({ titulo, subtitulo, accion }) {
  return (
    <div className="mb-6 flex flex-wrap items-end justify-between gap-3">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">{titulo}</h1>
        {subtitulo && <p className="mt-0.5 text-sm text-slate-500">{subtitulo}</p>}
      </div>
      {accion}
    </div>
  );
}

export function EstadoVacio({ icono: Icono, titulo, descripcion, accion }) {
  return (
    <div className="flex flex-col items-center rounded-2xl border border-dashed border-slate-300 bg-white px-6 py-12 text-center">
      {Icono && <Icono size={36} className="mb-3 text-slate-400" aria-hidden="true" />}
      <p className="font-medium text-slate-800">{titulo}</p>
      {descripcion && <p className="mt-1 max-w-sm text-sm text-slate-500">{descripcion}</p>}
      {accion && <div className="mt-4">{accion}</div>}
    </div>
  );
}

export function Aviso({ tipo = "error", children }) {
  const estilos = {
    error: "bg-red-50 text-red-700 border-red-200",
    info: "bg-sky-50 text-sky-800 border-sky-200",
  };
  return <p className={`rounded-lg border px-3 py-2 text-sm ${estilos[tipo]}`}>{children}</p>;
}

/** Ventana modal: se cierra con Escape, con la X o haciendo clic afuera. */
export function Modal({ titulo, abierto, alCerrar, children }) {
  useEffect(() => {
    if (!abierto) return undefined;
    const alPresionar = (evento) => {
      if (evento.key === "Escape") alCerrar();
    };
    window.addEventListener("keydown", alPresionar);
    return () => window.removeEventListener("keydown", alPresionar);
  }, [abierto, alCerrar]);

  if (!abierto) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-end justify-center bg-slate-900/40 p-4 sm:items-center" onClick={alCerrar}>
      <div
        role="dialog"
        aria-modal="true"
        aria-label={titulo}
        className="max-h-[90vh] w-full max-w-md overflow-y-auto rounded-2xl bg-white p-6 shadow-xl"
        onClick={(evento) => evento.stopPropagation()}
      >
        <div className="mb-4 flex items-center justify-between">
          <h2 className="text-lg font-semibold text-slate-900">{titulo}</h2>
          <button onClick={alCerrar} className="rounded-lg p-1 text-slate-500 hover:bg-slate-100" aria-label="Cerrar">
            <X size={18} />
          </button>
        </div>
        {children}
      </div>
    </div>
  );
}

export function Cargando() {
  return (
    <div className="space-y-3" aria-busy="true" aria-label="Cargando">
      {[1, 2, 3].map((i) => (
        <div key={i} className="h-16 animate-pulse rounded-xl bg-slate-200/70" />
      ))}
    </div>
  );
}
