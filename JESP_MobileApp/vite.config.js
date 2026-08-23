import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// En produccion el build se copia a JESP_Core/src/main/resources/static
// y Spring Boot lo sirve en el mismo puerto que la API (5001).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:5001',
        changeOrigin: true,
      },
    },
  },
  build: {
    outDir: '../JESP_Core/src/main/resources/static',
    emptyOutDir: true,
  },
})
