import { api, DATI_FINTI, messaggioErrore } from './api'
import {
    affluenzaPerOraFinta, CONTESTO_FINTO, incassoPerGiornoFinto, piattiFinti,
    riepilogoFinto, ritardo, scontriniPerMeseFinti, VOTI_FINTI,
} from './datiFinti'
import type {
    AffluenzaOra, Contesto, IncassoGiorno, PiattoVenduto,
    Riepilogo, ScontriniMese, VotiRecensioni,
} from '../types'

/* Una funzione per endpoint, e nient'altro: nessun calcolo, nessuna
   trasformazione. I numeri arrivano gia' pronti dal server - i mesi vuoti, i
   giorni a zero e le cinque righe dei voti li riempie StatisticheService in
   Java. E' una scelta, non una comodita': se li completasse il frontend, la
   stessa regola andrebbe riscritta in ogni pagina che li usa.

   Nessun endpoint qui riceve l'id del ristorante: il locale e' quello del
   gestore che ha fatto il login, ricavato dal token lato server. Non esiste un
   numero da cambiare nell'indirizzo per leggere la cassa di qualcun altro. */

async function leggi<T>(percorso: string, finto: T, ripiego: string): Promise<T> {
    if (DATI_FINTI) {
        return ritardo(finto)
    }
    try {
        const risposta = await api.get<T>(percorso)
        return risposta.data
    } catch (errore) {
        throw new Error(messaggioErrore(errore, ripiego))
    }
}

export function contesto(): Promise<Contesto> {
    return leggi('/statistiche/contesto', CONTESTO_FINTO,
        'Non e\' stato possibile leggere i dati del ristorante.')
}

export function riepilogo(anno: number): Promise<Riepilogo> {
    return leggi(`/statistiche/riepilogo?anno=${anno}`, riepilogoFinto(anno),
        'Non e\' stato possibile leggere il riepilogo.')
}

export function scontriniPerMese(anno: number): Promise<ScontriniMese[]> {
    return leggi(`/statistiche/scontrini-per-mese?anno=${anno}`, scontriniPerMeseFinti(anno),
        'Non e\' stato possibile leggere gli scontrini per mese.')
}

export function piattiPiuOrdinati(anno: number, limite = 8): Promise<PiattoVenduto[]> {
    return leggi(`/statistiche/piatti-piu-ordinati?anno=${anno}&limite=${limite}`,
        piattiFinti(anno, limite),
        'Non e\' stato possibile leggere i piatti piu\' ordinati.')
}

export function incassoPerGiorno(anno: number): Promise<IncassoGiorno[]> {
    return leggi(`/statistiche/incasso-per-giorno?anno=${anno}`, incassoPerGiornoFinto(anno),
        'Non e\' stato possibile leggere l\'incasso per giorno.')
}

export function affluenzaPerOra(anno: number): Promise<AffluenzaOra[]> {
    return leggi(`/statistiche/affluenza-per-ora?anno=${anno}`, affluenzaPerOraFinta(anno),
        'Non e\' stato possibile leggere l\'affluenza per ora.')
}

export function votiRecensioni(): Promise<VotiRecensioni> {
    return leggi('/statistiche/voti-recensioni', VOTI_FINTI,
        'Non e\' stato possibile leggere i voti delle recensioni.')
}
