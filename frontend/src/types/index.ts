export interface RichiestaLogin {
    username: string
    password: string
}

export interface RispostaLogin {
    token: string
    username: string
    userId: number
    role: 'DEFAULT' | 'ADMIN' | 'RISTORATORE'
}

export interface Contesto {
    ristoranteId: number
    nome: string
    indirizzo: string
    anniDisponibili: number[]
}

export interface Riepilogo {
    anno: number
    numeroScontrini: number
    incassoTotale: number
    scontrinoMedio: number
    pietanzeVendute: number
    durataMediaMinuti: number | null
    incassoAnnoPrecedente: number | null
    variazionePercentuale: number | null
    mediaVoti: number | null
    numeroRecensioni: number
}

export interface ScontriniMese {
    mese: number
    numeroScontrini: number
    incasso: number
}

export interface PiattoVenduto {
    piattoId: number
    nomePiatto: string
    quantita: number
    incasso: number
}

export interface IncassoGiorno {
    giorno: number
    numeroScontrini: number
    incasso: number
}

export interface AffluenzaOra {
    ora: number
    numeroScontrini: number
}

export interface VotoQuantita {
    voto: number
    quantita: number
}

export interface VotiRecensioni {
    mediaVoti: number | null
    totale: number
    distribuzione: VotoQuantita[]
}

export interface ApiError {
    message: string
    fieldErrors?: Record<string, string>
}
