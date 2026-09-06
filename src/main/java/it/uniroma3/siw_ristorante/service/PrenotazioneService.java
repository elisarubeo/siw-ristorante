package it.uniroma3.siw_ristorante.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.uniroma3.siw_ristorante.exception.PrenotazioneNonValidaException;
import it.uniroma3.siw_ristorante.exception.ResourceNotFoundException;
import it.uniroma3.siw_ristorante.model.Prenotazione;
import it.uniroma3.siw_ristorante.model.Ristorante;
import it.uniroma3.siw_ristorante.model.StatoPrenotazione;
import it.uniroma3.siw_ristorante.model.Tavolo;
import it.uniroma3.siw_ristorante.model.User;
import it.uniroma3.siw_ristorante.repository.PrenotazioneRepository;
import it.uniroma3.siw_ristorante.repository.RistoranteRepository;
import it.uniroma3.siw_ristorante.repository.TavoloRepository;

@Service
public class PrenotazioneService {

    /* Una prenotazione annullata non occupa piu' il tavolo. */
    private static final Set<StatoPrenotazione> STATI_IMPEGNATIVI =
            EnumSet.of(StatoPrenotazione.SCHEDULED, StatoPrenotazione.CONFIRMED);

    private final PrenotazioneRepository prenotazioneRepository;
    private final RistoranteRepository ristoranteRepository;
    private final TavoloRepository tavoloRepository;

    public PrenotazioneService(PrenotazioneRepository prenotazioneRepository,
            RistoranteRepository ristoranteRepository, TavoloRepository tavoloRepository) {
        this.prenotazioneRepository = prenotazioneRepository;
        this.ristoranteRepository = ristoranteRepository;
        this.tavoloRepository = tavoloRepository;
    }

    /* Il tavolo lo sceglie il sistema, non chi prenota: si prende il piu'
       piccolo capace di ospitare il gruppo e libero per tutto il turno. */
    @Transactional
    public Prenotazione prenota(Long ristoranteId, User user, Integer numeroPersone,
            LocalDate data, LocalTime orario) {

        Ristorante ristorante = ristoranteRepository.findById(ristoranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Nessun ristorante con id " + ristoranteId));

        LocalDateTime inizio = LocalDateTime.of(data, orario);
        LocalDateTime fine = inizio.plusMinutes(Prenotazione.DURATA_PREDEFINITA_MINUTI);

        /* Il controllo sta sull'istante e non sulla sola data: "oggi alle 9"
           e' passato se sono le 20, anche se la data e' quella di oggi. */
        if (!inizio.isAfter(LocalDateTime.now())) {
            throw new PrenotazioneNonValidaException(
                    "Non si può prenotare per un orario già passato");
        }

        List<Tavolo> candidati = tavoloRepository
                .findByRistoranteIdAndNumeroPostiGreaterThanEqualOrderByNumeroPostiAsc(
                        ristoranteId, numeroPersone);
        if (candidati.isEmpty()) {
            throw new PrenotazioneNonValidaException(
                    "Nessun tavolo di questo ristorante può ospitare " + numeroPersone + " persone");
        }

        Tavolo tavolo = candidati.stream()
                .filter(t -> isLibero(t, inizio, fine))
                .findFirst()
                .orElseThrow(() -> new PrenotazioneNonValidaException(
                        "Nessun tavolo libero per " + numeroPersone + " persone in questa fascia oraria"));

        Prenotazione prenotazione = new Prenotazione();
        prenotazione.setUser(user);
        prenotazione.setRistorante(ristorante);
        prenotazione.setTavolo(tavolo);
        prenotazione.setNumeroPersone(numeroPersone);
        prenotazione.setDataPrenotazione(data);
        prenotazione.setOrarioPrenotazione(orario);
        /* La durata si fissa qui e resta scritta nella prenotazione: se un
           domani la politica cambia, i turni gia' prenotati non si accorciano. */
        prenotazione.setDurataMinuti(Prenotazione.DURATA_PREDEFINITA_MINUTI);
        prenotazione.setStatus(StatoPrenotazione.SCHEDULED);

        return prenotazioneRepository.save(prenotazione);
    }

    private boolean isLibero(Tavolo tavolo, LocalDateTime inizio, LocalDateTime fine) {
        List<Prenotazione> esistenti = prenotazioneRepository
                .findByTavoloIdAndStatusInAndDataPrenotazioneBetween(
                        tavolo.getId(), STATI_IMPEGNATIVI,
                        inizio.toLocalDate().minusDays(1), fine.toLocalDate().plusDays(1));
        return esistenti.stream().noneMatch(p -> p.sovrappone(inizio, fine));
    }

    @Transactional(readOnly = true)
    public List<Prenotazione> prenotazioniPerUtente(Long userId) {
        return prenotazioneRepository
                .findByUserIdOrderByDataPrenotazioneDescOrarioPrenotazioneDesc(userId);
    }
}
