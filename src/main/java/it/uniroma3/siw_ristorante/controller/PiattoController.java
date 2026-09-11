package it.uniroma3.siw_ristorante.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.uniroma3.siw_ristorante.exception.EntityInUseException;
import it.uniroma3.siw_ristorante.exception.ResourceNotFoundException;
import it.uniroma3.siw_ristorante.model.Piatto;
import it.uniroma3.siw_ristorante.model.Ristorante;
import it.uniroma3.siw_ristorante.service.PiattoService;
import it.uniroma3.siw_ristorante.service.RistoranteService;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/ristoranti/{ristoranteId}/piatti")
public class PiattoController {
    private final PiattoService piattoService;
    private final RistoranteService ristoranteService;

    public PiattoController(PiattoService piattoService, RistoranteService ristoranteService) {
        this.piattoService = piattoService;
        this.ristoranteService = ristoranteService;
    }

    @ModelAttribute("ristorante")
    public Ristorante ristorante(@PathVariable Long ristoranteId) {
        return this.ristoranteService.findById(ristoranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Nessun ristorante con id " + ristoranteId));
    }

    @GetMapping("/new")
    public String createForm(@ModelAttribute("piatto") Piatto piatto) {
        return "piatti/form";
    }

    @PostMapping
    public String create(@PathVariable Long ristoranteId,
            @Valid @ModelAttribute("piatto") Piatto piatto,
            BindingResult bindingResult) {

        if (piatto.getNome() != null
                && this.piattoService.esisteNelMenu(ristoranteId, piatto.getNome())) {
            bindingResult.rejectValue("nome", "piatto.duplicato",
                    "Esiste gia' un piatto con questo nome nel menu");
        }

        if (bindingResult.hasErrors()) {
            return "piatti/form";
        }

        this.piattoService.save(ristoranteId, piatto);
        return "redirect:/ristoranti/{ristoranteId}/menu";
    }

    @PostMapping("/{piattoId}/delete")
    public String delete(@PathVariable Long ristoranteId, 
                        @PathVariable Long piattoId, RedirectAttributes redirectAttributes) {
        try {
            this.piattoService.delete(ristoranteId, piattoId);
            redirectAttributes.addFlashAttribute("successMessage", "Piatto eliminato.");
        } catch (EntityInUseException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/ristoranti/{ristoranteId}/menu";
    }

    @PostMapping("/{piattoId}/deactivate")
    public String disattiva(@PathVariable Long ristoranteId, 
        @PathVariable Long piattoId, RedirectAttributes redirectAttributes) {
            try {
                this.piattoService.disattiva(ristoranteId, piattoId);
                redirectAttributes.addFlashAttribute("successMessage", "Piatto disattivato.");
            } catch (EntityInUseException e) {
                redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            }
            return "redirect:/ristoranti/{ristoranteId}/menu";
    }

    @PostMapping("/{piattoId}/activate")
    public String riattiva(@PathVariable Long ristoranteId,
            @PathVariable Long piattoId, RedirectAttributes redirectAttributes) {
        this.piattoService.riattiva(ristoranteId, piattoId);
        redirectAttributes.addFlashAttribute("successMessage", "Piatto riattivato.");
        return "redirect:/ristoranti/{ristoranteId}/menu";
    }

    @GetMapping("/{piattoId}/edit")
    public String editForm(@PathVariable Long ristoranteId, @PathVariable Long piattoId, Model model) {
        Piatto piatto = piattoService.findByIdAndRistoranteId(piattoId, ristoranteId)
            .orElseThrow(() -> new ResourceNotFoundException("Il piatto che stai cercando non esiste in questo ristorante"));
        model.addAttribute("piatto", piatto);
        return "piatti/form";
    }

    @PostMapping("/{piattoId}")
    public String update(@PathVariable Long ristoranteId, @PathVariable Long piattoId,
                         @Valid @ModelAttribute("piatto") Piatto piatto,
                         BindingResult bindingResult) {
        if (piatto.getNome() != null
                && this.piattoService.existsByNomeAndRistoranteIdExcluding(piatto.getNome(), ristoranteId, piattoId)) {
            bindingResult.rejectValue("nome", "piatto.duplicato",
                    "Esiste già un altro piatto con questo nome in questo ristorante");
        }

        if (bindingResult.hasErrors()) {
            piatto.setId(piattoId);
            return "piatti/form";
        }
        this.piattoService.update(ristoranteId, piattoId, piatto);
        return "redirect:/ristoranti/{ristoranteId}/menu";
    }
    
    
}
