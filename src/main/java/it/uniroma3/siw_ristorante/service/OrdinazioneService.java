package it.uniroma3.siw_ristorante.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.uniroma3.siw_ristorante.exception.OrdinazioneNonValidaException;
import it.uniroma3.siw_ristorante.exception.ResourceNotFoundException;
import it.uniroma3.siw_ristorante.model.Ordinazione;
import it.uniroma3.siw_ristorante.model.Piatto;
import it.uniroma3.siw_ristorante.model.RigaOrdinazione;
import it.uniroma3.siw_ristorante.model.RigaScontrino;
import it.uniroma3.siw_ristorante.model.Scontrino;
import it.uniroma3.siw_ristorante.model.Tavolo;
import it.uniroma3.siw_ristorante.repository.OrdinazioneRepository;
import it.uniroma3.siw_ristorante.repository.PiattoRepository;
import it.uniroma3.siw_ristorante.repository.ScontrinoRepository;
import it.uniroma3.siw_ristorante.repository.TavoloRepository;

@Service
public class OrdinazioneService {
    private final OrdinazioneRepository ordinazioneRepository;
    private final TavoloRepository tavoloRepository;
    private final ScontrinoRepository scontrinoRepository;
    private final PiattoRepository piattoRepository;

    public OrdinazioneService(OrdinazioneRepository ordinazioneRepository, TavoloRepository tavoloRepository,
            ScontrinoRepository scontrinoRepository, PiattoRepository piattoRepository){
        this.ordinazioneRepository = ordinazioneRepository;
        this.tavoloRepository = tavoloRepository;
        this.scontrinoRepository = scontrinoRepository;
        this.piattoRepository = piattoRepository;
    }

