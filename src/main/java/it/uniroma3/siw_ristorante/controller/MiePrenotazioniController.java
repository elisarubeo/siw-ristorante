package it.uniroma3.siw_ristorante.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.uniroma3.siw_ristorante.exception.PrenotazioneNonValidaException;
import it.uniroma3.siw_ristorante.exception.ResourceNotFoundException;
import it.uniroma3.siw_ristorante.model.Prenotazione;
import it.uniroma3.siw_ristorante.model.User;
import it.uniroma3.siw_ristorante.service.CredentialsService;
import it.uniroma3.siw_ristorante.service.PrenotazioneService;
import jakarta.validation.Valid;

/* Controller separato da PrenotazioneController e non un metodo in piu' li'
   dentro: quella classe ha un @ModelAttribute che carica il ristorante dal
   percorso, e verrebbe eseguito anche per questa pagina, dove {ristoranteId}
   non esiste. Le prenotazioni di una persona attraversano piu' ristoranti,
   quindi l'indirizzo non e' annidato sotto nessuno di essi. */
@Controller
public class MiePrenotazioniController {
    private final PrenotazioneService prenotazioneService;
    private final CredentialsService credentialsService;

    public MiePrenotazioniController(PrenotazioneService prenotazioneService,
            CredentialsService credentialsService) {
        this.prenotazioneService = prenotazioneService;
        this.credentialsService = credentialsService;
    }

    @GetMapping("/prenotazioni")
    public String miePrenotazioni(Authentication authentication, Model model) {
        User utente = utenteCorrente(authentication);
        model.addAttribute("prenotazioni", this.prenotazioneService.prenotazioniPerUtente(utente.getId()));
        return "prenotazioni/list";
    }

    @GetMapping("/prenotazioni/{prenotazioneId}/edit")
    public String editForm(@PathVariable Long prenotazioneId,
            Authentication authentication, Model model) {
        Prenotazione prenotazione = this.prenotazioneService
                .miaPrenotazione(prenotazioneId, utenteCorrente(authentication));
        model.addAttribute("prenotazione", prenotazione);
        model.addAttribute("ristorante", prenotazione.getRistorante());
        aggiungiLimitiTemporali(model);
        return "prenotazioni/form";
    }

    @PostMapping("/prenotazioni/{prenotazioneId}")
    public String modifica(@PathVariable Long prenotazioneId,
            @Valid @ModelAttribute("prenotazione") Prenotazione prenotazione,
            BindingResult bindingResult,
            Authentication authentication,
            Model model,
            RedirectAttributes redirectAttributes) {

        User utente = utenteCorrente(authentication);

        if (bindingResult.hasErrors()) {
            return tornaAlModulo(prenotazioneId, utente, model);
        }
        try {
            this.prenotazioneService.modifica(prenotazioneId, utente,
                    prenotazione.getNumeroPersone(),
                    prenotazione.getDataPrenotazione(),
                    prenotazione.getOrarioPrenotazione());
            redirectAttributes.addFlashAttribute("successMessage", "Prenotazione aggiornata.");
            return "redirect:/prenotazioni";
        } catch (PrenotazioneNonValidaException e) {
            bindingResult.reject("prenotazione.nonValida", e.getMessage());
            return tornaAlModulo(prenotazioneId, utente, model);
        }
    }

    @PostMapping("/prenotazioni/{prenotazioneId}/cancel")
    public String annulla(@PathVariable Long prenotazioneId,
            Authentication authentication, RedirectAttributes redirectAttributes) {
        try {
            this.prenotazioneService.annulla(prenotazioneId, utenteCorrente(authentication));
            redirectAttributes.addFlashAttribute("successMessage", "Prenotazione annullata.");
        } catch (PrenotazioneNonValidaException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/prenotazioni";
    }

    /* Il ristorante non arriva dal modulo (non e' modificabile): va rimesso nel
       model ogni volta che si torna alla pagina per un errore. */
    private String tornaAlModulo(Long prenotazioneId, User utente, Model model) {
        model.addAttribute("ristorante",
                this.prenotazioneService.miaPrenotazione(prenotazioneId, utente).getRistorante());
        aggiungiLimitiTemporali(model);
        return "prenotazioni/form";
    }

    private void aggiungiLimitiTemporali(Model model) {
        model.addAttribute("dataMinima", LocalDate.now().toString());
        model.addAttribute("dataMassima",
                LocalDate.now().plusMonths(Prenotazione.ANTICIPO_MASSIMO_MESI).toString());
        model.addAttribute("oraMinimaOggi",
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
    }

    /* Si mostrano solo le proprie: l'utente arriva dall'autenticazione, non
       da un parametro, quindi non esiste un modo per chiedere quelle altrui. */
    private User utenteCorrente(Authentication authentication) {
        return this.credentialsService.getCredentials(authentication.getName())
                .map(c -> c.getUser())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nessun utente corrispondente a " + authentication.getName()));
    }
}
