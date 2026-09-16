package it.uniroma3.siw_ristorante.dto;

import java.util.List;

public record VotiRecensioniDto(
        Double mediaVoti,
        long totale,
        List<VotoQuantitaDto> distribuzione) {
}
