import axios, { AxiosError } from 'axios'
import type { ApiError } from '../types'

export const CHIAVE_TOKEN = 'statistiche.token'
export const CHIAVE_UTENTE = 'statistiche.utente'

export const DATI_FINTI = import.meta.env.VITE_FINTO === '1'

export const api = axios.create({
    baseURL: '/api',
    headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((richiesta) => {
    const token = localStorage.getItem(CHIAVE_TOKEN)
    if (token) {
        richiesta.headers.Authorization = `Bearer ${token}`
    }
    return richiesta
})

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

export function messaggioErrore(errore: unknown, ripiego: string): string {
    if (axios.isAxiosError(errore)) {
        const corpo = errore.response?.data as ApiError | undefined
        if (corpo?.message) {
            return corpo.message
        }
        if (errore.response?.status === 403) {
            return 'Non hai i permessi per vedere queste statistiche.'
        }
        if (!errore.response) {
            return 'Il server non risponde. Controlla che l\'applicazione sia avviata.'
        }
    }
    return ripiego
}
