import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

/* Il cancello davanti alle pagine che vogliono un utente collegato.

   E' anch'essa una layout route: avvolge le rotte da proteggere invece di
   ripetere il controllo in ognuna. Chi non e' collegato non vede la pagina,
   viene mandato al login.

   Va detto che questo NON e' un controllo di sicurezza: e' scritto in
   JavaScript che gira nel browser dell'utente, e chiunque lo puo' aggirare.
   Serve a non mostrare una pagina che resterebbe vuota. A difendere i dati e'
   la catena /api/** lato server, che senza un token valido non risponde -
   qualunque cosa faccia il frontend. */

export default function RottaProtetta() {
    const { autenticato } = useAuth()
    const posizione = useLocation()

    /* Nessuna attesa da gestire: AuthProvider legge localStorage
       nell'inizializzatore dello stato, quindi gia' al primo disegno sa se
       c'e' un utente. Fosse stato un useEffect, qui servirebbe uno stato
       "sto ancora guardando", altrimenti un F5 butterebbe fuori chi era
       entrato. */
    if (!autenticato) {
        /* replace: il login prende il posto della pagina protetta nella
           cronologia, cosi' il tasto "indietro" non ci riporta dentro.
           In state si tiene da dove si veniva, per tornarci dopo. */
        return <Navigate to="/login" replace state={{ da: posizione.pathname }} />
    }

    return <Outlet />
}
