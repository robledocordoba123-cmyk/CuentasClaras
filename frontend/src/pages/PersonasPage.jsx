import { useState } from "react";
import { ChevronDown, HandCoins, Plus, Trash2, UserPlus, Users } from "lucide-react";
import { api } from "../api/cliente";
import { useDatos } from "../utils/useDatos";
import { formatearFecha, formatearPesos, hoyISO } from "../utils/formato";
import { Aviso, Boton, Campo, Cargando, EncabezadoPagina, EstadoVacio, Modal } from "../components/ui";

// Lo que dice cada saldo, sin depender del color.
function frase(p) {
  const neto = Number(p.neto);
  if (neto > 0) return { texto: `Te debe ${formatearPesos(neto)}`, clase: "text-emerald-700" };
  if (neto < 0) return { texto: `Le debes ${formatearPesos(-neto)}`, clase: "text-red-700" };
  return { texto: "A paz y salvo", clase: "text-slate-500" };
}

function Persona({ persona, deudas, alAbonar, alCambiar }) {
  const [abierta, setAbierta] = useState(false);
  const { texto, clase } = frase(persona);
  const propias = deudas.filter((d) => d.personaId === persona.id);

  async function borrarDeuda(d) {
    if (!window.confirm(`¿Borrar "${d.descripcion}" y sus abonos?`)) return;
    await api.delete(`/deudas/${d.id}`);
    alCambiar();
  }

  async function quitarAbono(d, a) {
    if (!window.confirm(`¿Quitar el abono de ${formatearPesos(a.monto)}?`)) return;
    await api.delete(`/deudas/${d.id}/abonos/${a.id}`);
    alCambiar();
  }

  return (
    <article className="rounded-2xl border border-slate-200 bg-panel">
      <button type="button" onClick={() => setAbierta(!abierta)} aria-expanded={abierta}
        className="flex w-full items-center gap-4 p-4 text-left">
        <span className="grid h-11 w-11 shrink-0 place-items-center rounded-full bg-slate-100 font-display text-lg font-semibold text-slate-800" aria-hidden="true">
          {persona.nombre.charAt(0).toUpperCase()}
        </span>
        <span className="min-w-0 flex-1">
          <span className="block font-display text-lg font-semibold text-slate-900">{persona.nombre}</span>
          <span className="text-sm text-slate-500">{persona.deudasPendientes} {persona.deudasPendientes === 1 ? "cuenta pendiente" : "cuentas pendientes"}</span>
        </span>
        <span className={`cifra text-right text-lg font-semibold ${clase}`}>{texto}</span>
        <ChevronDown size={18} className={`shrink-0 text-slate-400 transition ${abierta ? "rotate-180" : ""}`} aria-hidden="true" />
      </button>

      {abierta && (
        <ul className="divide-y divide-slate-100 border-t border-slate-100">
          {propias.length === 0 && <li className="px-4 py-3 text-sm text-slate-500">Sin cuentas registradas con {persona.nombre}.</li>}
          {propias.map((d) => (
            <li key={d.id} className="px-4 py-3">
              <div className="flex flex-wrap items-start justify-between gap-2">
                <div className="min-w-0">
                  <p className="font-medium text-slate-900">{d.descripcion}</p>
                  <p className="text-xs text-slate-500">
                    {d.direccion === "ME_DEBEN" ? "Te debe" : "Le debes"} · {formatearFecha(d.fecha)} · total {formatearPesos(d.monto)}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  {d.saldada ? (
                    <span className="rounded-full bg-emerald-50 px-2 py-0.5 text-xs font-semibold text-emerald-700">Saldada</span>
                  ) : (
                    <>
                      <span className="cifra font-semibold text-slate-900">Faltan {formatearPesos(d.pendiente)}</span>
                      <Boton variante="secundario" icono={HandCoins} onClick={() => alAbonar(d)}>Abonar</Boton>
                    </>
                  )}
                  <button onClick={() => borrarDeuda(d)} className="rounded-lg p-1.5 text-slate-400 hover:bg-red-50 hover:text-red-600" aria-label={`Borrar ${d.descripcion}`}>
                    <Trash2 size={15} />
                  </button>
                </div>
              </div>
              {d.abonos.length > 0 && (
                <ul className="mt-2 flex flex-wrap gap-2 text-xs">
                  {d.abonos.map((a) => (
                    <li key={a.id} className="inline-flex items-center gap-1 rounded-full bg-slate-100 py-0.5 pl-2.5 pr-1 text-slate-700">
                      Abono de {formatearPesos(a.monto)} · {formatearFecha(a.fecha, { day: "numeric", month: "short" })}
                      <button onClick={() => quitarAbono(d, a)} className="rounded-full p-0.5 hover:bg-slate-200" aria-label={`Quitar abono de ${formatearPesos(a.monto)}`}>×</button>
                    </li>
                  ))}
                </ul>
              )}
            </li>
          ))}
        </ul>
      )}
    </article>
  );
}

