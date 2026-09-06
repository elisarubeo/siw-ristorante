package it.uniroma3.siw_ristorante.service;

import it.uniroma3.siw_ristorante.repository.OrdinazioneRepository;
import it.uniroma3.siw_ristorante.repository.PrenotazioneRepository;

import java.time.LocalDate;
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

    /* Una prenotazione annullata non impegna piu' il tavolo. */
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

    /* Aperta = gia' iniziata e non ancora chiusa. Il campo apertura non serve
       a distinguerle: e' obbligatorio e valorizzato sempre. */
    @Transactional(readOnly = true)
    public boolean esisteOrdinazioneAperta(Long tavoloId) {
        return ordinazioneRepository.existsByTavoloIdAndChiusuraIsNull(tavoloId);
    }

    /* Futura = il turno non e' ancora finito. Si guarda la fine e non solo la
       data, cosi' una prenotazione di stamattina gia' conclusa non blocca
       niente, mentre una di stasera si'. */
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
        /* Un solo accesso, con entrambe le condizioni: se il tavolo esiste ma
           appartiene a un altro ristorante, per questo indirizzo non esiste. */
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
            /* Il flush forza subito le scritture: senza, un'eventuale
               violazione di vincolo emergerebbe al commit, fuori da questo
               try, e diventerebbe una pagina di errore. */
            tavoloRepository.flush();
        } catch (DataIntegrityViolationException e) {
            /* Restano le righe storiche: ordinazioni gia' chiuse e prenotazioni
               passate, che puntano ancora al tavolo con una chiave esterna non
               annullabile. Il database rifiuta, e qui il rifiuto diventa un
               messaggio leggibile invece di una pagina di errore. */
            throw new EntityInUseException("Il tavolo " + tavolo.getNumeroTavolo()
                    + " ha ordinazioni o prenotazioni registrate in passato e non puo' essere eliminato", e);
        }
    }
}
