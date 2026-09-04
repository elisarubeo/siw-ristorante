package it.uniroma3.siw_ristorante.model;

/* Non e' mappato su nessuna colonna: lo stato di un tavolo non e' un suo
   attributo, e' una funzione dell'istante in cui lo si guarda. Lo calcola
   TavoloService a partire da prenotazioni e ordinazioni. */
public enum StatoTavolo {
    LIBERO,
    PRENOTATO,
    OCCUPATO
}
