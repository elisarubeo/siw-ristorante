package it.uniroma3.siw_ristorante.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
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
        verificaNonTroppoLontana(data);

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

    /* Il confronto e' sulla data e non sull'istante: "al massimo a due mesi"
       si intende sul giorno, altrimenti prenotare alle 20 di un giorno limite
       sarebbe ammesso e alle 21 no, per pochi minuti di differenza. */
    private void verificaNonTroppoLontana(LocalDate data) {
        LocalDate ultimoGiorno = LocalDate.now().plusMonths(Prenotazione.ANTICIPO_MASSIMO_MESI);
        if (data.isAfter(ultimoGiorno)) {
            throw new PrenotazioneNonValidaException(
                    "Si puo' prenotare al massimo con " + Prenotazione.ANTICIPO_MASSIMO_MESI
                            + " mesi di anticipo: l'ultimo giorno disponibile e' il "
                            + ultimoGiorno.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
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
        verificaNonTroppoLontana(data);

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

    /* Quando l'amministratore disattiva un ristorante: le prenotazioni ancora
       da onorare diventano annullate in blocco, e il cliente le ritrova come
       tali ne "Le mie prenotazioni". Torna quante ne sono state annullate,
       cosi' la pagina puo' dirlo a chi ha premuto il pulsante. */
    @Transactional
    public int annullaFuturePerRistorante(Long ristoranteId) {
        List<Prenotazione> daAnnullare = this.prenotazioniFutureDelRistorante(ristoranteId);
        for (Prenotazione prenotazione : daAnnullare) {
            prenotazione.setStatus(StatoPrenotazione.CANCELLED);
        }
        return daAnnullare.size();
    }

    /* L'agenda di un ristorante: tutte le prenotazioni non ancora finite, di
       qualunque cliente e di qualunque tavolo. Le annullate non ci sono, e una
       prenotazione in corso adesso resta in elenco finche' il turno non
       termina, perche' e' proprio quella che serve sapere in sala. */
    @Transactional(readOnly = true)
    public List<Prenotazione> prenotazioniFutureDelRistorante(Long ristoranteId) {
        LocalDateTime adesso = LocalDateTime.now();
        return prenotazioneRepository
                .findByRistoranteIdAndStatusNotAndDataPrenotazioneGreaterThanEqualOrderByDataPrenotazioneAscOrarioPrenotazioneAsc(
                        ristoranteId, StatoPrenotazione.CANCELLED, adesso.toLocalDate().minusDays(1))
                .stream()
                .filter(p -> p.getFine().isAfter(adesso))
                .toList();
    }

    /* Le stesse, ma di un tavolo solo. Il tavolo si cerca filtrato dal
       ristorante: senza, cambiando il numero nell'indirizzo si leggerebbe
       l'agenda di un tavolo di un altro locale. */
    @Transactional(readOnly = true)
    public List<Prenotazione> prenotazioniFutureDelTavolo(Long ristoranteId, Long tavoloId) {
        tavoloRepository.findByIdAndRistoranteId(tavoloId, ristoranteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nessun tavolo con id " + tavoloId + " nel ristorante " + ristoranteId));

        LocalDateTime adesso = LocalDateTime.now();
        return prenotazioneRepository
                .findByTavoloIdAndStatusNotAndDataPrenotazioneGreaterThanEqualOrderByDataPrenotazioneAscOrarioPrenotazioneAsc(
                        tavoloId, StatoPrenotazione.CANCELLED, adesso.toLocalDate().minusDays(1))
                .stream()
                .filter(p -> p.getFine().isAfter(adesso))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Prenotazione> prenotazioniPerUtente(Long userId) {
        return prenotazioneRepository
                .findByUserIdOrderByDataPrenotazioneDescOrarioPrenotazioneDesc(userId);
    }
}
