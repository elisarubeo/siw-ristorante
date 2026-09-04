package it.uniroma3.siw_ristorante.service;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.uniroma3.siw_ristorante.model.Ristorante;
import it.uniroma3.siw_ristorante.repository.RistoranteRepository;

@Service
public class RistoranteService {
    private final RistoranteRepository ristoranteRepository;

    public RistoranteService(RistoranteRepository ristoranteRepository) {
        this.ristoranteRepository = ristoranteRepository;
    }

    @Transactional(readOnly = true)
    public List<Ristorante> findAll() {
        return ristoranteRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Optional<Ristorante> findById(Long id) {
        return ristoranteRepository.findById(id);
    }

}
