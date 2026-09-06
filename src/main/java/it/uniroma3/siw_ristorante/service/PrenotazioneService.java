package it.uniroma3.siw_ristorante.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

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

    private final PrenotazioneRepository prenotazioneRepository;
    private final RistoranteRepository ristoranteRepository;
    private final TavoloRepository tavoloRepository;

    public PrenotazioneService(PrenotazioneRepository prenotazioneRepository,
            RistoranteRepository ristoranteRepository, TavoloRepository tavoloRepository) {
        this.prenotazioneRepository = prenotazioneRepository;
        this.ristoranteRepository = ristoranteRepository;
        this.tavoloRepository = tavoloRepository;
    }

    /* Il tavolo lo sceglie il sistema: si prende il piu'
       piccolo capace di ospitare il gruppo e libero per tutto il turno. */
    @Transactional
    public Prenotazione prenota(Long ristoranteId, User user, Integer numeroPersone,
            LocalDate data, LocalTime orario) {

        Ristorante ristorante = ristoranteRepository.findById(ristoranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Nessun ristorante con id " + ristoranteId));

        LocalDateTime inizio = LocalDateTime.of(data, orario);
        LocalDateTime fine = inizio.plusMinutes(Prenotazione.DURATA_PREDEFINITA_MINUTI);

        verificaNonPassata(inizio);

        Tavolo tavolo = assegnaTavolo(ristoranteId, numeroPersone, inizio, fine, null);

        Prenotazione prenotazione = new Prenotazione();
        prenotazione.setUser(user);
        prenotazione.setRistorante(ristorante);
        prenotazione.setTavolo(tavolo);
        prenotazione.setNumeroPersone(numeroPersone);
        prenotazione.setDataPrenotazione(data);
        prenotazione.setOrarioPrenotazione(orario);
        prenotazione.setDurataMinuti(Prenotazione.DURATA_PREDEFINITA_MINUTI);
        prenotazione.setStatus(StatoPrenotazione.SCHEDULED);

        return prenotazioneRepository.save(prenotazione);
    }

    /* escludiId serve alla modifica: senza, spostare una prenotazione dalle
       20:00 alle 20:30 sullo stesso tavolo troverebbe come ostacolo sé stessa. */
    private boolean isLibero(Tavolo tavolo, LocalDateTime inizio, LocalDateTime fine, Long escludiId) {
        List<Prenotazione> esistenti = prenotazioneRepository
                .findByTavoloIdAndStatusNotAndDataPrenotazioneBetween(
                        tavolo.getId(), StatoPrenotazione.CANCELLED,
                        inizio.toLocalDate().minusDays(1), fine.toLocalDate().plusDays(1));
        return esistenti.stream()
                .filter(p -> escludiId == null || !escludiId.equals(p.getId()))
                .noneMatch(p -> p.sovrappone(inizio, fine));
    }

    private Tavolo assegnaTavolo(Long ristoranteId, Integer numeroPersone,
            LocalDateTime inizio, LocalDateTime fine, Long escludiId) {
        List<Tavolo> candidati = tavoloRepository
                .findByRistoranteIdAndNumeroPostiGreaterThanEqualOrderByNumeroPostiAsc(
                        ristoranteId, numeroPersone);
        if (candidati.isEmpty()) {
            throw new PrenotazioneNonValidaException(
                    "Nessun tavolo di questo ristorante può ospitare " + numeroPersone + " persone");
        }
        /* Candidati ordinati per posti crescenti: si prende il piu' piccolo che
           basta. In modifica il tavolo attuale e' fra questi, quindi se va
           ancora bene viene riscelto senza doverlo trattare come caso a parte. */
        return candidati.stream()
                .filter(t -> isLibero(t, inizio, fine, escludiId))
                .findFirst()
                .orElseThrow(() -> new PrenotazioneNonValidaException(
                        "Nessun tavolo libero per " + numeroPersone + " persone in questa fascia oraria"));
    }

    private void verificaNonPassata(LocalDateTime inizio) {
        /* Il controllo sta sull'istante e non sulla sola data: "oggi alle 9"
           e' passato se sono le 20, anche se la data e' quella di oggi. */
        if (!inizio.isAfter(LocalDateTime.now())) {
            throw new PrenotazioneNonValidaException(
                    "Non si può prenotare per un orario già passato");
        }
    }

    /* Una prenotazione propria, per mostrarla nel modulo. */
    @Transactional(readOnly = true)
    public Prenotazione miaPrenotazione(Long prenotazioneId, User user) {
        return prenotazioneRepository.findByIdAndUserId(prenotazioneId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nessuna tua prenotazione con id " + prenotazioneId));
    }

    /* Solo il proprietario, e solo prima che il turno cominci. */
    private Prenotazione miaPrenotazioneModificabile(Long prenotazioneId, User user) {
        Prenotazione prenotazione = prenotazioneRepository
                .findByIdAndUserId(prenotazioneId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nessuna tua prenotazione con id " + prenotazioneId));
        if (prenotazione.getStatus() == StatoPrenotazione.CANCELLED) {
            throw new PrenotazioneNonValidaException("Questa prenotazione è già stata annullata");
        }
        if (!prenotazione.getInizio().isAfter(LocalDateTime.now())) {
            throw new PrenotazioneNonValidaException(
                    "Una prenotazione già iniziata o passata non si può più cambiare");
        }
        return prenotazione;
    }

    /* Stessa riga e stesso id: non si cancella e ricrea, altrimenti un
       fallimento a meta' lascerebbe la persona senza la prenotazione che aveva. */
    @Transactional
    public Prenotazione modifica(Long prenotazioneId, User user, Integer numeroPersone,
            LocalDate data, LocalTime orario) {

        Prenotazione prenotazione = miaPrenotazioneModificabile(prenotazioneId, user);

        LocalDateTime inizio = LocalDateTime.of(data, orario);
        LocalDateTime fine = inizio.plusMinutes(prenotazione.getDurataMinuti());
        verificaNonPassata(inizio);

        prenotazione.setTavolo(assegnaTavolo(prenotazione.getRistorante().getId(),
                numeroPersone, inizio, fine, prenotazione.getId()));
        prenotazione.setNumeroPersone(numeroPersone);
        prenotazione.setDataPrenotazione(data);
        prenotazione.setOrarioPrenotazione(orario);
        return prenotazione;
    }

    @Transactional
    public Prenotazione annulla(Long prenotazioneId, User user) {
        Prenotazione prenotazione = miaPrenotazioneModificabile(prenotazioneId, user);
        prenotazione.setStatus(StatoPrenotazione.CANCELLED);
        return prenotazione;
    }

    @Transactional(readOnly = true)
    public List<Prenotazione> prenotazioniPerUtente(Long userId) {
        return prenotazioneRepository
                .findByUserIdOrderByDataPrenotazioneDescOrarioPrenotazioneDesc(userId);
    }
}
