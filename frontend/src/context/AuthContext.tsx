import { createContext, useCallback, useContext, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { CHIAVE_TOKEN, CHIAVE_UTENTE } from '../services/api'
import * as authService from '../services/authService'
import { dimenticaContesto } from '../services/statisticheService'
import type { RispostaLogin } from '../types'

interface Utente {
    username: string
    userId: number
    role: RispostaLogin['role']
}

interface ValoreAuth {
    utente: Utente | null
    autenticato: boolean
    ristoratore: boolean
    login: (username: string, password: string) => Promise<void>
    logout: () => void
}

const AuthContext = createContext<ValoreAuth | undefined>(undefined)

/* Presenza del token in localStorage e sua validita' sono due cose diverse:
   senza guardare exp resteremmo "collegati" anche con un token gia' scaduto,
   fino al primo 401. Un token illeggibile vale quanto nessun token. */
function tokenScaduto(token: string): boolean {
    try {
        const parteCentrale = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')
        const { exp } = JSON.parse(atob(parteCentrale))
        return exp * 1000 <= Date.now()
    } catch {
        return true
    }
}

function utenteSalvato(): Utente | null {
    const token = localStorage.getItem(CHIAVE_TOKEN)
    const salvato = localStorage.getItem(CHIAVE_UTENTE)
    if (!token || !salvato || tokenScaduto(token)) {
        localStorage.removeItem(CHIAVE_TOKEN)
        localStorage.removeItem(CHIAVE_UTENTE)
        return null
    }
    try {
        return JSON.parse(salvato) as Utente
    } catch {
        localStorage.removeItem(CHIAVE_TOKEN)
        localStorage.removeItem(CHIAVE_UTENTE)
        return null
    }
}

export function AuthProvider({ children }: { children: ReactNode }) {
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
        dimenticaContesto()
        setUtente(null)
    }, [])

    const valore = useMemo<ValoreAuth>(() => ({
        utente,
        autenticato: utente !== null,
        ristoratore: utente?.role === 'RISTORATORE',
        login,
        logout,
    }), [utente, login, logout])

    return <AuthContext.Provider value={valore}>{children}</AuthContext.Provider>
}

// eslint-disable-next-line react-refresh/only-export-components
export function useAuth(): ValoreAuth {
    const valore = useContext(AuthContext)
    if (valore === undefined) {
        throw new Error('useAuth va usato dentro <AuthProvider>')
    }
    return valore
}
