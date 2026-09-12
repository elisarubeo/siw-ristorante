package it.uniroma3.siw_ristorante.service;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.uniroma3.siw_ristorante.exception.ResourceNotFoundException;
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

    /* Sostituisce la password di un account con un'altra, cifrandola come
       tutte le altre.

       Non chiede la password attuale, e non e' una dimenticanza: chi la usa e'
       l'amministratore, che la password del ristoratore non la conosce - nel
       database c'e' solo l'impronta BCrypt, e non e' reversibile. E' un
       "reimposta", non un "cambia". Il diritto di farlo non si controlla qui:
       lo garantisce la regola su /admin/** della SecurityConfiguration.

       La password in chiaro non viene mai memorizzata: entra, viene cifrata, e
       l'unica copia leggibile e' quella che il chiamante ha in mano e mostra
       una volta sola. */
    @Transactional
    public Credentials reimpostaPassword(Long userId, String nuovaPassword) {
        Credentials credenziali = credentialsRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nessun account collegato all'utente " + userId));
        credenziali.setPassword(passwordEncoder.encode(nuovaPassword));
        return credentialsRepository.save(credenziali);
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
