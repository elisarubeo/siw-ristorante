package it.uniroma3.siw_ristorante.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.uniroma3.siw_ristorante.exception.RecensioneNonValidaException;
import it.uniroma3.siw_ristorante.exception.ResourceNotFoundException;
import it.uniroma3.siw_ristorante.model.Recensione;
import it.uniroma3.siw_ristorante.model.Ristorante;
import it.uniroma3.siw_ristorante.model.User;
import it.uniroma3.siw_ristorante.repository.RecensioneRepository;
import it.uniroma3.siw_ristorante.repository.RistoranteRepository;

@Service
public class RecensioneService {
    private final RecensioneRepository recensioneRepository;
    private final RistoranteRepository ristoranteRepository;

    public RecensioneService(RecensioneRepository recensioneRepository, RistoranteRepository ristoranteRepository) {
        this.recensioneRepository = recensioneRepository;
        this.ristoranteRepository = ristoranteRepository;
    }

    @Transactional(readOnly = true)
    public List<Recensione> recensioniDelRistorante(Long ristoranteId) {
        return recensioneRepository.findByRistoranteIdOrderByDataDescIdDesc(ristoranteId);
    }

    /* null quando non c'e' ancora nessuna recensione: la pagina in quel caso
       non mostra nessuna media, che e' diverso da una media pari a zero. */
    @Transactional(readOnly = true)
    public Double mediaVoti(Long ristoranteId) {
        return recensioneRepository.mediaVoti(ristoranteId);
    }

    /* Ogni utente puo' lasciare una sola recensione per ristorante. */
    @Transactional(readOnly = true)
    public boolean haGiaRecensito(Long userId, Long ristoranteId) {
        return recensioneRepository.existsByUserIdAndRistoranteId(userId, ristoranteId);
    }

    @Transactional(readOnly = true)
    public Optional<Recensione> recensioneDiUtente(Long userId, Long ristoranteId) {
        return recensioneRepository.findByUserIdAndRistoranteId(userId, ristoranteId);
    }

    /* Solo la propria: l'autore fa parte della ricerca, quindi la recensione
       di un altro risulta inesistente invece che negata. */
    @Transactional(readOnly = true)
    public Recensione miaRecensione(Long ristoranteId, Long recensioneId, User autore) {
        return recensioneRepository.findByIdAndRistoranteIdAndUserId(recensioneId, ristoranteId, autore.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nessuna tua recensione con id " + recensioneId + " per il ristorante " + ristoranteId));
    }

    @Transactional
    public Recensione save(Long ristoranteId, User autore, Recensione recensione) {
        Ristorante ristorante = ristoranteRepository.findById(ristoranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Nessun ristorante con id " + ristoranteId));

        /* Il controllo c'e' anche nel controller, per mostrare l'errore dentro
           il modulo: qui e' l'ultima difesa, perche' il servizio non puo'
           dipendere dal fatto che chi lo chiama abbia gia' controllato. */
        if (recensioneRepository.existsByUserIdAndRistoranteId(autore.getId(), ristoranteId)) {
            throw new RecensioneNonValidaException("Hai gia' recensito questo ristorante");
        }

        recensione.setRistorante(ristorante);
        recensione.setUser(autore);
        recensione.setData(LocalDate.now());
        return recensioneRepository.save(recensione);
    }

    /* Solo l'autore cancella la sua: miaRecensione fa gia' il controllo, e su
       una recensione altrui la cancellazione non avviene perche' la recensione
       non viene proprio trovata. */
    @Transactional
    public void delete(Long ristoranteId, Long recensioneId, User autore) {
        recensioneRepository.delete(this.miaRecensione(ristoranteId, recensioneId, autore));
    }

    /* Niente save() finale: dentro la transazione l'entita' e' gestita da
       Hibernate, che al termine scrive da se' i campi cambiati. */
    @Transactional
    public Recensione update(Long ristoranteId, Long recensioneId, User autore, Recensione data) {
        Recensione recensione = this.miaRecensione(ristoranteId, recensioneId, autore);
        recensione.setTitolo(data.getTitolo());
        recensione.setVoto(data.getVoto());
        recensione.setTesto(data.getTesto());
        recensione.setData(LocalDate.now());
        return recensione;
    }
}
