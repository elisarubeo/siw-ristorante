import type { ReactNode } from 'react'

/* Il contenitore di un grafico, con i suoi tre stati.

   Sta in un componente solo perche' "sto caricando", "e' andata storta" e
   "non c'e' niente da mostrare" capitano a ogni pannello, e scritti dentro
   ognuno diventerebbero cinque varianti leggermente diverse della stessa cosa.

   E' presentazionale: riceve tutto dalle props e non chiama l'API. Chi lo usa
   tiene lo stato e glielo passa. */

interface Props {
    titolo: string
    descrizione?: string
    inCaricamento: boolean
    errore: string | null
    /* Il pannello sa dire "non ci sono dati" solo se glielo si dice: un array
       vuoto e un anno senza incassi si distinguono guardando i dati, cosa che
       tocca a chi li ha. */
    vuoto?: boolean
    messaggioVuoto?: string
    altezzaAlta?: boolean
    larghezzaPiena?: boolean
    children: ReactNode
}

export default function Pannello({
    titolo, descrizione, inCaricamento, errore,
    vuoto = false, messaggioVuoto = 'Nessun dato per questo periodo.',
    altezzaAlta = false, larghezzaPiena = false, children,
}: Props) {
    const classeArea = `area-grafico${altezzaAlta ? ' alta' : ''}`

    return (
        <section className={`pannello${larghezzaPiena ? ' larghezza-piena' : ''}`}>
            <h2>{titolo}</h2>
            {descrizione && <p className="descrizione">{descrizione}</p>}

            {/* Gli stati occupano la stessa altezza del grafico: cosi' quando i
                dati arrivano la pagina non sobbalza sotto il puntatore. */}
            {inCaricamento ? (
                <div className={classeArea}>
                    <div className="scheletro" role="status" aria-label={`${titolo}: caricamento in corso`} />
                </div>
            ) : errore ? (
                <div className={`${classeArea} stato-pannello`}>
                    <p className="avviso avviso-errore" style={{ margin: 0 }}>{errore}</p>
                </div>
            ) : vuoto ? (
                <div className={`${classeArea} stato-pannello`}>{messaggioVuoto}</div>
            ) : (
                children
            )}
        </section>
    )
}
