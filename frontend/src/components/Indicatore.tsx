import type { ReactNode } from 'react'

/* Un numero con la sua etichetta. Per un valore solo un grafico non serve:
   sarebbe una barra sola, cioe' un numero scritto in modo piu' lento da
   leggere. */

interface Props {
    etichetta: string
    valore: string
    nota?: ReactNode
    /* Il numero principale della pagina, disegnato piu' grande. Uno solo:
       se tutto e' importante, niente lo e'. */
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
