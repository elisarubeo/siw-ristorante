package it.uniroma3.siw_ristorante.dto;

import java.util.List;

/* Il primo endpoint che il frontend chiama: di che locale stiamo parlando, e
   quali anni ha senso mettere nel menu a tendina.

   anniDisponibili e' vuoto se il ristorante non ha mai chiuso un conto: in
   quel caso la SPA mostra "nessun dato" e non chiede nient'altro. Offrire anni
   senza scontrini sarebbe offrire grafici vuoti. */
public record ContestoDto(
        Long ristoranteId,
        String nome,
        String indirizzo,
        List<Integer> anniDisponibili) {
}
