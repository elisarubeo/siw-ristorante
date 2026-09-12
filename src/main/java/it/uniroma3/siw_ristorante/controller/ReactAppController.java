package it.uniroma3.siw_ristorante.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/* L'indirizzo con cui si entra nella SPA.

   Serve solo per /statistiche senza barra finale: quello e' un percorso a cui
   il gestore delle risorse statiche non sa rispondere, perche' non e' ne' un
   file ne' una cartella. Il forward lo trasforma nella richiesta di
   index.html senza cambiare l'indirizzo nella barra, cosi' React parte con il
   percorso che l'utente ha davvero scritto.

   @Controller e non @RestController: qui non si restituisce un corpo JSON ma
   un'istruzione per Spring MVC ("inoltra a quest'altra risorsa"). Con
   @RestController quella stringa finirebbe scritta nella pagina cosi' com'e'.

   E per lo stesso motivo sta nel package "controller" e non in "api", pur
   riguardando la parte React: cio' che consegna e' una pagina, quindi se
   qualcosa va storto deve rispondere il GlobalExceptionHandler con la pagina
   d'errore, non ApiExceptionHandler con un JSON che il browser mostrerebbe
   come testo nudo. I due advice si dividono il lavoro per package. */
@Controller
public class ReactAppController {

    @GetMapping({ "/statistiche", "/statistiche/" })
    public String statistiche() {
        return "forward:/statistiche/index.html";
    }
}
