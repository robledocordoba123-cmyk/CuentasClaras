import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// En desarrollo, /api se redirige al backend de Spring Boot (puerto 8080 por
// defecto; se puede cambiar con la variable API_URL). En producción el
// frontend usa VITE_API_URL con la dirección pública de la API.
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    proxy: {
      '/api': process.env.API_URL ?? 'http://localhost:8080',
    },
  },
})
