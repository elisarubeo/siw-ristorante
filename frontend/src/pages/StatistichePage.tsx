import { useState } from 'react'
import { useAuth } from '../context/AuthContext'
import { useRisorsa } from '../hooks/useRisorsa'
import * as statistiche from '../services/statisticheService'
import Indicatore from '../components/Indicatore'
import Pannello from '../components/Pannello'
import GraficoAndamentoMensile from '../components/grafici/GraficoAndamentoMensile'
import GraficoGiorni from '../components/grafici/GraficoGiorni'
import GraficoOre from '../components/grafici/GraficoOre'
import GraficoPiatti from '../components/grafici/GraficoPiatti'
import GraficoVoti from '../components/grafici/GraficoVoti'
import { decimale, euro, euroCorto, intero, minuti, percentuale } from '../utils/formato'

export default function StatistichePage() {
    const { utente, ristoratore } = useAuth()

    const contesto = useRisorsa(() => statistiche.contesto(), [])

    const [annoScelto, setAnnoScelto] = useState<number | null>(null)

    if (!ristoratore) {
        return (
            <>
                <h1>Statistiche</h1>
                <div className="vuoto">
                    L'account <strong>{utente?.username}</strong> non gestisce un ristorante,
                    e le statistiche sono i dati di cassa di un locale.
                    <p style={{ marginBottom: 0 }}>
                        <a href="/">Torna al sito</a>
                    </p>
                </div>
            </>
        )
    }

    if (contesto.errore) {
        return (
            <>
                <h1>Statistiche</h1>
                <p className="avviso avviso-errore">{contesto.errore}</p>
            </>
        )
    }

    if (contesto.inCaricamento || !contesto.dati) {
        return (
            <>
                <h1>Statistiche</h1>
                <div className="vuoto">Un momento…</div>
            </>
        )
    }

    const { nome, indirizzo, anniDisponibili } = contesto.dati

    const anno = annoScelto ?? anniDisponibili[0] ?? null

    return (
        <>
            <h1>Statistiche</h1>
            <p className="sottotitolo">
                <strong>{nome}</strong> · {indirizzo}
            </p>

            {anniDisponibili.length === 0 ? (
                <div className="vuoto">
                    Questo ristorante non ha ancora chiuso nessun conto: non c'è niente da
                    riassumere. Le statistiche compariranno dopo il primo scontrino.
                </div>
            ) : (
                <>
                    <div className="filtri">
                        <label htmlFor="anno">Anno</label>
                        <select
                            id="anno"
                            value={anno ?? ''}
                            onChange={(e) => setAnnoScelto(Number(e.target.value))}
                        >
                            {anniDisponibili.map((a) => (
                                <option key={a} value={a}>{a}</option>
                            ))}
                        </select>
                    </div>

                    {anno !== null && <Cruscotto anno={anno} />}
                </>
            )}
        </>
    )
}

