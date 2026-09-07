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

    Optional<Prenotazione> findByIdAndUserId(Long id, Long userId);

    List<Prenotazione> findByUserIdOrderByDataPrenotazioneDescOrarioPrenotazioneDesc(Long userId);


}
