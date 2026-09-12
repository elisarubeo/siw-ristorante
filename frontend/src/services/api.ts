import axios, { AxiosError } from 'axios'
import type { ApiError } from '../types'

/* Dove sta il token nel browser. Una costante e non la stringa ripetuta in
   giro: sbagliarla in un punto solo produrrebbe un bug che sembra magia
   (il login riesce e la richiesta dopo e' anonima). */
export const CHIAVE_TOKEN = 'statistiche.token'
export const CHIAVE_UTENTE = 'statistiche.utente'

/* Modalita' senza backend: con VITE_FINTO=1 i servizi rispondono da
   datiFinti.ts e non tocca la rete. Serve finche' i @RestController non
   esistono; si spegne mettendo VITE_FINTO=0 in .env.development. */
export const DATI_FINTI = import.meta.env.VITE_FINTO === '1'

/* Un'unica istanza condivisa, non axios.get(...) sparso nei componenti: il
   token e la gestione del 401 si scrivono una volta sola, qui.

   baseURL e' un percorso RELATIVO, non "http://localhost:8080/api". In
   sviluppo se lo prende il proxy di Vite; una volta pubblicata, la pagina e i
   dati arrivano dallo stesso server e '/api' punta da se' dove deve. Cosi' non
   c'e' nessun indirizzo da cambiare fra sviluppo e consegna. */
export const api = axios.create({
    baseURL: '/api',
    headers: { 'Content-Type': 'application/json' },
})

/* In uscita: allega il token a ogni richiesta.
   Si rilegge da localStorage a ogni chiamata invece di catturarlo una volta:
   dopo un logout la variabile catturata continuerebbe a essere spedita. */
api.interceptors.request.use((richiesta) => {
    const token = localStorage.getItem(CHIAVE_TOKEN)
    if (token) {
        richiesta.headers.Authorization = `Bearer ${token}`
    }
    return richiesta
})

/* In entrata: il 401 vuol dire che il token non c'e' piu' o e' scaduto, e non
   c'e' niente da fare in quella pagina. Si svuota il deposito e si torna al
   login.

   La guardia sull'indirizzo evita il giro a vuoto: se a rispondere 401 e' il
   login stesso (password sbagliata), rimandare al login significherebbe
   ricaricare la pagina e cancellare il messaggio d'errore proprio mentre lo si
   sta per mostrare. */
api.interceptors.response.use(
    (risposta) => risposta,
    (errore: AxiosError) => {
        const eIlLogin = errore.config?.url?.includes('/auth/login')
        if (errore.response?.status === 401 && !eIlLogin) {
            localStorage.removeItem(CHIAVE_TOKEN)
            localStorage.removeItem(CHIAVE_UTENTE)
            window.location.href = `${import.meta.env.BASE_URL}login`
        }
        return Promise.reject(errore)
    },
)

/* Da qualunque cosa sia andata storta a una frase da mostrare.
   Il backend manda sempre { "message": ... }: e' l'unico campo da leggere, ed
   e' il motivo per cui gli errori dell'API hanno tutti la stessa forma. Gli
   altri rami coprono i casi in cui una risposta non c'e' proprio. */
export function messaggioErrore(errore: unknown, ripiego: string): string {
    if (axios.isAxiosError(errore)) {
        const corpo = errore.response?.data as ApiError | undefined
        if (corpo?.message) {
            return corpo.message
        }
        if (errore.response?.status === 403) {
            return 'Non hai i permessi per vedere queste statistiche.'
        }
        /* Nessuna risposta: il server non e' raggiungibile. In sviluppo e'
           quasi sempre Spring Boot spento, e vale la pena dirlo. */
        if (!errore.response) {
            return 'Il server non risponde. Controlla che l\'applicazione sia avviata.'
        }
    }
    return ripiego
}
