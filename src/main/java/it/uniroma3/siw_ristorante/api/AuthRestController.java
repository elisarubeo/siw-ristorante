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
