package it.uniroma3.siw_ristorante.model;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
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
