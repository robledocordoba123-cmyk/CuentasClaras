import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { Coffee, LogIn, Sparkles, UserPlus } from "lucide-react";
import { useSesion } from "../context/SesionContext";
import { Logo } from "../components/Layout";
import { Aviso, Boton, Campo } from "../components/ui";
import Guilloche from "../components/Guilloche";
import Sobre from "../components/Sobre";

// Solo en la demo pública (VITE_MODO_DEMO=true): acceso de un clic a una
// cuenta con datos de ejemplo, para que quien visite pueda probar sin registrarse.
const MODO_DEMO = import.meta.env.VITE_MODO_DEMO === "true";
const CUENTA_DEMO = { email: "demo@cuentasclaras.co", password: "Demo2026!" };

// El plan gratuito del hosting apaga la API tras un rato sin uso y tarda cerca
// de un minuto en encender: si la respuesta se demora, se explica por qué.
const MS_ANTES_DE_AVISAR = 4000;

export default function AccesoPage() {
  const { iniciarSesion, registrarse } = useSesion();
  const [modo, setModo] = useState("login");
  const [datos, setDatos] = useState({ nombre: "", email: "", password: "", aceptaPolitica: false });
  const [error, setError] = useState("");
  const [errores, setErrores] = useState({});
  const [cargando, setCargando] = useState(false);
  const [despertando, setDespertando] = useState(false);

  useEffect(() => {
    if (!cargando) return undefined;
    const temporizador = setTimeout(() => setDespertando(true), MS_ANTES_DE_AVISAR);
    return () => {
      clearTimeout(temporizador);
      setDespertando(false);
    };
  }, [cargando]);

  const cambiar = (campo) => (e) => setDatos((d) => ({ ...d, [campo]: e.target.value }));

  async function enviar(accion) {
    setError("");
    setErrores({});
    setCargando(true);
    try {
      await accion();
    } catch (e) {
      setError(e.message);
      setErrores(e.errores || {});
    } finally {
      setCargando(false);
    }
  }

  function alEnviar(e) {
    e.preventDefault();
    enviar(() => (modo === "login" ? iniciarSesion(datos.email, datos.password) : registrarse(datos)));
  }

  return (
    <div className="grid min-h-screen lg:grid-cols-2">
      <div className="flex items-center justify-center px-4 py-12">
        <div className="w-full max-w-sm">
          <Logo />
          <h1 className="mt-10 text-4xl font-semibold text-slate-900">
            {modo === "login" ? "Inicia sesión" : "Crea tu cuenta"}
          </h1>
          <p className="mb-6 mt-1 text-sm text-slate-500">
            {modo === "login" ? "Tus finanzas, claras y en un solo lugar." : "Es gratis y toma menos de un minuto."}
          </p>

          <form onSubmit={alEnviar} className="space-y-4" noValidate>
            {modo === "registro" && (
              <Campo etiqueta="Nombre" value={datos.nombre} onChange={cambiar("nombre")} error={errores.nombre} autoComplete="name" />
            )}
            <Campo etiqueta="Correo" type="email" value={datos.email} onChange={cambiar("email")} error={errores.email} autoComplete="email" />
            <Campo
              etiqueta="Contraseña"
              type="password"
              value={datos.password}
              onChange={cambiar("password")}
              error={errores.password}
              autoComplete={modo === "login" ? "current-password" : "new-password"}
            />

            {modo === "registro" && (
              <label className="flex items-start gap-2 text-sm text-slate-600">
                <input type="checkbox" checked={datos.aceptaPolitica} onChange={(e) => setDatos((d) => ({ ...d, aceptaPolitica: e.target.checked }))}
                  className="mt-0.5 h-4 w-4 accent-[#0f7a8a]" />
                <span>
                  Autorizo el tratamiento de mis datos según la <Link to="/privacidad" className="font-medium underline">política de datos</Link>.
                  {errores.aceptaPolitica && <span className="mt-1 block text-xs text-red-600">{errores.aceptaPolitica}</span>}
                </span>
              </label>
            )}

            {despertando && (
              <p className="flex items-start gap-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800" role="status">
                <Coffee size={16} className="mt-0.5 shrink-0" aria-hidden="true" />
                El servidor estaba en reposo y está encendiendo. La primera entrada puede tardar hasta un minuto.
              </p>
            )}
            {error && Object.keys(errores).length === 0 && <Aviso>{error}</Aviso>}

            <Boton type="submit" disabled={cargando} icono={modo === "login" ? LogIn : UserPlus} className="w-full py-2.5">
              {cargando ? "Un momento..." : modo === "login" ? "Ingresar" : "Crear cuenta"}
            </Boton>
          </form>

          {MODO_DEMO && modo === "login" && (
            <div className="mt-6 rounded-2xl border border-slate-200 bg-panel p-4">
              <p className="text-sm font-medium text-slate-900">¿Solo quieres probarla?</p>
              <p className="mb-3 mt-0.5 text-xs text-slate-600">Entra a una cuenta con tres meses de datos de ejemplo.</p>
              <Boton
                variante="secundario"
                icono={Sparkles}
                disabled={cargando}
                className="w-full"
                onClick={() => enviar(() => iniciarSesion(CUENTA_DEMO.email, CUENTA_DEMO.password))}
              >
                Entrar a la demo
              </Boton>
            </div>
          )}

          <p className="mt-6 text-sm text-slate-600">
            {modo === "login" ? "¿No tienes cuenta? " : "¿Ya tienes cuenta? "}
            <button
              className="font-semibold text-brillo hover:underline"
              onClick={() => {
                setModo(modo === "login" ? "registro" : "login");
                setError("");
                setErrores({});
              }}
            >
              {modo === "login" ? "Regístrate" : "Inicia sesión"}
            </button>
          </p>
        </div>
      </div>

      <div className="relative hidden flex-col justify-between overflow-hidden bg-tinta p-12 text-white lg:flex">
        <Guilloche color="#3ec3d4" className="pointer-events-none absolute -right-40 -top-40 h-[42rem] w-[42rem]" opacidad={0.28} />
        <Guilloche color="#b7791f" lineas={10} ondas={12} className="pointer-events-none absolute -bottom-56 -left-40 h-[34rem] w-[34rem]" opacidad={0.22} />

        {/* Tres sobres del mes: el de mercado casi lleno, el de domicilios a medias y el de salidas vacío. */}
        <div className="relative mx-auto mt-10 grid w-full max-w-md grid-cols-3 gap-4" aria-hidden="true">
          {[
            ["Mercado", "#d9622b", 0.8, false],
            ["Domicilios", "#c23b5a", 0.35, false],
            ["Salidas", "#6a4fc2", 0, true],
          ].map(([nombre, color, restante, excedido]) => (
            <div key={nombre} className="rounded-2xl bg-white/95 p-3 text-center text-slate-800">
              <Sobre color={color} restante={restante} excedido={excedido} className="mx-auto h-20 w-24" />
              <p className="mt-1 text-xs font-semibold">{nombre}</p>
            </div>
          ))}
        </div>

        <div className="relative">
          <p className="font-display text-4xl font-semibold leading-tight">
            “Cuentas claras, <span className="italic text-[#7fd6e1]">amistades largas</span>.”
          </p>
          <p className="mt-4 max-w-md text-slate-300">
            Reparte la plata del mes en sobres, como se ha hecho siempre: efectivo, Nequi, Daviplata y el banco en un solo lugar,
            y un aviso antes de que un sobre se quede vacío.
          </p>
        </div>
      </div>
    </div>
  );
}
