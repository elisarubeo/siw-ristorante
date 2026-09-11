package it.uniroma3.siw_ristorante.controller;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.uniroma3.siw_ristorante.exception.OrdinazioneNonValidaException;
import it.uniroma3.siw_ristorante.model.Ordinazione;
import it.uniroma3.siw_ristorante.model.Ristorante;
import it.uniroma3.siw_ristorante.model.Scontrino;
import it.uniroma3.siw_ristorante.service.OrdinazioneService;
import it.uniroma3.siw_ristorante.service.PiattoService;
import it.uniroma3.siw_ristorante.service.RistoranteService;


@Controller
@RequestMapping("/ristoranti/{ristoranteId}")
public class OrdinazioneController {
    private final OrdinazioneService ordinazioneService;
    private final RistoranteService ristoranteService;
    private final PiattoService piattoService;

    public OrdinazioneController(OrdinazioneService ordinazioneService, RistoranteService ristoranteService,
            PiattoService piattoService){
        this.ordinazioneService = ordinazioneService;
        this.ristoranteService = ristoranteService;
        this.piattoService = piattoService;
    }

    /* Eseguito prima di ogni metodo della classe, ed e' anche il controllo di
       proprieta': un ristoratore che chiede un locale non suo, o disattivato,
       si ferma qui con un 403 e nessun metodo viene eseguito. */
    @ModelAttribute("ristorante")
    public Ristorante ristorante(@PathVariable Long ristoranteId, Authentication authentication) {
        return this.ristoranteService.ristoranteGestito(ristoranteId, authentication);
    }

    @PostMapping("/tavoli/{tavoloId}/ordinazione")
    public String apri(@PathVariable Long ristoranteId, @PathVariable Long tavoloId,
            RedirectAttributes redirectAttributes) {
        try {
            Ordinazione ordinazione = this.ordinazioneService.apriOrdinazione(ristoranteId, tavoloId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Conto aperto per il tavolo " + ordinazione.getTavolo().getNumeroTavolo() + ".");
            redirectAttributes.addAttribute("ordinazioneId", ordinazione.getId());
            return "redirect:/ristoranti/{ristoranteId}/ordinazioni/{ordinazioneId}";
        } catch (OrdinazioneNonValidaException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/ristoranti/{ristoranteId}/tavoli";
        }
    }

    /* L'annullamento di un'apertura sbagliata: cancella il conto senza emettere
       niente, e il servizio lo consente solo se il conto e' ancora vuoto. */
    @PostMapping("/ordinazioni/{ordinazioneId}/annulla")
    public String annulla(@PathVariable Long ristoranteId, @PathVariable Long ordinazioneId,
            RedirectAttributes redirectAttributes) {
        try {
            this.ordinazioneService.annullaApertura(ristoranteId, ordinazioneId);
            redirectAttributes.addFlashAttribute("successMessage", "Apertura annullata.");
            return "redirect:/ristoranti/{ristoranteId}/tavoli";
        } catch (OrdinazioneNonValidaException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addAttribute("ordinazioneId", ordinazioneId);
            return "redirect:/ristoranti/{ristoranteId}/ordinazioni/{ordinazioneId}";
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
        /* Solo i piatti disponibili: quelli disattivati non sono ordinabili, e
           il service rifiuterebbe comunque. Il filtro sta qui e non nella
           pagina, altrimenti finirebbero lo stesso nel browser. */
        model.addAttribute("menu", this.piattoService.getMenuDisponibile(ristoranteId));
        return "ordinazioni/dettaglio";
    }

    /* piattoId e quantita' come parametri sciolti e non un @ModelAttribute
       RigaOrdinazione: quell'entita' contiene ordinazione, piatto e prezzo, cioe'
       proprio i campi che deve decidere il server. */
    @PostMapping("/ordinazioni/{ordinazioneId}/righe")
    public String aggiungiPiatto(@PathVariable Long ristoranteId, @PathVariable Long ordinazioneId,
            @RequestParam Long piattoId,
            @RequestParam(defaultValue = "1") Integer quantita,
            RedirectAttributes redirectAttributes) {
        return modificaRighe(ristoranteId, ordinazioneId, redirectAttributes,
                () -> this.ordinazioneService.aggiungiPiatto(ristoranteId, ordinazioneId, piattoId, quantita));
    }

    @PostMapping("/ordinazioni/{ordinazioneId}/righe/diminuisci")
    public String diminuisciPiatto(@PathVariable Long ristoranteId, @PathVariable Long ordinazioneId,
            @RequestParam Long piattoId,
            @RequestParam(defaultValue = "1") Integer quantita,
            RedirectAttributes redirectAttributes) {
        return modificaRighe(ristoranteId, ordinazioneId, redirectAttributes,
                () -> this.ordinazioneService.diminuisciPiatto(ristoranteId, ordinazioneId, piattoId, quantita));
    }

    /* Le due azioni finiscono nello stesso posto e sbagliano allo stesso modo:
       la parte comune sta qui una volta sola. */
    private String modificaRighe(Long ristoranteId, Long ordinazioneId,
            RedirectAttributes redirectAttributes, Runnable azione) {
        try {
            azione.run();
        } catch (OrdinazioneNonValidaException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (DataIntegrityViolationException e) {
            /* Due aggiunte dello stesso piatto partite insieme: nessuna delle
               due trova la riga, entrambe la creano, il vincolo unico ne
               respinge una. I dati restano sani, serve solo dirlo. */
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Qualcun altro stava modificando questo conto: riprova.");
        }
        redirectAttributes.addAttribute("ordinazioneId", ordinazioneId);
        return "redirect:/ristoranti/{ristoranteId}/ordinazioni/{ordinazioneId}";
    }

    @PostMapping("/ordinazioni/{ordinazioneId}/chiudi")
    public String chiudi(@PathVariable Long ristoranteId, @PathVariable Long ordinazioneId,
                RedirectAttributes redirectAttributes) {
        try {
            Scontrino scontrino = this.ordinazioneService.chiudi(ristoranteId, ordinazioneId);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Conto del tavolo " + scontrino.getNumeroTavolo() + " chiuso.");
            redirectAttributes.addAttribute("scontrinoId", scontrino.getId());
            return "redirect:/ristoranti/{ristoranteId}/scontrini/{scontrinoId}";
        } catch (OrdinazioneNonValidaException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            redirectAttributes.addAttribute("ordinazioneId", ordinazioneId);
            return "redirect:/ristoranti/{ristoranteId}/ordinazioni/{ordinazioneId}";
        } catch (ObjectOptimisticLockingFailureException e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Questo conto e' appena stato chiuso da qualcun altro.");
            return "redirect:/ristoranti/{ristoranteId}/tavoli";
        }
    }
    
}
