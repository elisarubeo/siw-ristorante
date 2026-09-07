package it.uniroma3.siw_ristorante.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.springframework.security.core.Authentication;
import org.springframework.ui.Model;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.uniroma3.siw_ristorante.exception.PrenotazioneNonValidaException;
import it.uniroma3.siw_ristorante.exception.ResourceNotFoundException;
import it.uniroma3.siw_ristorante.model.Prenotazione;
import it.uniroma3.siw_ristorante.model.Ristorante;
import it.uniroma3.siw_ristorante.model.User;
import it.uniroma3.siw_ristorante.service.CredentialsService;
import it.uniroma3.siw_ristorante.service.PrenotazioneService;
import it.uniroma3.siw_ristorante.service.RistoranteService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/ristoranti/{ristoranteId}/prenotazioni")
public class PrenotazioneController {
    private final PrenotazioneService prenotazioneService;
    private final RistoranteService ristoranteService;
    private final CredentialsService credentialsService;

    public PrenotazioneController(PrenotazioneService prenotazioneService,
            RistoranteService ristoranteService, CredentialsService credentialsService) {
        this.prenotazioneService = prenotazioneService;
        this.ristoranteService = ristoranteService;
        this.credentialsService = credentialsService;
    }

    @ModelAttribute("ristorante")
    public Ristorante ristorante(@PathVariable Long ristoranteId) {
        return this.ristoranteService.findById(ristoranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Nessun ristorante con id " + ristoranteId));
    }

    @GetMapping("/new")
    public String createForm(@ModelAttribute("prenotazione") Prenotazione prenotazione, Model model) {
        aggiungiLimitiTemporali(model);
        return "prenotazioni/form";
    }

    /* Limiti calcolati dal server e passati alla pagina: il browser impedisce
       di scegliere un istante gia' passato. Restano un aiuto, non una
       protezione: il controllo che conta e' quello nel service. */
    private void aggiungiLimitiTemporali(Model model) {
        model.addAttribute("dataMinima", LocalDate.now().toString());
        model.addAttribute("dataMassima",
                LocalDate.now().plusMonths(Prenotazione.ANTICIPO_MASSIMO_MESI).toString());
        model.addAttribute("oraMinimaOggi",
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
    }

    @PostMapping
    public String create(@PathVariable Long ristoranteId,
            @Valid @ModelAttribute("prenotazione") Prenotazione prenotazione,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        if (bindingResult.hasErrors()) {
            aggiungiLimitiTemporali(model);
            return "prenotazioni/form";
        }

        try {
            /* Dei campi arrivati dal modulo si usano solo questi tre: tavolo,
               ristorante, utente e stato li decide il server. L'oggetto legato
               alla form non viene mai salvato. */
            Prenotazione salvata = this.prenotazioneService.prenota(
                    ristoranteId,
                    utenteCorrente(authentication),
                    prenotazione.getNumeroPersone(),
                    prenotazione.getDataPrenotazione(),
                    prenotazione.getOrarioPrenotazione());

            redirectAttributes.addFlashAttribute("successMessage",
                    "Prenotazione confermata: tavolo " + salvata.getTavolo().getNumeroTavolo()
                            + " per " + salvata.getNumeroPersone() + " persone.");
            return "redirect:/ristoranti/{ristoranteId}/menu";

        } catch (PrenotazioneNonValidaException e) {
            /* reject e non rejectValue: la ragione non riguarda un campo solo,
               riguarda la combinazione di persone, data e ora. */
            bindingResult.reject("prenotazione.nonValida", e.getMessage());
            aggiungiLimitiTemporali(model);
            return "prenotazioni/form";
        }
    }

    /* L'utente si ricava sempre dall'autenticazione, mai dal modulo: altrimenti
       basterebbe cambiare un campo nascosto per prenotare a nome di un altro. */
    private User utenteCorrente(Authentication authentication) {
        return this.credentialsService.getCredentials(authentication.getName())
                .map(c -> c.getUser())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nessun utente corrispondente a " + authentication.getName()));
    }
}
