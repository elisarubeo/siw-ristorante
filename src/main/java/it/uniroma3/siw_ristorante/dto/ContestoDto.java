package it.uniroma3.siw_ristorante.dto;

import java.util.List;

public record ContestoDto(
        Long ristoranteId,
        String nome,
        String indirizzo,
        List<Integer> anniDisponibili) {
}
