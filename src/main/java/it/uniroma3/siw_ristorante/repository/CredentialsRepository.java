package it.uniroma3.siw_ristorante.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import it.uniroma3.siw_ristorante.model.Credentials;

public interface CredentialsRepository extends JpaRepository<Credentials, Long> {
    public Optional<Credentials> findByUsername(String username);

}