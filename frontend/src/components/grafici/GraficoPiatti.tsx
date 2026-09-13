import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { PiattoVenduto } from '../../types'
import { euro, intero } from '../../utils/formato'
import TabellaDati from '../TabellaDati'
import { tooltipDa } from './Tooltip'
import {
    ASSE, COLORE_DATI, COLORE_GRIGLIA,
    CURSORE, DESTRA_ARROTONDATA, SPESSORE_BARRA,
} from './comune'

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
                        <CartesianGrid horizontal={false} stroke={COLORE_GRIGLIA} />
                        <XAxis type="number" allowDecimals={false} {...ASSE} />
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
