package it.uniroma3.siw_ristorante.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.uniroma3.siw_ristorante.exception.OrdinazioneNonValidaException;
import it.uniroma3.siw_ristorante.exception.ResourceNotFoundException;
import it.uniroma3.siw_ristorante.model.Ordinazione;
import it.uniroma3.siw_ristorante.model.RigaOrdinazione;
import it.uniroma3.siw_ristorante.model.RigaScontrino;
import it.uniroma3.siw_ristorante.model.Scontrino;
import it.uniroma3.siw_ristorante.model.Tavolo;
import it.uniroma3.siw_ristorante.repository.OrdinazioneRepository;
import it.uniroma3.siw_ristorante.repository.ScontrinoRepository;
import it.uniroma3.siw_ristorante.repository.TavoloRepository;

@Service
public class OrdinazioneService {
    private final OrdinazioneRepository ordinazioneRepository;
    private final TavoloRepository tavoloRepository;
    private final ScontrinoRepository scontrinoRepository;

    public OrdinazioneService(OrdinazioneRepository ordinazioneRepository, TavoloRepository tavoloRepository,
            ScontrinoRepository scontrinoRepository){
        this.ordinazioneRepository = ordinazioneRepository;
        this.tavoloRepository = tavoloRepository;
        this.scontrinoRepository = scontrinoRepository;
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
