import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { PiattoVenduto } from '../../types'
import { euro, intero } from '../../utils/formato'
import TabellaDati from '../TabellaDati'
import { tooltipDa } from './Tooltip'
import {
    ASSE, COLORE_DATI, COLORE_GRIGLIA,
    CURSORE, DESTRA_ARROTONDATA, SPESSORE_BARRA,
} from './comune'

/* I piatti piu' venduti.

   Barre ORIZZONTALI, non verticali: i nomi dei piatti sono lunghi
   ("Saltimbocca alla romana") e su un asse orizzontale finirebbero inclinati
   o troncati. Sulla verticale si leggono per intero, dritti, e l'ordine
   decrescente si segue dall'alto in basso come una classifica. */

const TooltipPiatto = tooltipDa<PiattoVenduto>((riga) => ({
    titolo: riga.nomePiatto,
    voci: [
        { etichetta: 'Porzioni', valore: intero(riga.quantita) },
        { etichetta: 'Incasso', valore: euro(riga.incasso) },
    ],
}))

export default function GraficoPiatti({ dati }: { dati: PiattoVenduto[] }) {
    return (
        <>
            <div className="area-grafico alta">
                <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={dati} layout="vertical" margin={{ top: 4, right: 12, bottom: 0, left: 0 }}>
                        {/* girato il grafico, e' la griglia verticale a
                            servire: accompagna l'occhio lungo le barre */}
                        <CartesianGrid horizontal={false} stroke={COLORE_GRIGLIA} />
                        <XAxis type="number" allowDecimals={false} {...ASSE} />
                        {/* largo abbastanza per "Saltimbocca alla romana": una misura
                            piu' stretta manderebbe il nome su due righe e lo
                            farebbe sbattere contro la riga accanto */}
                        <YAxis type="category" dataKey="nomePiatto" width={192} {...ASSE} />
                        <Tooltip content={<TooltipPiatto />} cursor={CURSORE} />
                        <Bar dataKey="quantita" fill={COLORE_DATI}
                             radius={DESTRA_ARROTONDATA} maxBarSize={SPESSORE_BARRA} />
                    </BarChart>
                </ResponsiveContainer>
            </div>

            <TabellaDati
                colonne={['Piatto', 'Porzioni', 'Incasso']}
                righe={dati.map((riga) => ({
                    chiave: riga.piattoId,
                    celle: [riga.nomePiatto, intero(riga.quantita), euro(riga.incasso)],
                }))}
            />
        </>
    )
}
