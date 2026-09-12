import { api, DATI_FINTI, messaggioErrore } from './api'
import { loginFinto, ritardo } from './datiFinti'
import type { RispostaLogin } from '../types'

/* Lo strato che parla con l'API, tenuto separato dai componenti: e' la stessa
   divisione che nel backend c'e' fra controller e service. Un componente non
   sa che esiste axios, sa che esiste `login`. */

export async function login(username: string, password: string): Promise<RispostaLogin> {
    if (DATI_FINTI) {
        return ritardo(loginFinto(username, password))
    }
    try {
        const risposta = await api.post<RispostaLogin>('/auth/login', { username, password })
        return risposta.data
    } catch (errore) {
        /* L'eccezione di axios diventa un'eccezione con un messaggio da
           mostrare: chi chiama non deve sapere com'e' fatto un AxiosError. */
        throw new Error(messaggioErrore(errore, 'Credenziali non valide.'))
    }
}
