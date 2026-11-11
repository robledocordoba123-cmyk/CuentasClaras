import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { CalendarClock, Download, Mail, ShieldCheck, Sparkles, Target, Users } from "lucide-react";
import { useSesion } from "../context/SesionContext";
import { Logo } from "../components/Layout";
import Guilloche from "../components/Guilloche";
import Sobre from "../components/Sobre";
import MetaPagina from "../components/MetaPagina";
import { CUENTA_DEMO, MODO_DEMO } from "../utils/demo";

const FUNCIONES = [
  { icono: Mail, titulo: "Sobres del mes", texto: "Un sobre por categoría. Empieza lleno y se vacía al gastar; te avisamos cuando le queda el 20 %." },
  { icono: Users, titulo: "Cuentas claras con personas", texto: "Quién te debe y a quién le debes, con abonos. Sin enredos ni capturas de pantalla." },
  { icono: CalendarClock, titulo: "Pagos fijos", texto: "Arriendo, servicios y el plan del celular: ves qué falta pagar y lo registras con un clic." },
  { icono: Target, titulo: "Metas de ahorro", texto: "Ponle monto y fecha, y te decimos cuánto apartar cada mes para llegar." },
];

// Datos para buscadores (schema.org): qué es la aplicación.
const DATOS_ESTRUCTURADOS = {
  "@context": "https://schema.org",
  "@type": "WebApplication",
  name: "CuentasClaras",
  applicationCategory: "FinanceApplication",
  operatingSystem: "Web",
  inLanguage: "es-CO",
  url: "https://cuentasclaras-demo.vercel.app/",
  description: "Finanzas personales para Colombia: efectivo, Nequi, Daviplata y el banco en un solo lugar, con presupuestos en sobres.",
  offers: { "@type": "Offer", price: "0", priceCurrency: "COP" },
};

