/* I numeri si formattano in un posto solo: se ogni componente scrivesse il suo
   toFixed(2), la stessa cifra comparirebbe in pagina in tre modi diversi.
   Intl parla italiano da se': virgola decimale, punto per le migliaia. */

const EURO = new Intl.NumberFormat('it-IT', {
    style: 'currency', currency: 'EUR', minimumFractionDigits: 2,
})

/* useGrouping esplicito: in italiano Intl di suo raggruppa solo da cinque
   cifre in su, quindi 7178 uscirebbe "7178 €" e 10062 "10.062 €" - due stili
   diversi nella stessa pagina, a seconda di quanto ha incassato il locale. */
const EURO_CORTO = new Intl.NumberFormat('it-IT', {
    style: 'currency', currency: 'EUR', maximumFractionDigits: 0, useGrouping: true,
})

const INTERO = new Intl.NumberFormat('it-IT', { useGrouping: true })

const DECIMALE = new Intl.NumberFormat('it-IT', {
    minimumFractionDigits: 1, maximumFractionDigits: 1,
})

/* Il trattino e non "0": un valore che non c'e' non e' un valore a zero.
   Le aggregazioni su un insieme vuoto tornano null dal database, ed e'
   un'informazione da mostrare come tale. */
export const ASSENTE = '—'

export function euro(valore: number | null | undefined): string {
    return valore == null ? ASSENTE : EURO.format(valore)
}

/* Per gli assi dei grafici, dove i centesimi sono rumore. */
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
    /* Il segno esplicito anche sui positivi: e' un confronto, e "+12,5%" si
       legge come variazione mentre "12,5%" si legge come quota. */
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

/* Sull'asse ci stanno tre lettere, nel tooltip il nome intero. */
export const MESI_CORTI = ['gen', 'feb', 'mar', 'apr', 'mag', 'giu',
    'lug', 'ago', 'set', 'ott', 'nov', 'dic']

/* 1 = lunedi, come DayOfWeek.getValue() in Java. */
export const GIORNI = ['lunedì', 'martedì', 'mercoledì', 'giovedì', 'venerdì', 'sabato', 'domenica']
export const GIORNI_CORTI = ['lun', 'mar', 'mer', 'gio', 'ven', 'sab', 'dom']
