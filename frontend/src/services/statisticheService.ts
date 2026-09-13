import { api, DATI_FINTI, messaggioErrore } from './api'
import {
    affluenzaPerOraFinta, CONTESTO_FINTO, incassoPerGiornoFinto, piattiFinti,
    riepilogoFinto, ritardo, scontriniPerMeseFinti, VOTI_FINTI,
} from './datiFinti'
import type {
    AffluenzaOra, Contesto, IncassoGiorno, PiattoVenduto,
    Riepilogo, ScontriniMese, VotiRecensioni,
} from '../types'

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

let contestoInVolo: Promise<Contesto> | null = null

export function contesto(): Promise<Contesto> {
    if (contestoInVolo === null) {
        contestoInVolo = leggi('/statistiche/contesto', CONTESTO_FINTO,
            'Non e\' stato possibile leggere i dati del ristorante.')
            .catch((errore: unknown) => {
                contestoInVolo = null
                throw errore
            })
    }
    return contestoInVolo
}

export function dimenticaContesto(): void {
    contestoInVolo = null
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
