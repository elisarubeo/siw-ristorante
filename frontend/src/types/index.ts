/* Le forme che arrivano dall'API, ricalcate sui DTO Java descritti in
   docs/api-statistiche.md.

   ATTENZIONE: questi tipi descrivono cio' che ci si ASPETTA di ricevere, non
   cio' che arriva davvero. TypeScript controlla il codice, non la rete: se il
   backend rinomina un campo, qui non si accende nessun errore e il valore
   diventa `undefined` a tempo di esecuzione. Quando un numero sparisce dalla
   pagina, il primo posto da guardare e' la corrispondenza fra questo file e i
   record Java.

   Dove il Java dichiara un tipo avvolto (Double, Integer) e non un primitivo,
   qui compare `| null`: la differenza non e' pignoleria, e' la distinzione fra
   "zero" e "non c'e' niente da dire", e l'interfaccia le disegna in modo
   diverso. */

/* ---------- autenticazione ---------- */

export interface RichiestaLogin {
    username: string
    password: string
}

export interface RispostaLogin {
    token: string
    username: string
    userId: number
    /* Gli stessi valori delle costanti di Credentials.java, senza prefisso
       ROLE_. Il tipo letterale fa si' che un confronto scritto male
       (role === 'RISTORANTE') sia un errore di compilazione e non un `false`
       silenzioso. */
    role: 'DEFAULT' | 'ADMIN' | 'RISTORATORE'
}

/* ---------- statistiche ---------- */

export interface Contesto {
    ristoranteId: number
    nome: string
    indirizzo: string
    /* Gli anni in cui esiste almeno uno scontrino, dal piu' recente.
       Vuoto se il locale non ha mai chiuso un conto. */
    anniDisponibili: number[]
}

export interface Riepilogo {
    anno: number
    numeroScontrini: number
    incassoTotale: number
    scontrinoMedio: number
    pietanzeVendute: number
    /* null se nessuno scontrino dell'anno ha l'orario di apertura: e' un campo
       ammesso nullo nel modello, e gli scontrini piu' vecchi della
       funzionalita' non ce l'hanno. */
    durataMediaMinuti: number | null
    incassoAnnoPrecedente: number | null
    /* null quando l'anno precedente non ha incassi: la variazione rispetto a
       zero non e' "+100%", e' una domanda senza risposta. */
    variazionePercentuale: number | null
    mediaVoti: number | null
    numeroRecensioni: number
}

export interface ScontriniMese {
    /* Da 1 a 12. Arrivano sempre tutti e dodici, anche i mesi a zero:
       li riempie il service, perche' un grafico a cui manca agosto non mostra
       agosto vuoto, salda luglio a settembre e mente. */
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
    /* 1 = lunedi ... 7 = domenica, la convenzione ISO di DayOfWeek. */
    giorno: number
    numeroScontrini: number
    incasso: number
}

export interface AffluenzaOra {
    /* Da 0 a 23, tutte e ventiquattro. E' l'ora in cui i clienti si sono
       seduti, non quella in cui hanno pagato. */
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
    /* Sempre cinque voci, da 1 a 5 stelle, anche quelle a zero. */
    distribuzione: VotoQuantita[]
}

/* La forma unica degli errori dell'API: un solo punto da leggere in tutto il
   frontend. `fieldErrors` compare soltanto nei 400 di validazione. */
export interface ApiError {
    message: string
    fieldErrors?: Record<string, string>
}
