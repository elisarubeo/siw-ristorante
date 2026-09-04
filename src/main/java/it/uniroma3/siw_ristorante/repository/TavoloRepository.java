package it.uniroma3.siw_ristorante.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import it.uniroma3.siw_ristorante.model.Tavolo;

public interface TavoloRepository extends JpaRepository<Tavolo, Long> {

    List<Tavolo> findByRistoranteIdOrderByNumeroTavolo(Long ristoranteId);

    boolean existsByNumeroTavoloAndRistoranteId(Integer numeroTavolo, Long ristoranteId);
}
