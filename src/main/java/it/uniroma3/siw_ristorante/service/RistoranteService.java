package it.uniroma3.siw_ristorante.service;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.uniroma3.siw_ristorante.exception.ResourceNotFoundException;
import it.uniroma3.siw_ristorante.model.Credentials;
import it.uniroma3.siw_ristorante.model.Ristorante;
import it.uniroma3.siw_ristorante.model.User;
import it.uniroma3.siw_ristorante.repository.RistoranteRepository;

@Service
public class RistoranteService {

    /* Quello che si puo' mostrare una volta sola: lo username, che nel
       database resta, e la password in chiaro, che non ci arriva mai. Un
       record e non due valori sciolti perche' hanno senso solo insieme -
       una password senza l'account a cui appartiene non si sa a chi darla. */
    public record CredenzialiGestore(String username, String password) {
    }

    /* Lunghezza della password generata per il ristoratore. Dieci caratteri
       casuali presi da un alfabeto senza lettere ambigue (niente O/0, l/1):
       la password va letta e ricopiata da una persona. */
    private static final int LUNGHEZZA_PASSWORD = 10;
    private static final String ALFABETO = "abcdefghijkmnpqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final RistoranteRepository ristoranteRepository;
    private final CredentialsService credentialsService;
    private final PrenotazioneService prenotazioneService;
    private final SecureRandom random = new SecureRandom();

    public RistoranteService(RistoranteRepository ristoranteRepository, CredentialsService credentialsService,
            PrenotazioneService prenotazioneService) {
        this.ristoranteRepository = ristoranteRepository;
        this.credentialsService = credentialsService;
        this.prenotazioneService = prenotazioneService;
    }

    /* ---------- consultazione ---------- */

    /* Quelli che vede il pubblico. I disattivati non ci sono: per il resto del
       sito e' come se non esistessero. */
    @Transactional(readOnly = true)
    public List<Ristorante> findAttivi() {
        return ristoranteRepository.findByAttivoTrueOrderByNome();
    }

    /* Tutti, disattivati compresi: serve solo all'amministratore, che e'
       l'unico a dover vedere anche i profili spenti. */
    @Transactional(readOnly = true)
    public List<Ristorante> findTutti() {
        return ristoranteRepository.findAllByOrderByNome();
    }

    @Transactional(readOnly = true)
    public Optional<Ristorante> findById(Long id) {
        return ristoranteRepository.findById(id);
    }

    /* Il ristorante come lo vedono clienti e visitatori: se e' disattivato la
       pagina non esiste, non "esiste ma e' vietata". */
    @Transactional(readOnly = true)
    public Ristorante ristorantePubblico(Long ristoranteId) {
        Ristorante ristorante = ristoranteRepository.findById(ristoranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Nessun ristorante con id " + ristoranteId));
        if (!ristorante.isAttivo()) {
            throw new ResourceNotFoundException("Il ristorante " + ristoranteId + " non e' piu' attivo");
        }
        return ristorante;
    }

    /* IL CONTROLLO CHE FA DA CARDINE A TUTTA LA GESTIONE.
       Le regole della SecurityConfiguration sanno dire "solo un ristoratore
       puo' entrare qui", ma non "solo il ristoratore DI QUESTO locale":
       l'indirizzo /ristoranti/2/piatti/new e' lecito per un gestore e vietato
       per un altro, e la differenza sta nei dati, non nell'indirizzo. Percio'
       il controllo vive qui, e i controller lo richiamano nel loro
       @ModelAttribute, cioe' prima di qualunque loro metodo. */
    @Transactional(readOnly = true)
    public Ristorante ristoranteGestito(Long ristoranteId, Authentication authentication) {
        Ristorante ristorante = ristoranteRepository.findById(ristoranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Nessun ristorante con id " + ristoranteId));

        if (!ristorante.isAttivo()) {
            throw new AccessDeniedException("Il ristorante " + ristoranteId + " e' disattivato");
        }
        if (authentication == null || !ristorante.getGestore().equals(utenteDi(authentication))) {
            throw new AccessDeniedException("Il ristorante " + ristoranteId + " e' gestito da un altro account");
        }
        return ristorante;
    }

    /* La stessa domanda di ristoranteGestito, ma posta da una pagina pubblica,
       che non deve rifiutare nessuno: serve solo a decidere se mostrare i
       comandi di gestione a chi sta guardando il proprio locale. */
    @Transactional(readOnly = true)
    public boolean gestitoDa(Ristorante ristorante, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return false;
        }
        return this.credentialsService.getCredentials(authentication.getName())
                .map(Credentials::getUser)
                .map(utente -> utente.equals(ristorante.getGestore()))
                .orElse(false);
    }

    /* Il locale di chi ha fatto il login, per la voce "Il mio ristorante". */
    @Transactional(readOnly = true)
    public Ristorante ristoranteDelGestore(Authentication authentication) {
        User gestore = utenteDi(authentication);
        return ristoranteRepository.findByGestoreId(gestore.getId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "L'account " + authentication.getName() + " non gestisce nessun ristorante"));
    }

