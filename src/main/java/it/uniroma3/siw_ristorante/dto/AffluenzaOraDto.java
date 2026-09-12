package it.uniroma3.siw_ristorante.dto;

/* Un'ora del giorno, da 0 a 23. Sempre tutte e ventiquattro, comprese quelle
   a zero: e' l'unico modo per vedere quanto sono staccate le due gobbe di
   pranzo e cena. Togliendo le ore vuote il locale sembrerebbe aperto senza
   interruzione. */
public record AffluenzaOraDto(
        int ora,
        long numeroScontrini) {
}
