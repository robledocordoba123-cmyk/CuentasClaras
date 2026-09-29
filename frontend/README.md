# CuentasClaras: frontend

React 19 + Vite + Tailwind CSS 4 + React Router + Recharts. Pantallas: acceso, resumen con gráficas, movimientos, cuentas (con transferencias), presupuestos y categorías. Funciona en computador y en celular.

```bash
npm install
npm run dev      # http://localhost:5173 (las llamadas a /api van al backend en :8080)
npm run lint
npm run build
```

Si el backend corre en otro puerto: `API_URL=http://localhost:8090 npm run dev`.

En producción, `VITE_API_URL` debe apuntar a la URL pública de la API (terminada en `/api`) y `VITE_MODO_DEMO=true` muestra el acceso de un clic a la cuenta de demostración.
