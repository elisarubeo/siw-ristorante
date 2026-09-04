package it.uniroma3.siw_ristorante.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.uniroma3.siw_ristorante.exception.ResourceNotFoundException;
import it.uniroma3.siw_ristorante.model.Ristorante;
import it.uniroma3.siw_ristorante.model.Tavolo;
import it.uniroma3.siw_ristorante.repository.RistoranteRepository;
import it.uniroma3.siw_ristorante.repository.TavoloRepository;

@Service
public class TavoloService {
    private final TavoloRepository tavoloRepository;
    private final RistoranteRepository ristoranteRepository;

    public TavoloService(TavoloRepository tavoloRepository, RistoranteRepository ristoranteRepository) {
        this.tavoloRepository = tavoloRepository;
        this.ristoranteRepository = ristoranteRepository;
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
}
