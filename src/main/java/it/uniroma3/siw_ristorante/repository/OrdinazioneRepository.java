package it.uniroma3.siw_ristorante.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import it.uniroma3.siw_ristorante.model.Ordinazione;

public interface OrdinazioneRepository extends JpaRepository<Ordinazione, Long> {

    /* Aperta significa non ancora chiusa: e' la chiusura nulla a dirlo,
       non l'apertura, che c'e' sempre. */
    boolean existsByTavoloIdAndChiusuraIsNull(Long tavoloId);

    boolean existsByTavoloId(Long tavoloId);

    List<Ordinazione> findByTavoloId(Long tavoloId);
}
