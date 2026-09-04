package it.uniroma3.siw_ristorante.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import it.uniroma3.siw_ristorante.model.Credentials;
import it.uniroma3.siw_ristorante.model.User;
import it.uniroma3.siw_ristorante.service.CredentialsService;
import jakarta.validation.Valid;

@Controller
public class UserController {
    private final CredentialsService credentialsService;

    public UserController(CredentialsService credentialsService) {
        this.credentialsService = credentialsService;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String register(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("credentials", new Credentials());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("user") User user,
                           BindingResult userBindingResult,
                           @Valid @ModelAttribute("credentials") Credentials credentials,
                           BindingResult credentialsBindingResult) {

        
        if (this.credentialsService.getCredentials(credentials.getUsername()).isPresent()) {
            credentialsBindingResult.rejectValue("username", "credentials.duplicate",
                    "Questo username è già in uso");
        }

        if (userBindingResult.hasErrors() || credentialsBindingResult.hasErrors()) {
            return "register";
        }

       
        this.credentialsService.registerUser(user, credentials);
        return "redirect:/login";
    }
}
