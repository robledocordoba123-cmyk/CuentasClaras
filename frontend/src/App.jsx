import { lazy, Suspense, useEffect, useState } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { useSesion } from "./context/SesionContext";
import { Logo } from "./components/Layout";
import MetaPagina from "./components/MetaPagina";
import InicioPage from "./pages/InicioPage";
import AccesoPage from "./pages/AccesoPage";

// Lo público se descarga de entrada; el panel (con las gráficas) llega solo al entrar.
const Layout = lazy(() => import("./components/Layout"));
const ResumenPage = lazy(() => import("./pages/ResumenPage"));
const MovimientosPage = lazy(() => import("./pages/MovimientosPage"));
const CuentasPage = lazy(() => import("./pages/CuentasPage"));
const PresupuestosPage = lazy(() => import("./pages/PresupuestosPage"));
const CategoriasPage = lazy(() => import("./pages/CategoriasPage"));
const PerfilPage = lazy(() => import("./pages/PerfilPage"));
const PersonasPage = lazy(() => import("./pages/PersonasPage"));
const PagosFijosPage = lazy(() => import("./pages/PagosFijosPage"));
const MetasPage = lazy(() => import("./pages/MetasPage"));
const PrivacidadPage = lazy(() => import("./pages/PrivacidadPage"));
const NoEncontradaPage = lazy(() => import("./pages/NoEncontradaPage"));

// Mientras se comprueba la sesión. Si la API estaba dormida, se explica por qué tarda.
function Abriendo() {
  const [tarda, setTarda] = useState(false);
  useEffect(() => {
    const temporizador = setTimeout(() => setTarda(true), 4000);
    return () => clearTimeout(temporizador);
  }, []);
  return (
    <div className="grid min-h-screen place-items-center px-6 text-center">
      <div>
        <Logo />
        <p className="mt-6 text-sm text-slate-500" role="status">
          {tarda ? "El servidor estaba en reposo y está encendiendo. Puede tardar hasta un par de minutos." : "Abriendo tus cuentas…"}
        </p>
      </div>
    </div>
  );
}

const RUTAS_DE_LA_APP = ["/movimientos", "/cuentas", "/presupuestos", "/categorias", "/perfil", "/personas", "/pagos-fijos", "/metas"];

// Una pantalla con sesión, con su título y el noindex.
const privada = (titulo, Pagina) => (
  <>
    <MetaPagina titulo={titulo} privada />
    <Pagina />
  </>
);

const publica = (titulo, ruta, Pagina) => (
  <>
    <MetaPagina titulo={titulo} ruta={ruta} />
    <Pagina />
  </>
);

export default function App() {
  const { estado, usuario } = useSesion();

  if (estado === "comprobando") return <Abriendo />;

  return (
    <Suspense fallback={<Abriendo />}>
      {!usuario ? (
        <Routes>
          <Route path="/" element={<InicioPage />} />
          <Route path="/acceso" element={publica("Iniciar sesión", "/acceso", AccesoPage)} />
          <Route path="/privacidad" element={publica("Política de datos", "/privacidad", PrivacidadPage)} />
          {/* Las direcciones de la app llevan al acceso; las que no existen, a la 404. */}
          {RUTAS_DE_LA_APP.map((ruta) => <Route key={ruta} path={ruta} element={<Navigate to="/acceso" replace />} />)}
          <Route path="*" element={privada("Página no encontrada", NoEncontradaPage)} />
        </Routes>
      ) : (
        <Routes>
          <Route path="/privacidad" element={publica("Política de datos", "/privacidad", PrivacidadPage)} />
          <Route element={<Layout />}>
            <Route index element={privada("Resumen", ResumenPage)} />
            <Route path="movimientos" element={privada("Movimientos", MovimientosPage)} />
            <Route path="cuentas" element={privada("Cuentas", CuentasPage)} />
            <Route path="presupuestos" element={privada("Sobres del mes", PresupuestosPage)} />
            <Route path="categorias" element={privada("Categorías", CategoriasPage)} />
            <Route path="perfil" element={privada("Tu cuenta", PerfilPage)} />
            <Route path="personas" element={privada("Personas", PersonasPage)} />
            <Route path="pagos-fijos" element={privada("Pagos fijos", PagosFijosPage)} />
            <Route path="metas" element={privada("Metas de ahorro", MetasPage)} />
          </Route>
          <Route path="/acceso" element={<Navigate to="/" replace />} />
          <Route path="*" element={privada("Página no encontrada", NoEncontradaPage)} />
        </Routes>
      )}
    </Suspense>
  );
}
