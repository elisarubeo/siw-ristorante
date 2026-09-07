package it.uniroma3.siw_ristorante.controller;

import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import it.uniroma3.siw_ristorante.exception.ResourceNotFoundException;
import it.uniroma3.siw_ristorante.model.Ristorante;
import it.uniroma3.siw_ristorante.model.Scontrino;
import it.uniroma3.siw_ristorante.service.RistoranteService;
import it.uniroma3.siw_ristorante.service.ScontrinoService;

/* Solo letture: uno scontrino nasce dalla chiusura di un conto e non ha ne'
   modifica ne' eliminazione. Sotto /ristoranti/** ricade da se' nelle regole di
   amministrazione della SecurityConfiguration. */
@Controller
@RequestMapping("/ristoranti/{ristoranteId}")
public class ScontrinoController {
    private final ScontrinoService scontrinoService;
    private final RistoranteService ristoranteService;

    public ScontrinoController(ScontrinoService scontrinoService, RistoranteService ristoranteService){
        this.scontrinoService = scontrinoService;
        this.ristoranteService = ristoranteService;
    }

    @ModelAttribute("ristorante")
    public Ristorante ristorante(@PathVariable Long ristoranteId) {
        return this.ristoranteService.findById(ristoranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Nessun ristorante con id " + ristoranteId));
    }

    @GetMapping("/scontrini")
    public String scontrini(@PathVariable Long ristoranteId, Model model) {
        List<Scontrino> scontrini = this.scontrinoService.scontriniDelRistorante(ristoranteId);
        model.addAttribute("scontrini", scontrini);
        model.addAttribute("incasso", this.scontrinoService.incasso(scontrini));
        return "scontrini/list";
    }

    @GetMapping("/scontrini/{scontrinoId}")
    public String scontrino(@PathVariable Long ristoranteId, @PathVariable Long scontrinoId, Model model) {
        model.addAttribute("scontrino", this.scontrinoService.scontrino(ristoranteId, scontrinoId));
        return "scontrini/dettaglio";
    }
}
