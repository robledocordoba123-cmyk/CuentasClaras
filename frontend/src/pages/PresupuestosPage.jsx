import { useState } from "react";
import { PiggyBank, Plus, Trash2 } from "lucide-react";
import { api } from "../api/cliente";
import { useSesion } from "../context/SesionContext";
import { useDatos } from "../utils/useDatos";
import { formatearPesos, mesActual } from "../utils/formato";
import { Aviso, Boton, Campo, Cargando, EncabezadoPagina, EstadoVacio, Modal } from "../components/ui";
import SelectorMes from "../components/SelectorMes";

// Semáforo de HU-07: los mismos umbrales que calcula la API (80 % y 100 %).
const ESTILO_ESTADO = {
  EN_CONTROL: { barra: "bg-emerald-500", etiqueta: "bg-emerald-50 text-emerald-700", texto: "En control" },
  ALERTA: { barra: "bg-amber-500", etiqueta: "bg-amber-50 text-amber-700", texto: "Alerta" },
  EXCEDIDO: { barra: "bg-red-500", etiqueta: "bg-red-50 text-red-700", texto: "Excedido" },
};

export default function PresupuestosPage() {
  const { token } = useSesion();
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
      await api.put("/presupuestos", { ...formulario, mes, montoLimite: formulario.montoLimite === "" ? null : Number(formulario.montoLimite) }, token);
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
    await api.delete(`/presupuestos/${p.id}`, token);
    recargar();
  }

  return (
    <div>
      <EncabezadoPagina
        titulo="Presupuestos"
        subtitulo="Ponle un tope a cada categoría; te avisamos desde el 80 %."
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
            return (
              <div key={p.id} className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                <div className="flex items-center justify-between gap-2">
                  <span className="flex items-center gap-2 font-medium text-slate-900">
                    <span className="h-3 w-3 rounded-full" style={{ backgroundColor: p.categoria.color }} />
                    {p.categoria.nombre}
                  </span>
                  <div className="flex items-center gap-1">
                    <span className={`rounded-full px-2 py-0.5 text-xs font-medium ${estilo.etiqueta}`}>{estilo.texto}</span>
                    <button onClick={() => eliminar(p)} className="rounded-lg p-1.5 text-slate-400 hover:bg-red-50 hover:text-red-600" aria-label="Eliminar">
                      <Trash2 size={15} />
                    </button>
                  </div>
                </div>
                <div className="mt-4 h-2.5 overflow-hidden rounded-full bg-slate-100" role="progressbar" aria-valuenow={Number(p.porcentajeUsado)} aria-valuemin={0} aria-valuemax={100}>
                  <div className={`h-full rounded-full ${estilo.barra}`} style={{ width: `${Math.min(Number(p.porcentajeUsado), 100)}%` }} />
                </div>
                <div className="mt-2 flex justify-between text-sm">
                  <span className="text-slate-600">
                    {formatearPesos(p.gastado)} de {formatearPesos(p.montoLimite)}
                  </span>
                  <span className={Number(p.disponible) < 0 ? "font-medium text-red-600" : "text-slate-500"}>
                    {Number(p.disponible) < 0 ? `Te pasaste ${formatearPesos(-p.disponible)}` : `Quedan ${formatearPesos(p.disponible)}`}
                  </span>
                </div>
              </div>
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
