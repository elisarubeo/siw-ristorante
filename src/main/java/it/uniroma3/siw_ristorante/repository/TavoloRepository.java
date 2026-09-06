package it.uniroma3.siw_ristorante.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.uniroma3.siw_ristorante.model.Tavolo;

public interface TavoloRepository extends JpaRepository<Tavolo, Long> {

    List<Tavolo> findByRistoranteIdOrderByNumeroTavolo(Long ristoranteId);

    /* Cercare il tavolo per id e ristorante insieme: cosi' l'indirizzo
       /ristoranti/1/tavoli/5 non puo' agire su un tavolo del ristorante 2. */
    Optional<Tavolo> findByIdAndRistoranteId(Long id, Long ristoranteId);

    boolean existsByNumeroTavoloAndRistoranteId(Integer numeroTavolo, Long ristoranteId);
}
