package it.uniroma3.siw_ristorante.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.uniroma3.siw_ristorante.model.Tavolo;

public interface TavoloRepository extends JpaRepository<Tavolo, Long> {

    List<Tavolo> findByRistoranteIdOrderByNumeroTavolo(Long ristoranteId);

    Optional<Tavolo> findByIdAndRistoranteId(Long id, Long ristoranteId);

    // Ordinati per posti crescenti: il primo che basta e' il piu' piccolo che basta
    List<Tavolo> findByRistoranteIdAndNumeroPostiGreaterThanEqualOrderByNumeroPostiAsc(
            Long ristoranteId, Integer numeroPersone);

    boolean existsByNumeroTavoloAndRistoranteId(Integer numeroTavolo, Long ristoranteId);

}
