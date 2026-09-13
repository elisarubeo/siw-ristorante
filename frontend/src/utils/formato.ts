const EURO = new Intl.NumberFormat('it-IT', {
    style: 'currency', currency: 'EUR', minimumFractionDigits: 2,
})

const EURO_CORTO = new Intl.NumberFormat('it-IT', {
    style: 'currency', currency: 'EUR', maximumFractionDigits: 0, useGrouping: true,
})

const INTERO = new Intl.NumberFormat('it-IT', { useGrouping: true })

const DECIMALE = new Intl.NumberFormat('it-IT', {
    minimumFractionDigits: 1, maximumFractionDigits: 1,
})

export const ASSENTE = '—'

export function euro(valore: number | null | undefined): string {
    return valore == null ? ASSENTE : EURO.format(valore)
}

export function euroCorto(valore: number): string {
    return EURO_CORTO.format(valore)
}

export function intero(valore: number | null | undefined): string {
    return valore == null ? ASSENTE : INTERO.format(valore)
}

export function decimale(valore: number | null | undefined): string {
    return valore == null ? ASSENTE : DECIMALE.format(valore)
}

export function percentuale(valore: number | null | undefined): string {
    if (valore == null) return ASSENTE
    const segno = valore > 0 ? '+' : ''
    return `${segno}${DECIMALE.format(valore)}%`
}

export function minuti(valore: number | null | undefined): string {
    if (valore == null) return ASSENTE
    const ore = Math.floor(valore / 60)
    const resto = valore % 60
    return ore > 0 ? `${ore} h ${resto} min` : `${resto} min`
}

export const MESI = [
    'gennaio', 'febbraio', 'marzo', 'aprile', 'maggio', 'giugno',
    'luglio', 'agosto', 'settembre', 'ottobre', 'novembre', 'dicembre',
]

export const MESI_CORTI = ['gen', 'feb', 'mar', 'apr', 'mag', 'giu',
    'lug', 'ago', 'set', 'ott', 'nov', 'dic']

export const GIORNI = ['lunedì', 'martedì', 'mercoledì', 'giovedì', 'venerdì', 'sabato', 'domenica']
export const GIORNI_CORTI = ['lun', 'mar', 'mer', 'gio', 'ven', 'sab', 'dom']
