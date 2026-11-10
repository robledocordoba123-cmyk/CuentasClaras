import Sobre from "./Sobre";
import { Marca } from "./Layout";

// Pantalla completa para cuando algo no sale como debía. No usa el enrutador:
// el aviso de error se muestra justo cuando la aplicación pudo haber fallado.
export default function PantallaAviso({ titulo, children, excedido = false }) {
  return (
    <main className="flex min-h-screen flex-col px-5 py-8">
      <a href="/" className="self-start" aria-label="CuentasClaras, ir al inicio">
        <Marca />
      </a>
      <div className="mx-auto flex w-full max-w-2xl flex-1 flex-col items-center justify-center gap-6 text-center sm:flex-row sm:text-left">
        <Sobre color={excedido ? "#c23b5a" : "#0f7a8a"} restante={0} excedido={excedido} className="h-36 w-44 shrink-0" />
        <div>
          <h1 className="text-4xl font-semibold text-slate-900">{titulo}</h1>
          {children}
        </div>
      </div>
    </main>
  );
}
