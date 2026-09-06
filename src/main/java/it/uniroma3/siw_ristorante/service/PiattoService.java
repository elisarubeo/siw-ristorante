package it.uniroma3.siw_ristorante.service;

import it.uniroma3.siw_ristorante.repository.RigaOrdinazioneRepository;
import java.util.List;
import java.util.Optional;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.uniroma3.siw_ristorante.exception.EntityInUseException;
import it.uniroma3.siw_ristorante.exception.ResourceNotFoundException;
import it.uniroma3.siw_ristorante.model.Piatto;
import it.uniroma3.siw_ristorante.model.Ristorante;
import it.uniroma3.siw_ristorante.repository.PiattoRepository;
import it.uniroma3.siw_ristorante.repository.RistoranteRepository;

@Service
public class PiattoService {
    private final RigaOrdinazioneRepository rigaOrdinazioneRepository;
    private final PiattoRepository piattoRepository;
    private final RistoranteRepository ristoranteRepository;

    public PiattoService(PiattoRepository piattoRepository, RistoranteRepository ristoranteRepository, RigaOrdinazioneRepository rigaOrdinazioneRepository) {
        this.piattoRepository = piattoRepository;
        this.ristoranteRepository = ristoranteRepository;
        this.rigaOrdinazioneRepository = rigaOrdinazioneRepository;
    }

    @Transactional(readOnly = true)
    /* Escludendo il piatto stesso: senza, riaprire un piatto e salvarlo senza
       toccare il nome verrebbe respinto come duplicato di sé. */
    public boolean existsByNomeAndRistoranteIdExcluding(String nome, Long ristoranteId, Long piattoId){
        return piattoRepository.existsByNomeAndRistoranteIdAndIdNot(nome, ristoranteId, piattoId);
    }

    @Transactional(readOnly = true)
    public Optional<Piatto> findByIdAndRistoranteId(Long piattoId, Long ristoranteId){
        return piattoRepository.findByIdAndRistoranteId(piattoId, ristoranteId);
    }

    /* Tutto il menu, disponibili e non: e' la vista di chi amministra. */
    @Transactional(readOnly = true)
    public List<Piatto> getMenu(Long ristoranteId) {
        return piattoRepository.findByRistoranteId(ristoranteId);
    }

    /* Quello che vede un cliente: i piatti spenti non compaiono. */
    @Transactional(readOnly = true)
    public List<Piatto> getMenuDisponibile(Long ristoranteId) {
        return piattoRepository.findByRistoranteIdAndDisponibileTrue(ristoranteId);
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

    @Transactional(readOnly = true)
    public boolean haRigaOrdinazione(Long piattoid){
        return rigaOrdinazioneRepository.existsByPiattoId(piattoid);
    }

    @Transactional
    public void delete(Long ristoranteId, Long piattoId){
        Piatto piatto = piattoRepository.findByIdAndRistoranteId(piattoId, ristoranteId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Nessun piatto con id " + piattoId + " nel ristorante " + ristoranteId));
        // Controllo se il piatto compare in un'ordinazione esistente
        // Per farlo controllo RigaOrdinazione
        if(this.haRigaOrdinazione(piattoId)){
            throw new EntityInUseException(
                    "Il piatto " + piatto.getNome() + " ha un'ordinazione ancora aperta");
        }
        try {
            piattoRepository.delete(piatto);
            piattoRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new EntityInUseException("Il piatto " + piatto.getNome()
                    + " e' collegato ad altri dati e non puo' essere eliminato", e);
        }
    }

    @Transactional
    public void disattiva(Long ristoranteId, Long piattoId){
        Piatto piatto = piattoRepository.findByIdAndRistoranteId(piattoId, ristoranteId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Nessun piatto con id " + piattoId + " nel ristorante " + ristoranteId));
        piatto.setDisponibile(false);
    }

    @Transactional
    public void riattiva(Long ristoranteId, Long piattoId){
        Piatto piatto = piattoRepository.findByIdAndRistoranteId(piattoId, ristoranteId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Nessun piatto con id " + piattoId + " nel ristorante " + ristoranteId));
        piatto.setDisponibile(true);
    }

    @Transactional
    public Piatto update(Long ristoranteId, Long piattoId, Piatto data) {
        Piatto piatto = piattoRepository.findByIdAndRistoranteId(piattoId, ristoranteId)
            .orElseThrow(() -> new ResourceNotFoundException(
                "Nessun piatto con id " + piattoId + " nel ristorante " + ristoranteId));
        piatto.setNome(data.getNome());
        piatto.setIngredienti(data.getIngredienti());
        piatto.setPrezzo(data.getPrezzo());
        return piatto;
    }
}
