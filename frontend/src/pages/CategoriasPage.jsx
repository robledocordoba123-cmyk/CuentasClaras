import { useState } from "react";
import { Lock, Pencil, Plus, Trash2 } from "lucide-react";
import { api } from "../api/cliente";
import { useSesion } from "../context/SesionContext";
import { useDatos } from "../utils/useDatos";
import { Aviso, Boton, Campo, Cargando, EncabezadoPagina, Modal, Tarjeta } from "../components/ui";

export default function CategoriasPage() {
  const { token } = useSesion();
  const { datos: categorias, error, recargar } = useDatos("/categorias");
  const [formulario, setFormulario] = useState(null);
  const [errorForm, setErrorForm] = useState({ mensaje: "", campos: {} });
  const [guardando, setGuardando] = useState(false);

  async function guardar(e) {
    e.preventDefault();
    setGuardando(true);
    setErrorForm({ mensaje: "", campos: {} });
    const cuerpo = { nombre: formulario.nombre, tipo: formulario.tipo, color: formulario.color };
    try {
      if (formulario.id) await api.put(`/categorias/${formulario.id}`, cuerpo, token);
      else await api.post("/categorias", cuerpo, token);
      setFormulario(null);
      recargar();
    } catch (err) {
      setErrorForm({ mensaje: err.message, campos: err.errores });
    } finally {
      setGuardando(false);
    }
  }

  async function eliminar(c) {
    if (!window.confirm(`¿Eliminar la categoría "${c.nombre}"? Si ya tiene movimientos, se archiva.`)) return;
    await api.delete(`/categorias/${c.id}`, token);
    recargar();
  }

  // Función que dibuja una lista (no un componente): así no se recrea en cada render.
  function lista(tipo, titulo) {
    return (
      <Tarjeta key={tipo} titulo={titulo}>
        <ul className="divide-y divide-slate-100">
          {categorias
            .filter((c) => c.tipo === tipo)
            .map((c) => (
              <li key={c.id} className="flex items-center gap-3 py-2.5">
                <span className="h-3.5 w-3.5 rounded-full" style={{ backgroundColor: c.color }} />
                <span className="flex-1 text-sm text-slate-800">{c.nombre}</span>
                {c.porDefecto ? (
                  <span className="flex items-center gap-1 text-xs text-slate-400" title="Categoría por defecto: no se puede modificar">
                    <Lock size={13} aria-hidden="true" /> Por defecto
                  </span>
                ) : (
                  <div className="flex">
                    <button
                      onClick={() => {
                        setErrorForm({ mensaje: "", campos: {} });
                        setFormulario({ id: c.id, nombre: c.nombre, tipo: c.tipo, color: c.color });
                      }}
                      className="rounded-lg p-1.5 text-slate-400 hover:bg-slate-100"
                      aria-label={`Editar ${c.nombre}`}
                    >
                      <Pencil size={15} />
                    </button>
                    <button onClick={() => eliminar(c)} className="rounded-lg p-1.5 text-slate-400 hover:bg-red-50 hover:text-red-600" aria-label={`Eliminar ${c.nombre}`}>
                      <Trash2 size={15} />
                    </button>
                  </div>
                )}
              </li>
            ))}
        </ul>
      </Tarjeta>
    );
  }

  return (
    <div>
      <EncabezadoPagina
        titulo="Categorías"
        subtitulo="Las que vienen por defecto más las tuyas."
        accion={
          <Boton
            icono={Plus}
            onClick={() => {
              setErrorForm({ mensaje: "", campos: {} });
              setFormulario({ nombre: "", tipo: "GASTO", color: "#6366F1" });
            }}
          >
            Nueva categoría
          </Boton>
        }
      />
      {error && <Aviso>{error}</Aviso>}
      {!categorias ? (
        <Cargando />
      ) : (
        <div className="grid gap-6 md:grid-cols-2">
          {lista("GASTO", "Gastos")}
          {lista("INGRESO", "Ingresos")}
        </div>
      )}

      <Modal titulo={formulario?.id ? "Editar categoría" : "Nueva categoría"} abierto={formulario !== null} alCerrar={() => setFormulario(null)}>
        {formulario && (
          <form onSubmit={guardar} className="space-y-4" noValidate>
            <Campo etiqueta="Nombre" placeholder="Ej: Mascotas, Gimnasio" value={formulario.nombre} onChange={(e) => setFormulario({ ...formulario, nombre: e.target.value })} error={errorForm.campos.nombre} />
            <Campo etiqueta="Tipo" value={formulario.tipo} onChange={(e) => setFormulario({ ...formulario, tipo: e.target.value })}>
              <option value="GASTO">Gasto</option>
              <option value="INGRESO">Ingreso</option>
            </Campo>
            <label className="block text-sm font-medium text-slate-700">
              Color
              <input type="color" value={formulario.color} onChange={(e) => setFormulario({ ...formulario, color: e.target.value })} className="mt-1 block h-10 w-20 cursor-pointer rounded border border-slate-300" />
            </label>
            {errorForm.mensaje && Object.keys(errorForm.campos).length === 0 && <Aviso>{errorForm.mensaje}</Aviso>}
            <Boton type="submit" disabled={guardando} className="w-full">{guardando ? "Guardando..." : "Guardar"}</Boton>
          </form>
        )}
      </Modal>
    </div>
  );
}
