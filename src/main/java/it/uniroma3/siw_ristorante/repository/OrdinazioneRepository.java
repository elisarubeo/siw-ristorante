package it.uniroma3.siw_ristorante.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.uniroma3.siw_ristorante.model.Ordinazione;

public interface OrdinazioneRepository extends JpaRepository<Ordinazione, Long> {

    /* Aperta significa non ancora chiusa: e' la chiusura nulla a dirlo,
       non l'apertura, che c'e' sempre. */
    boolean existsByTavoloIdAndChiusuraIsNull(Long tavoloId);

    boolean existsByTavoloId(Long tavoloId);

    List<Ordinazione> findByTavoloId(Long tavoloId);

    /* Il conto filtrato dal ristorante dell'indirizzo. L'ordinazione non ha un
       riferimento diretto al ristorante: ci si arriva attraversando il tavolo,
       e gli underscore separano i passi del cammino (tavolo.ristorante.id).
       Un parametro per ogni proprieta' nominata, in quello stesso ordine. */
    Optional<Ordinazione> findByIdAndTavolo_Ristorante_Id(Long id, Long ristoranteId);

    /* IsNull non consuma parametri: la condizione e' gia' scritta nel nome. */
    Optional<Ordinazione> findByTavoloIdAndChiusuraIsNull(Long tavoloId);

    List<Ordinazione> findByTavolo_Ristorante_IdAndChiusuraIsNullOrderByApertura(Long ristoranteId);
}
