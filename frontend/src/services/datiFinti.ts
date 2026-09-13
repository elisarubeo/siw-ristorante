import type {
    AffluenzaOra, Contesto, IncassoGiorno, PiattoVenduto,
    Riepilogo, RispostaLogin, ScontriniMese, VotiRecensioni,
} from '../types'

const ANNO_CORRENTE = new Date().getFullYear()

const STAGIONE = [0.60, 0.70, 0.90, 1.15, 1.30, 1.45, 1.35, 0.55, 1.20, 1.05, 0.65, 1.25]

function rumore(seme: number): number {
    const x = Math.sin(seme * 12.9898) * 43758.5453
    return x - Math.floor(x)
}

export const CONTESTO_FINTO: Contesto = {
    ristoranteId: 1,
    nome: 'Osteria del Borgo',
    indirizzo: 'Via dei Coronari 12, Roma',
    anniDisponibili: [ANNO_CORRENTE, ANNO_CORRENTE - 1],
}

export function scontriniPerMeseFinti(anno: number): ScontriniMese[] {
    const scala = anno === ANNO_CORRENTE ? 1 : 0.78
    return STAGIONE.map((stagione, indice) => {
        const numeroScontrini = Math.round(20 * stagione * scala * (0.85 + rumore(anno + indice) * 0.3))
        return {
            mese: indice + 1,
            numeroScontrini,
            incasso: Math.round(numeroScontrini * (36 + rumore(indice) * 12) * 100) / 100,
        }
    })
}

export function riepilogoFinto(anno: number): Riepilogo {
    const mesi = scontriniPerMeseFinti(anno)
    const precedenti = scontriniPerMeseFinti(anno - 1)

    const numeroScontrini = mesi.reduce((somma, m) => somma + m.numeroScontrini, 0)
    const incassoTotale = Math.round(mesi.reduce((somma, m) => somma + m.incasso, 0) * 100) / 100
    const incassoAnnoPrecedente = anno > ANNO_CORRENTE - 1
        ? Math.round(precedenti.reduce((somma, m) => somma + m.incasso, 0) * 100) / 100
        : null

    return {
        anno,
        numeroScontrini,
        incassoTotale,
        scontrinoMedio: Math.round((incassoTotale / numeroScontrini) * 100) / 100,
        pietanzeVendute: Math.round(numeroScontrini * 3.4),
        durataMediaMinuti: 97,
        incassoAnnoPrecedente,
        variazionePercentuale: incassoAnnoPrecedente
            ? Math.round(((incassoTotale - incassoAnnoPrecedente) / incassoAnnoPrecedente) * 1000) / 10
            : null,
        mediaVoti: 4.2,
        numeroRecensioni: 17,
    }
}

const PIATTI_FINTI: Array<[string, number, number]> = [
    ['Carbonara', 13.5, 224], ['Cacio e pepe', 12, 189], ['Amatriciana', 12.5, 160],
    ['Bruschette miste', 5.5, 128], ['Tiramisu', 6, 120], ['Puntarelle', 6.5, 93],
    ['Panna cotta', 5.5, 76], ['Saltimbocca alla romana', 17, 76], ['Coda alla vaccinara', 16.5, 53],
]

export function piattiFinti(anno: number, limite: number): PiattoVenduto[] {
    const scala = anno === ANNO_CORRENTE ? 1 : 0.78
    return PIATTI_FINTI
        .map(([nomePiatto, prezzo, quantita], indice) => {
            const venduti = Math.round(quantita * scala)
            return {
                piattoId: indice + 1,
                nomePiatto,
                quantita: venduti,
                incasso: Math.round(venduti * prezzo * 100) / 100,
            }
        })
        .slice(0, limite)
}

export function incassoPerGiornoFinto(anno: number): IncassoGiorno[] {
    const pesi = [0.45, 0.55, 0.75, 0.95, 1.60, 1.85, 1.35]
    const scala = anno === ANNO_CORRENTE ? 1 : 0.78
    return pesi.map((peso, indice) => {
        const numeroScontrini = Math.round(24 * peso * scala)
        return {
            giorno: indice + 1,
            numeroScontrini,
            incasso: Math.round(numeroScontrini * 40.9 * 100) / 100,
        }
    })
}

export function affluenzaPerOraFinta(anno: number): AffluenzaOra[] {
    const perOra: Record<number, number> = { 12: 35, 13: 56, 14: 13, 19: 37, 20: 71, 21: 60, 22: 19 }
    const scala = anno === ANNO_CORRENTE ? 1 : 0.78
    return Array.from({ length: 24 }, (_, ora) => ({
        ora,
        numeroScontrini: Math.round((perOra[ora] ?? 0) * scala),
    }))
}

export const VOTI_FINTI: VotiRecensioni = {
    mediaVoti: 4.2,
    totale: 17,
    distribuzione: [
        { voto: 1, quantita: 0 }, { voto: 2, quantita: 1 }, { voto: 3, quantita: 2 },
        { voto: 4, quantita: 6 }, { voto: 5, quantita: 8 },
    ],
}

export function loginFinto(username: string, password: string): RispostaLogin {
    if (username !== password) {
        throw new Error('Credenziali non valide.')
    }
    return {
        token: 'token-finto-non-firmato',
        username,
        userId: 4,
        role: username === 'admin' ? 'ADMIN' : username === 'borgo' ? 'RISTORATORE' : 'DEFAULT',
    }
}

export function ritardo<T>(valore: T, ms = 350): Promise<T> {
    return new Promise((risolvi) => setTimeout(() => risolvi(valore), ms))
}
