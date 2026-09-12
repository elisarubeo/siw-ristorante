package it.uniroma3.siw_ristorante.controller;

import java.util.Arrays;
import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import it.uniroma3.siw_ristorante.model.Ristorante;
import it.uniroma3.siw_ristorante.service.ImageStorageService;
import it.uniroma3.siw_ristorante.service.RistoranteService;

/* Le fotografie di un locale, viste da chi lo gestisce.

   La galleria la guardano tutti, dalla pagina del menu; qui invece si
   aggiunge e si toglie, ed e' roba del solo ristoratore di QUESTO ristorante.
   Il controllo non sta nell'indirizzo - "/ristoranti/2/immagini" e' lecito per
   un gestore e vietato per un altro, e la differenza sta nei dati - ma nel
   @ModelAttribute qui sotto, come negli altri controller di gestione. */
@Controller
@RequestMapping("/ristoranti/{ristoranteId}/immagini")
public class ImmagineRistoranteController {

    private final RistoranteService ristoranteService;
    private final ImageStorageService imageStorageService;

    public ImmagineRistoranteController(RistoranteService ristoranteService,
            ImageStorageService imageStorageService) {
        this.ristoranteService = ristoranteService;
        this.imageStorageService = imageStorageService;
    }

    /* Eseguito prima di ogni metodo della classe, ed e' anche il controllo di
       proprieta': un ristoratore che chiede un locale non suo, o disattivato,
       si ferma qui con un 403 e nessun metodo viene eseguito. E' cosi' che
       "solo le proprie immagini" diventa una regola e non una speranza. */
    @ModelAttribute("ristorante")
    public Ristorante ristorante(@PathVariable Long ristoranteId, Authentication authentication) {
        return this.ristoranteService.ristoranteGestito(ristoranteId, authentication);
    }

    @GetMapping
    public String gestisci(@PathVariable Long ristoranteId, Model model) {
        model.addAttribute("immagini", this.ristoranteService.immaginiDi(ristoranteId));
        return "ristoranti/immagini";
    }

    /* Piu' file in una volta sola: il campo della form ha l'attributo
       "multiple", quindi il browser puo' spedire un numero qualsiasi di
       immagini con lo stesso nome, e Spring le raccoglie nell'array. */
    @PostMapping
    public String carica(@PathVariable Long ristoranteId,
            @RequestParam(name = "file", required = false) MultipartFile[] file,
            RedirectAttributes redirectAttributes) {

        /* I formati si controllano PRIMA di salvare qualunque cosa: se una
           sola delle immagini scelte non va bene, non se ne carica nessuna e
           il ristoratore rifa' la selezione per intero, invece di ritrovarsi
           una galleria caricata a meta' senza sapere quale foto manca.
           Il giudizio su cosa sia un'immagine valida non e' scritto qui: lo
           da' ImageStorageService, che e' il posto in cui vivono i formati. */
        for (MultipartFile singolo : daCaricare(file)) {
            if (!this.imageStorageService.isSupported(singolo)) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Le immagini devono essere in formato JPG, PNG, WEBP o GIF: "
                                + "non e' stata caricata nessuna foto.");
                return "redirect:/ristoranti/{ristoranteId}/immagini";
            }
        }

        int caricate = this.ristoranteService.aggiungiImmagini(ristoranteId, file);
        if (caricate == 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Non hai scelto nessuna immagine.");
        } else {
            redirectAttributes.addFlashAttribute("successMessage",
                    caricate == 1 ? "Immagine aggiunta." : caricate + " immagini aggiunte.");
        }
        return "redirect:/ristoranti/{ristoranteId}/immagini";
    }

    /* Il nome del file viaggia in un campo nascosto e non nell'indirizzo:
       contiene un punto, e Spring MVC tratterebbe l'estensione come parte da
       interpretare. Che sia davvero una foto di questo locale lo verifica il
       service, che rimuove solo cio' che era nella sua galleria. */
    @PostMapping("/elimina")
    public String elimina(@PathVariable Long ristoranteId,
            @RequestParam("file") String file,
            RedirectAttributes redirectAttributes) {

        if (this.ristoranteService.rimuoviImmagine(ristoranteId, file)) {
            redirectAttributes.addFlashAttribute("successMessage", "Immagine eliminata.");
        } else {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Questa immagine non fa parte della galleria del tuo ristorante.");
        }
        return "redirect:/ristoranti/{ristoranteId}/immagini";
    }

    /* I soli file su cui ha senso ragionare: il campo di caricamento, se non
       si sceglie niente, viene spedito lo stesso ma vuoto. */
    private List<MultipartFile> daCaricare(MultipartFile[] file) {
        if (file == null) {
            return List.of();
        }
        return Arrays.stream(file)
                .filter(singolo -> singolo != null && !singolo.isEmpty())
                .toList();
    }
}