    @Transactional
    public Ordinazione apriOrdinazione(Long ristoranteId, Long tavoloId){
        // 1. Controllo se esiste il tavolo nel ristorante
        Tavolo tavolo = tavoloRepository.findByIdAndRistoranteId(tavoloId, ristoranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Il tavolo selezionato non esiste"));
        // 2. Posso aprire un'ordinazione solo se non ce ne sono altre aperte per il tavolo
        if(ordinazioneRepository.existsByTavoloId(tavoloId)){
            throw new OrdinazioneNonValidaException("Questo tavolo ha già un'ordinazione aperta");
        }
        Ordinazione ordinazione = new Ordinazione();
        ordinazione.setApertura(LocalDateTime.now());
        ordinazione.setTavolo(tavolo);
        ordinazione.setTotale(BigDecimal.ZERO);

        return ordinazioneRepository.save(ordinazione);
    }

    @Transactional(readOnly = true)
    public Ordinazione conto(Long ristoranteId, Long ordinazioneId) {
        return ordinazioneRepository.findByIdAndTavolo_Ristorante_Id(ordinazioneId, ristoranteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nessuna ordinazione con id " + ordinazioneId + " nel ristorante " + ristoranteId));
    }

    @Transactional(readOnly = true)
    public List<Ordinazione> contiAperti(Long ristoranteId) {
        return ordinazioneRepository
                .findByTavolo_Ristorante_IdOrderByApertura(ristoranteId);
    }

    @Transactional
    public void annullaApertura(Long ristoranteId, Long ordinazioneId) {
        Ordinazione ordinazione = conto(ristoranteId, ordinazioneId);
        if (!ordinazione.getRighe().isEmpty()) {
            throw new OrdinazioneNonValidaException(
                    "Questo conto ha gia' delle voci: va chiuso, non annullato");
        }
        ordinazioneRepository.delete(ordinazione);
    }

    @Transactional(readOnly = true)
    public Map<Long, Ordinazione> contiApertiPerTavolo(Long ristoranteId) {
        return contiAperti(ristoranteId).stream()
                .collect(Collectors.toMap(o -> o.getTavolo().getId(), Function.identity(),
                        (primo, secondo) -> primo, LinkedHashMap::new));
    }

    /* Il vincolo unico (ordinazione_id, piatto_id) vieta due righe per lo stesso
       piatto: aggiungere una seconda volta non crea una riga, somma alla
       quantita' di quella che c'e' gia'. */
    @Transactional
    public Ordinazione aggiungiPiatto(Long ristoranteId, Long ordinazioneId, Long piattoId, Integer quantita) {
        Ordinazione ordinazione = conto(ristoranteId, ordinazioneId);
        verificaQuantita(quantita);

        /* Cercato nel menu di QUESTO ristorante: piattoId arriva dal modulo, e
           senza il filtro basterebbe cambiare un numero per mettere sul conto
           il piatto di un altro locale, col suo prezzo. */
        Piatto piatto = piattoRepository.findByIdAndRistoranteId(piattoId, ristoranteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nessun piatto con id " + piattoId + " nel menu del ristorante " + ristoranteId));

        if (!piatto.isDisponibile()) {
            throw new OrdinazioneNonValidaException(
                    "Il piatto " + piatto.getNome() + " non e' disponibile");
        }

        Optional<RigaOrdinazione> esistente = cercaRiga(ordinazione, piattoId);
        if (esistente.isPresent()) {
            /* Cresce solo la quantita': il prezzo resta quello della prima
               aggiunta, altrimenti un ritocco al listino ri-prezzerebbe piatti
               gia' serviti. */
            RigaOrdinazione riga = esistente.get();
            riga.setQuantita(riga.getQuantita() + quantita);
        } else {
            RigaOrdinazione riga = new RigaOrdinazione();
            riga.setPiatto(piatto);
            riga.setQuantita(quantita);
            riga.setPrezzoUnitario(piatto.getPrezzo());
            ordinazione.aggiungiRiga(riga);
        }

        aggiornaTotale(ordinazione);
        return ordinazione;
    }

    /* Non esiste "togli la riga": si diminuisce la quantita', e quando arriva a
       zero la riga sparisce. Cosi' finche' il piatto resta sul conto il suo
       prezzo e' quello della prima aggiunta, e nel database non restano righe a
       zero che ogni lettura dovrebbe ricordarsi di ignorare. */
    @Transactional
    public Ordinazione diminuisciPiatto(Long ristoranteId, Long ordinazioneId, Long piattoId, Integer quantita) {
        Ordinazione ordinazione = conto(ristoranteId, ordinazioneId);
        verificaQuantita(quantita);

        RigaOrdinazione riga = cercaRiga(ordinazione, piattoId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Questo piatto non e' sul conto " + ordinazioneId));

        int rimaste = riga.getQuantita() - quantita;
        if (rimaste > 0) {
            riga.setQuantita(rimaste);
        } else {
            /* rimuoviRiga e non rigaOrdinazioneRepository.delete: staccandola
               dalla collezione l'orphanRemoval la cancella E il totale
               ricalcolato subito dopo non la conta piu'. Col repository
               sparirebbe dal database ma resterebbe in memoria. */
            ordinazione.rimuoviRiga(riga);
        }

        aggiornaTotale(ordinazione);
        return ordinazione;
    }

    /* La riga si cerca dentro l'ordinazione e non nel repository: la collezione
       serve comunque per ricalcolare il totale, e cosi' e' impossibile toccare
       la riga di un altro conto. */
    private Optional<RigaOrdinazione> cercaRiga(Ordinazione ordinazione, Long piattoId) {
        return ordinazione.getRighe().stream()
                .filter(riga -> piattoId.equals(riga.getPiatto().getId()))
                .findFirst();
    }

    private void verificaQuantita(Integer quantita) {
        if (quantita == null || quantita < 1) {
            throw new OrdinazioneNonValidaException("La quantita' deve essere almeno 1");
        }
    }

    /* Il totale memorizzato e' derivato: va riscritto dopo ogni modifica delle
       righe. Se ci si dimentica, lo scontrino resta comunque giusto perche' la
       chiusura ricalcola invece di copiare. */
    private void aggiornaTotale(Ordinazione ordinazione) {
        ordinazione.setTotale(ordinazione.calcolaTotaleRighe());
    }

    @Transactional
    public Scontrino chiudi(Long ristoranteId, Long ordinazioneId){
        Ordinazione ordinazione = conto(ristoranteId, ordinazioneId);

        if (ordinazione.getRighe().isEmpty()) {
            throw new OrdinazioneNonValidaException(
                    "Questo conto non ha voci: va annullato, non chiuso");
        }

        Tavolo tavolo = ordinazione.getTavolo();

        Scontrino scontrino = new Scontrino();
        scontrino.setDataOra(LocalDateTime.now());
        scontrino.setApertura(ordinazione.getApertura());
        scontrino.setNumeroTavolo(tavolo.getNumeroTavolo());
        scontrino.setRistorante(tavolo.getRistorante());

        for (RigaOrdinazione riga : ordinazione.getRighe()) {
            RigaScontrino copia = new RigaScontrino();
            copia.setNomePiatto(riga.getPiatto().getNome());
            copia.setPrezzoUnitario(riga.getPrezzoUnitario());
            copia.setQuantita(riga.getQuantita());
            copia.setPiattoId(riga.getPiatto().getId());
            scontrino.aggiungiRiga(copia);
        }

        scontrino.setTotale(scontrino.calcolaTotaleRighe());

        Scontrino emesso = scontrinoRepository.save(scontrino);
        ordinazioneRepository.delete(ordinazione);
        ordinazioneRepository.flush();

        return emesso;
    }
}
