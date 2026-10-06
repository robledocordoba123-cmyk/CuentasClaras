import { useState } from "react";
import { Archive, ArrowLeftRight, Banknote, Landmark, Pencil, Plus, RotateCcw, Smartphone, Wallet } from "lucide-react";
import { api } from "../api/cliente";
import { useSesion } from "../context/SesionContext";
import { useDatos } from "../utils/useDatos";
import { NOMBRES_TIPO_CUENTA, formatearPesos, hoyISO } from "../utils/formato";
import { Aviso, Boton, Campo, Cargando, EncabezadoPagina, EstadoVacio, Modal } from "../components/ui";

const ICONOS = { EFECTIVO: Banknote, BILLETERA_DIGITAL: Smartphone, BANCO: Landmark };

export default function CuentasPage() {
  const { token } = useSesion();
  const [verArchivadas, setVerArchivadas] = useState(false);
  const { datos: cuentas, error, recargar } = useDatos(`/cuentas?incluirArchivadas=${verArchivadas}`);

  const [cuenta, setCuenta] = useState(null); // formulario de cuenta
  const [transferencia, setTransferencia] = useState(null); // formulario de transferencia
  const [errorForm, setErrorForm] = useState({ mensaje: "", campos: {} });
  const [guardando, setGuardando] = useState(false);

  const activas = cuentas?.filter((c) => !c.archivada) ?? [];
  const total = activas.reduce((suma, c) => suma + Number(c.saldoActual), 0);

  async function ejecutar(accion, alTerminar) {
    setGuardando(true);
    setErrorForm({ mensaje: "", campos: {} });
    try {
      await accion();
      alTerminar();
      recargar();
    } catch (e) {
      setErrorForm({ mensaje: e.message, campos: e.errores });
    } finally {
      setGuardando(false);
    }
  }

  function guardarCuenta(e) {
    e.preventDefault();
    const cuerpo = { nombre: cuenta.nombre, tipo: cuenta.tipo, saldoInicial: cuenta.saldoInicial === "" ? null : Number(cuenta.saldoInicial) };
    ejecutar(
      () => (cuenta.id ? api.put(`/cuentas/${cuenta.id}`, cuerpo, token) : api.post("/cuentas", cuerpo, token)),
      () => setCuenta(null)
    );
  }

  function guardarTransferencia(e) {
    e.preventDefault();
    ejecutar(
      () => api.post("/transferencias", { ...transferencia, monto: transferencia.monto === "" ? null : Number(transferencia.monto) }, token),
      () => setTransferencia(null)
    );
  }

  async function archivarOEliminar(c) {
    if (!window.confirm(`¿Eliminar la cuenta "${c.nombre}"? Si ya tiene movimientos, se archiva y conservas su historial.`)) return;
    await api.delete(`/cuentas/${c.id}`, token);
    recargar();
  }

  async function restaurar(c) {
    try {
      await api.patch(`/cuentas/${c.id}/restaurar`, undefined, token);
      recargar();
    } catch (e) {
      window.alert(e.message);
    }
  }

  const cambiar = (setter) => (campo) => (e) => setter((f) => ({ ...f, [campo]: e.target.value }));

  return (
    <div>
      <EncabezadoPagina
        titulo="Cuentas"
        subtitulo={cuentas ? `Tienes ${formatearPesos(total)} en total.` : "Dónde está tu plata."}
        accion={
          <div className="flex flex-wrap gap-2">
            <Boton
              variante="secundario"
              icono={ArrowLeftRight}
              disabled={activas.length < 2}
              onClick={() => {
                setErrorForm({ mensaje: "", campos: {} });
                setTransferencia({ cuentaOrigenId: activas[0].id, cuentaDestinoId: activas[1].id, monto: "", fecha: hoyISO(), descripcion: "" });
              }}
            >
              Transferir
            </Boton>
            <Boton
              icono={Plus}
              onClick={() => {
                setErrorForm({ mensaje: "", campos: {} });
                setCuenta({ nombre: "", tipo: "EFECTIVO", saldoInicial: "0" });
              }}
            >
              Nueva cuenta
            </Boton>
          </div>
        }
      />
      {error && <Aviso>{error}</Aviso>}

      {!cuentas ? (
        <Cargando />
      ) : cuentas.length === 0 ? (
        <EstadoVacio icono={Wallet} titulo="Aún no tienes cuentas" descripcion="Crea una por cada lugar donde tengas plata: efectivo, Nequi, Daviplata, el banco…" />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {cuentas.map((c) => {
            const Icono = ICONOS[c.tipo];
            return (
              <div key={c.id} className={`rounded-2xl border bg-panel p-5 shadow-sm ${c.archivada ? "border-dashed border-slate-300 opacity-70" : "border-slate-200"}`}>
                <div className="flex items-start justify-between">
                  <span className="rounded-lg bg-emerald-50 p-2 text-brillo">
                    <Icono size={20} aria-hidden="true" />
                  </span>
                  <div className="flex">
                    {c.archivada ? (
                      <button onClick={() => restaurar(c)} className="rounded-lg p-2 text-slate-400 hover:bg-slate-100" aria-label="Restaurar">
                        <RotateCcw size={16} />
                      </button>
                    ) : (
                      <>
                        <button
                          onClick={() => {
                            setErrorForm({ mensaje: "", campos: {} });
                            setCuenta({ id: c.id, nombre: c.nombre, tipo: c.tipo, saldoInicial: c.saldoInicial });
                          }}
                          className="rounded-lg p-2 text-slate-400 hover:bg-slate-100"
                          aria-label="Editar"
                        >
                          <Pencil size={16} />
                        </button>
                        <button onClick={() => archivarOEliminar(c)} className="rounded-lg p-2 text-slate-400 hover:bg-red-50 hover:text-red-600" aria-label="Eliminar">
                          <Archive size={16} />
                        </button>
                      </>
                    )}
                  </div>
                </div>
                <p className="mt-3 font-medium text-slate-900">{c.nombre}</p>
                <p className="text-xs text-slate-500">
                  {NOMBRES_TIPO_CUENTA[c.tipo]}
                  {c.archivada && " · Archivada"}
                </p>
                <p className={`mt-3 text-2xl font-bold ${Number(c.saldoActual) < 0 ? "text-red-600" : "text-slate-900"}`}>{formatearPesos(c.saldoActual)}</p>
              </div>
            );
          })}
        </div>
      )}

      <label className="mt-6 inline-flex items-center gap-2 text-sm text-slate-600">
        <input type="checkbox" checked={verArchivadas} onChange={(e) => setVerArchivadas(e.target.checked)} className="accent-emerald-600" />
        Mostrar cuentas archivadas
      </label>

      <Modal titulo={cuenta?.id ? "Editar cuenta" : "Nueva cuenta"} abierto={cuenta !== null} alCerrar={() => setCuenta(null)}>
        {cuenta && (
          <form onSubmit={guardarCuenta} className="space-y-4" noValidate>
            <Campo etiqueta="Nombre" placeholder="Ej: Nequi, Bancolombia, Efectivo" value={cuenta.nombre} onChange={cambiar(setCuenta)("nombre")} error={errorForm.campos.nombre} />
            <Campo etiqueta="Tipo" value={cuenta.tipo} onChange={cambiar(setCuenta)("tipo")}>
              {Object.entries(NOMBRES_TIPO_CUENTA).map(([valor, texto]) => (
                <option key={valor} value={valor}>{texto}</option>
              ))}
            </Campo>
            <Campo etiqueta="Saldo inicial (COP)" type="number" min="0" step="0.01" value={cuenta.saldoInicial} onChange={cambiar(setCuenta)("saldoInicial")} error={errorForm.campos.saldoInicial} />
            {errorForm.mensaje && Object.keys(errorForm.campos).length === 0 && <Aviso>{errorForm.mensaje}</Aviso>}
            <Boton type="submit" disabled={guardando} className="w-full">{guardando ? "Guardando..." : "Guardar"}</Boton>
          </form>
        )}
      </Modal>

      <Modal titulo="Transferir entre tus cuentas" abierto={transferencia !== null} alCerrar={() => setTransferencia(null)}>
        {transferencia && (
          <form onSubmit={guardarTransferencia} className="space-y-4" noValidate>
            <p className="text-sm text-slate-500">Por ejemplo, un retiro del cajero: sale del banco y entra al efectivo. No cuenta como gasto.</p>
            <Campo etiqueta="Desde" value={transferencia.cuentaOrigenId} onChange={cambiar(setTransferencia)("cuentaOrigenId")}>
              {activas.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
            </Campo>
            <Campo etiqueta="Hacia" value={transferencia.cuentaDestinoId} onChange={cambiar(setTransferencia)("cuentaDestinoId")}>
              {activas.map((c) => <option key={c.id} value={c.id}>{c.nombre}</option>)}
            </Campo>
            <Campo etiqueta="Monto (COP)" type="number" min="0" step="0.01" value={transferencia.monto} onChange={cambiar(setTransferencia)("monto")} error={errorForm.campos.monto} />
            <Campo etiqueta="Fecha" type="date" value={transferencia.fecha} onChange={cambiar(setTransferencia)("fecha")} error={errorForm.campos.fecha} />
            <Campo etiqueta="Descripción (opcional)" value={transferencia.descripcion} onChange={cambiar(setTransferencia)("descripcion")} maxLength={200} />
            {errorForm.mensaje && Object.keys(errorForm.campos).length === 0 && <Aviso>{errorForm.mensaje}</Aviso>}
            <Boton type="submit" disabled={guardando} className="w-full">{guardando ? "Transfiriendo..." : "Transferir"}</Boton>
          </form>
        )}
      </Modal>
    </div>
  );
}
