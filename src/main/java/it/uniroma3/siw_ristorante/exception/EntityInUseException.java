package it.uniroma3.siw_ristorante.exception;

public class EntityInUseException extends RuntimeException {

    public EntityInUseException(String message) {
        super(message);
    }

    public EntityInUseException(String message, Throwable cause) {
        super(message, cause);
    }
}
