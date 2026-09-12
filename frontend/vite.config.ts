import { defineConfig, loadEnv } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backend = env.VITE_BACKEND ?? 'http://localhost:8080'

  return {
    plugins: [react()],

    /* Sotto quale percorso vivra' l'applicazione una volta pubblicata. Vite lo
       antepone a ogni riferimento a un file negli asset (script, CSS, immagini).
       DEVE combaciare con altre due cose, o i file danno 404 o le rotte non
       risolvono:
         - il basename di <BrowserRouter> in src/main.tsx
         - il resource handler di ReactAppConfig lato Spring            */
    base: '/statistiche/',

    build: {
      /* Il build finisce dentro le risorse statiche di Spring Boot: cosi' la
         consegna resta un solo progetto Maven, senza bisogno di npm per
         eseguirlo. emptyOutDir ripulisce prima, altrimenti i file con il
         codice nel nome (index-a1b2c3.js) si accumulerebbero a ogni build. */
      outDir: '../src/main/resources/static/statistiche',
      emptyOutDir: true,
    },

    server: {
      port: 5173,
      /* In sviluppo la pagina arriva da Vite (:5173) e i dati da Spring
         (:8080): due porte diverse sono due origini diverse, e il browser
         bloccherebbe le chiamate. Il proxy fa si' che per il browser tutto
         venga da :5173, e CORS non entra in gioco.
         In produzione il proxy non esiste e non serve: pagina e dati arrivano
         dallo stesso server, per questo in api.ts la baseURL e' il percorso
         relativo '/api' e non un indirizzo assoluto. */
      proxy: {
        '/api': { target: backend, changeOrigin: true },
      },
    },
  }
})
