package it.uniroma3.siw_ristorante.dto;

import java.math.BigDecimal;

/* Un giorno della settimana, da 1 = lunedi a 7 = domenica: la convenzione ISO,
   la stessa di java.time.DayOfWeek.getValue(). Sempre tutti e sette. */
public record IncassoGiornoDto(
        int giorno,
        long numeroScontrini,
        BigDecimal incasso) {
}
