package it.uniroma3.siw_ristorante.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.uniroma3.siw_ristorante.exception.ResourceNotFoundException;
import it.uniroma3.siw_ristorante.model.Piatto;
import it.uniroma3.siw_ristorante.model.Ristorante;
import it.uniroma3.siw_ristorante.repository.PiattoRepository;
import it.uniroma3.siw_ristorante.repository.RistoranteRepository;

@Service
public class PiattoService {
    private final PiattoRepository piattoRepository;
    private final RistoranteRepository ristoranteRepository;

    public PiattoService(PiattoRepository piattoRepository, RistoranteRepository ristoranteRepository) {
        this.piattoRepository = piattoRepository;
        this.ristoranteRepository = ristoranteRepository;
    }

    @Transactional(readOnly = true)
    public List<Piatto> getMenu(Long ristoranteId) {
        return piattoRepository.findByRistoranteId(ristoranteId);
    }

    @Transactional(readOnly = true)
    public boolean esisteNelMenu(Long ristoranteId, String nome) {
        return piattoRepository.existsByNomeAndRistoranteId(nome, ristoranteId);
    }

    @Transactional
    public Piatto save(Long ristoranteId, Piatto piatto) {
        Ristorante ristorante = ristoranteRepository.findById(ristoranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Nessun ristorante con id " + ristoranteId));

        if (piattoRepository.existsByNomeAndRistoranteId(piatto.getNome(), ristoranteId)) {
            throw new IllegalStateException(
                    "Esiste gia' un piatto di nome " + piatto.getNome() + " nel menu del ristorante " + ristoranteId);
        }

        ristorante.aggiungiPiatto(piatto);
        return piattoRepository.save(piatto);
    }
}
