package it.uniroma3.siw_ristorante.controller;

import org.springframework.stereotype.Controller;

import it.uniroma3.siw_ristorante.service.PiattoService;


@Controller
public class PiattoController {
    private final PiattoService piattoService;

    public PiattoController(PiattoService piattoService) {
        this.piattoService = piattoService;
    }

    

}
