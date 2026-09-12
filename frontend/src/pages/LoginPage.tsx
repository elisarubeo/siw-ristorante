import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { DATI_FINTI } from '../services/api'

/* L'accesso alla parte REST.

   Il login si rifa' anche a chi e' gia' entrato nel sito in Thymeleaf, e non
   e' una svista: quella meta' dell'applicazione riconosce l'utente da un
   cookie di sessione, questa da un token che il browser deve avere in mano
   per allegarlo a ogni richiesta. Sono due meccanismi diversi, e il cookie
   non si trasforma in un token da solo. */

export default function LoginPage() {
    /* Campi controllati: il valore vive nello stato di React, e l'<input> lo
       mostra soltanto. Il DOM non e' mai la fonte della verita' - ogni tasto
       premuto passa da onChange, e il valore e' sempre quello che si legge
       nello stato. */
    const [username, setUsername] = useState('')
    const [password, setPassword] = useState('')
    const [errore, setErrore] = useState<string | null>(null)
    const [inCorso, setInCorso] = useState(false)

    const { login, autenticato } = useAuth()
    const navigate = useNavigate()
    const posizione = useLocation()

    /* Da dove si veniva, messo qui dalla rotta protetta. Dopo il login si
       torna li' invece di finire sempre sulla home. */
    const destinazione = (posizione.state as { da?: string } | null)?.da ?? '/'

    /* Il reindirizzamento sta in un useEffect e non dentro il gestore del
       submit: navigare e' un effetto collaterale, e va fatto DOPO che React ha
       disegnato il nuovo stato, non durante. Serve anche a chi arriva sul
       login gia' collegato (per esempio riaprendo il segnalibro). */
    useEffect(() => {
        if (autenticato) {
            navigate(destinazione, { replace: true })
        }
    }, [autenticato, destinazione, navigate])

    async function invia(evento: React.FormEvent) {
        /* Senza questo il browser ricaricherebbe la pagina come un form HTML
           qualunque, e l'applicazione ripartirebbe da zero. */
        evento.preventDefault()
        setErrore(null)
        setInCorso(true)
        try {
            await login(username.trim(), password)
            /* Al reindirizzamento pensa l'effetto qui sopra. */
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
                            /* autoComplete e autoFocus non sono decorazione:
                               fanno funzionare il gestore di password del
                               browser e risparmiano un clic a ogni accesso. */
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

                    {/* disabilitato mentre la richiesta e' in volo: due clic
                        rapidi manderebbero due login. */}
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
