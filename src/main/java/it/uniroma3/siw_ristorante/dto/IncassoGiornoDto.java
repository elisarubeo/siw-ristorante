package it.uniroma3.siw_ristorante.dto;

import java.math.BigDecimal;

public record IncassoGiornoDto(
        int giorno,
        long numeroScontrini,
        BigDecimal incasso) {
}
