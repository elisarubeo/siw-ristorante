package it.uniroma3.siw_ristorante.controller;

import it.uniroma3.siw_ristorante.service.TavoloService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import it.uniroma3.siw_ristorante.exception.ResourceNotFoundException;
import it.uniroma3.siw_ristorante.model.Credentials;
import it.uniroma3.siw_ristorante.model.Ristorante;
import it.uniroma3.siw_ristorante.service.OrdinazioneService;
import it.uniroma3.siw_ristorante.service.PiattoService;
import it.uniroma3.siw_ristorante.service.RistoranteService;



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

    @GetMapping({ "/", "/index", "/ristoranti" })
    public String list(Model model) {
        model.addAttribute("ristoranti", this.ristoranteService.findAll());
        return "ristoranti/list";
    }

    @GetMapping("/ristoranti/{id}/menu")
    public String menu(@PathVariable("id") Long id, Authentication authentication, Model model) {
        Ristorante ristorante = this.ristoranteService.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Nessun ristorante con id " + id));
        model.addAttribute("menu", isAmministratore(authentication)
                ? piattoService.getMenu(id)
                : piattoService.getMenuDisponibile(id));
        model.addAttribute("ristorante", ristorante);
        return "ristoranti/menu";
    }

    private boolean isAmministratore(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(a -> Credentials.ADMIN_ROLE.equals(a.getAuthority()));
    }

    @GetMapping("/ristoranti/{id}/tavoli")
    public String getTavoli(@PathVariable("id") Long id, Model model) {
        Ristorante ristorante = this.ristoranteService.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Nessun ristorante con id " + id));
        model.addAttribute("tavoli", tavoloService.findByRistoranteIdOrderByNumeroTavolo(id));
        /* Quali tavoli hanno un conto aperto: una sola query per tutta la
           pagina, invece di una per tavolo. Lo stato "occupato" si ricava da
           qui. */
        model.addAttribute("contiAperti", ordinazioneService.contiApertiPerTavolo(id));
        model.addAttribute("ristorante", ristorante);
        return "ristoranti/tavoli";
    }
    

}
