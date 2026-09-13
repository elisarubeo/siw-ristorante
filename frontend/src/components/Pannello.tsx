import type { ReactNode } from 'react'

interface Props {
    titolo: string
    descrizione?: string
    inCaricamento: boolean
    errore: string | null
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
