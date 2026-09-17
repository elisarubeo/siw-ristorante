package it.uniroma3.siw_ristorante.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import it.uniroma3.siw_ristorante.model.Ordinazione;

public interface OrdinazioneRepository extends JpaRepository<Ordinazione, Long> {
    boolean existsByTavoloId(Long tavoloId);

    Optional<Ordinazione> findByTavoloId(Long tavoloId);

    /* Il conto con dentro tutto quello che la pagina di dettaglio mostra: il
       tavolo, le voci e il piatto di ogni voce. Senza il grafo sarebbero una
       query per la collezione piu' una per riga, perche' il nome del piatto
       non sta sulla riga ma sul piatto. Serve anche alla chiusura, che copia
       proprio quei nomi sullo scontrino. */
    @EntityGraph(attributePaths = { "tavolo", "righe", "righe.piatto" })
    Optional<Ordinazione> findByIdAndTavolo_Ristorante_Id(Long id, Long ristoranteId);

    /* L'elenco dei conti aperti mostra, per ognuno, il numero del tavolo e
       quante voci ha: entrambi si portano dietro dal principio, altrimenti
       sono due query per conto. */
    @EntityGraph(attributePaths = { "tavolo", "righe" })
    List<Ordinazione> findByTavolo_Ristorante_IdOrderByApertura(Long ristoranteId);
}
