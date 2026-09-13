import type { ReactNode } from 'react'

export interface VoceTooltip {
    etichetta: string
    valore: string
}

export function ContenutoTooltip({ titolo, voci }: { titolo: string; voci: VoceTooltip[] }) {
    return (
        <div className="tooltip">
            <div className="tooltip-titolo">{titolo}</div>
            <dl>
                {voci.map((voce) => (
                    <div key={voce.etichetta} style={{ display: 'contents' }}>
                        <dt>{voce.etichetta}</dt>
                        <dd>{voce.valore}</dd>
                    </div>
                ))}
            </dl>
        </div>
    )
}

export interface PropsTooltip<T> {
    active?: boolean
    payload?: Array<{ payload: T }>
}

export function tooltipDa<T>(
    disegna: (riga: T) => { titolo: string; voci: VoceTooltip[] },
) {
    return function Tooltip({ active, payload }: PropsTooltip<T>): ReactNode {
        if (!active || !payload?.length) return null
        const { titolo, voci } = disegna(payload[0].payload)
        return <ContenutoTooltip titolo={titolo} voci={voci} />
    }
}
