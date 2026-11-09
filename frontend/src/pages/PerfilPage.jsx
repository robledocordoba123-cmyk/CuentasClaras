import { useState } from "react";
import { Link } from "react-router-dom";
import { Download, KeyRound, Trash2 } from "lucide-react";
import { api } from "../api/cliente";
import { useSesion } from "../context/SesionContext";
import { Aviso, Boton, Campo, EncabezadoPagina, Tarjeta } from "../components/ui";

const VACIO_CLAVE = { actual: "", nueva: "" };

// Tu cuenta: cambiar la contraseña y los derechos de la Ley 1581 (descargar tus
// datos y borrar la cuenta).
export default function PerfilPage() {
  const { usuario, olvidarSesion } = useSesion();
  const [clave, setClave] = useState(VACIO_CLAVE);
  const [estadoClave, setEstadoClave] = useState({ errores: {}, mensaje: "", ok: false, enviando: false });
  const [borrar, setBorrar] = useState({ abierto: false, password: "", error: "", enviando: false });

  async function cambiarClave(e) {
    e.preventDefault();
    setEstadoClave({ errores: {}, mensaje: "", ok: false, enviando: true });
    try {
      await api.put("/auth/password", clave);
      setClave(VACIO_CLAVE);
      setEstadoClave({ errores: {}, mensaje: "Listo. Cerramos tus sesiones en los demás dispositivos.", ok: true, enviando: false });
    } catch (err) {
      setEstadoClave({ errores: err.errores, mensaje: err.message, ok: false, enviando: false });
    }
  }

  async function borrarCuenta(e) {
    e.preventDefault();
    setBorrar((b) => ({ ...b, error: "", enviando: true }));
    try {
      await api.delete("/auth/cuenta", { password: borrar.password });
      olvidarSesion();
    } catch (err) {
      setBorrar((b) => ({ ...b, error: err.message, enviando: false }));
    }
  }

  return (
    <>
      <EncabezadoPagina titulo="Tu cuenta" subtitulo={`${usuario?.nombre} · ${usuario?.email}`} />

      <div className="grid gap-6 lg:grid-cols-2">
        <Tarjeta titulo="Cambiar la contraseña">
          <form onSubmit={cambiarClave} className="space-y-4" noValidate>
            <Campo etiqueta="Contraseña actual" type="password" autoComplete="current-password" value={clave.actual}
              onChange={(e) => setClave({ ...clave, actual: e.target.value })} error={estadoClave.errores?.actual} />
            <Campo etiqueta="Nueva contraseña (mínimo 8 caracteres)" type="password" autoComplete="new-password" value={clave.nueva}
              onChange={(e) => setClave({ ...clave, nueva: e.target.value })} error={estadoClave.errores?.nueva} />
            {estadoClave.mensaje && <Aviso tipo={estadoClave.ok ? "exito" : "error"}>{estadoClave.mensaje}</Aviso>}
            <Boton type="submit" icono={KeyRound} disabled={estadoClave.enviando}>
              {estadoClave.enviando ? "Guardando…" : "Cambiar contraseña"}
            </Boton>
          </form>
        </Tarjeta>

        <Tarjeta titulo="Tus datos">
          <p className="text-sm text-slate-600">
            Descarga en un archivo todo lo que CuentasClaras guarda de ti. Así lo dice la <Link to="/privacidad" className="font-medium underline">política de datos</Link>.
          </p>
          <Boton variante="secundario" icono={Download} className="mt-4"
            onClick={() => api.descargar("/auth/mis-datos", "mis-datos-cuentasclaras.json")}>
            Descargar mis datos
          </Boton>

          <div className="mt-8 border-t border-slate-200 pt-6">
            <h3 className="font-semibold text-slate-900">Borrar mi cuenta</h3>
            <p className="mt-1 text-sm text-slate-600">Se borran tu cuenta y todos tus movimientos, cuentas y presupuestos. No se puede deshacer.</p>
            {!borrar.abierto ? (
              <Boton variante="peligro" icono={Trash2} className="mt-4" onClick={() => setBorrar({ ...borrar, abierto: true })}>
                Borrar mi cuenta
              </Boton>
            ) : (
              <form onSubmit={borrarCuenta} className="mt-4 space-y-3" noValidate>
                <Campo etiqueta="Escribe tu contraseña para confirmar" type="password" autoComplete="current-password"
                  value={borrar.password} onChange={(e) => setBorrar({ ...borrar, password: e.target.value })} />
                {borrar.error && <Aviso>{borrar.error}</Aviso>}
                <div className="flex gap-2">
                  <Boton type="submit" variante="peligro" icono={Trash2} disabled={borrar.enviando}>
                    {borrar.enviando ? "Borrando…" : "Sí, borrar todo"}
                  </Boton>
                  <Boton type="button" variante="secundario" onClick={() => setBorrar({ abierto: false, password: "", error: "", enviando: false })}>
                    Cancelar
                  </Boton>
                </div>
              </form>
            )}
          </div>
        </Tarjeta>
      </div>
    </>
  );
}
