package it.uniroma3.siw_ristorante.controller;

import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import it.uniroma3.siw_ristorante.exception.ResourceNotFoundException;
import it.uniroma3.siw_ristorante.model.Piatto;
import it.uniroma3.siw_ristorante.model.Ristorante;
import it.uniroma3.siw_ristorante.service.PiattoService;
import it.uniroma3.siw_ristorante.service.RistoranteService;
import jakarta.validation.Valid;

/* Gli indirizzi sono annidati sotto il ristorante perche' un piatto esiste solo
   nel suo menu, ma la classe resta dedicata ai piatti: il controller si sceglie
   in base alla risorsa che manipola, non al prefisso dell'indirizzo. */
@Controller
@RequestMapping("/ristoranti/{ristoranteId}/piatti")
public class PiattoController {
    private final PiattoService piattoService;
    private final RistoranteService ristoranteService;

    public PiattoController(PiattoService piattoService, RistoranteService ristoranteService) {
        this.piattoService = piattoService;
        this.ristoranteService = ristoranteService;
    }

    /* Eseguito prima di ogni metodo della classe: carica il ristorante una
       volta sola, risponde 404 se non esiste e lo lascia nel model. Cosi' la
       pagina lo ritrova anche quando si torna al modulo per un errore di
       validazione, che e' il caso in cui e' facile dimenticarselo. */
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
}
