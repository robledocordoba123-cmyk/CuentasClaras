import { Component } from "react";
import PantallaAviso from "./PantallaAviso";

// Si un componente falla al dibujarse, React borraría toda la página y dejaría la
// pantalla en blanco. Esto la reemplaza por un aviso con salida. Los límites de
// error de React todavía solo existen como clases.
export default class LimiteDeError extends Component {
  state = { conError: false };

  static getDerivedStateFromError() {
    return { conError: true };
  }

  componentDidCatch(error, info) {
    console.error("Error al dibujar la página", error, info.componentStack);
  }

  render() {
    if (!this.state.conError) return this.props.children;
    return (
      <PantallaAviso titulo="Algo se nos descuadró." excedido>
        <p className="mt-3 text-slate-600" role="alert">
          La página tuvo un problema. Tus datos están guardados en el servidor; recargar suele arreglarlo.
        </p>
        <div className="mt-5 flex flex-wrap justify-center gap-2 sm:justify-start">
          <button type="button" onClick={() => window.location.reload()} className="rounded-full bg-tinta px-5 py-2.5 text-sm font-semibold text-white hover:bg-slate-700">
            Recargar
          </button>
          <a href="/" className="rounded-full border border-slate-300 bg-white px-5 py-2.5 text-sm font-semibold text-slate-800 hover:bg-slate-50">
            Ir al inicio
          </a>
        </div>
      </PantallaAviso>
    );
  }
}
