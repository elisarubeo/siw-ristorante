package it.uniroma3.siw_ristorante.exception;

import org.springframework.ui.Model;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice(basePackages = "it.uniroma3.siw_ristorante.controller")
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(ResourceNotFoundException e, Model model) {
        model.addAttribute("errorMessage", "Pagina non trovata");
        return "error/404";
    }

    /* Il rifiuto che nasce dentro un controller, tipicamente il controllo di
       proprieta' su un ristorante. Senza questo metodo se lo prenderebbe il
       gestore generico qui sotto, e un divieto diventerebbe un errore 500.
       I rifiuti decisi dalle regole sulla catena dei filtri non passano di
       qui: quelli Spring Boot li manda da se' alla stessa pagina 403. */
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public String handleAccessDenied(AccessDeniedException e, Model model) {
        model.addAttribute("errorMessage", "Non hai i permessi per questa pagina");
        return "error/403";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleUnexpectedException(Exception e, Model model) {
        model.addAttribute("errorMessage","Si è verificato un errore. Riprovare più tardi.");
        return "error/500";
    }

}
