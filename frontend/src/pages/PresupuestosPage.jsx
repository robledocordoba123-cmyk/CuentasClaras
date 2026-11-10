import { useState } from "react";
import { PiggyBank, Plus, Trash2 } from "lucide-react";
import { api } from "../api/cliente";
import { useDatos } from "../utils/useDatos";
import { formatearPesos, mesActual } from "../utils/formato";
import { Aviso, Boton, Campo, Cargando, EncabezadoPagina, EstadoVacio, Modal } from "../components/ui";
import SelectorMes from "../components/SelectorMes";
import Sobre from "../components/Sobre";

// Semáforo de HU-07: los mismos umbrales que calcula la API (80 % y 100 %).
const ESTILO_ESTADO = {
  EN_CONTROL: { barra: "bg-billete-100", etiqueta: "bg-emerald-50 text-emerald-700", texto: "En control" },
  ALERTA: { barra: "bg-billete-5", etiqueta: "bg-amber-50 text-amber-700", texto: "Alerta" },
  EXCEDIDO: { barra: "bg-billete-10", etiqueta: "bg-red-50 text-red-700", texto: "Excedido" },
};

export default function PresupuestosPage() {
  const [mes, setMes] = useState(mesActual());
  const { datos: presupuestos, error, recargar } = useDatos(`/presupuestos?mes=${mes}`);
  const { datos: categorias } = useDatos("/categorias?tipo=GASTO");

  const [formulario, setFormulario] = useState(null);
  const [errorForm, setErrorForm] = useState({ mensaje: "", campos: {} });
  const [guardando, setGuardando] = useState(false);

  async function guardar(e) {
    e.preventDefault();
    setGuardando(true);
    setErrorForm({ mensaje: "", campos: {} });
    try {
      await api.put("/presupuestos", { ...formulario, mes, montoLimite: formulario.montoLimite === "" ? null : Number(formulario.montoLimite) });
      setFormulario(null);
      recargar();
    } catch (err) {
      setErrorForm({ mensaje: err.message, campos: err.errores });
    } finally {
      setGuardando(false);
    }
  }

  async function eliminar(p) {
    if (!window.confirm(`¿Quitar el presupuesto de ${p.categoria.nombre}?`)) return;
    await api.delete(`/presupuestos/${p.id}`);
    recargar();
  }

  return (
    <div>
      <EncabezadoPagina
        titulo="Presupuestos"
        subtitulo="Un sobre por categoría: empieza lleno y se vacía al gastar. Te avisamos cuando le quede el 20 %."
        accion={
          <div className="flex flex-wrap gap-2">
            <SelectorMes mes={mes} alCambiar={setMes} />
            <Boton
              icono={Plus}
              onClick={() => {
                setErrorForm({ mensaje: "", campos: {} });
                setFormulario({ categoriaId: "", montoLimite: "" });
              }}
            >
              Definir
            </Boton>
          </div>
        }
      />
      {error && <Aviso>{error}</Aviso>}

      {!presupuestos ? (
        <Cargando />
      ) : presupuestos.length === 0 ? (
        <EstadoVacio icono={PiggyBank} titulo="Sin presupuestos este mes" descripcion="Empieza por la categoría donde más se te va la plata, por ejemplo Domicilios." />
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {presupuestos.map((p) => {
            const estilo = ESTILO_ESTADO[p.estado];
            const usado = Number(p.porcentajeUsado);
            const excedido = Number(p.disponible) < 0;
            return (
              <article key={p.id} className="flex gap-3 rounded-2xl border border-slate-200 bg-panel p-4 sm:gap-4">
                <Sobre color={p.categoria.color} restante={1 - usado / 100} excedido={excedido} className="h-16 w-20 shrink-0 sm:h-24 sm:w-28" />
                <div className="min-w-0 flex-1">
                  <div className="flex flex-wrap items-start justify-between gap-x-2 gap-y-1">
                    <h2 className="font-display text-lg font-semibold text-slate-900">{p.categoria.nombre}</h2>
                    <div className="flex shrink-0 items-center gap-1">
                      <span className={`rounded-full px-2 py-0.5 text-xs font-semibold ${estilo.etiqueta}`}>{estilo.texto}</span>
                      <button onClick={() => eliminar(p)} className="rounded-lg p-1.5 text-slate-400 hover:bg-red-50 hover:text-red-600" aria-label={`Quitar el sobre de ${p.categoria.nombre}`}>
                        <Trash2 size={15} />
                      </button>
                    </div>
                  </div>
                  <p className={`cifra mt-1 text-2xl font-semibold ${excedido ? "text-red-700" : "text-slate-900"}`}>
                    {excedido ? `−${formatearPesos(-p.disponible)}` : formatearPesos(p.disponible)}
                  </p>
                  <p className="text-sm text-slate-500">{excedido ? "te pasaste" : "quedan en el sobre"} · gastado {formatearPesos(p.gastado)} de {formatearPesos(p.montoLimite)}</p>
                  <div className="mt-3 h-1.5 overflow-hidden rounded-full bg-slate-100" role="progressbar" aria-label={`Usado del sobre de ${p.categoria.nombre}`} aria-valuenow={Math.round(usado)} aria-valuemin={0} aria-valuemax={100}>
                    <div className={`h-full rounded-full ${estilo.barra}`} style={{ width: `${Math.min(usado, 100)}%` }} />
                  </div>
                </div>
              </article>
            );
          })}
        </div>
      )}

      <Modal titulo="Definir presupuesto" abierto={formulario !== null} alCerrar={() => setFormulario(null)}>
        {formulario && (
          <form onSubmit={guardar} className="space-y-4" noValidate>
            <p className="text-sm text-slate-500">Si la categoría ya tiene presupuesto este mes, se actualiza el tope.</p>
            <Campo etiqueta="Categoría de gasto" value={formulario.categoriaId} onChange={(e) => setFormulario({ ...formulario, categoriaId: e.target.value })} error={errorForm.campos.categoriaId}>
              <option value="">Selecciona…</option>
              {categorias?.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
            </Campo>
            <Campo etiqueta="Tope del mes (COP)" type="number" min="0" step="0.01" value={formulario.montoLimite} onChange={(e) => setFormulario({ ...formulario, montoLimite: e.target.value })} error={errorForm.campos.montoLimite} />
            {errorForm.mensaje && Object.keys(errorForm.campos).length === 0 && <Aviso>{errorForm.mensaje}</Aviso>}
            <Boton type="submit" disabled={guardando} className="w-full">{guardando ? "Guardando..." : "Guardar"}</Boton>
          </form>
        )}
      </Modal>
    </div>
  );
}
