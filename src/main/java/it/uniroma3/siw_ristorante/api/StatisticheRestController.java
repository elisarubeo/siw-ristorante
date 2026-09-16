package it.uniroma3.siw_ristorante.api;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.uniroma3.siw_ristorante.dto.AffluenzaOraDto;
import it.uniroma3.siw_ristorante.dto.ContestoDto;
import it.uniroma3.siw_ristorante.dto.IncassoGiornoDto;
import it.uniroma3.siw_ristorante.dto.PiattoVendutoDto;
import it.uniroma3.siw_ristorante.dto.RiepilogoDto;
import it.uniroma3.siw_ristorante.dto.ScontriniMeseDto;
import it.uniroma3.siw_ristorante.dto.VotiRecensioniDto;
import it.uniroma3.siw_ristorante.service.StatisticheService;

@RestController
@RequestMapping("/api/statistiche")
@Tag(name = "Statistiche", description = "L'andamento del locale del ristoratore collegato")
@SecurityRequirement(name = "bearerAuth")
public class StatisticheRestController {

    private final StatisticheService statisticheService;

    public StatisticheRestController(StatisticheService statisticheService) {
        this.statisticheService = statisticheService;
    }

    @Operation(summary = "Il locale e gli anni disponibili",
            description = """
                    La prima chiamata che fa il frontend: senza, non saprebbe che cosa \
                    scrivere nel menu a tendina degli anni. Se anniDisponibili e' vuoto, \
                    il locale non ha ancora chiuso nessun conto e non c'e' altro da chiedere.""")
    @GetMapping("/contesto")
    public ContestoDto contesto(Authentication autenticazione) {
        return this.statisticheService.contesto(autenticazione);
    }

    @Operation(summary = "I numeri in cima alla pagina",
            description = """
                    Incasso, conti chiusi, scontrino medio, piatti serviti, durata media \
                    del turno, confronto con l'anno precedente e giudizio dei clienti. \
                    Alcuni campi possono essere null: significa "non c'e' niente da dire", \
                    che e' diverso da zero.""")
    @GetMapping("/riepilogo")
    public RiepilogoDto riepilogo(Authentication autenticazione, @RequestParam int anno) {
        return this.statisticheService.riepilogo(autenticazione, anno);
    }

    @Operation(summary = "Conti e incasso, mese per mese",
            description = "Sempre dodici voci, da 1 a 12: i mesi senza scontrini valgono zero.")
    @GetMapping("/scontrini-per-mese")
    public List<ScontriniMeseDto> scontriniPerMese(Authentication autenticazione, @RequestParam int anno) {
        return this.statisticheService.scontriniPerMese(autenticazione, anno);
    }

    @Operation(summary = "La classifica dei piatti",
            description = """
                    Ordinata per porzioni vendute. Il limite e' facoltativo: se manca vale 8, \
                    e in ogni caso non supera 20.""")
    @GetMapping("/piatti-piu-ordinati")
    public List<PiattoVendutoDto> piattiPiuOrdinati(Authentication autenticazione,
            @RequestParam int anno,
            @RequestParam(required = false) Integer limite) {
        return this.statisticheService.piattiPiuOrdinati(autenticazione, anno, limite);
    }

    @Operation(summary = "L'incasso per giorno della settimana",
            description = "Sempre sette voci, da 1 = lunedi a 7 = domenica (la convenzione ISO).")
    @GetMapping("/incasso-per-giorno")
    public List<IncassoGiornoDto> incassoPerGiorno(Authentication autenticazione, @RequestParam int anno) {
        return this.statisticheService.incassoPerGiorno(autenticazione, anno);
    }

    @Operation(summary = "A che ora si riempie la sala",
            description = """
                    Sempre ventiquattro voci, da 0 a 23. E' l'ora in cui i clienti si siedono, \
                    non quella in cui pagano.""")
    @GetMapping("/affluenza-per-ora")
    public List<AffluenzaOraDto> affluenzaPerOra(Authentication autenticazione, @RequestParam int anno) {
        return this.statisticheService.affluenzaPerOra(autenticazione, anno);
    }

    @Operation(summary = "Come giudicano il locale",
            description = """
                    Media e distribuzione dei voti, sempre cinque voci da 1 a 5 stelle. \
                    Niente parametro anno: e' il giudizio corrente, non un fatto di cassa.""")
    @GetMapping("/voti-recensioni")
    public VotiRecensioniDto votiRecensioni(Authentication autenticazione) {
        return this.statisticheService.votiRecensioni(autenticazione);
    }
}
