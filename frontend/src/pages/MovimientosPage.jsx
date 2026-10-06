import { useState } from "react";
import { ArrowLeftRight, Download, Pencil, Plus, Trash2 } from "lucide-react";
import { api } from "../api/cliente";
import { useSesion } from "../context/SesionContext";
import { useDatos } from "../utils/useDatos";
import { formatearFecha, formatearPesos, hoyISO, mesActual } from "../utils/formato";
import { Aviso, Boton, Campo, Cargando, EncabezadoPagina, EstadoVacio, Modal } from "../components/ui";
import SelectorMes from "../components/SelectorMes";

const TAMANO_PAGINA = 15;
const VACIO = { tipo: "GASTO", cuentaId: "", categoriaId: "", monto: "", fecha: hoyISO(), descripcion: "" };

function rangoDelMes(mes) {
  const [a, m] = mes.split("-").map(Number);
  const ultimoDia = new Date(a, m, 0).getDate();
  return { desde: `${mes}-01`, hasta: `${mes}-${String(ultimoDia).padStart(2, "0")}` };
}

export default function MovimientosPage() {
  const { token } = useSesion();
  const [mes, setMes] = useState(mesActual());
  const [tipo, setTipo] = useState("");
  const [pagina, setPagina] = useState(0);
  const { desde, hasta } = rangoDelMes(mes);
  const ruta = `/movimientos?desde=${desde}&hasta=${hasta}${tipo ? `&tipo=${tipo}` : ""}&pagina=${pagina}&tamano=${TAMANO_PAGINA}`;
  const { datos, error, recargar } = useDatos(ruta);
  const { datos: cuentas } = useDatos("/cuentas");
  const { datos: categorias } = useDatos("/categorias");

  const [formulario, setFormulario] = useState(null); // null = cerrado; { id?, ...campos }
  const [errorForm, setErrorForm] = useState({ mensaje: "", campos: {} });
  const [guardando, setGuardando] = useState(false);

  const abrirNuevo = () => {
    setErrorForm({ mensaje: "", campos: {} });
    setFormulario({ ...VACIO, cuentaId: cuentas?.[0]?.id ?? "" });
  };

  const abrirEdicion = (m) => {
    setErrorForm({ mensaje: "", campos: {} });
    setFormulario({
      id: m.id,
      tipo: m.tipo,
      cuentaId: m.cuenta.id,
      categoriaId: m.categoria.id,
      monto: m.monto,
      fecha: m.fecha,
      descripcion: m.descripcion ?? "",
    });
  };

  const cambiar = (campo) => (e) =>
    setFormulario((f) => ({ ...f, [campo]: e.target.value, ...(campo === "tipo" ? { categoriaId: "" } : {}) }));

  async function guardar(e) {
    e.preventDefault();
    setGuardando(true);
    const cuerpo = { ...formulario, monto: formulario.monto === "" ? null : Number(formulario.monto) };
    delete cuerpo.id;
    try {
      if (formulario.id) await api.put(`/movimientos/${formulario.id}`, cuerpo, token);
      else await api.post("/movimientos", cuerpo, token);
      setFormulario(null);
      recargar();
    } catch (err) {
      setErrorForm({ mensaje: err.message, campos: err.errores });
    } finally {
      setGuardando(false);
    }
  }

  async function eliminar(m) {
    if (!window.confirm(`¿Eliminar el ${m.tipo === "INGRESO" ? "ingreso" : "gasto"} de ${formatearPesos(m.monto)}?`)) return;
    await api.delete(`/movimientos/${m.id}`, token);
    recargar();
  }

  const categoriasDelTipo = categorias?.filter((c) => c.tipo === (formulario?.tipo === "INGRESO" ? "INGRESO" : "GASTO")) ?? [];

  return (
    <div>
      <EncabezadoPagina
        titulo="Movimientos"
        subtitulo="Tus ingresos, gastos y transferencias."
        accion={
          <div className="flex flex-wrap gap-2">
            <Boton variante="secundario" icono={Download} onClick={() => api.descargar(`/movimientos/exportar?mes=${mes}`, token, `cuentasclaras-${mes}.csv`)}>
              Excel
            </Boton>
            <Boton icono={Plus} onClick={abrirNuevo} disabled={!cuentas?.length}>
              Nuevo
            </Boton>
          </div>
        }
      />

      <div className="mb-4 flex flex-wrap items-center gap-3">
        <SelectorMes mes={mes} alCambiar={(m) => { setMes(m); setPagina(0); }} />
        <div className="inline-flex rounded-lg border border-slate-300 bg-panel p-1 text-sm">
          {[["", "Todos"], ["INGRESO", "Ingresos"], ["GASTO", "Gastos"]].map(([valor, texto]) => (
            <button
              key={valor}
              onClick={() => { setTipo(valor); setPagina(0); }}
              className={`rounded-md px-3 py-1.5 ${tipo === valor ? "bg-emerald-50 font-medium text-emerald-700" : "text-slate-600"}`}
            >
              {texto}
            </button>
          ))}
        </div>
      </div>

      {cuentas?.length === 0 && <Aviso tipo="info">Primero crea una cuenta (Efectivo, Nequi, banco…) en la sección Cuentas.</Aviso>}
      {error && <Aviso>{error}</Aviso>}

      {!datos ? (
        <Cargando />
      ) : datos.contenido.length === 0 ? (
        <EstadoVacio icono={ArrowLeftRight} titulo="No hay movimientos en este mes" descripcion="Registra tu primer gasto o ingreso con el botón Nuevo." />
      ) : (
        <>
          <ul className="divide-y divide-slate-100 overflow-hidden rounded-2xl border border-slate-200 bg-panel">
            {datos.contenido.map((m) => {
              const esEntrada = m.tipo === "INGRESO" || m.tipo === "TRANSFERENCIA_ENTRADA";
              const esTransferencia = m.tipo.startsWith("TRANSFERENCIA");
              return (
                <li key={m.id} className="flex items-center gap-3 px-4 py-3">
                  <span
                    className="h-9 w-9 shrink-0 rounded-full"
                    style={{ backgroundColor: esTransferencia ? "#94a3b8" : m.categoria.color }}
                    aria-hidden="true"
                  />
                  <div className="min-w-0 flex-1">
                    <p className="truncate font-medium text-slate-900">
                      {m.descripcion || (esTransferencia ? "Transferencia" : m.categoria.nombre)}
                    </p>
                    <p className="truncate text-xs text-slate-500">
                      {esTransferencia ? (esEntrada ? "Entrada" : "Salida") : m.categoria.nombre} · {m.cuenta.nombre} · {formatearFecha(m.fecha)}
                    </p>
                  </div>
                  <span className={`shrink-0 font-semibold ${esTransferencia ? "text-slate-500" : esEntrada ? "text-brillo" : "text-slate-900"}`}>
                    {esEntrada ? "+" : "−"}
                    {formatearPesos(m.monto)}
                  </span>
                  {!esTransferencia && (
                    <div className="flex shrink-0">
                      <button onClick={() => abrirEdicion(m)} className="rounded-lg p-2 text-slate-400 hover:bg-slate-100 hover:text-slate-700" aria-label="Editar">
                        <Pencil size={16} />
                      </button>
                      <button onClick={() => eliminar(m)} className="rounded-lg p-2 text-slate-400 hover:bg-red-50 hover:text-red-600" aria-label="Eliminar">
                        <Trash2 size={16} />
                      </button>
                    </div>
                  )}
                </li>
              );
            })}
          </ul>

          {datos.totalPaginas > 1 && (
            <div className="mt-4 flex items-center justify-between text-sm text-slate-600">
              <span>
                Página {datos.pagina + 1} de {datos.totalPaginas} · {datos.totalElementos} movimientos
              </span>
              <div className="flex gap-2">
                <Boton variante="secundario" disabled={pagina === 0} onClick={() => setPagina(pagina - 1)}>
                  Anterior
                </Boton>
                <Boton variante="secundario" disabled={pagina + 1 >= datos.totalPaginas} onClick={() => setPagina(pagina + 1)}>
                  Siguiente
                </Boton>
              </div>
            </div>
          )}
        </>
      )}

      <Modal titulo={formulario?.id ? "Editar movimiento" : "Nuevo movimiento"} abierto={formulario !== null} alCerrar={() => setFormulario(null)}>
        {formulario && (
          <form onSubmit={guardar} className="space-y-4" noValidate>
            <div className="grid grid-cols-2 gap-2 rounded-lg bg-slate-100 p-1 text-sm">
              {[["GASTO", "Gasto"], ["INGRESO", "Ingreso"]].map(([valor, texto]) => (
                <button
                  type="button"
                  key={valor}
                  onClick={() => cambiar("tipo")({ target: { value: valor } })}
                  className={`rounded-md py-1.5 font-medium ${formulario.tipo === valor ? "bg-panel text-slate-900 shadow-sm" : "text-slate-500"}`}
                >
                  {texto}
                </button>
              ))}
            </div>
            <Campo etiqueta="Monto (COP)" type="number" inputMode="decimal" min="0" step="0.01" value={formulario.monto} onChange={cambiar("monto")} error={errorForm.campos.monto} />
            <Campo etiqueta="Cuenta" value={formulario.cuentaId} onChange={cambiar("cuentaId")} error={errorForm.campos.cuentaId}>
              {cuentas?.map((c) => (
                <option key={c.id} value={c.id}>{c.nombre}</option>
              ))}
            </Campo>
            <Campo etiqueta="Categoría" value={formulario.categoriaId} onChange={cambiar("categoriaId")} error={errorForm.campos.categoriaId}>
              <option value="">Selecciona…</option>
              {categoriasDelTipo.map((c) => (
                <option key={c.id} value={c.id}>{c.nombre}</option>
              ))}
            </Campo>
            <Campo etiqueta="Fecha" type="date" value={formulario.fecha} onChange={cambiar("fecha")} error={errorForm.campos.fecha} />
            <Campo etiqueta="Descripción (opcional)" value={formulario.descripcion} onChange={cambiar("descripcion")} maxLength={200} error={errorForm.campos.descripcion} />
            {errorForm.mensaje && Object.keys(errorForm.campos).length === 0 && <Aviso>{errorForm.mensaje}</Aviso>}
            <Boton type="submit" disabled={guardando} className="w-full">
              {guardando ? "Guardando..." : "Guardar"}
            </Boton>
          </form>
        )}
      </Modal>
    </div>
  );
}
