package it.uniroma3.siw_ristorante.dto;

/* Quello che si riceve entrando.

   userId sta QUI e non dentro il token, ed e' una scelta: nel token va solo
   cio' che serve al server per ricostruire l'autenticazione a ogni richiesta,
   cioe' chi sei (subject) e che cosa puoi fare (role). L'id serve
   all'interfaccia, non ad autorizzare, e il payload di un JWT e' Base64URL:
   leggibile da chiunque. Meno ci si mette, meglio e'. */
public record LoginResponse(
        String token,
        String username,
        Long userId,
        String role) {
}
