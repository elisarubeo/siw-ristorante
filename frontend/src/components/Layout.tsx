import { Link, Outlet } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { useRisorsa } from '../hooks/useRisorsa'
import * as statistiche from '../services/statisticheService'

/* La cornice comune a tutte le pagine. E' una "layout route": non compare
   nell'indirizzo, ma avvolge le rotte annidate, che React Router disegna
   dentro <Outlet />. Cosi' barra e piede si scrivono una volta sola e non
   si ridisegnano passando da una pagina all'altra.

   La barra e' la STESSA del sito in Thymeleaf, voce per voce: per chi gestisce
   un locale le statistiche non sono un'applicazione a parte in cui si entra e
   da cui si esce, sono una pagina come le altre, e cambiare barra a meta'
   strada lo farebbe sembrare un altro sito. Qui pero' i collegamenti sono <a>
   e non <Link>: portano FUORI dalla SPA, e un <Link> cercherebbe una rotta
   React che non esiste finendo sulla pagina "non trovata". */

export default function Layout() {
    const { utente, autenticato, ristoratore, logout } = useAuth()

    /* L'id del locale serve ai collegamenti della barra (conti, scontrini,
       agenda), e nella SPA nessun indirizzo lo porta con se': lo dice il
       contesto, che il server ricava dal token. La richiesta e' la stessa che
       fa la pagina delle statistiche - il service la tiene da parte, quindi in
       rete ne parte una sola.
       Chi non gestisce un locale non la fa affatto: risponderebbe 403. */
    const contesto = useRisorsa(
        () => (ristoratore ? statistiche.contesto() : Promise.resolve(null)),
        [ristoratore],
    )
    const idLocale = contesto.dati?.ristoranteId ?? null

    return (
        <>
            <header className="intestazione">
                <div className="barra">
                    {/* Per il ristoratore la casa e' il suo locale: il marchio
                        lo riporta li', esattamente come nel sito. */}
                    <a className="marchio" href={ristoratore ? '/mio-ristorante' : '/'}>Ristorante</a>

                    <nav className="navigazione">
                        {ristoratore && (
                            <>
                                {/* Le voci legate al locale compaiono solo
                                    quando l'id e' arrivato: per il mezzo
                                    secondo della richiesta la barra e' piu'
                                    corta, ma non porta a un 403. */}
                                {idLocale !== null && (
                                    <>
                                        <a href={`/ristoranti/${idLocale}/ordinazioni`}>Conti aperti</a>
                                        <a href={`/ristoranti/${idLocale}/scontrini`}>Scontrini</a>
                                        <a href={`/ristoranti/${idLocale}/agenda`}>Agenda</a>
                                    </>
                                )}
                                {/* Questa invece e' la pagina in cui siamo gia',
                                    ed e' l'unica voce che resta un <Link>:
                                    ricaricare tutto per tornare dove si e' non
                                    avrebbe senso. */}
                                <Link to="/">Statistiche</Link>
                                <a href="/ristoranti">Tutti i ristoranti</a>
                            </>
                        )}

                        {/* Chi non e' entrato, o e' entrato con un altro ruolo,
                            ha almeno la via del ritorno. */}
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
