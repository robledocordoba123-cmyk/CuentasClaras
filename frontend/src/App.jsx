import { useEffect, useState } from "react";
import { Navigate, Route, Routes } from "react-router-dom";
import { useSesion } from "./context/SesionContext";
import Layout, { Logo } from "./components/Layout";
import AccesoPage from "./pages/AccesoPage";
import ResumenPage from "./pages/ResumenPage";
import MovimientosPage from "./pages/MovimientosPage";
import CuentasPage from "./pages/CuentasPage";
import PresupuestosPage from "./pages/PresupuestosPage";
import CategoriasPage from "./pages/CategoriasPage";
import PerfilPage from "./pages/PerfilPage";
import PersonasPage from "./pages/PersonasPage";
import PagosFijosPage from "./pages/PagosFijosPage";
import MetasPage from "./pages/MetasPage";
import PrivacidadPage from "./pages/PrivacidadPage";

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

export default function App() {
  const { estado, usuario } = useSesion();

  if (estado === "comprobando") return <Abriendo />;

  // Sin sesión, solo están el acceso y la política de datos.
  if (!usuario) {
    return (
      <Routes>
        <Route path="/acceso" element={<AccesoPage />} />
        <Route path="/privacidad" element={<PrivacidadPage />} />
        <Route path="*" element={<Navigate to="/acceso" replace />} />
      </Routes>
    );
  }

  return (
    <Routes>
      <Route path="/privacidad" element={<PrivacidadPage />} />
      <Route element={<Layout />}>
        <Route index element={<ResumenPage />} />
        <Route path="movimientos" element={<MovimientosPage />} />
        <Route path="cuentas" element={<CuentasPage />} />
        <Route path="presupuestos" element={<PresupuestosPage />} />
        <Route path="categorias" element={<CategoriasPage />} />
        <Route path="perfil" element={<PerfilPage />} />
        <Route path="personas" element={<PersonasPage />} />
        <Route path="pagos-fijos" element={<PagosFijosPage />} />
        <Route path="metas" element={<MetasPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
