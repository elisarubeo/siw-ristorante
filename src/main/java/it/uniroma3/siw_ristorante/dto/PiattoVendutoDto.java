package it.uniroma3.siw_ristorante.dto;

import java.math.BigDecimal;

/* Una voce della classifica dei piatti.

   nomePiatto e' la copia salvata nella riga dello scontrino, non un join su
   Piatto: quel piatto puo' essere stato rinominato o tolto dal menu, e la
   classifica deve continuare a dire che cosa si e' venduto allora. */
public record PiattoVendutoDto(
        Long piattoId,
        String nomePiatto,
        long quantita,
        BigDecimal incasso) {
}
