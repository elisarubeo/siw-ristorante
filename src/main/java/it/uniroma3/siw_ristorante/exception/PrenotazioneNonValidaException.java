package it.uniroma3.siw_ristorante.exception;

/* Non e' un errore di sistema: e' un rifiuto con una ragione da mostrare a chi
   sta prenotando (orario passato, nessun tavolo abbastanza grande, tutto
   occupato in quella fascia). */
public class PrenotazioneNonValidaException extends RuntimeException {

    public PrenotazioneNonValidaException(String message) {
        super(message);
    }
}
