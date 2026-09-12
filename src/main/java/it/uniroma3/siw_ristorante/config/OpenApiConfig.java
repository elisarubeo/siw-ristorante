package it.uniroma3.siw_ristorante.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;

/* La documentazione dell'API, generata da springdoc leggendo le annotazioni
   dei controller e pubblicata su /swagger-ui.html.

   Non serve solo a chi legge: e' il posto da cui si provano gli endpoint uno
   per uno mentre li si scrive, senza React di mezzo. Quando un grafico e'
   vuoto, la prima domanda e' "l'API risponde?", e la si risponde qui.

   Il SecurityScheme e' quello che fa comparire il pulsante "Authorize":
   senza, ogni chiamata partirebbe senza token e risponderebbe 401. */
@Configuration
public class OpenApiConfig {

    private static final String SCHEMA = "bearerAuth";

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Statistiche del ristoratore")
                        .version("1.0")
                        .description("""
                                La parte REST di siw-ristorante, quella che alimenta la SPA React \
                                pubblicata su /statistiche/.

                                Come si prova: POST /api/auth/login con le credenziali di un \
                                ristoratore (per esempio borgo / borgo), poi si copia il campo \
                                "token" della risposta e lo si incolla in Authorize, qui sopra. \
                                Da quel momento tutte le chiamate partono con l'intestazione \
                                Authorization e gli endpoint sotto /api/statistiche rispondono.

                                Il ristorante non compare mai nell'indirizzo: e' quello del \
                                gestore autenticato, ricavato dal token."""))
                .components(new Components().addSecuritySchemes(SCHEMA,
                        new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Incolla qui il token ricevuto da /api/auth/login.")));
    }
}
