package it.uniroma3.siw_ristorante.dto;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

/* La forma UNICA degli errori dell'API.

   Una forma sola vuol dire un punto solo da leggere in React: in tutto il
   frontend c'e' scritto error.response.data.message, e basta. Se ogni
   endpoint avesse il suo formato, ogni chiamata avrebbe il suo modo di
   scoprire che cosa e' andato storto.

   fieldErrors compare solo nei 400 di validazione. NON_NULL fa sparire il
   campo dal JSON negli altri casi, invece di mandarlo a null: "assente" e
   "presente e vuoto" sono due cose diverse per chi legge. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(String message, Map<String, String> fieldErrors) {

    /* Il caso normale: un messaggio e nient'altro. */
    public ApiError(String message) {
        this(message, null);
    }
}
