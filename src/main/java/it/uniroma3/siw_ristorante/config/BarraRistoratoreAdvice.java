package it.uniroma3.siw_ristorante.config;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import it.uniroma3.siw_ristorante.model.Credentials;
import it.uniroma3.siw_ristorante.service.RistoranteService;

/* L'unico dato che la barra in alto chiede a tutte le pagine. */
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
