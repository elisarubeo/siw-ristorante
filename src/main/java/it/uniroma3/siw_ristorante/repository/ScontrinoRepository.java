package it.uniroma3.siw_ristorante.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.uniroma3.siw_ristorante.model.Scontrino;

public interface ScontrinoRepository extends JpaRepository<Scontrino, Long>{

    /* Ordinati per data del pagamento, dal piu' recente: e' l'ordine in cui si
       guarda un registro di cassa. Non per apertura, che e' ammessa nulla e
       manderebbe in fondo proprio gli scontrini piu' vecchi. */
    List<Scontrino> findByRistoranteIdOrderByDataOraDesc(Long ristoranteId);

    /* Filtrato dal ristorante, mai findById nudo: cambiando il numero
       nell'indirizzo non si legge la cassa di un altro locale. */
    Optional<Scontrino> findByIdAndRistoranteId(Long id, Long ristoranteId);
}
