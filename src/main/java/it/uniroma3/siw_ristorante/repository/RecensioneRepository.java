package it.uniroma3.siw_ristorante.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import it.uniroma3.siw_ristorante.model.Recensione;

public interface RecensioneRepository extends JpaRepository<Recensione, Long> {
    List<Recensione> findByRistoranteIdOrderByDataDescIdDesc(Long ristoranteId);

    boolean existsByUserIdAndRistoranteId(Long userId, Long ristoranteId);

    Optional<Recensione> findByUserIdAndRistoranteId(Long userId, Long ristoranteId);

    Optional<Recensione> findByIdAndRistoranteId(Long recensioneId, Long ristoranteId);

    Optional<Recensione> findByIdAndRistoranteIdAndUserId(Long recensioneId, Long ristoranteId, Long userId);

    @Query("select avg(r.voto) from Recensione r where r.ristorante.id = ?1")
    Double mediaVoti(Long ristoranteId);

    long countByRistoranteId(Long ristoranteId);

    @Query("""
            select r.voto as voto, count(r) as quantita
            from Recensione r
            where r.ristorante.id = ?1
            group by r.voto
            order by r.voto
            """)
    List<AggregatoVoto> distribuzioneVoti(Long ristoranteId);

    interface AggregatoVoto {
        Integer getVoto();

        Long getQuantita();
    }

    @Query("""
        select r.id as id, r.nome as nome, r.indirizzo as indirizzo,
               avg(rec.voto) as media, count(rec) as quantita
        from Recensione rec join rec.ristorante r
        where r.attivo = true
        group by r.id, r.nome, r.indirizzo
        having count(rec) >= ?1
        order by avg(rec.voto) desc, count(rec) desc, r.nome
        """)
   List<RistoranteInClassifica> classifica(long minimoRecensioni, Limit quanti);

   interface RistoranteInClassifica {
      Long getId();
      String getNome();
      String getIndirizzo();
      Double getMedia();
      Long getQuantita();
   }
}
