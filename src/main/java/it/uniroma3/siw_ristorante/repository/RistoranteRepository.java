package it.uniroma3.siw_ristorante.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import it.uniroma3.siw_ristorante.model.Ristorante;

public interface RistoranteRepository extends JpaRepository<Ristorante, Long> {
}
