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

/* La pagina delle statistiche.

   Tiene lo stato (quale anno si sta guardando, che cosa e' arrivato
   dall'API) e lo passa in giu' ai componenti, che sono presentazionali:
   ricevono dati e li disegnano, senza sapere che esista un server. E' lo
   stesso principio per cui in Java i controller non fanno query.

   Nessuna chiamata qui porta l'id del ristorante: e' quello del gestore che ha
   fatto il login, deciso dal server a partire dal token. */

export default function StatistichePage() {
    const { utente, ristoratore } = useAuth()

    /* Il contesto si chiede una volta sola, al primo disegno: il ristorante e
       gli anni disponibili non cambiano cambiando anno. */
    const contesto = useRisorsa(() => statistiche.contesto(), [])

    /* L'anno scelto DALL'UTENTE. Resta null finche' non tocca il menu, e va
       tenuto distinto dall'anno mostrato: se fosse uno stato solo,
       preselezionare l'anno piu' recente all'arrivo del contesto richiederebbe
       un effetto che scrive nello stato, cioe' un secondo disegno per una cosa
       che si sa gia' calcolare. */
    const [annoScelto, setAnnoScelto] = useState<number | null>(null)

    /* Chi non gestisce un locale non ha statistiche da vedere. Il server
       risponderebbe comunque 403: questo evita di mostrargli una pagina di
       errori al posto di una spiegazione. */
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

    /* L'anno mostrato: quello scelto, o il piu' recente fra quelli che hanno
       dati. Calcolato durante il disegno, non conservato: un valore che si
       ricava da altri due non e' uno stato. */
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
                    {/* I filtri su una riga sola, sopra tutto il resto: si vede
                        a colpo d'occhio a che cosa si riferiscono i numeri. */}
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

                    {/* Montato solo quando l'anno e' noto: cosi' dentro non
                        serve nessuna guardia sul null. */}
                    {anno !== null && <Cruscotto anno={anno} />}
                </>
            )}
        </>
    )
}

/* Tutto quello che dipende dall'anno scelto. E' un componente a parte perche'
   cambiando anno deve ricaricarsi lui, non la pagina intera: il ristorante e
   l'elenco degli anni restano dove sono. */
function Cruscotto({ anno }: { anno: number }) {
    /* Sei richieste indipendenti, ognuna con il suo stato. Partono insieme,
       non una dopo l'altra, e ogni pannello si riempie appena ha i suoi dati:
       se una fallisce, gli altri cinque restano leggibili invece di sparire
       tutti dietro lo stesso messaggio d'errore. */
    const riepilogo = useRisorsa(() => statistiche.riepilogo(anno), [anno])
    const mesi = useRisorsa(() => statistiche.scontriniPerMese(anno), [anno])
    const piatti = useRisorsa(() => statistiche.piattiPiuOrdinati(anno), [anno])
    const giorni = useRisorsa(() => statistiche.incassoPerGiorno(anno), [anno])
    const ore = useRisorsa(() => statistiche.affluenzaPerOra(anno), [anno])
    /* I voti non dipendono dall'anno: sono il giudizio del locale oggi.
       Dipendenze vuote, quindi una richiesta sola per tutta la visita. */
    const voti = useRisorsa(() => statistiche.votiRecensioni(), [])

    const r = riepilogo.dati

    return (
        <>
            {/* Gli indicatori: un valore solo ciascuno, e per un valore solo un
                grafico non serve. */}
            {riepilogo.errore ? (
                <p className="avviso avviso-errore">{riepilogo.errore}</p>
            ) : (
                <div className="indicatori">
                    <Indicatore
                        primario
                        etichetta="Incasso"
                        /* Senza centesimi: su una cifra annuale i decimali
                           sono rumore, e in una scheda stretta rubano lo
                           spazio alle migliaia. L'importo esatto sta nella
                           tabella del grafico mensile. */
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

/* Il confronto con l'anno prima. Il colore non basta mai da solo: chi non
   distingue il verde dal rosso legge comunque il segno davanti al numero. */
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
