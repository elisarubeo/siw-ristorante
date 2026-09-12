package it.uniroma3.siw_ristorante.api;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.uniroma3.siw_ristorante.dto.LoginRequest;
import it.uniroma3.siw_ristorante.dto.LoginResponse;
import it.uniroma3.siw_ristorante.model.Credentials;
import it.uniroma3.siw_ristorante.service.CredentialsService;
import it.uniroma3.siw_ristorante.service.JwtService;
import jakarta.validation.Valid;

/* L'unico indirizzo pubblico dell'API: quello da cui si ottiene il token.

   Perche' esiste, visto che nel sito il login c'e' gia': sono due meccanismi
   diversi. Il sito riconosce l'utente da un cookie di sessione, l'API da un
   token che il browser allega a ogni richiesta, e un cookie non si trasforma
   in un token da solo. Chi passa dal sito alla SPA rifa' l'accesso. */
@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticazione", description = "Come ottenere il token per le altre chiamate")
public class AuthRestController {

    private final AuthenticationManager authenticationManager;
    private final CredentialsService credentialsService;
    private final JwtService jwtService;

    public AuthRestController(AuthenticationManager authenticationManager,
            CredentialsService credentialsService, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.credentialsService = credentialsService;
        this.jwtService = jwtService;
    }

    @Operation(summary = "Entra e ricevi un token",
            description = """
                    Restituisce un token da incollare in "Authorize" qui sopra. \
                    Con i dati di prova: borgo / borgo.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token emesso"),
            @ApiResponse(responseCode = "400", description = "Username o password mancanti"),
            @ApiResponse(responseCode = "401", description = "Credenziali errate, o gestore di un locale chiuso")
    })
    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest richiesta) {

        /* La verifica la fa l'AuthenticationManager, NON questo metodo.
           Confrontare gli hash a mano vorrebbe dire riscrivere - e prima o poi
           sbagliare - regole che esistono gia': il BCrypt, e soprattutto la
           colonna "enabled" calcolata nella UserDetailsService, che tiene
           fuori il gestore di un ristorante disattivato. Cosi' l'API e il form
           login del sito accettano e rifiutano esattamente le stesse persone.

           Se le credenziali non vanno, di qui esce un'AuthenticationException
           che ApiExceptionHandler trasforma nel 401. */
        this.authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(richiesta.username(), richiesta.password()));

        /* Arrivati qui l'utente esiste ed e' abilitato: la sua riga c'e'. */
        Credentials credenziali = this.credentialsService.getCredentials(richiesta.username())
                .orElseThrow(() -> new IllegalStateException(
                        "Autenticato ma senza credenziali: " + richiesta.username()));

        String token = this.jwtService.generaToken(credenziali.getUsername(), credenziali.getRole());

        /* userId viaggia nel corpo e non nel token: serve all'interfaccia, non
           ad autorizzare, e il contenuto di un token e' leggibile da chiunque. */
        return new LoginResponse(token, credenziali.getUsername(),
                credenziali.getUser().getId(), credenziali.getRole());
    }
}
