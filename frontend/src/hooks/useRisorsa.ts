import { useEffect, useState } from 'react'

export interface Risorsa<T> {
    dati: T | null
    errore: string | null
    inCaricamento: boolean
}

export function useRisorsa<T>(carica: () => Promise<T>, dipendenze: unknown[]): Risorsa<T> {
    const [dati, setDati] = useState<T | null>(null)
    const [errore, setErrore] = useState<string | null>(null)
    const [inCaricamento, setInCaricamento] = useState(true)

    useEffect(() => {
        let annullato = false

        setInCaricamento(true)
        setErrore(null)

        carica()
            .then((risultato) => { if (!annullato) setDati(risultato) })
            .catch((e: unknown) => {
                if (!annullato) {
                    setErrore(e instanceof Error ? e.message : 'Qualcosa è andato storto.')
                }
            })
            .finally(() => { if (!annullato) setInCaricamento(false) })

        return () => { annullato = true }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, dipendenze)

    return { dati, errore, inCaricamento }
}
