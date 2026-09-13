import { Bar, BarChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { VotiRecensioni } from '../../types'
import { decimale, intero } from '../../utils/formato'
import TabellaDati from '../TabellaDati'
import { tooltipDa } from './Tooltip'
import {
    ASSE, COLORE_DATI, COLORE_GRIGLIA,
    CURSORE, DESTRA_ARROTONDATA, SPESSORE_BARRA,
} from './comune'

const TooltipVoto = tooltipDa<{ voto: number; quantita: number }>((riga) => ({
    titolo: `${riga.voto} ${riga.voto === 1 ? 'stella' : 'stelle'}`,
    voci: [{ etichetta: 'Recensioni', valore: intero(riga.quantita) }],
}))

export default function GraficoVoti({ dati }: { dati: VotiRecensioni }) {
    const righe = [...dati.distribuzione].sort((a, b) => b.voto - a.voto)

    return (
        <>
            <p className="descrizione" style={{ marginBottom: '.6rem' }}>
                Media <strong>{decimale(dati.mediaVoti)}</strong> su 5
                {' · '}
                {intero(dati.totale)} {dati.totale === 1 ? 'recensione' : 'recensioni'}
            </p>

            <div className="area-grafico">
                <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={righe} layout="vertical" margin={{ top: 4, right: 12, bottom: 0, left: 0 }}>
                        <CartesianGrid horizontal={false} stroke={COLORE_GRIGLIA} />
                        <XAxis type="number" allowDecimals={false} {...ASSE} />
                        <YAxis type="category" dataKey="voto" width={78}
                               tickFormatter={(voto: number) => '★'.repeat(voto)} {...ASSE} />
                        <Tooltip content={<TooltipVoto />} cursor={CURSORE} />
                        <Bar dataKey="quantita" fill={COLORE_DATI}
                             radius={DESTRA_ARROTONDATA} maxBarSize={SPESSORE_BARRA} />
                    </BarChart>
                </ResponsiveContainer>
            </div>

            <TabellaDati
                colonne={['Voto', 'Recensioni']}
                righe={righe.map((riga) => ({
                    chiave: riga.voto,
                    celle: [`${riga.voto} su 5`, intero(riga.quantita)],
                }))}
            />
        </>
    )
}
