import { Navigate, Route, Routes } from "react-router-dom";
import { useSesion } from "./context/SesionContext";
import Layout from "./components/Layout";
import AccesoPage from "./pages/AccesoPage";
import ResumenPage from "./pages/ResumenPage";
import MovimientosPage from "./pages/MovimientosPage";
import CuentasPage from "./pages/CuentasPage";
import PresupuestosPage from "./pages/PresupuestosPage";
import CategoriasPage from "./pages/CategoriasPage";

export default function App() {
  const { token } = useSesion();

  // Sin sesión, la única pantalla disponible es la de acceso.
  if (!token) {
    return (
      <Routes>
        <Route path="/acceso" element={<AccesoPage />} />
        <Route path="*" element={<Navigate to="/acceso" replace />} />
      </Routes>
    );
  }

  return (
    <Routes>
      <Route element={<Layout />}>
        <Route index element={<ResumenPage />} />
        <Route path="movimientos" element={<MovimientosPage />} />
        <Route path="cuentas" element={<CuentasPage />} />
        <Route path="presupuestos" element={<PresupuestosPage />} />
        <Route path="categorias" element={<CategoriasPage />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
