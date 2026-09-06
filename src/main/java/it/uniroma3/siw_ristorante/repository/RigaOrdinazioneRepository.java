package it.uniroma3.siw_ristorante.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import it.uniroma3.siw_ristorante.model.RigaOrdinazione;

public interface RigaOrdinazioneRepository extends JpaRepository<RigaOrdinazione, Long>{
    boolean existsByPiattoId(Long piattoId);
}
