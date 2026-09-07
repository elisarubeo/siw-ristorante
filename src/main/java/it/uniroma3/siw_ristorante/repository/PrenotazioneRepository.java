package it.uniroma3.siw_ristorante.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.uniroma3.siw_ristorante.model.Prenotazione;
import it.uniroma3.siw_ristorante.model.StatoPrenotazione;

public interface PrenotazioneRepository extends JpaRepository<Prenotazione, Long> {
    List<Prenotazione> findByTavoloIdAndStatusNotAndDataPrenotazioneGreaterThanEqual(
            Long tavoloId, StatoPrenotazione status, LocalDate dataPrenotazione);

    boolean existsByTavoloId(Long tavoloId);

    List<Prenotazione> findByTavoloIdAndStatusNotAndDataPrenotazioneBetween(
            Long tavoloId, StatoPrenotazione status, LocalDate da, LocalDate a);

    /* Per l'agenda dell'amministratore: tutte quelle di un ristorante, o di un
       singolo tavolo, in ordine di calendario. Il filtro sulla data e' grezzo
       (da ieri in poi) perche' una prenotazione di ieri sera puo' finire dopo
       la mezzanotte: a scartare quelle finite ci pensa il service. */
    List<Prenotazione> findByRistoranteIdAndStatusNotAndDataPrenotazioneGreaterThanEqualOrderByDataPrenotazioneAscOrarioPrenotazioneAsc(
            Long ristoranteId, StatoPrenotazione status, LocalDate da);

    List<Prenotazione> findByTavoloIdAndStatusNotAndDataPrenotazioneGreaterThanEqualOrderByDataPrenotazioneAscOrarioPrenotazioneAsc(
            Long tavoloId, StatoPrenotazione status, LocalDate da);

    Optional<Prenotazione> findByIdAndUserId(Long id, Long userId);

    List<Prenotazione> findByUserIdOrderByDataPrenotazioneDescOrarioPrenotazioneDesc(Long userId);


}
