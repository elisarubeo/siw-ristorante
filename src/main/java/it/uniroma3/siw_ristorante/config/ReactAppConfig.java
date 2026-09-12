package it.uniroma3.siw_ristorante.config;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/* Serve la SPA buildata, e soprattutto fa in modo che ricaricare la pagina su
   una rotta React non dia 404.

   IL PROBLEMA. Dentro l'applicazione React il cambio di pagina non passa dal
   server: e' JavaScript che riscrive l'indirizzo nella barra e ridisegna.
   Ma se a quel punto si preme F5, il browser chiede DAVVERO
   /statistiche/login al server - un indirizzo a cui non corrisponde nessun
   file, perche' l'unico file che esiste e' index.html.

   LA SOLUZIONE SBAGLIATA, quella che viene in mente per prima: un controller
   che inoltra tutto /statistiche/** a index.html. Cosi' pero' si intercetta
   anche /statistiche/assets/index-a1b2c3.js, e il browser riceve una pagina
   HTML dove si aspetta del JavaScript. L'errore che ne esce parla di sintassi
   e manda a cercare il bug nella parte sbagliata del progetto.

   LA SOLUZIONE. Un resolver che prima va a vedere se il file esiste davvero.
   Se esiste, lo serve (e i .js restano .js). Se non esiste, allora e' una
   rotta React e si consegna index.html, che ricostruisce la pagina da se'
   leggendo l'indirizzo. */
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
