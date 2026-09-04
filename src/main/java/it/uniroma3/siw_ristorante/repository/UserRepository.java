package it.uniroma3.siw_ristorante.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import it.uniroma3.siw_ristorante.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    
}
