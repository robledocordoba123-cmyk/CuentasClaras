import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import { BrowserRouter } from "react-router-dom";
import "./index.css";
import App from "./App.jsx";
import { SesionProvider } from "./context/SesionContext.jsx";
import LimiteDeError from "./components/LimiteDeError.jsx";

// Tras publicar una versión nueva, una pestaña abierta puede pedir partes que ya no
// existen: se recarga una vez para traer la versión nueva en vez de fallar.
window.addEventListener("vite:preloadError", (evento) => {
  try {
    if (sessionStorage.getItem("cc_recargada")) return;
    sessionStorage.setItem("cc_recargada", "1");
  } catch {
    return;
  }
  evento.preventDefault();
  window.location.reload();
});

createRoot(document.getElementById("root")).render(
  <StrictMode>
    <LimiteDeError>
      <BrowserRouter>
        <SesionProvider>
          <App />
        </SesionProvider>
      </BrowserRouter>
    </LimiteDeError>
  </StrictMode>
);
