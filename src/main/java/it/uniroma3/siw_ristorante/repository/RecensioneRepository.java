package it.uniroma3.siw_ristorante.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import it.uniroma3.siw_ristorante.model.Recensione;

public interface RecensioneRepository extends JpaRepository<Recensione, Long> {

    /* Dalla piu' recente: e' l'ordine con cui si leggono le recensioni. Il
       secondo criterio serve perche' la data e' un giorno e non un istante:
       due recensioni dello stesso giorno resterebbero in ordine arbitrario. */
    List<Recensione> findByRistoranteIdOrderByDataDescIdDesc(Long ristoranteId);

    /* Ogni utente ha al massimo una recensione per ristorante. */
    boolean existsByUserIdAndRistoranteId(Long userId, Long ristoranteId);

    Optional<Recensione> findByUserIdAndRistoranteId(Long userId, Long ristoranteId);

    Optional<Recensione> findByIdAndRistoranteId(Long recensioneId, Long ristoranteId);

    /* Con l'autore nella query e non controllato dopo: cosi' la recensione di
       un altro utente non viene proprio caricata, e modificarla e' impossibile
       anche cambiando l'id nell'indirizzo. */
    Optional<Recensione> findByIdAndRistoranteIdAndUserId(Long recensioneId, Long ristoranteId, Long userId);

    /* La media la calcola il database: le recensioni possono essere tante e
       non serve portarle tutte in memoria per farne la media. Il tipo e'
       Double e non double perche' senza recensioni il risultato e' null. */
    @Query("select avg(r.voto) from Recensione r where r.ristorante.id = ?1")
    Double mediaVoti(Long ristoranteId);
}
