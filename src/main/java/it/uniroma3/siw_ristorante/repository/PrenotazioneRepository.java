package it.uniroma3.siw_ristorante.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import it.uniroma3.siw_ristorante.model.Prenotazione;
import it.uniroma3.siw_ristorante.model.StatoPrenotazione;

public interface PrenotazioneRepository extends JpaRepository<Prenotazione, Long> {

    /* Si filtra per giorno nel database e per orario in memoria: le
       prenotazioni di un tavolo sono poche, e la sovrapposizione la sa
       calcolare Prenotazione con getFine(). Si parte dal giorno prima perche'
       un turno serale puo' scavalcare la mezzanotte. */
    List<Prenotazione> findByTavoloIdAndStatusInAndDataPrenotazioneGreaterThanEqual(
            Long tavoloId, Collection<StatoPrenotazione> status, LocalDate dataPrenotazione);

    boolean existsByTavoloId(Long tavoloId);

    List<Prenotazione> findByTavoloId(Long tavoloId);
}
