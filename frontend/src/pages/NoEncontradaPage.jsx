import PantallaAviso from "../components/PantallaAviso";

// Página 404: un sobre vacío. El noindex evita que los buscadores guarden la dirección.
export default function NoEncontradaPage() {
  return (
    <PantallaAviso titulo="Este sobre está vacío.">
      <p className="mt-3 text-slate-600">La página que buscas no existe. Puede que el enlace esté mal escrito.</p>
      <a href="/" className="mt-5 inline-flex rounded-full bg-tinta px-5 py-2.5 text-sm font-semibold text-white hover:bg-slate-700">
        Volver al inicio
      </a>
    </PantallaAviso>
  );
}
