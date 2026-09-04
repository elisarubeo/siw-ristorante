package it.uniroma3.siw_ristorante.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import it.uniroma3.siw_ristorante.model.Piatto;

public interface PiattoRepository extends JpaRepository<Piatto, Long> {

    List<Piatto> findByRistoranteId(Long ristoranteId);

    boolean existsByNomeAndRistoranteId(String nome, Long ristoranteId);
}
