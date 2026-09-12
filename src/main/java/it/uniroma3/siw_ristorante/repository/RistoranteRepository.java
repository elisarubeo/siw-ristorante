package it.uniroma3.siw_ristorante.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import it.uniroma3.siw_ristorante.model.Ristorante;

public interface RistoranteRepository extends JpaRepository<Ristorante, Long> {
    List<Ristorante> findByAttivoTrueOrderByNome();

    List<Ristorante> findAllByOrderByNome();

    Optional<Ristorante> findByGestoreId(Long gestoreId);

    boolean existsByNomeIgnoreCaseAndIndirizzoIgnoreCase(String nome, String indirizzo);

    @Query("select r from Ristorante r "
         + "where lower(r.nome) like :pattern "
         + "or lower(r.indirizzo) like :pattern "
         + "order by r.nome")
    List<Ristorante> search(@Param("pattern") String pattern);
}
