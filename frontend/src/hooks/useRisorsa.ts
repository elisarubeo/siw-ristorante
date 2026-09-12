import { useEffect, useState } from 'react'

/* Caricare qualcosa dall'API sono sempre gli stessi quattro gesti: segnare che
   e' in corso, chiedere, mettere via il risultato o l'errore, smettere di
   segnalare l'attesa. Scritti in ogni componente sarebbero sei copie della
   stessa cosa, ognuna con la sua occasione di dimenticare un pezzo.

   Un hook personalizzato e' esattamente questo: una funzione che usa gli hook
   di React e si comporta come loro. Il nome deve cominciare per "use",
   altrimenti React non lo riconosce come tale. */

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
        /* La bandiera del cleanup. Se il componente sparisce (o le dipendenze
           cambiano) mentre la richiesta e' ancora in volo, la risposta arriva
           comunque: senza questa guardia si scriverebbe nello stato di un
           componente che non c'e' piu'.
           Serve anche contro le risposte fuori ordine: cambiando anno due
           volte di fila, la prima richiesta puo' rispondere dopo la seconda e
           rimettere in pagina i dati vecchi. */
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
        /* `carica` e' una funzione nuova a ogni disegno e non puo' stare fra le
           dipendenze: rifarebbe la richiesta all'infinito. Le dipendenze vere
           le passa chi chiama (di solito l'anno selezionato). */
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, dipendenze)

    return { dati, errore, inCaricamento }
}
