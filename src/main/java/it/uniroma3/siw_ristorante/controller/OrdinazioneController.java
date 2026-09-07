package it.uniroma3.siw_ristorante.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.uniroma3.siw_ristorante.exception.OrdinazioneNonValidaException;
import it.uniroma3.siw_ristorante.exception.ResourceNotFoundException;
import it.uniroma3.siw_ristorante.model.Ordinazione;
import it.uniroma3.siw_ristorante.model.Ristorante;
import it.uniroma3.siw_ristorante.service.OrdinazioneService;
import it.uniroma3.siw_ristorante.service.RistoranteService;

/* Il prefisso di classe si ferma al ristorante perche' i due indirizzi che
   servono stanno su rami diversi: aprire e' un'azione sul tavolo, leggere e'
   un'azione sull'ordinazione. Restando sotto /ristoranti/** la classe ricade
   nelle regole di amministrazione della SecurityConfiguration senza aggiungere
   nulla. */
@Controller
@RequestMapping("/ristoranti/{ristoranteId}")
public class OrdinazioneController {
    private final OrdinazioneService ordinazioneService;
    private final RistoranteService ristoranteService;

    public OrdinazioneController(OrdinazioneService ordinazioneService, RistoranteService ristoranteService){
        this.ordinazioneService = ordinazioneService;
        this.ristoranteService = ristoranteService;
    }

    @ModelAttribute("ristorante")
    public Ristorante ristorante(@PathVariable Long ristoranteId) {
        return this.ristoranteService.findById(ristoranteId)
                .orElseThrow(() -> new ResourceNotFoundException("Nessun ristorante con id " + ristoranteId));
    }

    /* Nessun modulo e nessun @Valid: l'apertura non ha campi da compilare, e'
       un solo pulsante. Se ci fosse un @ModelAttribute Ordinazione, il browser
       potrebbe proporre il proprio totale o la propria ora di apertura. */
    @PostMapping("/tavoli/{tavoloId}/ordinazione")
    public String apri(@PathVariable Long ristoranteId, @PathVariable Long tavoloId,
            RedirectAttributes redirectAttributes) {
        try {
            Ordinazione ordinazione = this.ordinazioneService.apriOrdinazione(ristoranteId, tavoloId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Conto aperto per il tavolo " + ordinazione.getTavolo().getNumeroTavolo() + ".");
            /* addAttribute (non addFlashAttribute) riempie il segnaposto
               nell'indirizzo di destinazione: {ristoranteId} arriva dalla
               richiesta corrente, {ordinazioneId} da qui. */
            redirectAttributes.addAttribute("ordinazioneId", ordinazione.getId());
            return "redirect:/ristoranti/{ristoranteId}/ordinazioni/{ordinazioneId}";
        } catch (OrdinazioneNonValidaException e) {
            /* Intercettata qui e non lasciata al GlobalExceptionHandler: non e'
               un errore di sistema da pagina 500, e' un rifiuto con una ragione
               da mostrare sulla pagina da cui si e' arrivati. */
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/ristoranti/{ristoranteId}/tavoli";
        }
    }

    @GetMapping("/ordinazioni")
    public String contiAperti(@PathVariable Long ristoranteId, Model model) {
        model.addAttribute("ordinazioni", this.ordinazioneService.contiAperti(ristoranteId));
        return "ordinazioni/list";
    }

    @GetMapping("/ordinazioni/{ordinazioneId}")
    public String conto(@PathVariable Long ristoranteId, @PathVariable Long ordinazioneId, Model model) {
        model.addAttribute("ordinazione", this.ordinazioneService.conto(ristoranteId, ordinazioneId));
        return "ordinazioni/dettaglio";
    }
}
