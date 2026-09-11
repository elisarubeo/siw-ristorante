package it.uniroma3.siw_ristorante.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.uniroma3.siw_ristorante.model.Ristorante;
import it.uniroma3.siw_ristorante.service.CredentialsService;
import it.uniroma3.siw_ristorante.service.RistoranteService;
import jakarta.validation.Valid;

/* L'amministratore della piattaforma fa due cose sole: apre un ristorante,
   creandone insieme l'account del gestore, e lo chiude. Dentro i locali non
   entra: menu, tavoli, conti e prenotazioni sono mestiere del ristoratore.
   Tutto quello che sta qui e' sotto /admin, che la SecurityConfiguration
   riserva al ruolo ADMIN con una regola sola. */
@Controller
@RequestMapping("/admin/ristoranti")
public class AdminRistoranteController {
    private final RistoranteService ristoranteService;
    private final CredentialsService credentialsService;

    public AdminRistoranteController(RistoranteService ristoranteService, CredentialsService credentialsService) {
        this.ristoranteService = ristoranteService;
        this.credentialsService = credentialsService;
    }

    @GetMapping
    public String list(Model model) {
        List<Ristorante> ristoranti = this.ristoranteService.findTutti();
        model.addAttribute("ristoranti", ristoranti);
        model.addAttribute("gestori", nomiDeiGestori(ristoranti));
        return "admin/ristoranti";
    }

    @GetMapping("/new")
    public String createForm(@ModelAttribute("ristorante") Ristorante ristorante) {
        return "admin/form";
    }

    /* Lo username non arriva da un @ModelAttribute su Credentials: quella
       classe pretende anche una password, che qui non si scrive perche' la
       genera il server. Arriva percio' come parametro semplice, e se il
       modulo torna indietro per un errore va rimesso nel model a mano. */
    @PostMapping
    public String create(@Valid @ModelAttribute("ristorante") Ristorante ristorante,
            BindingResult bindingResult,
            @RequestParam(name = "username", defaultValue = "") String username,
            Model model,
            RedirectAttributes redirectAttributes) {

        String nomeUtente = username.trim();

        if (nomeUtente.isEmpty()) {
            bindingResult.reject("gestore.mancante", "Serve uno username per l'account del gestore");
        } else if (this.credentialsService.getCredentials(nomeUtente).isPresent()) {
            bindingResult.reject("gestore.duplicato", "Questo username è già in uso");
        }

        if (ristorante.getNome() != null && ristorante.getIndirizzo() != null
                && this.ristoranteService.esisteGia(ristorante.getNome(), ristorante.getIndirizzo())) {
            bindingResult.reject("ristorante.duplicato",
                    "Esiste già un ristorante con questo nome a questo indirizzo");
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("username", nomeUtente);
            return "admin/form";
        }

        String password = this.ristoranteService.crea(ristorante, nomeUtente);

        /* La password si puo' leggere solo adesso: nel database e' cifrata.
           Viaggia come flash attribute, quindi compare una volta sola nella
           pagina dopo il redirect e non resta nell'indirizzo. */
        redirectAttributes.addFlashAttribute("successMessage",
                "Ristorante " + ristorante.getNome() + " creato.");
        redirectAttributes.addFlashAttribute("nuovoUsername", nomeUtente);
        redirectAttributes.addFlashAttribute("nuovaPassword", password);
        return "redirect:/admin/ristoranti";
    }

    @PostMapping("/{ristoranteId}/disattiva")
    public String disattiva(@PathVariable Long ristoranteId, RedirectAttributes redirectAttributes) {
        int annullate = this.ristoranteService.disattiva(ristoranteId);
        redirectAttributes.addFlashAttribute("successMessage", annullate == 0
                ? "Ristorante disattivato."
                : "Ristorante disattivato. Annullate " + annullate
                        + (annullate == 1 ? " prenotazione." : " prenotazioni."));
        return "redirect:/admin/ristoranti";
    }

    @PostMapping("/{ristoranteId}/attiva")
    public String attiva(@PathVariable Long ristoranteId, RedirectAttributes redirectAttributes) {
        this.ristoranteService.riattiva(ristoranteId);
        redirectAttributes.addFlashAttribute("successMessage",
                "Ristorante riattivato. Le prenotazioni annullate con la chiusura non tornano.");
        return "redirect:/admin/ristoranti";
    }

    /* Gli username dei gestori in una query sola, come per l'agenda: da id
       utente a nome. */
    private java.util.Map<Long, String> nomiDeiGestori(List<Ristorante> ristoranti) {
        return this.credentialsService.usernamePerUtente(
                ristoranti.stream()
                        .map(r -> r.getGestore().getId())
                        .collect(Collectors.toSet()));
    }
}
