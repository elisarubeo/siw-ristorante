package it.uniroma3.siw_ristorante.dto;

import jakarta.validation.constraints.NotBlank;

/* Quello che il frontend manda per entrare. Un record e non un'entita':
   la password in chiaro non deve avere nessuna possibilita' di finire
   accidentalmente in una tabella. */
public record LoginRequest(

        @NotBlank(message = "Il nome utente non può essere vuoto")
        String username,

        @NotBlank(message = "La password non può essere vuota")
        String password) {
}
