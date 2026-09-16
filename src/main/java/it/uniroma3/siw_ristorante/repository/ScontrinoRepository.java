package it.uniroma3.siw_ristorante.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import it.uniroma3.siw_ristorante.model.Scontrino;

public interface ScontrinoRepository extends JpaRepository<Scontrino, Long>{

    /* Ordinati per data del pagamento, dal piu' recente */
    List<Scontrino> findByRistoranteIdOrderByDataOraDesc(Long ristoranteId);

    /* Filtrato dal ristorante: cambiando il numero
       nell'indirizzo non si legge la cassa di un altro locale. */
    Optional<Scontrino> findByIdAndRistoranteId(Long id, Long ristoranteId);

    /* Quante voci ha ogni scontrino, contate dal database. Senza, l'elenco
       chiede la collezione righe uno scontrino per volta */
    @Query("""
            select r.scontrino.id as scontrinoId, count(r) as voci
            from RigaScontrino r
            where r.scontrino.ristorante.id = ?1
            group by r.scontrino.id
            """)
    List<ConteggioVoci> vociPerScontrino(Long ristoranteId);

    interface ConteggioVoci {
        Long getScontrinoId();

        Long getVoci();
    }

    /* ================= statistiche =================
       Tutte aggregazioni: contano e sommano nel database e riportano poche
       righe, invece di portare in memoria centinaia di scontrini per fare la
       somma qui. La differenza si vede quando il locale lavora da qualche anno.

       I risultati arrivano come PROIEZIONI (le interfacce in fondo) e non come
       Object[]: l'alias della select diventa il nome del getter, e chi legge il
       service trova getIncasso() invece di riga[2] con un cast. */

    @Query("""
            select distinct year(s.dataOra)
            from Scontrino s
            where s.ristorante.id = ?1
            order by year(s.dataOra) desc
            """)
    List<Integer> anniConScontrini(Long ristoranteId);

    @Query("select count(s) from Scontrino s where s.ristorante.id = ?1 and year(s.dataOra) = ?2")
    long contaPerAnno(Long ristoranteId, int anno);

    @Query("select sum(s.totale) from Scontrino s where s.ristorante.id = ?1 and year(s.dataOra) = ?2")
    BigDecimal incassoPerAnno(Long ristoranteId, int anno);

    @Query("""
            select sum(r.quantita)
            from RigaScontrino r
            where r.scontrino.ristorante.id = ?1 and year(r.scontrino.dataOra) = ?2
            """)
    Long pietanzeVendutePerAnno(Long ristoranteId, int anno);

    @Query("""
            select avg(timestampdiff(minute, s.apertura, s.dataOra))
            from Scontrino s
            where s.ristorante.id = ?1 and year(s.dataOra) = ?2 and s.apertura is not null
            """)
    Double durataMediaMinuti(Long ristoranteId, int anno);

    @Query("""
            select month(s.dataOra) as mese, count(s) as numero, sum(s.totale) as incasso
            from Scontrino s
            where s.ristorante.id = ?1 and year(s.dataOra) = ?2
            group by month(s.dataOra)
            order by month(s.dataOra)
            """)
    List<AggregatoMese> perMese(Long ristoranteId, int anno);

    @Query("""
            select hour(coalesce(s.apertura, s.dataOra)) as ora, count(s) as numero
            from Scontrino s
            where s.ristorante.id = ?1 and year(s.dataOra) = ?2
            group by hour(coalesce(s.apertura, s.dataOra))
            order by hour(coalesce(s.apertura, s.dataOra))
            """)
    List<AggregatoOra> perOra(Long ristoranteId, int anno);
    
    @Query("""
            select r.piattoId as piattoId, max(r.nomePiatto) as nome,
                   sum(r.quantita) as quantita,
                   sum(r.prezzoUnitario * r.quantita) as incasso
            from RigaScontrino r
            where r.scontrino.ristorante.id = ?1 and year(r.scontrino.dataOra) = ?2
            group by r.piattoId
            order by sum(r.quantita) desc, sum(r.prezzoUnitario * r.quantita) desc
            """)
    List<AggregatoPiatto> piattiPiuVenduti(Long ristoranteId, int anno, Pageable limite);

    /* Data e totale di ogni scontrino dell'anno, senza aggregare. */
    @Query("""
            select s.dataOra as dataOra, s.totale as totale
            from Scontrino s
            where s.ristorante.id = ?1 and year(s.dataOra) = ?2
            """)
    List<RigaCassa> cassaDellAnno(Long ristoranteId, int anno);

    interface AggregatoMese {
        Integer getMese();

        Long getNumero();

        BigDecimal getIncasso();
    }

    interface AggregatoOra {
        Integer getOra();

        Long getNumero();
    }

    interface AggregatoPiatto {
        Long getPiattoId();

        String getNome();

        Long getQuantita();

        BigDecimal getIncasso();
    }

    interface RigaCassa {
        LocalDateTime getDataOra();

        BigDecimal getTotale();
    }
}
