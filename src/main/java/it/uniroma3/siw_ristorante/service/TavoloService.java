package it.uniroma3.siw_ristorante.service;

import it.uniroma3.siw_ristorante.repository.OrdinazioneRepository;
import it.uniroma3.siw_ristorante.repository.PrenotazioneRepository;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.uniroma3.siw_ristorante.exception.EntityInUseException;
import it.uniroma3.siw_ristorante.exception.ResourceNotFoundException;
import it.uniroma3.siw_ristorante.model.Prenotazione;
import it.uniroma3.siw_ristorante.model.Ristorante;
import it.uniroma3.siw_ristorante.model.StatoPrenotazione;
import it.uniroma3.siw_ristorante.model.Tavolo;
import it.uniroma3.siw_ristorante.repository.RistoranteRepository;
import it.uniroma3.siw_ristorante.repository.TavoloRepository;

@Service
public class TavoloService {

    private static final Set<StatoPrenotazione> STATI_IMPEGNATIVI =
            EnumSet.of(StatoPrenotazione.SCHEDULED, StatoPrenotazione.CONFIRMED);

    private final OrdinazioneRepository ordinazioneRepository;
    private final TavoloRepository tavoloRepository;
    private final RistoranteRepository ristoranteRepository;
    private final PrenotazioneRepository prenotazioneRepository;

    public TavoloService(TavoloRepository tavoloRepository, RistoranteRepository ristoranteRepository, OrdinazioneRepository ordinazioneRepository, PrenotazioneRepository prenotazioneRepository) {
        this.tavoloRepository = tavoloRepository;
        this.ristoranteRepository = ristoranteRepository;
        this.ordinazioneRepository = ordinazioneRepository;
        this.prenotazioneRepository = prenotazioneRepository;
    }

    @Transactional(readOnly = true)
    public List<Tavolo> findByRistoranteIdOrderByNumeroTavolo(Long ristoranteId) {
        return tavoloRepository.findByRistoranteIdOrderByNumeroTavolo(ristoranteId);
    }

    @Transactional(readOnly = true)
    public boolean esisteNelRistorante(Long ristoranteId, Integer numeroTavolo) {
        return tavoloRepository.existsByNumeroTavoloAndRistoranteId(numeroTavolo, ristoranteId);
    }

    @Transactional
    public Tavolo save(Long ristoranteId, Tavolo tavolo) {
        Ristorante ristorante = ristoranteRepository.findById(ristoranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Nessun ristorante con id " + ristoranteId));

        if (tavoloRepository.existsByNumeroTavoloAndRistoranteId(tavolo.getNumeroTavolo(), ristoranteId)) {
            throw new IllegalStateException(
                    "Il tavolo " + tavolo.getNumeroTavolo() + " esiste gia' nel ristorante " + ristoranteId);
        }

        ristorante.aggiungiTavolo(tavolo);
        return tavoloRepository.save(tavolo);
    }

    @Transactional(readOnly = true)
    public boolean esisteOrdinazioneAperta(Long tavoloId) {
        return ordinazioneRepository.existsByTavoloIdAndChiusuraIsNull(tavoloId);
    }

    @Transactional(readOnly = true)
    public boolean esistePrenotazioneFutura(Long tavoloId) {
        LocalDateTime adesso = LocalDateTime.now();
        List<Prenotazione> candidate = prenotazioneRepository
                .findByTavoloIdAndStatusInAndDataPrenotazioneGreaterThanEqual(
                        tavoloId, STATI_IMPEGNATIVI, adesso.toLocalDate().minusDays(1));
        return candidate.stream().anyMatch(p -> p.getFine().isAfter(adesso));
    }

    @Transactional
    public void delete(Long ristoranteId, Long tavoloId) {
        Tavolo tavolo = tavoloRepository.findByIdAndRistoranteId(tavoloId, ristoranteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nessun tavolo con id " + tavoloId + " nel ristorante " + ristoranteId));

        if (esisteOrdinazioneAperta(tavoloId)) {
            throw new EntityInUseException(
                    "Il tavolo " + tavolo.getNumeroTavolo() + " ha un conto ancora aperto");
        }
        if (esistePrenotazioneFutura(tavoloId)) {
            throw new EntityInUseException(
                    "Il tavolo " + tavolo.getNumeroTavolo() + " ha prenotazioni ancora valide");
        }

        try {
            tavoloRepository.delete(tavolo);
            tavoloRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new EntityInUseException("Il tavolo " + tavolo.getNumeroTavolo()
                    + " ha ordinazioni o prenotazioni registrate in passato e non puo' essere eliminato", e);
        }
    }
}
