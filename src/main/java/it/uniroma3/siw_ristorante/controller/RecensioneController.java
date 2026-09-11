package it.uniroma3.siw_ristorante.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.uniroma3.siw_ristorante.exception.ResourceNotFoundException;
import it.uniroma3.siw_ristorante.model.Recensione;
import it.uniroma3.siw_ristorante.model.Ristorante;
import it.uniroma3.siw_ristorante.model.User;
import it.uniroma3.siw_ristorante.service.CredentialsService;
import it.uniroma3.siw_ristorante.service.RecensioneService;
import it.uniroma3.siw_ristorante.service.RistoranteService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/ristoranti/{ristoranteId}/recensioni")
public class RecensioneController {
    private final RecensioneService recensioneService;
    private final RistoranteService ristoranteService;
    private final CredentialsService credentialsService;

    public RecensioneController(RecensioneService recensioneService, RistoranteService ristoranteService,
            CredentialsService credentialsService) {
        this.recensioneService = recensioneService;
        this.ristoranteService = ristoranteService;
        this.credentialsService = credentialsService;
    }

    /* Eseguito prima di ogni metodo della classe. La pagina e' rivolta ai
       clienti, quindi il ristorante deve solo esistere ed essere attivo: di un
       locale disattivato non si parla piu'. */
    @ModelAttribute("ristorante")
    public Ristorante ristorante(@PathVariable Long ristoranteId) {
        return this.ristoranteService.ristorantePubblico(ristoranteId);
    }

    /* La pagina e' pubblica: la si vede anche senza aver fatto il login,
       quindi authentication puo' essere null o anonimo e "la tua recensione"
       semplicemente non c'e'. */
    @GetMapping
    public String recensioni(@PathVariable Long ristoranteId, Authentication authentication, Model model) {
        List<Recensione> recensioni = this.recensioneService.recensioniDelRistorante(ristoranteId);
        model.addAttribute("recensioni", recensioni);
        model.addAttribute("media", this.recensioneService.mediaVoti(ristoranteId));
        model.addAttribute("autori", nomiDegliAutori(recensioni));

        User utente = utenteCorrenteOppureNull(authentication);
        model.addAttribute("miaRecensione", utente == null ? null
                : this.recensioneService.recensioneDiUtente(utente.getId(), ristoranteId).orElse(null));
        return "ristoranti/recensioni";
    }

    @GetMapping("/new")
    public String createForm(@PathVariable Long ristoranteId, Authentication authentication,
            @ModelAttribute("recensione") Recensione recensione, RedirectAttributes redirectAttributes) {
        /* Una sola recensione per ristorante: chi l'ha gia' scritta non deve
           vedere un modulo vuoto che al salvataggio verrebbe respinto. */
        if (this.recensioneService.haGiaRecensito(utenteCorrente(authentication).getId(), ristoranteId)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Hai gia' recensito questo ristorante: puoi modificare la recensione che hai scritto.");
            return "redirect:/ristoranti/{ristoranteId}/recensioni";
        }
        return "recensioni/form";
    }

    @PostMapping
    public String create(@PathVariable Long ristoranteId,
            Authentication authentication,
            @Valid @ModelAttribute("recensione") Recensione recensione,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        User autore = utenteCorrente(authentication);

        /* reject e non rejectValue: il doppione non e' colpa di un campo del
           modulo, quindi l'errore e' della recensione nel suo insieme. */
        if (this.recensioneService.haGiaRecensito(autore.getId(), ristoranteId)) {
            bindingResult.reject("recensione.duplicato", "Hai gia' recensito questo ristorante");
        }

        if (bindingResult.hasErrors()) {
            return "recensioni/form";
        }

        this.recensioneService.save(ristoranteId, autore, recensione);
        redirectAttributes.addFlashAttribute("successMessage", "La tua recensione e' stata pubblicata.");
        return "redirect:/ristoranti/{ristoranteId}/recensioni";
    }

    @GetMapping("/{recensioneId}/edit")
    public String editForm(@PathVariable Long ristoranteId, @PathVariable Long recensioneId,
            Authentication authentication, Model model) {
        model.addAttribute("recensione",
                this.recensioneService.miaRecensione(ristoranteId, recensioneId, utenteCorrente(authentication)));
        return "recensioni/form";
    }

    @PostMapping("/{recensioneId}")
    public String update(@PathVariable Long ristoranteId, @PathVariable Long recensioneId,
            Authentication authentication,
            @Valid @ModelAttribute("recensione") Recensione recensione,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            /* L'id non viaggia nel modulo: senza rimetterlo la pagina non
               saprebbe che si tratta di una modifica e non di una creazione. */
            recensione.setId(recensioneId);
            return "recensioni/form";
        }

        this.recensioneService.update(ristoranteId, recensioneId, utenteCorrente(authentication), recensione);
        redirectAttributes.addFlashAttribute("successMessage", "La tua recensione e' stata aggiornata.");
        return "redirect:/ristoranti/{ristoranteId}/recensioni";
    }

    /* POST e non GET: una richiesta che cancella non deve poter partire da un
       semplice collegamento. */
    @PostMapping("/{recensioneId}/delete")
    public String delete(@PathVariable Long ristoranteId, @PathVariable Long recensioneId,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        this.recensioneService.delete(ristoranteId, recensioneId, utenteCorrente(authentication));
        redirectAttributes.addFlashAttribute("successMessage", "La tua recensione e' stata eliminata.");
        return "redirect:/ristoranti/{ristoranteId}/recensioni";
    }

    /* Gli username degli autori in elenco, in una query sola: da id a nome.
       User non ha un nome, ce l'hanno solo le sue credenziali. */
    private java.util.Map<Long, String> nomiDegliAutori(List<Recensione> recensioni) {
        return this.credentialsService.usernamePerUtente(
                recensioni.stream()
                        .map(r -> r.getUser().getId())
                        .collect(Collectors.toSet()));
    }

    /* Il principal di Spring Security NON e' il nostro User: e' lo UserDetails
       costruito da JdbcUserDetailsManager a partire dalla tabella credentials.
       Dal suo username si risale alle credenziali e da queste all'utente. */
    private User utenteCorrente(Authentication authentication) {
        return this.credentialsService.getCredentials(authentication.getName())
                .map(c -> c.getUser())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nessun utente corrispondente a " + authentication.getName()));
    }

    /* Chi non ha fatto il login non ha un Authentication nullo: ha quello
       anonimo, che ha un nome ("anonymousUser") ma nessun utente dietro. */
    private User utenteCorrenteOppureNull(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return null;
        }
        return this.credentialsService.getCredentials(authentication.getName())
                .map(c -> c.getUser())
                .orElse(null);
    }
}
