import { Link } from "react-router-dom";
import { ArrowLeft } from "lucide-react";
import { Logo } from "../components/Layout";

// Política de tratamiento de datos (Ley 1581 de 2012). CuentasClaras es un
// proyecto de portafolio: un servicio real la revisaría con un abogado.
export default function PrivacidadPage() {
  return (
    <main className="mx-auto max-w-2xl px-5 py-10">
      <Logo />
      <Link to="/" className="mt-6 inline-flex items-center gap-1 text-sm font-medium text-slate-500 hover:text-slate-900">
        <ArrowLeft size={14} aria-hidden="true" /> Volver
      </Link>
      <h1 className="mt-6 text-3xl font-bold text-slate-900">Política de tratamiento de datos</h1>
      <p className="mt-2 text-sm text-slate-500">Versión de noviembre de 2026. CuentasClaras es un proyecto de portafolio.</p>

      <div className="mt-8 space-y-6 text-slate-700">
        <section>
          <h2 className="text-lg font-semibold text-slate-900">Qué datos guardamos</h2>
          <p className="mt-1">Tu nombre, tu correo y lo que registras: cuentas, movimientos, categorías y presupuestos. La contraseña se guarda cifrada con BCrypt; nadie puede leerla.</p>
        </section>
        <section>
          <h2 className="text-lg font-semibold text-slate-900">Para qué</h2>
          <p className="mt-1">Solo para mostrarte tus finanzas. No vendemos tus datos, no los compartimos y no los usamos para publicidad.</p>
        </section>
        <section>
          <h2 className="text-lg font-semibold text-slate-900">Tus derechos</h2>
          <p className="mt-1">Según la Ley 1581 de 2012 puedes conocer, actualizar y suprimir tus datos. En <strong>Tu cuenta</strong> puedes descargar todo lo que guardamos en un archivo y borrar tu cuenta con todos sus datos, sin pedírselo a nadie. También puedes presentar quejas ante la Superintendencia de Industria y Comercio.</p>
        </section>
        <section>
          <h2 className="text-lg font-semibold text-slate-900">Cuánto tiempo</h2>
          <p className="mt-1">Mientras tengas la cuenta. Cuando la borras, se eliminan de inmediato tus datos y todos tus movimientos.</p>
        </section>
      </div>
    </main>
  );
}
