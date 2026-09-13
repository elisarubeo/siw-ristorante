import type { ReactNode } from 'react'

interface Props {
    etichetta: string
    valore: string
    nota?: ReactNode
    primario?: boolean
}

export default function Indicatore({ etichetta, valore, nota, primario = false }: Props) {
    return (
        <div className={`indicatore${primario ? ' primario' : ''}`}>
            <span className="etichetta-indicatore">{etichetta}</span>
            <span className="valore">{valore}</span>
            {nota && <span className="nota">{nota}</span>}
        </div>
    )
}
