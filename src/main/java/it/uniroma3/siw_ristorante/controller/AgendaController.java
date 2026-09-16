package it.uniroma3.siw_ristorante.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import it.uniroma3.siw_ristorante.model.Prenotazione;
import it.uniroma3.siw_ristorante.model.Ristorante;
import it.uniroma3.siw_ristorante.service.CredentialsService;
import it.uniroma3.siw_ristorante.service.PrenotazioneService;
import it.uniroma3.siw_ristorante.service.RistoranteService;
import it.uniroma3.siw_ristorante.service.TavoloService;

@Controller
@RequestMapping("/ristoranti/{ristoranteId}")
public class AgendaController {
    private final PrenotazioneService prenotazioneService;
    private final RistoranteService ristoranteService;
    private final TavoloService tavoloService;
    private final CredentialsService credentialsService;

    public AgendaController(PrenotazioneService prenotazioneService, RistoranteService ristoranteService,
            TavoloService tavoloService, CredentialsService credentialsService) {
        this.prenotazioneService = prenotazioneService;
        this.ristoranteService = ristoranteService;
        this.tavoloService = tavoloService;
        this.credentialsService = credentialsService;
    }

    /* Eseguito prima di ogni metodo della classe, ed e' anche il controllo di
       proprieta': un ristoratore che chiede un locale non suo, o disattivato,
       si ferma qui con un 403 e nessun metodo viene eseguito. */
    @ModelAttribute("ristorante")
    public Ristorante ristorante(@PathVariable Long ristoranteId, Authentication authentication) {
        return this.ristoranteService.ristoranteGestito(ristoranteId, authentication);
    }

    /* Tutte le prenotazioni ancora valide del locale, di ogni tavolo. */
    @GetMapping("/agenda")
    public String agenda(@PathVariable Long ristoranteId, Model model) {
        List<Prenotazione> prenotazioni = this.prenotazioneService.prenotazioniFutureDelRistorante(ristoranteId);
        model.addAttribute("prenotazioni", prenotazioni);
        model.addAttribute("clienti", nomiDeiClienti(prenotazioni));
        return "prenotazioni/agenda";
    }

    /* Le stesse, ristrette a un tavolo: "chi arriva a questo tavolo". */
    @GetMapping("/tavoli/{tavoloId}/prenotazioni")
    public String prenotazioniDelTavolo(@PathVariable Long ristoranteId, @PathVariable Long tavoloId,
            Model model) {
        List<Prenotazione> prenotazioni = this.prenotazioneService
                .prenotazioniFutureDelTavolo(ristoranteId, tavoloId);
        model.addAttribute("tavolo", this.tavoloService.tavolo(ristoranteId, tavoloId));
        model.addAttribute("prenotazioni", prenotazioni);
        model.addAttribute("clienti", nomiDeiClienti(prenotazioni));
        return "prenotazioni/tavolo";
    }

    /* Gli username degli utenti che compaiono in elenco, presi in una query
       sola e consegnati alla pagina come mappa: da id a nome. */
    private java.util.Map<Long, String> nomiDeiClienti(List<Prenotazione> prenotazioni) {
        return this.credentialsService.usernamePerUtente(
                prenotazioni.stream()
                        .map(p -> p.getUser().getId())
                        .collect(Collectors.toSet()));
    }
}
