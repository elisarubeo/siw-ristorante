package it.uniroma3.siw_ristorante.controller;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import it.uniroma3.siw_ristorante.exception.EntityInUseException;
import it.uniroma3.siw_ristorante.exception.ResourceNotFoundException;
import it.uniroma3.siw_ristorante.model.Ristorante;
import it.uniroma3.siw_ristorante.model.Tavolo;
import it.uniroma3.siw_ristorante.service.RistoranteService;
import it.uniroma3.siw_ristorante.service.TavoloService;
import jakarta.validation.Valid;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


/* Il @RequestMapping di classe non e' solo estetica: senza, i percorsi dei
   metodi sono assoluti e finiscono fuori da /ristoranti/**, cioe' fuori dalle
   regole di amministrazione della SecurityConfiguration. */
@Controller
@RequestMapping("/ristoranti/{ristoranteId}/tavoli")
public class TavoloController {
    private final TavoloService tavoloService;
    private final RistoranteService ristoranteService;

    public TavoloController(TavoloService tavoloService, RistoranteService ristoranteService) {
        this.tavoloService = tavoloService;
        this.ristoranteService = ristoranteService;
    }

    @ModelAttribute("ristorante")
    public Ristorante ristorante(@PathVariable Long ristoranteId) {
        return this.ristoranteService.findById(ristoranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Nessun ristorante con id " + ristoranteId));
    }

    @GetMapping("/new")
    public String createForm(@ModelAttribute("tavolo") Tavolo tavolo) {
        return "tavoli/form";
    }

    @PostMapping
    public String create(@PathVariable Long ristoranteId,
            @Valid @ModelAttribute("tavolo") Tavolo tavolo,
            BindingResult bindingResult) {

        if (tavolo.getNumeroTavolo() != null
                && this.tavoloService.esisteNelRistorante(ristoranteId, tavolo.getNumeroTavolo())) {
            bindingResult.rejectValue("numeroTavolo", "tavolo.duplicato",
                    "Questo ristorante ha gia' un tavolo con questo numero");
        }

        if (bindingResult.hasErrors()) {
            return "tavoli/form";
        }

        this.tavoloService.save(ristoranteId, tavolo);
        return "redirect:/ristoranti/{ristoranteId}/tavoli";
    }

    @PostMapping("/{tavoloId}/delete")
    public String delete(@PathVariable Long ristoranteId,
            @PathVariable Long tavoloId,
            RedirectAttributes redirectAttributes) {
        try {
            this.tavoloService.delete(ristoranteId, tavoloId);
            redirectAttributes.addFlashAttribute("successMessage", "Tavolo eliminato.");
        } catch (EntityInUseException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/ristoranti/{ristoranteId}/tavoli";
    }
    
}
