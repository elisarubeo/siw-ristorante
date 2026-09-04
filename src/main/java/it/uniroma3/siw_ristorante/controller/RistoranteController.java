package it.uniroma3.siw_ristorante.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;

import it.uniroma3.siw_ristorante.model.Ristorante;
import it.uniroma3.siw_ristorante.service.PiattoService;
import it.uniroma3.siw_ristorante.service.RistoranteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import it.uniroma3.siw_ristorante.exception.ResourceNotFoundException;



@Controller
public class RistoranteController {
    private final RistoranteService ristoranteService;
    private final PiattoService piattoService;

    public RistoranteController(RistoranteService ristoranteService, PiattoService piattoService) {
        this.piattoService = piattoService;
        this.ristoranteService = ristoranteService;
    }

    /* La stessa pagina fa da home: l'elenco dei ristoranti e' la prima cosa
       che serve a chi arriva, senza un passaggio intermedio. */
    @GetMapping({ "/", "/index", "/ristoranti" })
    public String list(Model model) {
        model.addAttribute("ristoranti", this.ristoranteService.findAll());
        return "ristoranti/list";
    }

    @GetMapping("/ristoranti/{id}/menu")
    public String menu(@PathVariable("id") Long id, Model model) {
        Ristorante ristorante = this.ristoranteService.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("Nessun ristorante con id " + id));
        model.addAttribute("menu", piattoService.getMenu(id));
        model.addAttribute("ristorante", ristorante);
        return "ristoranti/menu";
    }
    
    
}
