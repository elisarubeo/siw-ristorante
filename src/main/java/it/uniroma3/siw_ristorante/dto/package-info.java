/**
 * Gli oggetti che viaggiano sull'API REST, in ingresso e in uscita. Sono
 * {@code record}: immutabili, senza logica, con i soli campi che servono al
 * frontend.
 *
 * <p><b>Perche' non si serializzano direttamente le entita'.</b> Due motivi,
 * tutti e due gia' pagati in altri progetti:
 * <ul>
 *   <li><i>ricorsione</i>: {@code Scontrino} ha un {@code Ristorante}, che ha
 *       una lista di {@code Piatto}, ognuno con il suo {@code Ristorante}...
 *       Jackson segue le associazioni all'infinito;</li>
 *   <li><i>proxy LAZY</i>: fuori dalla transazione le associazioni pigre non
 *       sono inizializzate e la serializzazione fallisce.</li>
 * </ul>
 * Un DTO taglia il grafo dove decide chi scrive l'API, non dove capita. In
 * piu' disaccoppia: rinominare un campo dell'entita' non cambia il JSON.
 *
 * <p>Per le statistiche i DTO non ricalcano nessuna entita': sono il risultato
 * di aggregazioni ({@code count}, {@code sum}, {@code group by}) che nel
 * modello non esistono come oggetti.
 *
 * <p>La forma esatta di ogni record e' fissata in {@code docs/api-statistiche.md}:
 * quel documento e' il contratto che il frontend React da' per buono.
 */
package it.uniroma3.siw_ristorante.dto;
