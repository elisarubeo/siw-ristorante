package it.uniroma3.siw_ristorante.dto;

/* Quante recensioni hanno dato questo voto. */
public record VotoQuantitaDto(
        int voto,
        long quantita) {
}
