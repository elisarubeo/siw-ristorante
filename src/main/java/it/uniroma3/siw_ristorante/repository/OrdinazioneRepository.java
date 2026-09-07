package it.uniroma3.siw_ristorante.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.uniroma3.siw_ristorante.model.Ordinazione;

public interface OrdinazioneRepository extends JpaRepository<Ordinazione, Long> {

    /* Un'ordinazione esiste solo se il conto e' aperto, quindi non serve piu'
       chiedersi se e' chiusa: "esiste" e "aperta" sono la stessa domanda. */
    boolean existsByTavoloId(Long tavoloId);

    /* Optional e non List: il vincolo unico su tavolo_id garantisce che ce ne
       sia al massimo una. */
    Optional<Ordinazione> findByTavoloId(Long tavoloId);

    Optional<Ordinazione> findByIdAndTavolo_Ristorante_Id(Long id, Long ristoranteId);

    List<Ordinazione> findByTavolo_Ristorante_IdOrderByApertura(Long ristoranteId);
}
