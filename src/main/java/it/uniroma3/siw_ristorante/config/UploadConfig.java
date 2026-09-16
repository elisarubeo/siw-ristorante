package it.uniroma3.siw_ristorante.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import it.uniroma3.siw_ristorante.service.ImageStorageService;

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
