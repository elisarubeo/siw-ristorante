import { api, DATI_FINTI, messaggioErrore } from './api'
import { loginFinto, ritardo } from './datiFinti'
import type { RispostaLogin } from '../types'

export async function login(username: string, password: string): Promise<RispostaLogin> {
    if (DATI_FINTI) {
        return ritardo(loginFinto(username, password))
    }
    try {
        const risposta = await api.post<RispostaLogin>('/auth/login', { username, password })
        return risposta.data
    } catch (errore) {
        throw new Error(messaggioErrore(errore, 'Credenziali non valide.'))
    }
}
