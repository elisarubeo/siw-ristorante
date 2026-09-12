package it.uniroma3.siw_ristorante.config;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import it.uniroma3.siw_ristorante.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/* Legge il token dall'intestazione Authorization e dice a Spring Security chi
   sta chiamando.

   QUESTO FILTRO NON BLOCCA NIENTE, e non e' una svista. Se il token manca, e'
   scaduto o e' falso, la richiesta prosegue come anonima: a decidere se quella
   richiesta anonima puo' proseguire e' la catena di filtri, con le sue
   requestMatchers. Separare le due cose significa che i permessi si leggono
   tutti in un posto solo (SecurityConfiguration) invece che meta' li' e meta'
   qui dentro.

   OncePerRequestFilter e non Filter: garantisce una sola esecuzione per
   richiesta. Senza, un inoltro interno - per esempio verso /error - lo
   rifarebbe passare una seconda volta sulla stessa richiesta. */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String INTESTAZIONE = "Authorization";
    private static final String PREFISSO = "Bearer ";

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest richiesta, HttpServletResponse risposta,
            FilterChain catena) throws ServletException, IOException {

        String token = estraiToken(richiesta);
        String username = token == null ? null : this.jwtService.estraiUsername(token);

        /* La seconda condizione evita di sovrascrivere un'autenticazione gia'
           stabilita da un altro filtro: chi e' arrivato qui gia' riconosciuto
           resta quello che e'. */
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            String ruolo = this.jwtService.estraiRuolo(token);

            /* I permessi si ricostruiscono dal token, SENZA interrogare il
               database: e' esattamente il senso dello stateless, e il motivo
               per cui questa catena regge tante richieste senza una query di
               contorno. Il prezzo e' che un ruolo cambiato nel database non
               ha effetto finche' il vecchio token non scade.

               Niente prefisso ROLE_: le authorities sono le stesse stringhe
               che Credentials mette nella colonna role, cosi' le regole si
               scrivono con hasAuthority come nel resto dell'applicazione. */
            var permessi = ruolo == null
                    ? List.<SimpleGrantedAuthority>of()
                    : List.of(new SimpleGrantedAuthority(ruolo));

            var autenticazione = new UsernamePasswordAuthenticationToken(username, null, permessi);
            autenticazione.setDetails(new WebAuthenticationDetailsSource().buildDetails(richiesta));
            SecurityContextHolder.getContext().setAuthentication(autenticazione);
        }

        catena.doFilter(richiesta, risposta);
    }

    private String estraiToken(HttpServletRequest richiesta) {
        String intestazione = richiesta.getHeader(INTESTAZIONE);
        if (intestazione == null || !intestazione.startsWith(PREFISSO)) {
            return null;
        }
        return intestazione.substring(PREFISSO.length()).trim();
    }
}
