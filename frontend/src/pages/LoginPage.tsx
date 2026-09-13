import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { DATI_FINTI } from '../services/api'

export default function LoginPage() {
    const [username, setUsername] = useState('')
    const [password, setPassword] = useState('')
    const [errore, setErrore] = useState<string | null>(null)
    const [inCorso, setInCorso] = useState(false)

    const { login, autenticato } = useAuth()
    const navigate = useNavigate()
    const posizione = useLocation()

    const destinazione = (posizione.state as { da?: string } | null)?.da ?? '/'

    useEffect(() => {
        if (autenticato) {
            navigate(destinazione, { replace: true })
        }
    }, [autenticato, destinazione, navigate])

    async function invia(evento: React.FormEvent) {
        evento.preventDefault()
        setErrore(null)
        setInCorso(true)
        try {
            await login(username.trim(), password)
        } catch (e) {
            setErrore(e instanceof Error ? e.message : 'Accesso non riuscito.')
            setInCorso(false)
        }
    }

    return (
        <div className="accesso">
            <h1>Statistiche</h1>
            <p className="sottotitolo">
                L'andamento del tuo locale. Entra con le credenziali da ristoratore.
            </p>

            <div className="riquadro">
                {errore && <p className="avviso avviso-errore">{errore}</p>}

                <form onSubmit={invia} noValidate>
                    <div className="campo">
                        <label htmlFor="username">Nome utente</label>
                        <input
                            id="username" name="username" type="text"
                            autoComplete="username" autoFocus
                            value={username}
                            onChange={(e) => setUsername(e.target.value)}
                            required
                        />
                    </div>

                    <div className="campo">
                        <label htmlFor="password">Password</label>
                        <input
                            id="password" name="password" type="password"
                            autoComplete="current-password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />
                    </div>

                    <button type="submit" className="bottone" disabled={inCorso}>
                        {inCorso ? 'Accesso in corso…' : 'Entra'}
                    </button>
                </form>

                <p className="nota-credenziali">
                    {DATI_FINTI
                        ? 'Dati finti attivi: entra con borgo / borgo.'
                        : 'Le stesse credenziali con cui entri nel sito.'}
                </p>
            </div>
        </div>
    )
}
