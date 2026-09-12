/**
 * I controller della parte REST, quella che serve la SPA React delle
 * statistiche. Sono {@code @RestController}: restituiscono oggetti, che
 * Jackson trasforma in JSON, e non nomi di viste Thymeleaf.
 *
 * <p><b>Perche' un package separato da {@code controller}.</b> Non e' ordine
 * fine a se stesso: il {@link it.uniroma3.siw_ristorante.exception.GlobalExceptionHandler}
 * esistente e' dichiarato
 * {@code @ControllerAdvice(basePackages = "it.uniroma3.siw_ristorante.controller")}
 * e per ogni eccezione restituisce il nome di una pagina di errore. Se
 * intercettasse anche le chiamate a {@code /api}, il frontend riceverebbe
 * l'HTML della pagina 500 al posto del JSON, e axios fallirebbe nel leggerlo.
 * Tenendo i due gruppi di controller in package disgiunti, i due advice si
 * possono selezionare per {@code basePackages} e non si pestano i piedi.
 *
 * <p>Regole di casa:
 * <ul>
 *   <li>mai un'entita' JPA come valore di ritorno: si usano i record di
 *       {@link it.uniroma3.siw_ristorante.dto};</li>
 *   <li>il ristorante di cui si leggono le statistiche non arriva
 *       dall'indirizzo ma dall'{@code Authentication}: cosi' non esiste un id
 *       da cambiare nell'URL per leggere la cassa di un altro locale;</li>
 *   <li>il codice di stato e' parte della risposta quanto il corpo:
 *       200, 401 (non autenticato), 403 (autenticato ma non suo), 404, 400.</li>
 * </ul>
 */
package it.uniroma3.siw_ristorante.api;
