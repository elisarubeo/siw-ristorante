import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { IncassoGiorno } from '../../types'
import { euro, euroCorto, GIORNI, GIORNI_CORTI, intero } from '../../utils/formato'
import TabellaDati from '../TabellaDati'
import { tooltipDa } from './Tooltip'
import {
    ASSE, CIMA_ARROTONDATA, COLORE_DATI, COLORE_GRIGLIA,
    CURSORE, MARGINE, SPESSORE_BARRA,
} from './comune'

/* In che giorni si lavora. L'incasso e non il numero di conti, perche' la
   domanda vera ("quando conviene tenere aperto, dove mettere il personale")
   si risponde con i soldi; il numero di conti resta nel tooltip.

   L'asse parte da lunedi come DayOfWeek in Java, non da domenica: e' anche il
   modo in cui si legge un calendario italiano. */

const TooltipGiorno = tooltipDa<IncassoGiorno>((riga) => ({
    titolo: GIORNI[riga.giorno - 1],
    voci: [
        { etichetta: 'Incasso', valore: euro(riga.incasso) },
        { etichetta: 'Conti chiusi', valore: intero(riga.numeroScontrini) },
    ],
}))

export default function GraficoGiorni({ dati }: { dati: IncassoGiorno[] }) {
    return (
        <>
            <div className="area-grafico">
                <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={dati} margin={MARGINE}>
                        <CartesianGrid vertical={false} stroke={COLORE_GRIGLIA} />
                        <XAxis dataKey="giorno" tickFormatter={(g: number) => GIORNI_CORTI[g - 1]} {...ASSE} />
                        <YAxis width={62} tickFormatter={euroCorto} {...ASSE} />
                        <Tooltip content={<TooltipGiorno />} cursor={CURSORE} />
                        <Bar dataKey="incasso" fill={COLORE_DATI}
                             radius={CIMA_ARROTONDATA} maxBarSize={SPESSORE_BARRA} />
                    </BarChart>
                </ResponsiveContainer>
            </div>

            <TabellaDati
                colonne={['Giorno', 'Incasso', 'Conti chiusi']}
                righe={dati.map((riga) => ({
                    chiave: riga.giorno,
                    celle: [GIORNI[riga.giorno - 1], euro(riga.incasso), intero(riga.numeroScontrini)],
                }))}
            />
        </>
    )
}
