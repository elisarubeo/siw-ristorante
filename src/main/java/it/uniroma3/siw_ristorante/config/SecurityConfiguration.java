package it.uniroma3.siw_ristorante.config;

import java.io.IOException;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/* tools.jackson e non com.fasterxml.jackson: Spring Boot 4 e' passato a
   Jackson 3, che ha cambiato package. Sul classpath c'e' anche la 2.x, tirata
   dentro da jjwt e da springdoc, e importare quella darebbe un errore poco
   chiaro - "nessun bean di tipo ObjectMapper" - perche' il bean di Spring e'
   dell'altro tipo. */
import tools.jackson.databind.ObjectMapper;

import it.uniroma3.siw_ristorante.dto.ApiError;
import it.uniroma3.siw_ristorante.model.Credentials;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    private final DataSource dataSource;

    public SecurityConfiguration(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Bean
    public UserDetailsService userDetailsService() {
        JdbcUserDetailsManager manager = new JdbcUserDetailsManager(dataSource);
        /* La colonna "enabled" non e' memorizzata: si calcola. Un account vale
           finche' il ristorante che gestisce e' attivo, e per tutti gli altri
           (amministratore e clienti, che non gestiscono niente) il LEFT JOIN
           non trova righe e COALESCE lascia true. Cosi' il gestore di un
           locale disattivato non riesce nemmeno a entrare, e non serve
           ricordarsi di controllarlo in ogni pagina. */
        manager.setUsersByUsernameQuery(
                "SELECT c.username, c.password, COALESCE(r.attivo, true) AS enabled"
                        + " FROM credentials c LEFT JOIN ristorante r ON r.gestore_id = c.user_id"
                        + " WHERE c.username = ?");
        manager.setAuthoritiesByUsernameQuery(
                "SELECT username, role FROM credentials WHERE username = ?");
        return manager;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    /* L'oggetto che verifica davvero username e password. Serve al
       AuthRestController, che non deve confrontare hash a mano: cosi' vale la
       stessa UserDetailsService del form login, compreso il COALESCE che
       calcola "enabled". Un gestore di un locale disattivato non entra
       nemmeno dall'API, e la regola e' scritta una volta sola. */
    @Bean
    public AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    /* Da dove si accetta che il browser chiami l'API.

       In sviluppo la pagina arriva da Vite (:5173) e i dati da Spring (:8080):
       due origini diverse, e senza CORS il browser scarta la risposta. Il
       proxy di Vite di solito evita il problema, ma configurarlo permette di
       chiamare il backend anche direttamente.

       ATTENZIONE a due punti. Primo: CORS va dichiarato come bean e agganciato
       alla catena con .cors(...), non come WebMvcConfigurer - Spring Security
       gira PRIMA di Spring MVC, e il preflight OPTIONS si prenderebbe un 401
       senza mai arrivare a MVC. Secondo: fra le intestazioni ammesse ci deve
       essere Authorization, altrimenti il browser non spedisce il token e
       ogni richiesta risulta anonima. */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configurazione = new CorsConfiguration();
        configurazione.setAllowedOrigins(List.of("http://localhost:5173", "http://localhost:4173"));
        configurazione.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configurazione.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource sorgente = new UrlBasedCorsConfigurationSource();
        sorgente.registerCorsConfiguration("/api/**", configurazione);
        return sorgente;
    }

    /* LA CATENA DELLA PARTE REST.

       @Order(1) e securityMatcher("/api/**") insieme vogliono dire: le
       richieste che cominciano per /api passano di qui e non toccano mai la
       catena web. Sono due mondi separati nella stessa applicazione - stesso
       database di credenziali, stesso BCrypt, due modi diversi di riconoscere
       chi chiama.

       Le tre righe di configurazione che seguono sono conseguenze l'una
       dell'altra: niente sessione, quindi niente cookie; niente cookie,
       quindi CSRF non e' attaccabile e si puo' disattivare. Sulla catena
       Thymeleaf, che i cookie li usa, disattivarlo sarebbe un buco. */
    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity httpSecurity,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectMapper objectMapper) throws Exception {

        httpSecurity.securityMatcher("/api/**");
        httpSecurity.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        httpSecurity.csrf(csrf -> csrf.disable());
        httpSecurity.sessionManagement(sessione -> sessione.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        httpSecurity.authorizeHttpRequests(authorize -> {
            /* L'unico indirizzo pubblico: e' quello che serve per ottenere il
               token, e chiederlo gia' autenticati non avrebbe senso. */
            authorize.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll();

            /* Le statistiche sono i dati di cassa di un locale: niente
               letture pubbliche, a differenza per esempio delle recensioni.
               hasAuthority e non hasRole perche' le authorities sono le
               stringhe di Credentials, senza prefisso ROLE_. */
            authorize.requestMatchers("/api/statistiche/**").hasAuthority(Credentials.RISTORATORE_ROLE);

            authorize.anyRequest().authenticated();
        });

        /* Prima del filtro che gestisce il form login: quando la richiesta
           arriva li', il token e' gia' stato letto e l'utente riconosciuto. */
        httpSecurity.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        /* SENZA QUESTO BLOCCO, UNA RICHIESTA SENZA TOKEN RICEVE 403, NON 401.
           Spring Security, non trovando su questa catena nessun modo di
           chiedere le credenziali (niente form, niente Basic), ripiega sul
           403. Ma la differenza conta per il frontend: sul 401 React svuota il
           deposito e manda al login, il 403 vuol dire "sei riconosciuto, ma
           questo non ti spetta".

           Si scrive a mano sulla risposta perche' qui siamo dentro i filtri:
           ApiExceptionHandler entra in gioco piu' avanti, sui controller, e
           queste due situazioni ai controller non ci arrivano nemmeno. La
           forma JSON pero' e' la stessa, cosi' il frontend ha sempre un solo
           campo da leggere. */
        httpSecurity.exceptionHandling(eccezioni -> {
            eccezioni.authenticationEntryPoint((richiesta, risposta, e) -> scriviErrore(risposta, objectMapper,
                    HttpStatus.UNAUTHORIZED, "Devi accedere per vedere le statistiche."));
            eccezioni.accessDeniedHandler((richiesta, risposta, e) -> scriviErrore(risposta, objectMapper,
                    HttpStatus.FORBIDDEN, "Non hai i permessi per vedere queste statistiche."));
        });

        return httpSecurity.build();
    }

    /* Un ApiError scritto direttamente sulla risposta, nella stessa forma che
       usa ApiExceptionHandler. */
    private void scriviErrore(HttpServletResponse risposta, ObjectMapper objectMapper,
            HttpStatus stato, String messaggio) throws IOException {
        risposta.setStatus(stato.value());
        risposta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        risposta.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(risposta.getWriter(), new ApiError(messaggio));
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity httpSecurity) throws Exception {

        /* La catena di tutto il resto del sito: form login, sessione, cookie.
           Le richieste a /api non arrivano mai qui, se le prende la catena
           @Order(1) con il suo securityMatcher.

           ATTENZIONE ALL'ORDINE: vince la prima regola che corrisponde alla
           richiesta, quindi le regole piu' specifiche vanno prima di quelle
           generiche. Le poche rotte pubbliche vanno elencate una per una PRIMA
           delle regole generiche di amministrazione, altrimenti
           "/ristoranti/**" si prende anche "/ristoranti" e il menu. */
        httpSecurity.authorizeHttpRequests(authorize -> {

            // risorse statiche: sempre accessibili
            authorize.requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll();

            /* /error deve restare accessibile: quando una richiesta produce un
               404 o un 500, Spring la inoltra internamente qui. Se fosse
               protetta, ogni errore diventerebbe un redirect al login. */
            authorize.requestMatchers("/error").permitAll();

            /* La SPA delle statistiche: HTML, JavaScript e CSS, cioe' codice
               che gira nel browser di chi guarda. Pubblico come il foglio di
               stile del sito, per lo stesso motivo. I DATI stanno dietro
               /api/statistiche/**, sull'altra catena, e senza un token con il
               ruolo giusto non escono.
               Senza questo permitAll, anyRequest().authenticated() manderebbe
               la SPA al form login di Thymeleaf, che non e' il suo: lei il
               login ce l'ha per conto proprio, ed e' a token. */
            authorize.requestMatchers("/statistiche", "/statistiche/**").permitAll();

            /* Swagger. Tre percorsi e non uno: la pagina, i file che la
               compongono e il documento OpenAPI che la riempie. Se ne manca
               uno l'interfaccia si apre vuota, ed e' un errore che sembra un
               problema di springdoc mentre e' di permessi. */
            authorize.requestMatchers("/swagger-ui.html", "/swagger-ui/**",
                    "/v3/api-docs", "/v3/api-docs/**").permitAll();

            /* La registrazione pubblica crea solo clienti: l'account di un
               ristoratore lo crea l'amministratore insieme al locale. */
            authorize.requestMatchers(HttpMethod.GET, "/", "/index", "/register", "/login").permitAll();
            authorize.requestMatchers(HttpMethod.POST, "/register").permitAll();

            /* Consultazione pubblica: l'elenco dei ristoranti, il menu di un
               ristorante e le sue recensioni li vedono tutti, anche senza
               autenticazione. Un ristorante disattivato non compare in elenco
               e le sue pagine rispondono 404, ma il filtro sta nel service:
               qui non si puo' esprimere. */
            authorize.requestMatchers(HttpMethod.GET,
                    "/ristoranti",
                    "/ristoranti/*/menu",
                    "/ristoranti/*/recensioni").permitAll();

            /* Cose da clienti. Prenotare e recensire e' loro mestiere: ne'
               l'amministratore ne' il ristoratore hanno queste voci. */
            authorize.requestMatchers("/prenotazioni/**").hasAuthority(Credentials.DEFAULT_ROLE);
            authorize.requestMatchers("/ristoranti/*/prenotazioni/**").hasAuthority(Credentials.DEFAULT_ROLE);
            authorize.requestMatchers("/ristoranti/*/recensioni", "/ristoranti/*/recensioni/**")
                    .hasAuthority(Credentials.DEFAULT_ROLE);

            /* L'amministratore della piattaforma fa due cose sole, e stanno
               tutte e due qui sotto: crea un ristorante con le sue credenziali
               e lo disattiva. Non entra nella gestione dei locali. */
            authorize.requestMatchers("/admin/**").hasAuthority(Credentials.ADMIN_ROLE);

            /* Tutto il resto sotto /ristoranti/** e' la gestione del locale:
               menu, tavoli, conti, scontrini, agenda. E' mestiere del
               ristoratore.

               ATTENZIONE: questa regola dice "un ristoratore", non "IL
               ristoratore di quel locale". Il secondo controllo non e'
               esprimibile in un indirizzo e vive in
               RistoranteService.ristoranteGestito, richiamato dal
               @ModelAttribute di ogni controller di gestione. */
            authorize.requestMatchers("/mio-ristorante").hasAuthority(Credentials.RISTORATORE_ROLE);
            authorize.requestMatchers("/ristoranti/**").hasAuthority(Credentials.RISTORATORE_ROLE);

            // tutto il resto richiede un utente autenticato
            authorize.anyRequest().authenticated();
        });

        httpSecurity.formLogin(form -> {
            form.loginPage("/login").permitAll();
            form.defaultSuccessUrl("/", true);
            form.failureUrl("/login?error=true");
        });

        httpSecurity.logout(logout -> {
            logout.logoutUrl("/logout");
            logout.logoutSuccessUrl("/");
            logout.invalidateHttpSession(true);
            logout.deleteCookies("JSESSIONID");
            logout.clearAuthentication(true);
            logout.permitAll();
        });

        return httpSecurity.build();
    }
}
