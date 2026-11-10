import { useState } from "react";
import { CalendarClock, CheckCircle2, Pencil, Plus, Trash2 } from "lucide-react";
import { api } from "../api/cliente";
import { useDatos } from "../utils/useDatos";
import { formatearFecha, formatearPesos, hoyISO, mesActual } from "../utils/formato";
import { Aviso, Boton, Campo, Cargando, EncabezadoPagina, EstadoVacio, Modal } from "../components/ui";
import SelectorMes from "../components/SelectorMes";
import Pestanas from "../components/Pestanas";
import { GRUPO_MOVIMIENTOS } from "../utils/grupos";

const ESTADOS = {
  PAGADO: { texto: "Pagado", clase: "bg-emerald-50 text-emerald-700" },
  PENDIENTE: { texto: "Pendiente", clase: "bg-slate-100 text-slate-700" },
  VENCIDO: { texto: "Vencido", clase: "bg-red-50 text-red-700" },
};
const VACIO = { nombre: "", monto: "", diaDelMes: "1", categoriaId: "", cuentaId: "" };

// Pagos que se repiten cada mes: arriendo, servicios, plan de celular.
export default function PagosFijosPage() {
  const [mes, setMes] = useState(mesActual());
  const { datos: pagos, error, recargar } = useDatos(`/pagos-fijos?mes=${mes}`);
  const { datos: categorias } = useDatos("/categorias?tipo=GASTO");
  const { datos: cuentas } = useDatos("/cuentas");
  const [formulario, setFormulario] = useState(null);
  const [pago, setPago] = useState(null);
  const [errorForm, setErrorForm] = useState({ mensaje: "", campos: {} });
  const [guardando, setGuardando] = useState(false);

  const pendiente = pagos?.filter((p) => p.estado !== "PAGADO").reduce((s, p) => s + Number(p.monto), 0) ?? 0;

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

  function abrirFormulario(p) {
    setErrorForm({ mensaje: "", campos: {} });
    setFormulario(p
      ? { id: p.id, nombre: p.nombre, monto: String(Number(p.monto)), diaDelMes: String(p.diaDelMes), categoriaId: p.categoria.id, cuentaId: p.cuenta.id }
      : { ...VACIO, categoriaId: categorias?.[0]?.id ?? "", cuentaId: cuentas?.[0]?.id ?? "" });
  }

  async function eliminar(p) {
    if (!window.confirm(`¿Quitar el pago fijo "${p.nombre}"?`)) return;
    const respuesta = await api.delete(`/pagos-fijos/${p.id}`);
    if (respuesta?.desactivado) window.alert("Tenía pagos registrados: se desactivó para conservar el historial.");
    recargar();
  }

  const cuerpo = (f) => ({ ...f, id: undefined, monto: f.monto === "" ? null : Number(f.monto), diaDelMes: Number(f.diaDelMes) });

  return (
    <div>
      <EncabezadoPagina titulo="Pagos fijos" subtitulo="Lo que se paga cada mes, para que no se te pase ninguno."
        accion={
          <div className="flex flex-wrap gap-2">
            <SelectorMes mes={mes} alCambiar={setMes} />
            <Boton icono={Plus} onClick={() => abrirFormulario(null)} disabled={!cuentas?.length}>Nuevo</Boton>
          </div>
        } />
      <Pestanas grupo={GRUPO_MOVIMIENTOS} etiqueta="Movimientos" />
      {error && <Aviso>{error}</Aviso>}

      {!pagos ? (
        <Cargando />
      ) : pagos.length === 0 ? (
        <EstadoVacio icono={CalendarClock} titulo="Sin pagos fijos"
          descripcion="Agrega el arriendo, los servicios o el plan de celular, y marca cada mes lo que ya pagaste." />
      ) : (
        <>
          <p className="mb-3 text-sm text-slate-600">
            {pendiente > 0 ? <>Te falta pagar <strong className="cifra text-slate-900">{formatearPesos(pendiente)}</strong> este mes.</> : "Todo pagado este mes."}
          </p>
          <ul className="divide-y divide-slate-100 rounded-2xl border border-slate-200 bg-panel">
            {pagos.map((p) => {
              const estado = ESTADOS[p.estado];
              return (
                <li key={p.id} className="flex flex-wrap items-center gap-3 px-4 py-3">
                  <span className="grid h-11 w-11 shrink-0 place-items-center rounded-xl text-center leading-none text-white" style={{ backgroundColor: p.categoria.color }}>
                    <span className="cifra text-lg font-semibold">{new Date(`${p.vence}T12:00:00`).getDate()}</span>
                  </span>
                  <div className="min-w-0 flex-1 basis-40">
                    <p className="font-medium text-slate-900">{p.nombre}{!p.activo && <span className="text-slate-500"> · desactivado</span>}</p>
                    <p className="text-xs text-slate-500">{p.categoria.nombre} · {p.cuenta.nombre} · vence el {formatearFecha(p.vence, { day: "numeric", month: "long" })}</p>
                  </div>
                  <div className="flex w-full items-center justify-end gap-2 sm:w-auto">
                  <span className="cifra mr-auto font-semibold text-slate-900 sm:mr-0">{formatearPesos(p.montoPagado ?? p.monto)}</span>
                  <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${estado.clase}`}>{estado.texto}</span>
                  <div className="flex items-center gap-1">
                    {p.estado !== "PAGADO" && p.activo && (
                      <Boton variante="secundario" icono={CheckCircle2} aria-label={`Pagar ${p.nombre}`}
                        onClick={() => { setErrorForm({ mensaje: "", campos: {} }); setPago({ pago: p, monto: String(Number(p.monto)), fecha: mes === mesActual() ? hoyISO() : p.vence }); }}>
                        <span className="hidden sm:inline">Pagar</span>
                      </Boton>
                    )}
                    {p.activo && (
                      <button onClick={() => abrirFormulario(p)} className="rounded-lg p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-700" aria-label={`Editar ${p.nombre}`}>
                        <Pencil size={16} />
                      </button>
                    )}
                    <button onClick={() => eliminar(p)} className="rounded-lg p-2 text-slate-400 hover:bg-red-50 hover:text-red-600" aria-label={`Quitar ${p.nombre}`}>
                      <Trash2 size={16} />
                    </button>
                  </div>
                  </div>
                </li>
              );
            })}
          </ul>
        </>
      )}

      <Modal titulo={formulario?.id ? "Editar pago fijo" : "Nuevo pago fijo"} abierto={formulario !== null} alCerrar={() => setFormulario(null)}>
        {formulario && (
          <form onSubmit={(e) => ejecutar(e, () => (formulario.id ? api.put(`/pagos-fijos/${formulario.id}`, cuerpo(formulario)) : api.post("/pagos-fijos", cuerpo(formulario))), () => setFormulario(null))}
            className="space-y-4" noValidate>
            <Campo etiqueta="Nombre" value={formulario.nombre} onChange={(e) => setFormulario({ ...formulario, nombre: e.target.value })} error={errorForm.campos.nombre} placeholder="Ej: Arriendo" />
            <div className="grid grid-cols-2 gap-3">
              <Campo etiqueta="Monto (COP)" type="number" min="0" value={formulario.monto} onChange={(e) => setFormulario({ ...formulario, monto: e.target.value })} error={errorForm.campos.monto} />
              <Campo etiqueta="Día del mes" type="number" min="1" max="31" value={formulario.diaDelMes} onChange={(e) => setFormulario({ ...formulario, diaDelMes: e.target.value })} error={errorForm.campos.diaDelMes} />
            </div>
            <Campo etiqueta="Categoría" value={formulario.categoriaId} onChange={(e) => setFormulario({ ...formulario, categoriaId: e.target.value })} error={errorForm.campos.categoriaId}>
              {categorias?.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
            </Campo>
            <Campo etiqueta="Sale de la cuenta" value={formulario.cuentaId} onChange={(e) => setFormulario({ ...formulario, cuentaId: e.target.value })} error={errorForm.campos.cuentaId}>
              {cuentas?.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
            </Campo>
            <p className="text-xs text-slate-500">Si el mes tiene menos días (por ejemplo, el 31 en febrero), vence el último día.</p>
            {errorForm.mensaje && Object.keys(errorForm.campos).length === 0 && <Aviso>{errorForm.mensaje}</Aviso>}
            <Boton type="submit" disabled={guardando} className="w-full">{guardando ? "Guardando…" : "Guardar"}</Boton>
          </form>
        )}
      </Modal>

      <Modal titulo={`Pagar ${pago?.pago.nombre ?? ""}`} abierto={pago !== null} alCerrar={() => setPago(null)}>
        {pago && (
          <form onSubmit={(e) => ejecutar(e, () => api.post(`/pagos-fijos/${pago.pago.id}/pagar`, { mes, monto: Number(pago.monto), fecha: pago.fecha }), () => setPago(null))} className="space-y-4" noValidate>
            <p className="text-sm text-slate-600">Se registra como un gasto de {pago.pago.categoria.nombre} desde {pago.pago.cuenta.nombre}.</p>
            <Campo etiqueta="Monto pagado (COP)" type="number" min="0" value={pago.monto} onChange={(e) => setPago({ ...pago, monto: e.target.value })} error={errorForm.campos.monto} />
            <Campo etiqueta="Fecha" type="date" max={hoyISO()} value={pago.fecha} onChange={(e) => setPago({ ...pago, fecha: e.target.value })} />
            {errorForm.mensaje && Object.keys(errorForm.campos).length === 0 && <Aviso>{errorForm.mensaje}</Aviso>}
            <Boton type="submit" icono={CheckCircle2} disabled={guardando} className="w-full">{guardando ? "Registrando…" : "Registrar el pago"}</Boton>
          </form>
        )}
      </Modal>
    </div>
  );
}
