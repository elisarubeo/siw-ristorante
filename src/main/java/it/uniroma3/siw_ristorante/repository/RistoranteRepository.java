package it.uniroma3.siw_ristorante.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.uniroma3.siw_ristorante.model.Ristorante;

public interface RistoranteRepository extends JpaRepository<Ristorante, Long> {

    /* Quello che vede il pubblico: i locali disattivati non compaiono. */
    List<Ristorante> findByAttivoTrueOrderByNome();

    List<Ristorante> findAllByOrderByNome();

    /* Il locale di chi lo gestisce: e' uno solo, per il vincolo di unicita'
       sulla colonna gestore_id. */
    Optional<Ristorante> findByGestoreId(Long gestoreId);

    boolean existsByNomeIgnoreCaseAndIndirizzoIgnoreCase(String nome, String indirizzo);
}
