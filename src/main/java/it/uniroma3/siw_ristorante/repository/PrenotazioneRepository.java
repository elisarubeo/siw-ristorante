package it.uniroma3.siw_ristorante.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import it.uniroma3.siw_ristorante.model.Prenotazione;
import it.uniroma3.siw_ristorante.model.StatoPrenotazione;

public interface PrenotazioneRepository extends JpaRepository<Prenotazione, Long> {
    List<Prenotazione> findByTavoloIdAndStatusNotAndDataPrenotazioneGreaterThanEqual(
            Long tavoloId, StatoPrenotazione status, LocalDate dataPrenotazione);

    boolean existsByTavoloId(Long tavoloId);

    /* Tutti i candidati insieme e non un tavolo per volta: chi prenota fa
       scegliere il tavolo al sistema, e con una query per candidato prenotare
       in un locale con venti tavoli costava venti query. */
    List<Prenotazione> findByTavoloIdInAndStatusNotAndDataPrenotazioneBetween(
            Collection<Long> tavoloIds, StatoPrenotazione status, LocalDate da, LocalDate a);

    /* Per l'agenda dell'amministratore: tutte quelle di un ristorante, o di un
       singolo tavolo, in ordine di calendario. Il filtro sulla data e' grezzo
       (da ieri in poi) perche' una prenotazione di ieri sera puo' finire dopo
       la mezzanotte: a scartare quelle finite ci pensa il service.
       Il numero del tavolo viene stampato riga per riga, percio' il tavolo
       arriva insieme: senza il grafo sarebbe una query per prenotazione. Il
       cliente invece no, lo risolve la mappa degli username del controller. */
    @EntityGraph(attributePaths = { "tavolo" })
    List<Prenotazione> findByRistoranteIdAndStatusNotAndDataPrenotazioneGreaterThanEqualOrderByDataPrenotazioneAscOrarioPrenotazioneAsc(
            Long ristoranteId, StatoPrenotazione status, LocalDate da);

    List<Prenotazione> findByTavoloIdAndStatusNotAndDataPrenotazioneGreaterThanEqualOrderByDataPrenotazioneAscOrarioPrenotazioneAsc(
            Long tavoloId, StatoPrenotazione status, LocalDate da);

    Optional<Prenotazione> findByIdAndUserId(Long id, Long userId);

    /* "Le mie prenotazioni" attraversa piu' ristoranti e mostra di ognuna il
       nome del locale e il numero del tavolo: due associazioni diverse, quindi
       senza il grafo due query per riga. */
    @EntityGraph(attributePaths = { "ristorante", "tavolo" })
    List<Prenotazione> findByUserIdOrderByDataPrenotazioneDescOrarioPrenotazioneDesc(Long userId);


}
