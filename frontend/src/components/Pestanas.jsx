import { NavLink } from "react-router-dom";

// Pestañas entre pantallas hermanas; los grupos están en utils/grupos.js.
export default function Pestanas({ grupo, etiqueta }) {
  return (
    <nav aria-label={etiqueta} className="mb-6 flex gap-1 overflow-x-auto border-b border-slate-200">
      {grupo.map(({ a, texto }) => (
        <NavLink
          key={a}
          to={a}
          className={({ isActive }) =>
            `-mb-px shrink-0 border-b-2 px-3 py-2 text-sm font-semibold transition ${
              isActive ? "border-tinta text-slate-900" : "border-transparent text-slate-500 hover:text-slate-900"
            }`
          }
        >
          {texto}
        </NavLink>
      ))}
    </nav>
  );
}
