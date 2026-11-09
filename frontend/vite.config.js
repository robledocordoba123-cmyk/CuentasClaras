import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// En desarrollo, /api se redirige al backend de Spring Boot (puerto 8080 por
// defecto; se puede cambiar con la variable API_URL). Al publicar, Vercel
// reenvía /api a la API (vercel.json): la página y la API son el mismo sitio.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      // /api/salud es la ruta de salud de Spring (igual que la reescritura de Vercel).
      '/api/salud': { target: process.env.API_URL ?? 'http://localhost:8080', rewrite: () => '/actuator/health' },
      '/api': process.env.API_URL ?? 'http://localhost:8080',
    },
  },
})