// Página pública: qué es CuentasClaras y cómo probarla.
export default function InicioPage() {
  const { iniciarSesion } = useSesion();
  const navegar = useNavigate();
  const [entrando, setEntrando] = useState(false);
  const [error, setError] = useState("");

  async function probarDemo() {
    setEntrando(true);
    setError("");
    try {
      await iniciarSesion(CUENTA_DEMO.email, CUENTA_DEMO.password);
      navegar("/");
    } catch (e) {
      setError(e.message);
      setEntrando(false);
    }
  }

  return (
    <div className="min-h-screen">
      <MetaPagina ruta="/" />
      <script type="application/ld+json">{JSON.stringify(DATOS_ESTRUCTURADOS)}</script>

      <header className="mx-auto flex max-w-6xl items-center justify-between px-5 py-5">
        <Logo />
        <Link to="/acceso" className="rounded-full border border-slate-300 bg-white px-4 py-2 text-sm font-semibold text-slate-800 hover:bg-slate-50">Iniciar sesión</Link>
      </header>

      <main>
        <section className="mx-auto grid max-w-6xl items-center gap-10 px-5 pb-16 pt-6 lg:grid-cols-[1.1fr_1fr] lg:pt-12">
          <div>
            <p className="text-sm font-semibold uppercase tracking-[0.16em] text-brillo">Finanzas personales para Colombia</p>
            <h1 className="mt-3 text-5xl font-semibold leading-[1.02] text-slate-900 sm:text-6xl">Reparte la plata del mes en sobres.</h1>
            <p className="mt-5 max-w-lg text-lg text-slate-600">
              Efectivo, Nequi, Daviplata y el banco en un solo lugar. Sabes cuánto te queda en cada sobre, quién te debe y qué falta pagar.
            </p>
            <div className="mt-8 flex flex-wrap gap-3">
              {MODO_DEMO && (
                <button type="button" onClick={probarDemo} disabled={entrando}
                  className="inline-flex items-center gap-2 rounded-full bg-tinta px-6 py-3 font-semibold text-white hover:bg-slate-700 disabled:opacity-60">
                  <Sparkles size={18} aria-hidden="true" /> {entrando ? "Abriendo la demo…" : "Probar la demo"}
                </button>
              )}
              <Link to="/acceso?modo=registro" className="inline-flex items-center rounded-full border border-slate-300 bg-white px-6 py-3 font-semibold text-slate-800 hover:bg-slate-50">
                Crear mi cuenta gratis
              </Link>
            </div>
            {entrando && <p className="mt-3 text-sm text-slate-500" role="status">Si la demo estaba en reposo, puede tardar hasta un par de minutos en encender.</p>}
            {error && <p className="mt-3 text-sm text-red-700" role="alert">{error}</p>}
          </div>

          <div className="relative overflow-hidden rounded-3xl bg-tinta p-8 text-white" aria-hidden="true">
            <Guilloche color="#3ec3d4" className="pointer-events-none absolute -right-24 -top-24 h-96 w-96" opacidad={0.3} />
            <p className="relative text-sm text-[#9fdde6]">Saldo en tus cuentas</p>
            <p className="cifra relative mt-1 text-5xl font-semibold">$ 3.794.800</p>
            <div className="relative mt-8 grid grid-cols-3 gap-3">
              {[["Mercado", "#d9622b", 0.8, false], ["Domicilios", "#c23b5a", 0.3, false], ["Salidas", "#6a4fc2", 0, true]].map(([nombre, color, restante, excedido]) => (
                <div key={nombre} className="rounded-2xl bg-white/95 p-3 text-center text-slate-800">
                  <Sobre color={color} restante={restante} excedido={excedido} className="mx-auto h-16 w-20" />
                  <p className="mt-1 text-xs font-semibold">{nombre}</p>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section className="border-y border-slate-200 bg-panel py-16" aria-labelledby="como-funciona">
          <div className="mx-auto max-w-6xl px-5">
            <h2 id="como-funciona" className="text-3xl font-semibold text-slate-900">Cuentas claras, amistades largas.</h2>
            <ul className="mt-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-4">
              {FUNCIONES.map(({ icono: Icono, titulo, texto }) => (
                <li key={titulo}>
                  <span className="inline-flex rounded-xl bg-slate-100 p-2.5 text-slate-800"><Icono size={20} aria-hidden="true" /></span>
                  <h3 className="mt-3 font-display text-lg font-semibold text-slate-900">{titulo}</h3>
                  <p className="mt-1 text-sm text-slate-600">{texto}</p>
                </li>
              ))}
            </ul>
          </div>
        </section>

        <section className="mx-auto grid max-w-6xl gap-6 px-5 py-16 md:grid-cols-[auto_1fr] md:items-center" aria-labelledby="tus-datos">
          <span className="inline-flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-50 text-emerald-700"><ShieldCheck size={28} aria-hidden="true" /></span>
          <div>
            <h2 id="tus-datos" className="text-2xl font-semibold text-slate-900">Tus datos son tuyos.</h2>
            <p className="mt-2 max-w-2xl text-slate-600">
              La sesión viaja en una cookie que ningún script puede leer. Puedes descargar todo lo que guardamos y borrar tu cuenta cuando quieras,
              como lo pide la Ley 1581. <Link to="/privacidad" className="font-semibold text-slate-900 underline">Política de datos</Link>.
            </p>
            <p className="mt-2 inline-flex items-center gap-2 text-sm text-slate-500"><Download size={14} aria-hidden="true" /> También exportas tus movimientos a Excel.</p>
          </div>
        </section>
      </main>

      <footer className="border-t border-slate-200 py-8">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center justify-between gap-3 px-5 text-sm text-slate-500">
          <p>Proyecto de portafolio de Manuela Córdoba Robledo.</p>
          <div className="flex gap-4">
            <Link to="/privacidad" className="hover:text-slate-900">Política de datos</Link>
            <a href="https://github.com/robledocordoba123-cmyk/CuentasClaras" className="hover:text-slate-900">Código en GitHub</a>
          </div>
        </div>
      </footer>
    </div>
  );
}
