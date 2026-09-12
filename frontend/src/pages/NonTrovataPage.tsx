import { Link } from 'react-router-dom'

export default function NonTrovataPage() {
    return (
        <div className="errore">
            <p className="codice">404</p>
            <h1>Pagina non trovata</h1>
            <p className="sottotitolo" style={{ margin: '0 auto 1.5rem' }}>
                Questo indirizzo non esiste dentro le statistiche.
            </p>
            <Link className="bottone" to="/">Torna alle statistiche</Link>
        </div>
    )
}
