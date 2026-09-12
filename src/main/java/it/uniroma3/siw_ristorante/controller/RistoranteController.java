package it.uniroma3.siw_ristorante.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import it.uniroma3.siw_ristorante.model.Ristorante;
import it.uniroma3.siw_ristorante.service.OrdinazioneService;
import it.uniroma3.siw_ristorante.service.PiattoService;
import it.uniroma3.siw_ristorante.service.RistoranteService;
import it.uniroma3.siw_ristorante.service.TavoloService;

@Controller
public class RistoranteController {
    private final TavoloService tavoloService;
    private final RistoranteService ristoranteService;
    private final PiattoService piattoService;
    private final OrdinazioneService ordinazioneService;

    public RistoranteController(RistoranteService ristoranteService, PiattoService piattoService,
            TavoloService tavoloService, OrdinazioneService ordinazioneService) {
        this.ristoranteService = ristoranteService;
        this.piattoService = piattoService;
        this.tavoloService = tavoloService;
        this.ordinazioneService = ordinazioneService;
    }

    /* La vetrina del sito: solo i locali attivi. Quelli disattivati non
       compaiono a nessuno, nemmeno all'amministratore, che per vederli ha la
       sua pagina sotto /admin. */
    @GetMapping({ "/", "/index", "/ristoranti" })
    public String list(Model model) {
        model.addAttribute("ristoranti", this.ristoranteService.findAttivi());
        return "ristoranti/list";
    }

    /* Scorciatoia per il ristoratore: dalla barra in alto al proprio locale,
       senza dover ricordare l'indirizzo. */
    @GetMapping("/mio-ristorante")
    public String mioRistorante(Authentication authentication) {
        Ristorante ristorante = this.ristoranteService.ristoranteDelGestore(authentication);
        return "redirect:/ristoranti/" + ristorante.getId() + "/menu";
    }

    /* La stessa pagina serve due pubblici: chi passa di qui vede solo i piatti
       disponibili, il ristoratore del locale anche quelli spenti, altrimenti
       non potrebbe piu' riattivarli. Il filtro sta qui e non nella pagina: se
       fosse solo nel template, i piatti spenti verrebbero comunque inviati al
       browser di chiunque. */
    @GetMapping("/ristoranti/{id}/menu")
    public String menu(@PathVariable("id") Long id, Authentication authentication, Model model) {
        Ristorante ristorante = this.ristoranteService.ristorantePubblico(id);
        boolean gestore = this.ristoranteService.gestitoDa(ristorante, authentication);

        model.addAttribute("menu", gestore ? piattoService.getMenu(id) : piattoService.getMenuDisponibile(id));
        /* La pagina non interroga i ruoli da se': riceve gia' la risposta alla
           domanda che le interessa, cioe' "chi guarda gestisce questo locale?" */
        model.addAttribute("gestore", gestore);
        /* Le foto non si leggono da ristorante.getImmagini(): la collezione e'
           LAZY e qui la transazione e' gia' chiusa. Le chiede il service, che
           le legge dentro la propria. */
        model.addAttribute("immagini", this.ristoranteService.immaginiDi(id));
        model.addAttribute("ristorante", ristorante);
        return "ristoranti/menu";
    }

    @GetMapping("/ristoranti/{id}/tavoli")
    public String getTavoli(@PathVariable("id") Long id, Authentication authentication, Model model) {
        Ristorante ristorante = this.ristoranteService.ristoranteGestito(id, authentication);
        model.addAttribute("tavoli", tavoloService.findByRistoranteIdOrderByNumeroTavolo(id));
        /* Quali tavoli hanno un conto aperto: una sola query per tutta la
           pagina, invece di una per tavolo. Lo stato "occupato" si ricava da
           qui. */
        model.addAttribute("contiAperti", ordinazioneService.contiApertiPerTavolo(id));
        model.addAttribute("ristorante", ristorante);
        return "ristoranti/tavoli";
    }
}
