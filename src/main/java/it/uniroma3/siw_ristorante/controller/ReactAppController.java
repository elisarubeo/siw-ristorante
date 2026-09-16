package it.uniroma3.siw_ristorante.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/* L'indirizzo con cui si entra nella SPA. */
@Controller
public class ReactAppController {

    @GetMapping({ "/statistiche", "/statistiche/" })
    public String statistiche() {
        return "forward:/statistiche/index.html";
    }
}
