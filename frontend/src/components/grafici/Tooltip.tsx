import type { ReactNode } from 'react'

/* Il tooltip dei grafici: uno solo per tutti, perche' cinque tooltip scritti
   a mano diventano cinque tooltip leggermente diversi. Recharts disegna il
   contenuto che gli si passa, e quello che gli si passa e' questo.

   Sta in un file suo e non insieme ai colori in comune.ts: un file che esporta
   sia componenti sia costanti rompe il ricaricamento a caldo di Vite, che per
   decidere cosa aggiornare guarda proprio che cosa un file esporta. */

export interface VoceTooltip {
    etichetta: string
    valore: string
}

/* Il vestito del tooltip, uguale per tutti i grafici. Recharts disegna quello
   che gli si passa: qui si passa questo. */
export function ContenutoTooltip({ titolo, voci }: { titolo: string; voci: VoceTooltip[] }) {
    return (
        <div className="tooltip">
            <div className="tooltip-titolo">{titolo}</div>
            <dl>
                {voci.map((voce) => (
                    /* key: senza, React non sa quale riga e' quale quando la
                       lista cambia, e riusa i nodi sbagliati. */
                    <div key={voce.etichetta} style={{ display: 'contents' }}>
                        <dt>{voce.etichetta}</dt>
                        <dd>{voce.valore}</dd>
                    </div>
                ))}
            </dl>
        </div>
    )
}

/* La forma con cui Recharts chiama il contenuto del tooltip. La si dichiara a
   mano perche' i tipi della libreria qui sono generici al punto da non
   aiutare. */
export interface PropsTooltip<T> {
    active?: boolean
    payload?: Array<{ payload: T }>
}

/* Costruisce un tooltip a partire da una funzione che sa leggere UNA riga di
   dati. Cosi' ogni grafico scrive solo cio' che ha di suo. */
export function tooltipDa<T>(
    disegna: (riga: T) => { titolo: string; voci: VoceTooltip[] },
) {
    return function Tooltip({ active, payload }: PropsTooltip<T>): ReactNode {
        if (!active || !payload?.length) return null
        const { titolo, voci } = disegna(payload[0].payload)
        return <ContenutoTooltip titolo={titolo} voci={voci} />
    }
}

