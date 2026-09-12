package it.uniroma3.siw_ristorante.dto;

import java.util.List;

/* Come i clienti giudicano il locale.

   distribuzione ha sempre cinque voci, da 1 a 5 stelle: il group by del
   database restituisce righe solo per i voti che qualcuno ha dato, e riempire
   gli assenti e' compito del service. Una scala a cui mancano dei gradini non
   e' una scala.

   Niente parametro anno: le recensioni sono il giudizio corrente, non un
   fatto di cassa. */
public record VotiRecensioniDto(
        Double mediaVoti,
        long totale,
        List<VotoQuantitaDto> distribuzione) {
}
