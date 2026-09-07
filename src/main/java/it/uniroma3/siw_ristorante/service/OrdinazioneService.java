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
import it.uniroma3.siw_ristorante.model.Tavolo;
import it.uniroma3.siw_ristorante.repository.OrdinazioneRepository;
import it.uniroma3.siw_ristorante.repository.TavoloRepository;

@Service
public class OrdinazioneService {
    private final OrdinazioneRepository ordinazioneRepository;
    private final TavoloRepository tavoloRepository;

    public OrdinazioneService(OrdinazioneRepository ordinazioneRepository, TavoloRepository tavoloRepository){
        this.ordinazioneRepository = ordinazioneRepository;
        this.tavoloRepository = tavoloRepository;
    }

    @Transactional
    public Ordinazione apriOrdinazione(Long ristoranteId, Long tavoloId){
        // 1. Controllo se esiste il tavolo nel ristorante
        Tavolo tavolo = tavoloRepository.findByIdAndRistoranteId(tavoloId, ristoranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Il tavolo selezionato non esiste"));
        // 2. Posso aprire un'ordinazione solo se non ce ne sono altre aperte per il tavolo
        if(ordinazioneRepository.findByTavoloIdAndChiusuraIsNull(tavoloId).isPresent()){
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
                .findByTavolo_Ristorante_IdAndChiusuraIsNullOrderByApertura(ristoranteId);
    }

    @Transactional(readOnly = true)
    public Map<Long, Ordinazione> contiApertiPerTavolo(Long ristoranteId) {
        return contiAperti(ristoranteId).stream()
                .collect(Collectors.toMap(o -> o.getTavolo().getId(), Function.identity(),
                        (primo, secondo) -> primo, LinkedHashMap::new));
    }
}
