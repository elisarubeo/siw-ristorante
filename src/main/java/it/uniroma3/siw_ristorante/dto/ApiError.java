package it.uniroma3.siw_ristorante.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String message, Map<String, String> fieldErrors) {

    /* Il caso normale: un messaggio e nient'altro. */
    public ApiError(String message) {
        this(message, null);
    }
}
