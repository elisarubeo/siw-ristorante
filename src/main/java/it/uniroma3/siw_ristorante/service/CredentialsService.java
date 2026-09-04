package it.uniroma3.siw_ristorante.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.uniroma3.siw_ristorante.model.Credentials;
import it.uniroma3.siw_ristorante.model.User;
import it.uniroma3.siw_ristorante.repository.CredentialsRepository;

@Service
public class CredentialsService {
    private PasswordEncoder passwordEncoder;
    private CredentialsRepository credentialsRepository;

    public CredentialsService(CredentialsRepository credentialsRepository,
                              PasswordEncoder passwordEncoder) {
        this.credentialsRepository = credentialsRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Credentials getCredentials(Long id) {
        return credentialsRepository.findById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public Optional<Credentials> getCredentials(String username) {
        return credentialsRepository.findByUsername(username);
    }

    @Transactional
    public Credentials saveCredentials(Credentials credentials) {
        return credentialsRepository.save(credentials);
    }

    @Transactional
    public Credentials registerUser(User user, Credentials credentials) {
        // la password va cifrata: al login Spring confronta gli hash
        credentials.setPassword(passwordEncoder.encode(credentials.getPassword()));
        // il ruolo lo decide il server, mai la form
        credentials.setRole(Credentials.DEFAULT_ROLE);
        // la cascata su Credentials.user salva anche lo User
        credentials.setUser(user);
        return credentialsRepository.save(credentials);
    }
}
