package it.uniroma3.siw_ristorante.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import it.uniroma3.siw_ristorante.exception.ResourceNotFoundException;
import it.uniroma3.siw_ristorante.model.User;
import it.uniroma3.siw_ristorante.service.CredentialsService;
import it.uniroma3.siw_ristorante.service.PrenotazioneService;

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

    /* Si mostrano solo le proprie: l'utente arriva dall'autenticazione, non
       da un parametro, quindi non esiste un modo per chiedere quelle altrui. */
    private User utenteCorrente(Authentication authentication) {
        return this.credentialsService.getCredentials(authentication.getName())
                .map(c -> c.getUser())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nessun utente corrispondente a " + authentication.getName()));
    }
}
