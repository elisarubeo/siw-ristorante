package it.uniroma3.siw_ristorante.model;

/* Due soli stati: una prenotazione e' presa oppure annullata. CONFIRMED e'
   stato tolto perche' nessuna operazione dell'applicazione ci portava: restava
   un valore irraggiungibile che l'interfaccia mostrava come "in attesa di
   conferma", promettendo una conferma che non sarebbe mai arrivata. */
public enum StatoPrenotazione {
    SCHEDULED,
    CANCELLED
}
