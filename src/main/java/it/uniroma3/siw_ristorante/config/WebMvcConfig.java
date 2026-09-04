package it.uniroma3.siw_ristorante.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/* Un template da solo non e' raggiungibile: serve qualcosa che associ un
   indirizzo a un nome di vista. Queste pagine sono statiche (nessun dato dal
   database), quindi bastano i view controller e non un @Controller vero.
   In particolare formLogin().loginPage("/login") pretende che GET /login
   risponda: senza questa riga ogni accesso a una pagina protetta rimbalza su
   /login, che risponderebbe 404. */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("index");
        registry.addViewController("/index").setViewName("index");
        registry.addViewController("/login").setViewName("login");
    }
}
