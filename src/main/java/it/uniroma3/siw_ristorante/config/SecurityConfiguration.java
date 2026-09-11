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
        manager.setUsersByUsernameQuery(
                "SELECT username, password, true AS enabled FROM credentials WHERE username = ?");
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

            // pagine pubbliche di servizio
            authorize.requestMatchers(HttpMethod.GET, "/", "/index", "/register", "/login").permitAll();
            authorize.requestMatchers(HttpMethod.POST, "/register").permitAll();

            /* Consultazione pubblica: l'elenco dei ristoranti e il menu di un
               ristorante e le sue recensioni li vedono tutti, anche senza
               autenticazione. Solo queste GET sono pubbliche sotto
               /ristoranti: tutto il resto ricade nelle regole piu' sotto. */
            authorize.requestMatchers(HttpMethod.GET,
                    "/ristoranti",
                    "/ristoranti/*/menu",
                    "/ristoranti/*/recensioni").permitAll();

            /* Le recensioni le legge chiunque (regola qui sopra), ma le scrive
               solo un cliente: l'amministratore non recensisce il proprio
               locale. La regola sta prima di "/ristoranti/**", che altrimenti
               si prenderebbe anche questo percorso e risponderebbe 403. */
            authorize.requestMatchers("/ristoranti/*/recensioni", "/ristoranti/*/recensioni/**")
                    .hasAuthority(Credentials.DEFAULT_ROLE);

            /* "Le mie prenotazioni" e' una pagina da clienti: riservata al
               ruolo DEFAULT, quindi nemmeno l'amministratore la vede. */
            authorize.requestMatchers("/prenotazioni/**").hasAuthority(Credentials.DEFAULT_ROLE);

            /* Prenotare e' cosa da clienti, non da amministratori: serve il
               ruolo DEFAULT, e la regola va elencata qui sopra perche' piu'
               sotto "/ristoranti/**" si prenderebbe anche questo percorso. */
            authorize.requestMatchers("/ristoranti/*/prenotazioni/**")
                    .hasAuthority(Credentials.DEFAULT_ROLE);

            // funzionalita' riservate all'amministratore
            authorize.requestMatchers("/admin/**").hasAuthority(Credentials.ADMIN_ROLE);

            /* Tavoli e piatti non hanno pagine pubbliche, quindi basta una
               regola per ciascuno valida per qualunque metodo HTTP. Su
               /ristoranti/** questa regola raccoglie tutto cio' che non e'
               stato dichiarato pubblico sopra: /ristoranti/new,
               /ristoranti/{id}/edit e tutte le POST. */
            authorize.requestMatchers("/tavoli/**").hasAuthority(Credentials.ADMIN_ROLE);
            authorize.requestMatchers("/piatti/**").hasAuthority(Credentials.ADMIN_ROLE);
            authorize.requestMatchers("/ristoranti/**").hasAuthority(Credentials.ADMIN_ROLE);

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
