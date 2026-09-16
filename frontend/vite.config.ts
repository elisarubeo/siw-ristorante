import { cpSync, existsSync, rmSync } from 'node:fs'
import { resolve } from 'node:path'

import { defineConfig, loadEnv, type Plugin, type Logger } from 'vite'
import react from '@vitejs/plugin-react'

/* La build finisce in src/main/resources/static/statistiche/, ma l'applicazione
   avviata da VS Code legge le risorse dal classpath, cioe' da target/classes/:
   senza questa copia il bundle nuovo si vedrebbe solo dopo aver fatto
   ricompilare Maven. La copia in src resta ed e' quella che va in git; questa e'
   solo una comodita' di sviluppo e non cambia nulla nel jar impacchettato. */
function copiaNelClasspath(): Plugin {
  let sorgente = ''
  let destinazione = ''
  let classi = ''
  let logger: Logger

  return {
    name: 'copia-nel-classpath',
    apply: 'build',

    configResolved(config) {
      sorgente = resolve(config.root, config.build.outDir)
      classi = resolve(config.root, '../target/classes')
      destinazione = resolve(classi, 'static/statistiche')
      logger = config.logger
    },

    closeBundle() {
      /* Nessuna build Maven ancora fatta: non c'e' nessun classpath da tenere
         allineato, e crearlo a mano confonderebbe e basta. */
      if (!existsSync(classi)) {
        logger.warn(`target/classes non esiste: salto la copia in ${destinazione}`)
        return
      }

      /* Da svuotare: i nomi dei file contengono l'hash del contenuto, quindi
         una copia senza cancellare lascerebbe li' i bundle di tutte le build
         precedenti. */
      rmSync(destinazione, { recursive: true, force: true })
      cpSync(sorgente, destinazione, { recursive: true })
      logger.info(`copiato in ${destinazione}`)
    },
  }
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '')
  const backend = env.VITE_BACKEND ?? 'http://localhost:8080'

  return {
    plugins: [react(), copiaNelClasspath()],

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
