package it.uniroma3.siw_ristorante.config;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
public class ReactAppConfig implements WebMvcConfigurer {

    /* Questi tre valori devono combaciare, o i file danno 404 o le rotte non
       risolvono:
         - qui sotto,
         - "base" in frontend/vite.config.ts,
         - il "basename" di <BrowserRouter> in frontend/src/main.tsx. */
    private static final String PERCORSO = "/statistiche/**";
    private static final String CARTELLA = "classpath:/static/statistiche/";
    private static final String PAGINA = "/static/statistiche/index.html";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        registro.addResourceHandler(PERCORSO)
                .addResourceLocations(CARTELLA)
                .resourceChain(false)
                .addResolver(new PathResourceResolver() {
                    @Override
                    protected Resource getResource(String percorsoRichiesto, Resource cartella)
                            throws IOException {

                        /* Il percorso vuoto o che finisce con "/" e' la
                           cartella stessa, non un file: createRelative
                           risponderebbe che "esiste" e si finirebbe a servire
                           una directory. E' proprio il caso di
                           /statistiche/, che si vuole risolvere in index.html. */
                        if (percorsoRichiesto.isEmpty() || percorsoRichiesto.endsWith("/")) {
                            return new ClassPathResource(PAGINA);
                        }

                        Resource richiesto = cartella.createRelative(percorsoRichiesto);
                        if (richiesto.exists() && richiesto.isReadable()) {
                            return richiesto;
                        }

                        /* Non e' un file: e' una rotta React. */
                        return new ClassPathResource(PAGINA);
                    }
                });
    }
}
