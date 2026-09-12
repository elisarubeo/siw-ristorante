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

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "gestore_id", nullable = false, unique = true)
    private User gestore;

    @Column(nullable = false)
    private boolean attivo = true;

    @OneToMany(mappedBy = "ristorante", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Tavolo> tavoli = new ArrayList<>();

    @OneToMany(mappedBy = "ristorante", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Piatto> piatti = new ArrayList<>();

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
        if (!(obj instanceof Ristorante other))
            return false;
        return id != null && id.equals(other.getId());
    }

    
}