// "Cuentas claras, amistades largas": quién te debe y a quién le debes.
export default function PersonasPage() {
  const { datos: personas, error, recargar: recargarPersonas } = useDatos("/personas");
  const { datos: deudas, recargar: recargarDeudas } = useDatos("/deudas");
  const [modal, setModal] = useState(null); // { tipo: "persona" | "deuda" | "abono", ... }
  const [errorForm, setErrorForm] = useState({ mensaje: "", campos: {} });
  const [guardando, setGuardando] = useState(false);

  const recargar = () => {
    recargarPersonas();
    recargarDeudas();
  };
  const teDeben = personas?.reduce((s, p) => s + Number(p.teDebe), 0) ?? 0;
  const debes = personas?.reduce((s, p) => s + Number(p.leDebes), 0) ?? 0;

  async function enviar(e, accion) {
    e.preventDefault();
    setGuardando(true);
    setErrorForm({ mensaje: "", campos: {} });
    try {
      await accion();
      setModal(null);
      recargar();
    } catch (err) {
      setErrorForm({ mensaje: err.message, campos: err.errores });
    } finally {
      setGuardando(false);
    }
  }

  const abrir = (datos) => {
    setErrorForm({ mensaje: "", campos: {} });
    setModal(datos);
  };
  const cambiar = (campo) => (e) => setModal((m) => ({ ...m, [campo]: e.target.value }));
  const numero = (v) => (v === "" ? null : Number(v));

  return (
    <div>
      <EncabezadoPagina
        titulo="Personas"
        subtitulo="Cuentas claras, amistades largas: lo que te deben y lo que debes."
        accion={
          <div className="flex flex-wrap gap-2">
            <Boton variante="secundario" icono={UserPlus} onClick={() => abrir({ tipo: "persona", nombre: "" })}>Persona</Boton>
            <Boton icono={Plus} disabled={!personas?.length}
              onClick={() => abrir({ tipo: "deuda", personaId: personas[0].id, direccion: "ME_DEBEN", monto: "", descripcion: "", fecha: hoyISO() })}>
              Registrar
            </Boton>
          </div>
        }
      />
      {error && <Aviso>{error}</Aviso>}

      {personas && personas.length > 0 && (
        <div className="mb-6 grid grid-cols-2 gap-3">
          <div className="rounded-2xl border border-slate-200 bg-panel p-4">
            <p className="text-sm text-slate-500">Te deben</p>
            <p className="cifra text-2xl font-semibold text-emerald-700">{formatearPesos(teDeben)}</p>
          </div>
          <div className="rounded-2xl border border-slate-200 bg-panel p-4">
            <p className="text-sm text-slate-500">Debes</p>
            <p className="cifra text-2xl font-semibold text-red-700">{formatearPesos(debes)}</p>
          </div>
        </div>
      )}

      {!personas || !deudas ? (
        <Cargando />
      ) : personas.length === 0 ? (
        <EstadoVacio icono={Users} titulo="Nadie en la lista todavía"
          descripcion="Agrega a quien le prestaste o con quien dividiste una cuenta, y lleva los abonos sin enredos." />
      ) : (
        <div className="space-y-3">
          {personas.map((p) => (
            <Persona key={p.id} persona={p} deudas={deudas} alCambiar={recargar}
              alAbonar={(d) => abrir({ tipo: "abono", deuda: d, monto: String(Number(d.pendiente)), fecha: hoyISO() })} />
          ))}
        </div>
      )}

      <Modal titulo="Nueva persona" abierto={modal?.tipo === "persona"} alCerrar={() => setModal(null)}>
        {modal?.tipo === "persona" && (
          <form onSubmit={(e) => enviar(e, () => api.post("/personas", { nombre: modal.nombre }))} className="space-y-4" noValidate>
            <Campo etiqueta="Nombre" value={modal.nombre} onChange={cambiar("nombre")} error={errorForm.campos.nombre} placeholder="Ej: Valentina" />
            {errorForm.mensaje && Object.keys(errorForm.campos).length === 0 && <Aviso>{errorForm.mensaje}</Aviso>}
            <Boton type="submit" disabled={guardando} className="w-full">{guardando ? "Guardando…" : "Agregar"}</Boton>
          </form>
        )}
      </Modal>

      <Modal titulo="Registrar una cuenta pendiente" abierto={modal?.tipo === "deuda"} alCerrar={() => setModal(null)}>
        {modal?.tipo === "deuda" && (
          <form onSubmit={(e) => enviar(e, () => api.post("/deudas", { ...modal, tipo: undefined, monto: numero(modal.monto) }))} className="space-y-4" noValidate>
            <Campo etiqueta="Persona" value={modal.personaId} onChange={cambiar("personaId")} error={errorForm.campos.personaId}>
              {personas.map((p) => <option key={p.id} value={p.id}>{p.nombre}</option>)}
            </Campo>
            <fieldset>
              <legend className="text-sm font-medium text-slate-700">¿Quién le debe a quién?</legend>
              <div className="mt-2 grid grid-cols-2 gap-2">
                {[["ME_DEBEN", "Me debe a mí"], ["DEBO", "Yo le debo"]].map(([valor, texto]) => (
                  <label key={valor} className={`cursor-pointer rounded-xl border px-3 py-2 text-center text-sm font-semibold ${modal.direccion === valor ? "border-tinta bg-tinta text-white" : "border-slate-300 text-slate-700"}`}>
                    <input type="radio" name="direccion" value={valor} checked={modal.direccion === valor} onChange={cambiar("direccion")} className="sr-only" />
                    {texto}
                  </label>
                ))}
              </div>
            </fieldset>
            <Campo etiqueta="Monto (COP)" type="number" min="0" value={modal.monto} onChange={cambiar("monto")} error={errorForm.campos.monto} />
            <Campo etiqueta="¿De qué es?" value={modal.descripcion} onChange={cambiar("descripcion")} error={errorForm.campos.descripcion} placeholder="Ej: mitad del almuerzo" />
            <Campo etiqueta="Fecha" type="date" max={hoyISO()} value={modal.fecha} onChange={cambiar("fecha")} error={errorForm.campos.fecha} />
            {errorForm.mensaje && Object.keys(errorForm.campos).length === 0 && <Aviso>{errorForm.mensaje}</Aviso>}
            <Boton type="submit" disabled={guardando} className="w-full">{guardando ? "Guardando…" : "Registrar"}</Boton>
          </form>
        )}
      </Modal>

      <Modal titulo="Registrar un abono" abierto={modal?.tipo === "abono"} alCerrar={() => setModal(null)}>
        {modal?.tipo === "abono" && (
          <form onSubmit={(e) => enviar(e, () => api.post(`/deudas/${modal.deuda.id}/abonos`, { monto: numero(modal.monto), fecha: modal.fecha }))} className="space-y-4" noValidate>
            <p className="text-sm text-slate-600">{modal.deuda.descripcion} · faltan {formatearPesos(modal.deuda.pendiente)}</p>
            <Campo etiqueta="Monto del abono (COP)" type="number" min="0" value={modal.monto} onChange={cambiar("monto")} error={errorForm.campos.monto} />
            <Campo etiqueta="Fecha" type="date" min={modal.deuda.fecha} max={hoyISO()} value={modal.fecha} onChange={cambiar("fecha")} error={errorForm.campos.fecha} />
            {errorForm.mensaje && Object.keys(errorForm.campos).length === 0 && <Aviso>{errorForm.mensaje}</Aviso>}
            <Boton type="submit" disabled={guardando} className="w-full">{guardando ? "Guardando…" : "Abonar"}</Boton>
          </form>
        )}
      </Modal>
    </div>
  );
}
