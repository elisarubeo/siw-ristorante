package it.uniroma3.siw_ristorante.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.uniroma3.siw_ristorante.exception.ResourceNotFoundException;
import it.uniroma3.siw_ristorante.model.Scontrino;
import it.uniroma3.siw_ristorante.repository.ScontrinoRepository;

/* Lo scontrino si legge soltanto: viene creato dalla chiusura di un conto, in
   OrdinazioneService, e da quel momento non si tocca piu'. */
@Service 
public class ScontrinoService {
    private final ScontrinoRepository scontrinoRepository;

    public ScontrinoService(ScontrinoRepository scontrinoRepository){
        this.scontrinoRepository = scontrinoRepository;
    }

    @Transactional(readOnly = true)
    public List<Scontrino> scontriniDelRistorante(Long ristoranteId){
        return scontrinoRepository.findByRistoranteIdOrderByDataOraDesc(ristoranteId);
    }

    @Transactional(readOnly = true)
    public Scontrino scontrino(Long ristoranteId, Long scontrinoId){
        return scontrinoRepository.findByIdAndRistoranteId(scontrinoId, ristoranteId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nessuno scontrino con id " + scontrinoId + " nel ristorante " + ristoranteId));
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> vociPerScontrino(Long ristoranteId) {
        return scontrinoRepository.vociPerScontrino(ristoranteId).stream()
                .collect(Collectors.toMap(ScontrinoRepository.ConteggioVoci::getScontrinoId,
                        ScontrinoRepository.ConteggioVoci::getVoci));
    }

    /* L'incasso mostrato in cima all'elenco: si somma cio' che si sta gia'
       mostrando, senza tornare al database. */
    public BigDecimal incasso(List<Scontrino> scontrini){
        return scontrini.stream()
                .map(Scontrino::getTotale)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
