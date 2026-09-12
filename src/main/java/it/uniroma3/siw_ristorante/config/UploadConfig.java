package it.uniroma3.siw_ristorante.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import it.uniroma3.siw_ristorante.service.ImageStorageService;

/* Rende accessibili dal browser le immagini caricate: /uploads/<nome file>.

   I file stanno in una cartella esterna al progetto (uploads/ristoranti), non
   fra le risorse statiche dentro il JAR, quindi Spring non li servirebbe da
   solo: qui si aggiunge un resource handler che mappa /uploads/** su quella
   cartella.

   Serve un handler e non un controller che legga i byte: cosi' se ne occupa
   Spring, che gestisce per conto suo il tipo di contenuto, gli header di cache
   e le richieste parziali, e soprattutto rifiuta i percorsi sospetti.

   La cartella la conosce ImageStorageService, che la crea all'avvio: prenderla
   da li' evita di leggere due volte la stessa proprieta' e di ritrovarsi, un
   giorno, a scrivere in una cartella e a leggere da un'altra. */
@Configuration
public class UploadConfig implements WebMvcConfigurer {

    private final ImageStorageService imageStorageService;

    public UploadConfig(ImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String cartella = this.imageStorageService.getDirectory().toUri().toString();
        /* La location DEVE finire con "/": senza, Spring la interpreta come un
           file e ogni immagine risponde 404. */
        if (!cartella.endsWith("/")) {
            cartella = cartella + "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(cartella);
    }
}
