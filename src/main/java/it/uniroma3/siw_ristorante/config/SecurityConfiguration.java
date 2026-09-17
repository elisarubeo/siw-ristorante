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

        httpSecurity.authorizeHttpRequests(authorize -> {

            // risorse statiche: sempre accessibili
            authorize.requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll();

            /* Le foto caricate dai ristoratori. Sono pubbliche come il resto
               della vetrina: compaiono nella pagina del locale, che si guarda
               senza aver fatto il login. Solo in lettura, pero': aggiungerle
               e toglierle passa dalle rotte "immagini" di un ristorante, che
               ricadono sotto le regole di gestione piu' in basso. */
            authorize.requestMatchers(HttpMethod.GET, "/uploads/**").permitAll();

            /* /error deve restare accessibile: quando una richiesta produce un
               404 o un 500, Spring la inoltra internamente qui. Se fosse
               protetta, ogni errore diventerebbe un redirect al login. */
            authorize.requestMatchers("/error").permitAll();

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