function Cruscotto({ anno }: { anno: number }) {
    const riepilogo = useRisorsa(() => statistiche.riepilogo(anno), [anno])
    const mesi = useRisorsa(() => statistiche.scontriniPerMese(anno), [anno])
    const piatti = useRisorsa(() => statistiche.piattiPiuOrdinati(anno), [anno])
    const giorni = useRisorsa(() => statistiche.incassoPerGiorno(anno), [anno])
    const ore = useRisorsa(() => statistiche.affluenzaPerOra(anno), [anno])
    const voti = useRisorsa(() => statistiche.votiRecensioni(), [])

    const r = riepilogo.dati

    return (
        <>
            {riepilogo.errore ? (
                <p className="avviso avviso-errore">{riepilogo.errore}</p>
            ) : (
                <div className="indicatori">
                    <Indicatore
                        primario
                        etichetta="Incasso"
                        valore={riepilogo.inCaricamento ? '…' : euroCorto(r?.incassoTotale ?? 0)}
                        nota={<Variazione valore={r?.variazionePercentuale ?? null} anno={anno} />}
                    />
                    <Indicatore
                        etichetta="Conti chiusi"
                        valore={riepilogo.inCaricamento ? '…' : intero(r?.numeroScontrini)}
                    />
                    <Indicatore
                        etichetta="Scontrino medio"
                        valore={riepilogo.inCaricamento ? '…' : euro(r?.scontrinoMedio)}
                    />
                    <Indicatore
                        etichetta="Piatti serviti"
                        valore={riepilogo.inCaricamento ? '…' : intero(r?.pietanzeVendute)}
                    />
                    <Indicatore
                        etichetta="Durata media"
                        valore={riepilogo.inCaricamento ? '…' : minuti(r?.durataMediaMinuti)}
                        nota="a tavola"
                    />
                    <Indicatore
                        etichetta="Giudizio"
                        valore={riepilogo.inCaricamento ? '…' : decimale(r?.mediaVoti)}
                        nota={r ? `su ${intero(r.numeroRecensioni)} recensioni` : undefined}
                    />
                </div>
            )}

            <div className="pannelli">
                <Pannello
                    larghezzaPiena
                    titolo={`Mese per mese, ${anno}`}
                    descrizione="Quando viene più gente e quando si incassa di più. Due grafici e non uno: sovrapporre conti ed euro su due scale diverse farebbe sembrare confrontabili due misure che non lo sono."
                    inCaricamento={mesi.inCaricamento}
                    errore={mesi.errore}
                    vuoto={mesi.dati?.every((m) => m.numeroScontrini === 0)}
                    messaggioVuoto="Nessun conto chiuso in questo anno."
                >
                    {mesi.dati && <GraficoAndamentoMensile dati={mesi.dati} />}
                </Pannello>

                <Pannello
                    titolo="I piatti più ordinati"
                    descrizione="Porzioni vendute, dal più richiesto."
                    altezzaAlta
                    inCaricamento={piatti.inCaricamento}
                    errore={piatti.errore}
                    vuoto={piatti.dati?.length === 0}
                >
                    {piatti.dati && <GraficoPiatti dati={piatti.dati} />}
                </Pannello>

                <Pannello
                    titolo="I giorni della settimana"
                    descrizione="Quanto entra in cassa in ogni giorno, sommando tutto l'anno."
                    inCaricamento={giorni.inCaricamento}
                    errore={giorni.errore}
                    vuoto={giorni.dati?.every((g) => g.numeroScontrini === 0)}
                >
                    {giorni.dati && <GraficoGiorni dati={giorni.dati} />}
                </Pannello>

                <Pannello
                    titolo="A che ora si riempie la sala"
                    descrizione="L'ora in cui i clienti si siedono, non quella in cui pagano."
                    inCaricamento={ore.inCaricamento}
                    errore={ore.errore}
                    vuoto={ore.dati?.every((o) => o.numeroScontrini === 0)}
                >
                    {ore.dati && <GraficoOre dati={ore.dati} />}
                </Pannello>

                <Pannello
                    titolo="Come ci giudicano"
                    descrizione="Le recensioni dei clienti. Non dipendono dall'anno scelto: sono il giudizio del locale oggi."
                    inCaricamento={voti.inCaricamento}
                    errore={voti.errore}
                    vuoto={voti.dati?.totale === 0}
                    messaggioVuoto="Nessuno ha ancora recensito il locale."
                >
                    {voti.dati && <GraficoVoti dati={voti.dati} />}
                </Pannello>
            </div>
        </>
    )
}

function Variazione({ valore, anno }: { valore: number | null; anno: number }) {
    if (valore === null) {
        return <>nessun confronto con il {anno - 1}</>
    }
    const classe = valore >= 0 ? 'variazione-su' : 'variazione-giu'
    return (
        <>
            <span className={classe}>{percentuale(valore)}</span> sul {anno - 1}
        </>
    )
}
