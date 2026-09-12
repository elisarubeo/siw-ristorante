package it.uniroma3.siw_ristorante.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderColumn;
import jakarta.validation.constraints.NotBlank;

@Entity
public class Ristorante {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotBlank(message = "Il nome del ristorante non può essere vuoto")
    @Column(nullable = false)
    private String nome;

    @NotBlank(message = "L'indirizzo del ristorante non può essere vuoto")
    @Column(nullable = false)
    private String indirizzo;

    /* Chi gestisce il locale: l'account che l'amministratore crea insieme al
       ristorante. Uno e uno solo, e non condiviso con altri ristoranti (unique
       sulla colonna), perche' il ristoratore risponde di un locale soltanto. */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gestore_id", nullable = false, unique = true)
    private User gestore;

    /* Un ristorante disattivato e' come un profilo chiuso: sparisce dal sito e
       il suo gestore non puo' piu' operare. I dati pero' restano, perche'
       l'amministratore puo' riattivarlo. */
    @Column(nullable = false)
    private boolean attivo = true;

    /* Tavoli e piatti compongono il ristorante: sono insiemi piccoli, li si
       vuole quasi sempre per intero, e il cascade risolve la cancellazione
       (le loro colonne ristorante_id sono NOT NULL, quindi senza di esso
       cancellare un ristorante violerebbe la foreign key).
       Recensioni e prenotazioni invece non stanno qui: crescono senza limite e
       le si vuole sempre filtrate, ordinate e paginate, cioe' dai repository. */
    @OneToMany(mappedBy = "ristorante", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tavolo> tavoli = new ArrayList<>();

    @OneToMany(mappedBy = "ristorante", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Piatto> piatti = new ArrayList<>();

    /* Le fotografie del locale: sala, dehors, un piatto in tavola. Nel
       database non ci sono le immagini ma i NOMI dei file che le contengono -
       i byte stanno su disco, nella cartella di ImageStorageService, e il
       browser li chiede a /uploads/<nome>.

       @ElementCollection e non un'entita' a se': un'immagine qui e' solo una
       stringa che non esiste fuori dal suo ristorante, non ha un'identita'
       propria ne' qualcuno che la referenzi. Hibernate le tiene in una
       tabella a parte, ristorante_immagine, e le cancella con il ristorante.

       @OrderColumn aggiunge la colonna "posizione": senza, l'ordine in cui si
       rileggono non e' garantito e la galleria si rimescolerebbe a ogni
       caricamento di pagina. Con essa, le foto restano nell'ordine in cui il
       ristoratore le ha caricate.

       LAZY (e' il valore predefinito, scritto qui per chiarezza): la maggior
       parte delle pagine carica un Ristorante senza doverne mostrare le foto.
       Chi le vuole le chiede a RistoranteService.immaginiDi, che le legge
       dentro la propria transazione. */
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "ristorante_immagine",
            joinColumns = @JoinColumn(name = "ristorante_id"))
    @OrderColumn(name = "posizione")
    @Column(name = "nome_file", nullable = false, length = 100)
    private List<String> immagini = new ArrayList<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getIndirizzo() {
        return indirizzo;
    }

    public void setIndirizzo(String indirizzo) {
        this.indirizzo = indirizzo;
    }

    public User getGestore() {
        return gestore;
    }

    public void setGestore(User gestore) {
        this.gestore = gestore;
    }

    public boolean isAttivo() {
        return attivo;
    }

    public void setAttivo(boolean attivo) {
        this.attivo = attivo;
    }

    public List<Tavolo> getTavoli() {
        return tavoli;
    }

    public void setTavoli(List<Tavolo> tavoli) {
        this.tavoli = tavoli;
    }

    public List<Piatto> getPiatti() {
        return piatti;
    }

    public void setPiatti(List<Piatto> piatti) {
        this.piatti = piatti;
    }

    public List<String> getImmagini() {
        return immagini;
    }

    public void setImmagini(List<String> immagini) {
        this.immagini = immagini;
    }

    public void aggiungiTavolo(Tavolo tavolo) {
        tavoli.add(tavolo);
        tavolo.setRistorante(this);
    }

    public void rimuoviTavolo(Tavolo tavolo) {
        tavoli.remove(tavolo);
        tavolo.setRistorante(null);
    }

    public void aggiungiPiatto(Piatto piatto) {
        piatti.add(piatto);
        piatto.setRistorante(this);
    }

    public void rimuoviPiatto(Piatto piatto) {
        piatti.remove(piatto);
        piatto.setRistorante(null);
    }

    public void aggiungiImmagine(String nomeFile) {
        immagini.add(nomeFile);
    }

    /* Toglie l'immagine dalla galleria, se c'e'. Il nome e' un UUID generato
       da ImageStorageService, quindi identifica una foto sola: non serve un
       indice, che per giunta il browser potrebbe mandare sfasato rispetto a
       quello che si vede a schermo. */
    public boolean rimuoviImmagine(String nomeFile) {
        return immagini.remove(nomeFile);
    }

    @Override
    public int hashCode() {
        return 31 + ((id == null) ? 0 : id.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        /* instanceof e non getClass(): con le associazioni LAZY Hibernate
           consegna dei proxy, la cui classe e' una sottoclasse generata a
           runtime. getClass() != obj.getClass() farebbe risultare diversi un
           proxy e l'entita' che rappresenta. */
        if (!(obj instanceof Ristorante other))
            return false;
        /* getId() e non other.id: su un proxy l'accesso diretto al campo
           restituisce null, il getter invece lo inizializza. */
        return id != null && id.equals(other.getId());
    }

    
}
