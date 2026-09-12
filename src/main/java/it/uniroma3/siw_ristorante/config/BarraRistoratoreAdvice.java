package it.uniroma3.siw_ristorante.config;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import it.uniroma3.siw_ristorante.model.Credentials;
import it.uniroma3.siw_ristorante.service.RistoranteService;

/* L'unico dato che la barra in alto chiede a tutte le pagine.

   La barra del ristoratore porta le voci di gestione - conti aperti,
   scontrini, agenda - e ognuna ha bisogno dell'id del locale. Le pagine di
   gestione ce l'hanno nel model, ma le altre no: l'elenco dei ristoranti, la
   pagina di un collega, una pagina di errore non parlano di un locale
   preciso, e li' le voci sparivano.

   L'id non si puo' prendere da ${ristorante}: sulle pagine pubbliche quella
   variabile e' il ristorante che si sta guardando, che per un ristoratore puo'
   benissimo essere quello di un altro, e i collegamenti porterebbero a un 403.
   Quello che serve e' un'altra cosa: il locale di CHI GUARDA, che non dipende
   dalla pagina. Da qui il @ControllerAdvice, che aggiunge l'attributo al model
   di ogni pagina invece di farlo ripetere a dodici controller.

   Senza basePackages apposta: cosi' l'attributo arriva anche alle pagine di
   errore, che sono disegnate da un controller di Spring e non nostro. I
   @RestController lo ricevono e lo ignorano, perche' rispondono JSON e il
   model non lo guardano nemmeno. */
@ControllerAdvice
public class BarraRistoratoreAdvice {

    private final RistoranteService ristoranteService;

    public BarraRistoratoreAdvice(RistoranteService ristoranteService) {
        this.ristoranteService = ristoranteService;
    }

    /* null per tutti gli altri: clienti, amministratore e chi non ha fatto il
       login non hanno un locale, e la barra non mostra loro quelle voci. */
    @ModelAttribute("mioRistoranteId")
    public Long mioRistoranteId(Authentication authentication) {
        if (!ristoratore(authentication)) {
            return null;
        }
        try {
            return this.ristoranteService.ristoranteDelGestore(authentication).getId();
        } catch (RuntimeException nessunLocale) {
            /* Un account RISTORATORE il cui locale e' stato chiuso
               dall'amministratore esiste ancora e puo' entrare. Qui vale null
               come per tutti gli altri: senza questa rete, ogni pagina che
               apre - compresa quella di errore - finirebbe in errore a sua
               volta, e non riuscirebbe nemmeno a leggere il perche'. */
            return null;
        }
    }

    private boolean ristoratore(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().stream()
                        .anyMatch(autorita -> Credentials.RISTORATORE_ROLE.equals(autorita.getAuthority()));
    }
}
