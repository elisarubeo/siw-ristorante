export const COLORE_DATI = '#b4532a'

export const COLORE_GRIGLIA = '#e7ded2'
export const COLORE_TESTO = '#6d635c'
export const COLORE_SUPERFICIE = '#ffffff'

export const SPESSORE_BARRA = 24

export const CIMA_ARROTONDATA: [number, number, number, number] = [4, 4, 0, 0]
export const DESTRA_ARROTONDATA: [number, number, number, number] = [0, 4, 4, 0]

export const ASSE = {
    tick: { fill: COLORE_TESTO, fontSize: 12 },
    tickLine: false,
    axisLine: false,
} as const

export const MARGINE = { top: 20, right: 8, bottom: 0, left: 0 }

export const CURSORE = { fill: 'rgba(43, 37, 33, .04)' }
