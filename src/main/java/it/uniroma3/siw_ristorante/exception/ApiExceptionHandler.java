package it.uniroma3.siw_ristorante.exception;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import it.uniroma3.siw_ristorante.dto.ApiError;

@RestControllerAdvice(basePackages = "it.uniroma3.siw_ristorante.api")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ApiExceptionHandler {

    /* 400: il corpo della richiesta non ha superato la Bean Validation.
       E' l'unico caso in cui la risposta porta anche fieldErrors, perche' e'
       l'unico in cui il frontend puo' indicare QUALE campo correggere. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> datiNonValidi(MethodArgumentNotValidException e) {
        /* LinkedHashMap e non HashMap: conserva l'ordine in cui i campi sono
           dichiarati, cosi' gli errori compaiono nell'ordine del modulo. */
        Map<String, String> erroriDeiCampi = new LinkedHashMap<>();
        for (FieldError errore : e.getBindingResult().getFieldErrors()) {
            /* merge: se un campo ha piu' violazioni se ne mostra una sola.
               Tre messaggi sullo stesso campo non aiutano nessuno. */
            erroriDeiCampi.putIfAbsent(errore.getField(), errore.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(new ApiError("Dati non validi.", erroriDeiCampi));
    }

    /* 401: credenziali sbagliate, o account disabilitato (il gestore di un
       locale chiuso). Il messaggio non distingue i due casi di proposito:
       dire "utente giusto, password sbagliata" e' un modo per far sapere a
       chi prova quali nomi utente esistono. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiError> nonAutenticato(AuthenticationException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiError("Nome utente o password non corretti."));
    }

    /* 403: riconosciuto, ma questa cosa non gli spetta. Diverso dal 401, e la
       differenza guida il frontend: sul 401 si rifa' il login, sul 403 no. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> vietato(AccessDeniedException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ApiError("Non hai i permessi per vedere queste statistiche."));
    }

    /* 404: qui vuol dire quasi sempre una cosa sola, cioe' che l'account
       autenticato non gestisce nessun ristorante. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> nonTrovato(ResourceNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiError("Questo account non gestisce nessun ristorante."));
    }

    /* 500: all'utente un messaggio generico, nei log tutto il resto.
       Rimandare indietro il messaggio dell'eccezione sarebbe comodo per
       sviluppare e imprudente in consegna: racconta nomi di classi, di
       tabelle e a volte pezzi di query. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> imprevisto(Exception e) {
        org.slf4j.LoggerFactory.getLogger(ApiExceptionHandler.class)
                .error("Errore non gestito in una chiamata all'API", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiError("Si è verificato un errore. Riprovare più tardi."));
    }
}
