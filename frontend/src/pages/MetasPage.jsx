import { useState } from "react";
import { Minus, Plus, Target, Trash2 } from "lucide-react";
import { api } from "../api/cliente";
import { useDatos } from "../utils/useDatos";
import { formatearFecha, formatearPesos, formatearPorcentaje, hoyISO } from "../utils/formato";
import { Aviso, Boton, Campo, Cargando, EncabezadoPagina, EstadoVacio, Modal } from "../components/ui";
import Sobre from "../components/Sobre";
import Pestanas from "../components/Pestanas";
import { GRUPO_SOBRES } from "../utils/grupos";

// Metas de ahorro: un sobre que, al revés de los del mes, se va llenando.
export default function MetasPage() {
  const { datos: metas, error, recargar } = useDatos("/metas");
  const [formulario, setFormulario] = useState(null);
  const [aporte, setAporte] = useState(null);
  const [errorForm, setErrorForm] = useState({ mensaje: "", campos: {} });
  const [guardando, setGuardando] = useState(false);

  async function ejecutar(e, accion, alTerminar) {
    e.preventDefault();
    setGuardando(true);
    setErrorForm({ mensaje: "", campos: {} });
    try {
      await accion();
      alTerminar();
      recargar();
    } catch (err) {
      setErrorForm({ mensaje: err.message, campos: err.errores });
    } finally {
      setGuardando(false);
    }
  }

  async function eliminar(m) {
    if (!window.confirm(`¿Borrar la meta "${m.nombre}" y sus aportes?`)) return;
    await api.delete(`/metas/${m.id}`);
    recargar();
  }

  const abrirAporte = (meta, signo) => {
    setErrorForm({ mensaje: "", campos: {} });
    setAporte({ meta, signo, monto: signo > 0 && meta.aporteMensual ? String(Number(meta.aporteMensual)) : "", fecha: hoyISO() });
  };

  return (
    <div>
      <EncabezadoPagina titulo="Metas de ahorro" subtitulo="Un sobre para lo que quieres: se llena con cada aporte."
        accion={<Boton icono={Plus} onClick={() => { setErrorForm({ mensaje: "", campos: {} }); setFormulario({ nombre: "", montoObjetivo: "", fechaObjetivo: "" }); }}>Nueva meta</Boton>} />
      <Pestanas grupo={GRUPO_SOBRES} etiqueta="Sobres" />
      {error && <Aviso>{error}</Aviso>}

      {!metas ? (
        <Cargando />
      ) : metas.length === 0 ? (
        <EstadoVacio icono={Target} titulo="Aún no tienes metas" descripcion="Un viaje, un computador o un fondo para emergencias: ponle un monto y, si quieres, una fecha." />
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {metas.map((m) => (
            <article key={m.id} className="flex gap-3 rounded-2xl border border-slate-200 bg-panel p-4 sm:gap-4">
              <Sobre color={m.cumplida ? "#2d7d55" : "#0f7a8a"} restante={Number(m.porcentaje) / 100} className="h-16 w-20 shrink-0 sm:h-24 sm:w-28" />
              <div className="min-w-0 flex-1">
                <div className="flex flex-wrap items-start justify-between gap-x-2 gap-y-1">
                  <h2 className="font-display text-lg font-semibold text-slate-900">{m.nombre}</h2>
                  <div className="flex items-center gap-1">
                    {m.cumplida && <span className="rounded-full bg-emerald-50 px-2 py-0.5 text-xs font-semibold text-emerald-700">¡Lo lograste!</span>}
                    <button onClick={() => eliminar(m)} className="rounded-lg p-1.5 text-slate-400 hover:bg-red-50 hover:text-red-600" aria-label={`Borrar la meta ${m.nombre}`}>
                      <Trash2 size={15} />
                    </button>
                  </div>
                </div>
                <p className="cifra mt-1 text-2xl font-semibold text-slate-900">{formatearPesos(m.ahorrado)}</p>
                <p className="text-sm text-slate-500">de {formatearPesos(m.montoObjetivo)} · {formatearPorcentaje(m.porcentaje)}</p>
                <div className="mt-2 h-1.5 overflow-hidden rounded-full bg-slate-100" role="progressbar" aria-label={`Avance de ${m.nombre}`} aria-valuenow={Math.round(Number(m.porcentaje))} aria-valuemin={0} aria-valuemax={100}>
                  <div className="h-full rounded-full bg-brillo" style={{ width: `${Math.min(Number(m.porcentaje), 100)}%` }} />
                </div>
                {m.aporteMensual && (
                  <p className="mt-2 text-sm text-slate-700">
                    Aparta <strong className="cifra">{formatearPesos(m.aporteMensual)}</strong> al mes para llegar el {formatearFecha(m.fechaObjetivo, { day: "numeric", month: "long", year: "numeric" })}.
                  </p>
                )}
                <div className="mt-3 flex gap-2">
                  {!m.cumplida && <Boton variante="secundario" icono={Plus} onClick={() => abrirAporte(m, 1)}>Aportar</Boton>}
                  {Number(m.ahorrado) > 0 && <Boton variante="fantasma" icono={Minus} onClick={() => abrirAporte(m, -1)}>Sacar</Boton>}
                </div>
              </div>
            </article>
          ))}
        </div>
      )}

      <Modal titulo="Nueva meta" abierto={formulario !== null} alCerrar={() => setFormulario(null)}>
        {formulario && (
          <form onSubmit={(e) => ejecutar(e, () => api.post("/metas", { nombre: formulario.nombre, montoObjetivo: formulario.montoObjetivo === "" ? null : Number(formulario.montoObjetivo), fechaObjetivo: formulario.fechaObjetivo || null }), () => setFormulario(null))}
            className="space-y-4" noValidate>
            <Campo etiqueta="¿Para qué ahorras?" value={formulario.nombre} onChange={(e) => setFormulario({ ...formulario, nombre: e.target.value })} error={errorForm.campos.nombre} placeholder="Ej: viaje a Cartagena" />
            <Campo etiqueta="Cuánto necesitas (COP)" type="number" min="0" value={formulario.montoObjetivo} onChange={(e) => setFormulario({ ...formulario, montoObjetivo: e.target.value })} error={errorForm.campos.montoObjetivo} />
            <Campo etiqueta="Para cuándo (opcional)" type="date" min={hoyISO()} value={formulario.fechaObjetivo} onChange={(e) => setFormulario({ ...formulario, fechaObjetivo: e.target.value })} />
            {errorForm.mensaje && Object.keys(errorForm.campos).length === 0 && <Aviso>{errorForm.mensaje}</Aviso>}
            <Boton type="submit" disabled={guardando} className="w-full">{guardando ? "Guardando…" : "Crear meta"}</Boton>
          </form>
        )}
      </Modal>

      <Modal titulo={aporte?.signo > 0 ? `Aportar a ${aporte?.meta.nombre}` : `Sacar de ${aporte?.meta.nombre ?? ""}`} abierto={aporte !== null} alCerrar={() => setAporte(null)}>
        {aporte && (
          <form onSubmit={(e) => ejecutar(e, () => api.post(`/metas/${aporte.meta.id}/aportes`, { monto: aporte.monto === "" ? null : aporte.signo * Number(aporte.monto), fecha: aporte.fecha }), () => setAporte(null))}
            className="space-y-4" noValidate>
            <Campo etiqueta="Monto (COP)" type="number" min="0" value={aporte.monto} onChange={(e) => setAporte({ ...aporte, monto: e.target.value })} error={errorForm.campos.monto} />
            <Campo etiqueta="Fecha" type="date" max={hoyISO()} value={aporte.fecha} onChange={(e) => setAporte({ ...aporte, fecha: e.target.value })} />
            {errorForm.mensaje && Object.keys(errorForm.campos).length === 0 && <Aviso>{errorForm.mensaje}</Aviso>}
            <Boton type="submit" disabled={guardando} className="w-full">{guardando ? "Guardando…" : aporte.signo > 0 ? "Aportar" : "Sacar"}</Boton>
          </form>
        )}
      </Modal>
    </div>
  );
}
