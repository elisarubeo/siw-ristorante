import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backend = env.VITE_BACKEND ?? 'http://localhost:8080'

  return {
    plugins: [react()],

    base: '/statistiche/',

    build: {
      outDir: '../src/main/resources/static/statistiche',
      emptyOutDir: true,
    },

    server: {
      port: 5173,
      proxy: {
        '/api': { target: backend, changeOrigin: true },
      },
    },
  }
})
