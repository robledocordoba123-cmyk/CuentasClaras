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
      <span className="text-lg font-bold text-slate-900">
        Cuentas<span className="text-emerald-600">Claras</span>
      </span>
    </div>
  );
}

export default function Layout() {
  const { usuario, cerrarSesion } = useSesion();

  const claseEnlace = ({ isActive }) =>
    `flex items-center gap-2.5 rounded-lg px-3 py-2 text-sm font-medium transition ${
      isActive ? "bg-emerald-50 text-emerald-700" : "text-slate-600 hover:bg-slate-100"
    }`;

  return (
    <div className="min-h-screen lg:flex">
      {/* Menú lateral en computador */}
      <aside className="sticky top-0 hidden h-screen w-60 shrink-0 flex-col border-r border-slate-200 bg-white p-4 lg:flex">
        <Logo />
        <nav className="mt-8 flex flex-1 flex-col gap-1">
          {OPCIONES.map(({ a, texto, icono: Icono }) => (
            <NavLink key={a} to={a} end={a === "/"} className={claseEnlace}>
              <Icono size={18} aria-hidden="true" /> {texto}
            </NavLink>
          ))}
        </nav>
        <div className="border-t border-slate-200 pt-4">
          <p className="truncate text-sm font-medium text-slate-800">{usuario?.nombre}</p>
          <p className="truncate text-xs text-slate-500">{usuario?.email}</p>
          <button onClick={cerrarSesion} className="mt-3 flex items-center gap-2 text-sm text-slate-500 hover:text-red-600">
            <LogOut size={16} aria-hidden="true" /> Cerrar sesión
          </button>
        </div>
      </aside>

      {/* Barra superior en celular */}
      <header className="flex items-center justify-between border-b border-slate-200 bg-white px-4 py-3 lg:hidden">
        <Logo />
        <button onClick={cerrarSesion} className="rounded-lg p-2 text-slate-500 hover:bg-slate-100" aria-label="Cerrar sesión">
          <LogOut size={18} />
        </button>
      </header>

      <main className="mx-auto w-full max-w-6xl flex-1 px-4 pb-24 pt-6 lg:px-8 lg:pb-8">
        <Outlet />
      </main>

      {/* Menú inferior en celular */}
      <nav className="fixed inset-x-0 bottom-0 z-40 grid grid-cols-5 border-t border-slate-200 bg-white lg:hidden">
        {OPCIONES.map(({ a, texto, icono: Icono }) => (
          <NavLink
            key={a}
            to={a}
            end={a === "/"}
            className={({ isActive }) =>
              `flex flex-col items-center gap-0.5 py-2 text-[11px] ${isActive ? "text-emerald-600" : "text-slate-500"}`
            }
          >
            <Icono size={20} aria-hidden="true" /> {texto}
          </NavLink>
        ))}
      </nav>
    </div>
  );
}
