import { Link, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useRisorsa } from '../hooks/useRisorsa'
import * as statistiche from '../services/statisticheService'

export default function Layout() {
    const { utente, autenticato, ristoratore, logout } = useAuth()

    const contesto = useRisorsa(
        () => (ristoratore ? statistiche.contesto() : Promise.resolve(null)),
        [ristoratore],
    )
    const idLocale = contesto.dati?.ristoranteId ?? null

    return (
        <>
            <header className="intestazione">
                <div className="barra">
                    <a className="marchio" href={ristoratore ? '/mio-ristorante' : '/'}>Ristorante</a>

                    <nav className="navigazione">
                        {ristoratore && (
                            <>
                                {idLocale !== null && (
                                    <>
                                        <a href={`/ristoranti/${idLocale}/ordinazioni`}>Conti aperti</a>
                                        <a href={`/ristoranti/${idLocale}/scontrini`}>Scontrini</a>
                                        <a href={`/ristoranti/${idLocale}/agenda`}>Agenda</a>
                                    </>
                                )}
                                <Link to="/">Statistiche</Link>
                                <a href="/ristoranti">Tutti i ristoranti</a>
                            </>
                        )}

                        {!ristoratore && <a href="/">Torna al sito</a>}

                        {autenticato && (
                            <span className="utente">
                                <span>{utente?.username}</span>
                                <button type="button" className="bottone bottone-tenue" onClick={logout}>
                                    Esci
                                </button>
                            </span>
                        )}
                    </nav>
                </div>
            </header>

            <main className="contenuto">
                <Outlet />
            </main>

            <footer className="chiusura">
                <div>Progetto SIW · Universita degli Studi Roma Tre</div>
            </footer>
        </>
    )
}
