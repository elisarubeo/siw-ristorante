package it.uniroma3.siw_ristorante.service;

import it.uniroma3.siw_ristorante.repository.RigaOrdinazioneRepository;
import java.util.List;

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
            /* Il flush forza subito la scrittura: senza, un'eventuale violazione
               di vincolo emergerebbe al commit, fuori da questo try, e
               diventerebbe una pagina di errore invece di un messaggio. */
            piattoRepository.flush();
        } catch (DataIntegrityViolationException e) {
            /* Oggi non scatta, perche' RigaOrdinazione e' l'unica cosa che
               punta al piatto ed e' gia' controllata sopra. Resta come rete se
               un domani qualcos'altro lo referenziasse. */
            throw new EntityInUseException("Il piatto " + piatto.getNome()
                    + " e' collegato ad altri dati e non puo' essere eliminato", e);
        }
    }
}
