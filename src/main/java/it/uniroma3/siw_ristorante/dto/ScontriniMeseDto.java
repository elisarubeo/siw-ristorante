package it.uniroma3.siw_ristorante.dto;

import java.math.BigDecimal;

/* Un mese dell'anno. Il frontend ne riceve sempre dodici, anche quelli a zero:
   vedi StatisticheService, e' li' che i buchi vengono riempiti. */
public record ScontriniMeseDto(
        int mese,
        long numeroScontrini,
        BigDecimal incasso) {
}
