import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { CHIAVE_TOKEN, CHIAVE_UTENTE } from '../services/api'
import * as authService from '../services/authService'
import { dimenticaContesto } from '../services/statisticheService'
import type { RispostaLogin } from '../types'

/* Chi ha fatto il login serve in punti lontani dell'albero dei componenti: la
   barra in alto per il nome e il pulsante Esci, la rotta protetta per decidere
   se lasciar passare, la pagina delle statistiche per sapere se il ruolo e'
   quello giusto. Passarlo di prop in prop vorrebbe dire attraversare
   componenti che di autenticazione non sanno niente (il "prop drilling"):
   useContext esiste per questo. */

interface Utente {
    username: string
    userId: number
    role: RispostaLogin['role']
}

interface ValoreAuth {
    utente: Utente | null
    autenticato: boolean
    /* Le statistiche sono dati di cassa: le puo' vedere solo chi gestisce il
       locale. Il controllo che conta e' lato server (hasAuthority sulla catena
       /api/**); questo serve a non mostrare una pagina che risponderebbe 403. */
    ristoratore: boolean
    login: (username: string, password: string) => Promise<void>
    logout: () => void
}

/* undefined e non null come valore iniziale: cosi' useAuth puo' distinguere
   "nessun utente collegato" da "sei fuori dal Provider", che e' un errore di
   programmazione e non uno stato dell'applicazione. */
const AuthContext = createContext<ValoreAuth | undefined>(undefined)

/* Chi era entrato prima della ricarica.

   Lo stato di React vive in memoria e sparisce a ogni F5; localStorage
   sopravvive ma non fa ridisegnare niente. Si scrive in tutti e due e si
   rilegge all'avvio: e' cosi' che ricaricare la pagina non butta fuori
   nessuno. */
function utenteSalvato(): Utente | null {
    const token = localStorage.getItem(CHIAVE_TOKEN)
    const salvato = localStorage.getItem(CHIAVE_UTENTE)
    if (!token || !salvato) {
        return null
    }
    try {
        return JSON.parse(salvato) as Utente
    } catch {
        /* Deposito manomesso o rimasto da una versione precedente: si riparte
           puliti invece di trascinarsi uno stato incoerente. */
        localStorage.removeItem(CHIAVE_TOKEN)
        localStorage.removeItem(CHIAVE_UTENTE)
        return null
    }
}

export function AuthProvider({ children }: { children: ReactNode }) {
    /* La funzione passata a useState e' l'inizializzatore pigro: React la
       chiama una volta sola, al primo disegno, e non a ogni disegno come
       farebbe con useState(utenteSalvato()).

       Perche' qui e non in un useEffect, che sarebbe la soluzione d'istinto:
       leggere localStorage e' sincrono, il valore si puo' avere subito. Con un
       effetto il primo disegno mostrerebbe "nessun utente" e solo il secondo
       la verita' - cioe' un lampo di pagina di login a ogni ricarica, e la
       necessita' di uno stato "sto ancora guardando" per nasconderlo. Un
       effetto serve a sincronizzarsi con qualcosa di esterno, non a calcolare
       uno stato iniziale che si sa gia'. */
    const [utente, setUtente] = useState<Utente | null>(utenteSalvato)

    const login = useCallback(async (username: string, password: string) => {
        const risposta = await authService.login(username, password)
        const collegato: Utente = {
            username: risposta.username,
            userId: risposta.userId,
            role: risposta.role,
        }
        localStorage.setItem(CHIAVE_TOKEN, risposta.token)
        localStorage.setItem(CHIAVE_UTENTE, JSON.stringify(collegato))
        setUtente(collegato)
    }, [])

    const logout = useCallback(() => {
        localStorage.removeItem(CHIAVE_TOKEN)
        localStorage.removeItem(CHIAVE_UTENTE)
        /* Il contesto (il locale di chi era collegato) e' tenuto da parte nel
           service: senza questo, chi entra dopo si troverebbe in barra i
           collegamenti al ristorante di chi c'era prima. */
        dimenticaContesto()
        setUtente(null)
    }, [])

    /* useMemo perche' il valore del contesto e' un oggetto: ricrearlo a ogni
       disegno lo farebbe risultare sempre "cambiato" e obbligherebbe ogni
       componente che lo consuma a ridisegnarsi senza motivo. */
    const valore = useMemo<ValoreAuth>(() => ({
        utente,
        autenticato: utente !== null,
        ristoratore: utente?.role === 'RISTORATORE',
        login,
        logout,
    }), [utente, login, logout])

    return <AuthContext.Provider value={valore}>{children}</AuthContext.Provider>
}

/* Un hook invece di esportare il contesto nudo: i componenti scrivono
   useAuth() senza sapere che dietro c'e' un contesto, e l'errore di averlo
   usato fuori dal Provider si legge subito invece di arrivare come
   "cannot read property of undefined" da qualche altra parte. */
// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): ValoreAuth {
    const valore = useContext(AuthContext)
    if (valore === undefined) {
        throw new Error('useAuth va usato dentro <AuthProvider>')
    }
    return valore
}
