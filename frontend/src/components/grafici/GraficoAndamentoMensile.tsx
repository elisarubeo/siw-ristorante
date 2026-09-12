import {
    Bar, BarChart, CartesianGrid, LabelList, ResponsiveContainer, Tooltip, XAxis, YAxis,
} from 'recharts'
import type { ScontriniMese } from '../../types'
import { euro, euroCorto, intero, MESI, MESI_CORTI } from '../../utils/formato'
import TabellaDati from '../TabellaDati'
import { tooltipDa } from './Tooltip'
import {
    ASSE, CIMA_ARROTONDATA, COLORE_DATI, COLORE_GRIGLIA,
    CURSORE, MARGINE, SPESSORE_BARRA,
} from './comune'

/* Il grafico da cui e' nata la pagina: in quale periodo dell'anno viene piu'
   gente, e quanto si incassa.

   DUE GRAFICI E NON UNO. La tentazione e' mettere le barre dei conti e la
   linea dell'incasso nello stesso riquadro, con due assi verticali. E' il
   modo piu' rapido per mentire: con due scale indipendenti la posizione
   relativa delle due serie dipende da dove si sceglie di far partire i due
   assi, e "la linea sta sopra le barre" non significa niente. Due grafici
   impilati che condividono i mesi si confrontano leggendo in verticale, e
   ognuno ha la sua scala onesta a partire da zero. */

const TooltipConti = tooltipDa<ScontriniMese>((riga) => ({
    titolo: MESI[riga.mese - 1],
    voci: [
        { etichetta: 'Conti chiusi', valore: intero(riga.numeroScontrini) },
        { etichetta: 'Incasso', valore: euro(riga.incasso) },
    ],
}))

const TooltipIncasso = tooltipDa<ScontriniMese>((riga) => ({
    titolo: MESI[riga.mese - 1],
    voci: [
        { etichetta: 'Incasso', valore: euro(riga.incasso) },
        { etichetta: 'Conti chiusi', valore: intero(riga.numeroScontrini) },
    ],
}))

/* L'etichetta diretta sul mese migliore, e su quello soltanto.
   Un numero sopra ogni barra sarebbe un muro di cifre che nessuno legge; uno
   solo risponde alla domanda della pagina senza aggiungere rumore. */
function etichettaMassimo(indiceMassimo: number, formatta: (valore: number) => string) {
    /* I tipi che Recharts passa a `content` sono piu' larghi di quello che
       serve qui (un valore puo' essere anche null o un array): si accetta
       `unknown` e si converte, invece di dichiarare tipi piu' stretti di
       quelli veri - TypeScript non lo permetterebbe. */
    return function Etichetta(props: {
        x?: unknown; y?: unknown; width?: unknown; value?: unknown; index?: unknown
    }) {
        if (Number(props.index) !== indiceMassimo) return null
        const x = Number(props.x) + Number(props.width) / 2
        return (
            <text
                x={x} y={Number(props.y) - 7}
                textAnchor="middle"
                /* il testo non porta mai il colore dei dati: e' inchiostro,
                   il colore identifica le barre */
                fill="#2b2521" fontSize={12} fontWeight={600}
            >
                {formatta(Number(props.value))}
            </text>
        )
    }
}

export default function GraficoAndamentoMensile({ dati }: { dati: ScontriniMese[] }) {
    const indiceMassimoConti = dati.reduce(
        (migliore, riga, indice) => (riga.numeroScontrini > dati[migliore].numeroScontrini ? indice : migliore), 0)
    const indiceMassimoIncasso = dati.reduce(
        (migliore, riga, indice) => (riga.incasso > dati[migliore].incasso ? indice : migliore), 0)

    return (
        <>
            <p className="descrizione" style={{ marginBottom: '.2rem' }}>Conti chiusi</p>
            <div className="area-grafico">
                <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={dati} margin={MARGINE}>
                        {/* solo le orizzontali: le verticali dietro le barre
                            sono inchiostro che non dice niente */}
                        <CartesianGrid vertical={false} stroke={COLORE_GRIGLIA} />
                        <XAxis dataKey="mese" tickFormatter={(mese: number) => MESI_CORTI[mese - 1]} {...ASSE} />
                        <YAxis allowDecimals={false} width={40} {...ASSE} />
                        <Tooltip content={<TooltipConti />} cursor={CURSORE} />
                        <Bar dataKey="numeroScontrini" fill={COLORE_DATI}
                             radius={CIMA_ARROTONDATA} maxBarSize={SPESSORE_BARRA}>
                            <LabelList dataKey="numeroScontrini"
                                       content={etichettaMassimo(indiceMassimoConti, intero)} />
                        </Bar>
                    </BarChart>
                </ResponsiveContainer>
            </div>

            <p className="descrizione" style={{ margin: '1.2rem 0 .2rem' }}>Incasso</p>
            <div className="area-grafico">
                <ResponsiveContainer width="100%" height="100%">
                    <BarChart data={dati} margin={MARGINE}>
                        <CartesianGrid vertical={false} stroke={COLORE_GRIGLIA} />
                        <XAxis dataKey="mese" tickFormatter={(mese: number) => MESI_CORTI[mese - 1]} {...ASSE} />
                        <YAxis width={62} tickFormatter={euroCorto} {...ASSE} />
                        <Tooltip content={<TooltipIncasso />} cursor={CURSORE} />
                        <Bar dataKey="incasso" fill={COLORE_DATI}
                             radius={CIMA_ARROTONDATA} maxBarSize={SPESSORE_BARRA}>
                            <LabelList dataKey="incasso"
                                       content={etichettaMassimo(indiceMassimoIncasso, euroCorto)} />
                        </Bar>
                    </BarChart>
                </ResponsiveContainer>
            </div>

            <TabellaDati
                colonne={['Mese', 'Conti chiusi', 'Incasso']}
                righe={dati.map((riga) => ({
                    chiave: riga.mese,
                    celle: [MESI[riga.mese - 1], intero(riga.numeroScontrini), euro(riga.incasso)],
                }))}
            />
        </>
    )
}
