package it.uniroma3.siw_ristorante.service;

import java.util.List;

import org.springframework.stereotype.Service;

import it.uniroma3.siw_ristorante.model.Piatto;
import it.uniroma3.siw_ristorante.repository.PiattoRepository;

@Service
public class PiattoService {
    private final PiattoRepository piattoRepository;

    public PiattoService(PiattoRepository piattoRepository) {
        this.piattoRepository = piattoRepository;
    }

    public List<Piatto> getMenu(Long ristoranteId) {
        return piattoRepository.findByRistoranteId(ristoranteId);
    }
}
