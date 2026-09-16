package it.uniroma3.siw_ristorante.dto;

import java.math.BigDecimal;

public record RiepilogoDto(
        int anno,
        long numeroScontrini,

        /* Mai null: se non ci sono scontrini vale zero, e zero incassato e'
           un'informazione vera. */
        BigDecimal incassoTotale,
        BigDecimal scontrinoMedio,
        long pietanzeVendute,

        /* null se nessuno scontrino dell'anno ha l'orario di apertura: e' un
           campo ammesso nullo, gli scontrini piu' vecchi della funzionalita'
           non ce l'hanno. */
        Integer durataMediaMinuti,

        /* null se l'anno precedente non ha incassi. */
        BigDecimal incassoAnnoPrecedente,

        /* null quando il precedente e' null o zero: la variazione rispetto a
           zero non e' "+100%", e' una divisione per zero. */
        Double variazionePercentuale,

        /* null senza recensioni. Non dipende dall'anno: e' il giudizio del
           locale oggi, e resta uguale cambiando anno. */
        Double mediaVoti,
        long numeroRecensioni) {
}
