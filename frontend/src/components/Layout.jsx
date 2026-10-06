import { NavLink, Outlet } from "react-router-dom";
import { ArrowLeftRight, LayoutDashboard, LogOut, PiggyBank, Tags, Wallet } from "lucide-react";
import { useSesion } from "../context/SesionContext";

const OPCIONES = [
  { a: "/", texto: "Resumen", icono: LayoutDashboard },
  { a: "/movimientos", texto: "Movimientos", icono: ArrowLeftRight },
  { a: "/cuentas", texto: "Cuentas", icono: Wallet },
  { a: "/presupuestos", texto: "Presupuestos", icono: PiggyBank },
  { a: "/categorias", texto: "Categorías", icono: Tags },
];

export function Logo() {
  return (
    <div className="flex items-center gap-2">
      <img src="/favicon.svg" alt="" className="h-8 w-8" />
      <span className="font-display text-lg font-semibold tracking-tight text-slate-900">
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

  return (
    <div className="min-h-screen">
      {/* Barra superior: logo, menú en forma de cápsula flotante y usuario. */}
      <header className="sticky top-0 z-40 border-b border-slate-200/70 bg-fondo/80 backdrop-blur-xl">
        <div className="mx-auto flex max-w-6xl items-center gap-4 px-4 py-3 lg:px-8">
          <Logo />

          <nav className="mx-auto hidden items-center gap-1 rounded-full border border-slate-200 bg-panel p-1 lg:flex">
            {OPCIONES.map(({ a, texto, icono: Icono }) => (
              <NavLink
                key={a}
                to={a}
                end={a === "/"}
                className={({ isActive }) =>
                  `flex items-center gap-2 rounded-full px-4 py-1.5 text-sm font-semibold transition ${
                    isActive ? "bg-brillo text-fondo shadow-[0_0_20px_rgb(61_242_162/0.35)]" : "text-slate-500 hover:text-slate-900"
                  }`
                }
              >
                <Icono size={16} aria-hidden="true" /> {texto}
              </NavLink>
            ))}
          </nav>

          <div className="ml-auto flex items-center gap-2 lg:ml-0">
            <div className="hidden text-right leading-tight sm:block">
              <p className="max-w-40 truncate text-sm font-semibold text-slate-900">{usuario?.nombre}</p>
              <p className="max-w-40 truncate text-xs text-slate-500">{usuario?.email}</p>
            </div>
            <span className="grid h-9 w-9 place-items-center rounded-full border border-slate-300 bg-slate-100 text-xs font-bold text-brillo">
              {iniciales(usuario?.nombre)}
            </span>
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
      <nav className="fixed inset-x-3 bottom-3 z-40 grid grid-cols-5 rounded-2xl border border-slate-200 bg-panel/90 p-1 backdrop-blur-xl lg:hidden">
        {OPCIONES.map(({ a, texto, icono: Icono }) => (
          <NavLink
            key={a}
            to={a}
            end={a === "/"}
            className={({ isActive }) =>
              `flex flex-col items-center gap-0.5 rounded-xl py-2 text-[10px] font-semibold ${
                isActive ? "bg-brillo text-fondo" : "text-slate-500"
              }`
            }
          >
            <Icono size={19} aria-hidden="true" /> {texto}
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
