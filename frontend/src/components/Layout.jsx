import { Link, NavLink, Outlet, useLocation } from "react-router-dom";
import { ArrowLeftRight, LayoutDashboard, LogOut, Mail, Users, Wallet } from "lucide-react";
import { useSesion } from "../context/SesionContext";

const OPCIONES = [
  { a: "/", texto: "Resumen", icono: LayoutDashboard },
  { a: "/movimientos", texto: "Movimientos", icono: ArrowLeftRight },
  { a: "/cuentas", texto: "Cuentas", icono: Wallet },
  // "Sobres" agrupa los presupuestos del mes y las metas de ahorro; Movimientos
  // agrupa también los pagos fijos y las categorías (ver Pestanas.jsx).
  { a: "/presupuestos", texto: "Sobres", icono: Mail, tambien: ["/metas"] },
  { a: "/personas", texto: "Personas", icono: Users },
];

// El logo: un sobre cuya solapa es un chulo. Cuentas claras = cuentas en orden.
export function Marca({ className = "h-8 w-8" }) {
  return (
    <svg viewBox="0 0 32 32" className={className} aria-hidden="true" focusable="false">
      <rect width="32" height="32" rx="8" fill="#16181d" />
      <path d="M6.5 11.5h19v12a2 2 0 0 1-2 2h-15a2 2 0 0 1-2-2z" fill="none" stroke="#f3f4ef" strokeWidth="2" strokeLinejoin="round" />
      <path d="M8.5 13.5l5 5 10-10" fill="none" stroke="#3ec3d4" strokeWidth="2.8" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export function Logo() {
  return (
    <div className="flex items-center gap-2">
      <Marca />
      <span className="font-display text-xl font-semibold tracking-tight text-slate-900">
        Cuentas<span className="text-brillo">Claras</span>
      </span>
    </div>
  );
}

function iniciales(nombre = "") {
  return nombre
    .split(" ")
    .filter((parte) => /^\p{L}/u.test(parte))
    .slice(0, 2)
    .map((parte) => parte[0].toUpperCase())
    .join("");
}

export default function Layout() {
  const { usuario, cerrarSesion } = useSesion();
  const { pathname } = useLocation();
  // Una sección queda marcada también en sus pestañas hermanas.
  const activa = (opcion, isActive) => isActive || (opcion.tambien ?? []).includes(pathname) || (opcion.a === "/movimientos" && ["/pagos-fijos", "/categorias"].includes(pathname));

  return (
    <div className="min-h-screen">
      {/* Barra superior: logo, menú en forma de cápsula flotante y usuario. */}
      <header className="sticky top-0 z-40 border-b border-slate-200 bg-fondo/90 backdrop-blur-xl">
        <div className="mx-auto flex max-w-6xl items-center gap-4 px-4 py-3 lg:px-8">
          <Logo />

          <nav className="mx-auto hidden items-center gap-1 rounded-full border border-slate-200 bg-panel p-1 lg:flex" aria-label="Secciones">
            {OPCIONES.map((opcion) => { const { a, texto, icono: Icono } = opcion; return (
              <NavLink
                key={a}
                to={a}
                end={a === "/"}
                className={({ isActive }) =>
                  `flex items-center gap-2 rounded-full px-4 py-1.5 text-sm font-semibold transition ${
                    activa(opcion, isActive) ? "bg-tinta text-white" : "text-slate-600 hover:text-slate-900"
                  }`
                }
              >
                <Icono size={16} aria-hidden="true" /> {texto}
              </NavLink>
            ); })}
          </nav>

          <div className="ml-auto flex items-center gap-2 lg:ml-0">
            <Link to="/perfil" className="flex items-center gap-2 rounded-full p-1 hover:bg-slate-100" title="Tu cuenta">
              <div className="hidden text-right leading-tight sm:block">
                <p className="max-w-40 truncate text-sm font-semibold text-slate-900">{usuario?.nombre}</p>
                <p className="max-w-40 truncate text-xs text-slate-500">{usuario?.email}</p>
              </div>
              <span className="grid h-9 w-9 place-items-center rounded-full border border-slate-300 bg-slate-100 text-xs font-bold text-slate-800" aria-label="Tu cuenta">
                {iniciales(usuario?.nombre)}
              </span>
            </Link>
            <button
              onClick={cerrarSesion}
              className="rounded-full p-2 text-slate-500 transition hover:bg-slate-100 hover:text-red-600"
              aria-label="Cerrar sesión"
              title="Cerrar sesión"
            >
              <LogOut size={18} />
            </button>
          </div>
        </div>
      </header>

      <main className="mx-auto w-full max-w-6xl px-4 pb-28 pt-8 lg:px-8 lg:pb-12">
        <Outlet />
      </main>

      {/* Menú inferior flotante en celular */}
      <nav className="fixed inset-x-3 bottom-3 z-40 grid grid-cols-5 rounded-2xl border border-slate-200 bg-panel/95 p-1 shadow-lg backdrop-blur-xl lg:hidden" aria-label="Secciones">
        {OPCIONES.map((opcion) => { const { a, texto, icono: Icono } = opcion; return (
          <NavLink
            key={a}
            to={a}
            end={a === "/"}
            className={({ isActive }) =>
              `flex flex-col items-center gap-0.5 rounded-xl py-2 text-[10px] font-semibold ${
                activa(opcion, isActive) ? "bg-tinta text-white" : "text-slate-600"
              }`
            }
          >
            <Icono size={19} aria-hidden="true" /> {texto}
          </NavLink>
        ); })}
      </nav>
    </div>
  );
}
