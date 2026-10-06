import { useEffect, useState } from "react";
import { Coffee, LogIn, Sparkles, UserPlus } from "lucide-react";
import { useSesion } from "../context/SesionContext";
import { Logo } from "../components/Layout";
import { Aviso, Boton, Campo } from "../components/ui";

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
  const [datos, setDatos] = useState({ nombre: "", email: "", password: "" });
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
          <h1 className="mt-10 text-2xl font-bold text-slate-900">
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

            {despertando && (
              <p className="flex items-start gap-2 rounded-lg border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800">
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
            <div className="mt-6 rounded-xl border border-emerald-200 bg-emerald-50 p-4">
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
              className="font-medium text-emerald-700 hover:underline"
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

      <div className="relative hidden flex-col justify-between overflow-hidden border-l border-slate-200 bg-panel p-12 lg:flex">
        <div className="pointer-events-none absolute -right-24 top-10 h-96 w-96 rounded-full bg-brillo/15 blur-3xl" />

        {/* Una "tarjeta" con el saldo, como la de una app de banco. */}
        <div className="relative mx-auto mt-16 w-full max-w-sm rotate-[-4deg] rounded-3xl border border-emerald-200 bg-[linear-gradient(135deg,#123a2b,#0c1210_70%)] p-6 shadow-[0_30px_80px_rgb(0_0_0/0.6)]">
          <div className="flex items-center justify-between text-xs text-slate-500">
            <span>Saldo en tus cuentas</span>
            <span className="font-semibold text-brillo">+12,4 % este mes</span>
          </div>
          <p className="mt-3 font-display text-4xl font-semibold tracking-tight text-slate-900">$ 3.018.700</p>
          <div className="mt-6 grid grid-cols-3 gap-2 text-xs">
            {[
              ["Efectivo", "$ 412.000"],
              ["Nequi", "$ 689.700"],
              ["Banco", "$ 1.917.000"],
            ].map(([cuenta, saldo]) => (
              <div key={cuenta} className="rounded-xl border border-slate-200 bg-black/20 p-2.5">
                <p className="text-slate-500">{cuenta}</p>
                <p className="mt-0.5 font-semibold text-slate-800">{saldo}</p>
              </div>
            ))}
          </div>
        </div>

        <div className="relative">
          <p className="font-display text-3xl font-semibold leading-snug text-slate-900">
            “Cuentas claras, <span className="text-brillo">amistades largas</span>.”
          </p>
          <p className="mt-4 max-w-md text-slate-500">
            Efectivo, Nequi, Daviplata y el banco en un solo lugar. Ponte un presupuesto por categoría y la app te avisa antes
            de que te pases.
          </p>
        </div>
      </div>
    </div>
  );
}
