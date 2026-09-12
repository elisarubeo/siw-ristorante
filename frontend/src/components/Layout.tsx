import { Link, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

/* La cornice comune a tutte le pagine. E' una "layout route": non compare
   nell'indirizzo, ma avvolge le rotte annidate, che React Router disegna
   dentro <Outlet />. Cosi' barra e piede si scrivono una volta sola e non
   si ridisegnano passando da una pagina all'altra. */

export default function Layout() {
    const { utente, autenticato, logout } = useAuth()

    return (
        <>
            <header className="intestazione">
                <div className="barra">
                    {/* <Link> e non <a>: siamo dentro l'applicazione, e il
                        cambio di pagina non deve ricaricare niente. */}
                    <Link className="marchio" to="/">Statistiche</Link>

                    <nav className="navigazione">
                        {/* Questo invece e' un <a> vero, e deve esserlo: porta
                            FUORI dalla SPA, al sito in Thymeleaf. Un <Link>
                            cercherebbe una rotta React che non esiste e
                            finirebbe sulla pagina "non trovata". */}
                        <a href="/mio-ristorante">Torna al sito</a>

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
