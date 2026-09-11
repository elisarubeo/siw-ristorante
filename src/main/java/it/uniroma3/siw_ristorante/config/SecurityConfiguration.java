package it.uniroma3.siw_ristorante.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import it.uniroma3.siw_ristorante.model.Credentials;

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
    
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity httpSecurity) throws Exception {

        /* ATTENZIONE ALL'ORDINE: vince la prima regola che corrisponde alla
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