    /* ---------- amministrazione ---------- */

    /* Il ristorante e il suo account nascono insieme: un locale senza gestore
       non servirebbe a nessuno. La password generata torna al chiamante in
       chiaro perche' e' l'unico momento in cui si puo' leggere: nel database
       finisce cifrata e non e' piu' recuperabile. */
    @Transactional
    public String crea(Ristorante ristorante, String username) {
        String password = passwordCasuale();

        Credentials credenziali = new Credentials();
        credenziali.setUsername(username);
        credenziali.setPassword(password);

        User gestore = new User();
        /* registraGestore cifra la password, assegna il ruolo e salva anche lo
           User per cascata. */
        this.credentialsService.registraGestore(gestore, credenziali);

        ristorante.setGestore(gestore);
        ristorante.setAttivo(true);
        ristoranteRepository.save(ristorante);
        return password;
    }

    @Transactional(readOnly = true)
    public boolean esisteGia(String nome, String indirizzo) {
        return ristoranteRepository.existsByNomeIgnoreCaseAndIndirizzoIgnoreCase(nome, indirizzo);
    }

    /* Spegnere il profilo non cancella niente, ma le prenotazioni gia' prese
       vanno annullate subito: un cliente non deve presentarsi a un locale che
       dal sito e' sparito. */
    @Transactional
    public int disattiva(Long ristoranteId) {
        Ristorante ristorante = ristoranteRepository.findById(ristoranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Nessun ristorante con id " + ristoranteId));
        ristorante.setAttivo(false);
        return this.prenotazioneService.annullaFuturePerRistorante(ristoranteId);
    }

    /* Le prenotazioni annullate con la disattivazione non tornano: erano di
       clienti che nel frattempo si sono organizzati altrimenti. */
    @Transactional
    public void riattiva(Long ristoranteId) {
        Ristorante ristorante = ristoranteRepository.findById(ristoranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Nessun ristorante con id " + ristoranteId));
        ristorante.setAttivo(true);
    }

    /* Una password nuova per il gestore di un locale.

       Serve quando quella consegnata alla creazione e' andata persa: era
       leggibile solo in quel momento, perche' nel database c'e' l'impronta
       BCrypt e da li' non si torna indietro. L'unica cosa che si puo' fare e'
       sostituirla, ed e' quello che fa questo metodo.

       La vecchia password smette di funzionare subito: e' la stessa riga che
       viene sovrascritta. Chi la stava usando viene disconnesso al primo
       accesso successivo, non all'istante - la sessione gia' aperta vive nel
       server e non dipende dalla password.

       Il ristorante puo' essere anche disattivato: rigenerare le credenziali
       di un locale chiuso e' legittimo, si puo' voler preparare la
       riapertura. Entrare, quello no: a impedirlo e' la colonna "enabled"
       calcolata nella SecurityConfiguration, che per un locale spento resta
       falsa qualunque sia la password. */
    @Transactional
    public CredenzialiGestore rigeneraPassword(Long ristoranteId) {
        Ristorante ristorante = ristoranteRepository.findById(ristoranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Nessun ristorante con id " + ristoranteId));

        String password = passwordCasuale();
        Credentials credenziali = this.credentialsService
                .reimpostaPassword(ristorante.getGestore().getId(), password);

        /* Lo username si legge dalle credenziali appena aggiornate e non da
           una seconda query: e' gia' qui. La password e' quella in chiaro
           generata sopra - dentro l'oggetto Credentials ormai c'e' l'impronta,
           e non serve a nessuno. */
        return new CredenzialiGestore(credenziali.getUsername(), password);
    }

    /* ---------- utilita' ---------- */

    private User utenteDi(Authentication authentication) {
        return this.credentialsService.getCredentials(authentication.getName())
                .map(Credentials::getUser)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nessun utente corrispondente a " + authentication.getName()));
    }

    private String passwordCasuale() {
        StringBuilder password = new StringBuilder(LUNGHEZZA_PASSWORD);
        for (int i = 0; i < LUNGHEZZA_PASSWORD; i++) {
            password.append(ALFABETO.charAt(random.nextInt(ALFABETO.length())));
        }
        return password.toString();
    }
}
