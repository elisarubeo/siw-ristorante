package it.uniroma3.siw_ristorante.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import it.uniroma3.siw_ristorante.model.Prenotazione;
import it.uniroma3.siw_ristorante.model.StatoPrenotazione;

public interface PrenotazioneRepository extends JpaRepository<Prenotazione, Long> {
    List<Prenotazione> findByTavoloIdAndStatusInAndDataPrenotazioneGreaterThanEqual(
            Long tavoloId, Collection<StatoPrenotazione> status, LocalDate dataPrenotazione);

    boolean existsByTavoloId(Long tavoloId);

    List<Prenotazione> findByTavoloIdAndStatusInAndDataPrenotazioneBetween(
            Long tavoloId, Collection<StatoPrenotazione> status, LocalDate da, LocalDate a);

    List<Prenotazione> findByUserIdOrderByDataPrenotazioneDescOrarioPrenotazioneDesc(Long userId);


}
