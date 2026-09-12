package it.uniroma3.siw_ristorante.exception;

/* Il file caricato non e' un'immagine utilizzabile: formato non ammesso, file
   vuoto, oppure un nome che non si puo' risolvere dentro la cartella delle
   immagini.

   E' distinta dalle altre eccezioni del dominio (un piatto, una prenotazione,
   una recensione) perche' non riguarda una regola del ristorante ma il file in
   se': chi la riceve deve far scegliere un altro file, non spiegare che
   l'operazione era impossibile. */
public class InvalidImageException extends RuntimeException {

    public InvalidImageException(String message) {
        super(message);
    }
}
