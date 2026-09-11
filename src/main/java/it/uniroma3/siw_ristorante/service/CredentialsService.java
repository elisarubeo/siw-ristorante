package it.uniroma3.siw_ristorante.service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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

    /* Da identificativo utente a nome da mostrare. User non ha un nome: il solo
       nome che una persona ha in questo progetto e' lo username delle sue
       credenziali, ed e' li' che bisogna andarlo a prendere. */
    @Transactional(readOnly = true)
    public Map<Long, String> usernamePerUtente(Collection<Long> userIds) {
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return credentialsRepository.findByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(c -> c.getUser().getId(), Credentials::getUsername,
                        (primo, secondo) -> primo));
    }

    @Transactional(readOnly = true)
    public Optional<Credentials> getCredentials(String username) {
        return credentialsRepository.findByUsername(username);
    }

    @Transactional
    public Credentials saveCredentials(Credentials credentials) {
        return credentialsRepository.save(credentials);
    }

    /* L'account di un ristoratore non nasce da una registrazione ma dalle mani
       dell'amministratore, insieme al locale: percio' e' un metodo a parte, e
       il ruolo RISTORATORE non e' assegnabile dal modulo pubblico. */
    @Transactional
    public Credentials registraGestore(User user, Credentials credentials) {
        credentials.setPassword(passwordEncoder.encode(credentials.getPassword()));
        credentials.setRole(Credentials.RISTORATORE_ROLE);
        credentials.setUser(user);
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
