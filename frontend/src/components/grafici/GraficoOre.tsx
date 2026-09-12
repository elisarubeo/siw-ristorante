import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import type { AffluenzaOra } from '../../types'
import { intero } from '../../utils/formato'
import TabellaDati from '../TabellaDati'
import { tooltipDa } from './Tooltip'
import { ASSE, COLORE_DATI, COLORE_GRIGLIA, COLORE_SUPERFICIE, MARGINE } from './comune'

/* A che ora si riempie la sala.

   Un'area e non delle barre: le ore sono una scala continua, e la forma
   d'insieme (due gobbe, pranzo e cena) e' proprio quello che si vuole
   vedere - ventiquattro barre la spezzetterebbero in ventiquattro fatti
   separati. Il riempimento resta un velo al 12%: serve a dare corpo alla
   curva, non a diventare un blocco di colore.

   Si mostrano tutte e ventiquattro le ore, comprese quelle a zero: e' l'unico
   modo per vedere quanto sono nette le due gobbe. Il taglio delle ore vuote
   farebbe sembrare il locale aperto ininterrottamente. */

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
                        {/* una tacca ogni tre ore: ventiquattro etichette si
                            sovrapporrebbero e non si leggerebbe piu' niente */}
                        <XAxis dataKey="ora" interval={2}
                               tickFormatter={(ora: number) => `${ora}`} {...ASSE} />
                        <YAxis allowDecimals={false} width={40} {...ASSE} />
                        {/* la linea verticale che segue il puntatore: su una
                            curva dice quale ora si sta leggendo meglio di
                            qualunque evidenziazione dell'area */}
                        <Tooltip content={<TooltipOra />}
                                 cursor={{ stroke: COLORE_DATI, strokeWidth: 1 }} />
                        <Area type="monotone" dataKey="numeroScontrini"
                              stroke={COLORE_DATI} strokeWidth={2}
                              fill={COLORE_DATI} fillOpacity={0.12}
                              /* il pallino sotto il puntatore ha un anello del
                                 colore della carta: resta leggibile anche dove
                                 la curva e' fitta */
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
