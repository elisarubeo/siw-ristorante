package it.uniroma3.siw_ristorante.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.uniroma3.siw_ristorante.model.Credentials;

public interface CredentialsRepository extends JpaRepository<Credentials, Long> {
    public Optional<Credentials> findByUsername(String username);

    /* Tutte insieme e non una per volta: l'agenda mostra decine di
       prenotazioni, e una query per riga sarebbe una query per riga. */
    List<Credentials> findByUserIdIn(Collection<Long> userIds);

    /* Le credenziali di un utente, partendo dall'utente. Serve perche' il
       verso dell'associazione e' l'altro: Credentials conosce il suo User, ma
       User non conosce le sue Credentials, e dal ristorante si arriva al
       gestore (uno User), non al suo account. */
    Optional<Credentials> findByUserId(Long userId);

}