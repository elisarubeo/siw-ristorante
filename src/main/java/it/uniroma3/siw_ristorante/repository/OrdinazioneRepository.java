package it.uniroma3.siw_ristorante.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.uniroma3.siw_ristorante.model.Ordinazione;

public interface OrdinazioneRepository extends JpaRepository<Ordinazione, Long> {
    boolean existsByTavoloId(Long tavoloId);

    Optional<Ordinazione> findByTavoloId(Long tavoloId);

    Optional<Ordinazione> findByIdAndTavolo_Ristorante_Id(Long id, Long ristoranteId);

    List<Ordinazione> findByTavolo_Ristorante_IdOrderByApertura(Long ristoranteId);
}
