import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { AffluenzaOra } from '../../types'
import { intero } from '../../utils/formato'
import TabellaDati from '../TabellaDati'
import { tooltipDa } from './Tooltip'
import { ASSE, COLORE_DATI, COLORE_GRIGLIA, COLORE_SUPERFICIE, MARGINE } from './comune'

const TooltipOra = tooltipDa<AffluenzaOra>((riga) => ({
    titolo: `${String(riga.ora).padStart(2, '0')}:00 – ${String(riga.ora).padStart(2, '0')}:59`,
    voci: [{ etichetta: 'Tavoli occupati', valore: intero(riga.numeroScontrini) }],
}))

export default function GraficoOre({ dati }: { dati: AffluenzaOra[] }) {
    return (
        <>
            <div className="area-grafico">
                <ResponsiveContainer width="100%" height="100%">
                    <AreaChart data={dati} margin={MARGINE}>
                        <CartesianGrid vertical={false} stroke={COLORE_GRIGLIA} />
                        <XAxis dataKey="ora" interval={2}
                               tickFormatter={(ora: number) => `${ora}`} {...ASSE} />
                        <YAxis allowDecimals={false} width={40} {...ASSE} />
                        <Tooltip content={<TooltipOra />}
                                 cursor={{ stroke: COLORE_DATI, strokeWidth: 1 }} />
                        <Area type="monotone" dataKey="numeroScontrini"
                              stroke={COLORE_DATI} strokeWidth={2}
                              fill={COLORE_DATI} fillOpacity={0.12}
                              activeDot={{ r: 5, fill: COLORE_DATI, stroke: COLORE_SUPERFICIE, strokeWidth: 2 }} />
                    </AreaChart>
                </ResponsiveContainer>
            </div>

            <TabellaDati
                etichetta="Mostra i numeri (solo le ore con clienti)"
                colonne={['Ora', 'Tavoli occupati']}
                righe={dati
                    .filter((riga) => riga.numeroScontrini > 0)
                    .map((riga) => ({
                        chiave: riga.ora,
                        celle: [`${String(riga.ora).padStart(2, '0')}:00`, intero(riga.numeroScontrini)],
                    }))}
            />
        </>
    )
}
