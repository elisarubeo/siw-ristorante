package it.uniroma3.siw_ristorante.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import it.uniroma3.siw_ristorante.dto.AffluenzaOraDto;
import it.uniroma3.siw_ristorante.dto.ContestoDto;
import it.uniroma3.siw_ristorante.dto.IncassoGiornoDto;
import it.uniroma3.siw_ristorante.dto.PiattoVendutoDto;
import it.uniroma3.siw_ristorante.dto.RiepilogoDto;
import it.uniroma3.siw_ristorante.dto.ScontriniMeseDto;
import it.uniroma3.siw_ristorante.dto.VotiRecensioniDto;
import it.uniroma3.siw_ristorante.dto.VotoQuantitaDto;
import it.uniroma3.siw_ristorante.model.Ristorante;
import it.uniroma3.siw_ristorante.repository.RecensioneRepository;
import it.uniroma3.siw_ristorante.repository.ScontrinoRepository;

/* Tutte le statistiche del ristoratore.

   DUE PRINCIPI, e sono quelli che spiegano quasi ogni riga qui sotto.

   1. IL RISTORANTE NON ARRIVA MAI DA FUORI. Ogni metodo pubblico riceve
      l'Authentication e ricava il locale da li'. Non esiste un parametro
      ristoranteId che il frontend possa mandare, quindi non esiste un numero
      da cambiare per leggere la cassa di qualcun altro: il problema non viene
      risolto con un controllo, viene proprio tolto di mezzo.

   2. I BUCHI LI RIEMPIE IL SERVER. Un "group by" restituisce righe solo per i
      gruppi che esistono: se ad agosto il locale ha chiuso per ferie, agosto
      non c'e'. Consegnare undici mesi a un grafico significa fargli disegnare
      una linea che salda luglio a settembre, cioe' un'estate senza pausa che
      non c'e' mai stata. Percio' da qui escono sempre dodici mesi, sette
      giorni, ventiquattro ore e cinque voti, anche quando valgono zero.
      Potrebbe farlo React, ma allora la stessa regola andrebbe riscritta in
      ogni pagina che usa quei dati - e sarebbe una regola sul significato dei
      dati messa nel posto che si occupa di disegnarli. */
@Service
public class StatisticheService {

    /* Quanti piatti mostrare nella classifica, se non viene chiesto altro. */
    private static final int PIATTI_PREDEFINITI = 8;
    private static final int PIATTI_MASSIMI = 20;

    private final RistoranteService ristoranteService;
    private final ScontrinoRepository scontrinoRepository;
    private final RecensioneRepository recensioneRepository;

    public StatisticheService(RistoranteService ristoranteService,
            ScontrinoRepository scontrinoRepository,
            RecensioneRepository recensioneRepository) {
        this.ristoranteService = ristoranteService;
        this.scontrinoRepository = scontrinoRepository;
        this.recensioneRepository = recensioneRepository;
    }

    /* Di che locale stiamo parlando e quali anni ha senso guardare.
       E' la prima chiamata che fa il frontend: senza, non saprebbe nemmeno che
       cosa scrivere nel menu a tendina degli anni. */
    @Transactional(readOnly = true)
    public ContestoDto contesto(Authentication autenticazione) {
        Ristorante ristorante = locale(autenticazione);
        return new ContestoDto(
                ristorante.getId(),
                ristorante.getNome(),
                ristorante.getIndirizzo(),
                this.scontrinoRepository.anniConScontrini(ristorante.getId()));
    }

    @Transactional(readOnly = true)
    public RiepilogoDto riepilogo(Authentication autenticazione, int anno) {
        Long id = locale(autenticazione).getId();

        long numeroScontrini = this.scontrinoRepository.contaPerAnno(id, anno);
        /* Zero e non null: "non ha incassato niente" e' un'informazione vera,
           diversa da "non si sa". Sull'incasso il nulla non esiste. */
        BigDecimal incasso = valoreOZero(this.scontrinoRepository.incassoPerAnno(id, anno));

        /* Lo scontrino medio si calcola qui e non nel database: dividere per
           il numero di scontrini vuol dire dividere per zero quando l'anno e'
           vuoto, e il controllo si scrive meglio in Java che in una query.
           HALF_UP perche' sono soldi da leggere, non da sommare ancora. */
        BigDecimal medio = numeroScontrini == 0
                ? BigDecimal.ZERO
                : incasso.divide(BigDecimal.valueOf(numeroScontrini), 2, RoundingMode.HALF_UP);

        Long pietanze = this.scontrinoRepository.pietanzeVendutePerAnno(id, anno);

        /* La media della durata arriva come Double perche' puo' non esserci
           (nessuno scontrino con l'orario di apertura). Si arrotonda ai minuti
           interi solo se c'e' qualcosa da arrotondare: Math.round su null
           sarebbe un NullPointerException, e un valore predefinito a zero
           sarebbe una bugia ("i clienti restano zero minuti"). */
        Double durata = this.scontrinoRepository.durataMediaMinuti(id, anno);
        Integer durataMinuti = durata == null ? null : (int) Math.round(durata);

        /* Il confronto con l'anno prima. Resta null se l'anno precedente non
           ha incassi: la variazione rispetto a zero non e' "+100%", e' una
           divisione per zero, cioe' una domanda senza risposta. Meglio non
           mostrare niente che mostrare un numero inventato. */
        BigDecimal precedente = this.scontrinoRepository.incassoPerAnno(id, anno - 1);
        Double variazione = null;
        if (precedente != null && precedente.signum() != 0) {
            variazione = incasso.subtract(precedente)
                    .divide(precedente, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100))
                    .setScale(1, RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return new RiepilogoDto(
                anno,
                numeroScontrini,
                incasso,
                medio,
                pietanze == null ? 0L : pietanze,
                durataMinuti,
                precedente,
                variazione,
                this.recensioneRepository.mediaVoti(id),
                this.recensioneRepository.countByRistoranteId(id));
    }

    /* Dodici voci, sempre. */
    @Transactional(readOnly = true)
    public List<ScontriniMeseDto> scontriniPerMese(Authentication autenticazione, int anno) {
        Long id = locale(autenticazione).getId();

        /* Prima si indicizza quello che il database ha restituito, poi si
           scorrono i dodici mesi: cosi' i mesi assenti si riconoscono da soli,
           senza confrontare liste. */
        Map<Integer, ScontrinoRepository.AggregatoMese> perMese = new HashMap<>();
        for (ScontrinoRepository.AggregatoMese riga : this.scontrinoRepository.perMese(id, anno)) {
            perMese.put(riga.getMese(), riga);
        }

        List<ScontriniMeseDto> mesi = new ArrayList<>(12);
        for (int mese = 1; mese <= 12; mese++) {
            ScontrinoRepository.AggregatoMese riga = perMese.get(mese);
            mesi.add(riga == null
                    ? new ScontriniMeseDto(mese, 0L, BigDecimal.ZERO)
                    : new ScontriniMeseDto(mese, riga.getNumero(), valoreOZero(riga.getIncasso())));
        }
        return mesi;
    }

    @Transactional(readOnly = true)
    public List<PiattoVendutoDto> piattiPiuOrdinati(Authentication autenticazione, int anno, Integer limite) {
        Long id = locale(autenticazione).getId();

        /* Il limite arriva dal frontend, quindi non ci si fida: un valore
           assente diventa quello predefinito, uno assurdo viene riportato
           dentro i margini. Senza questo, "?limite=1000000" sarebbe una query
           che nessuno ha chiesto. */
        int quanti = limite == null
                ? PIATTI_PREDEFINITI
                : Math.min(Math.max(limite, 1), PIATTI_MASSIMI);

        return this.scontrinoRepository.piattiPiuVenduti(id, anno, PageRequest.of(0, quanti)).stream()
                .map(riga -> new PiattoVendutoDto(
                        riga.getPiattoId(),
                        riga.getNome(),
                        riga.getQuantita() == null ? 0L : riga.getQuantita(),
                        valoreOZero(riga.getIncasso())))
                .toList();
    }

    /* Sette voci, da 1 = lunedi a 7 = domenica.

       E' l'unico calcolo fatto in Java invece che nel database: vedi il
       commento su cassaDellAnno in ScontrinoRepository, il motivo e' che i
       database non concordano su quale giorno sia il numero 1. */
    @Transactional(readOnly = true)
    public List<IncassoGiornoDto> incassoPerGiorno(Authentication autenticazione, int anno) {
        Long id = locale(autenticazione).getId();

        long[] conteggi = new long[8];               // indice 0 non usato: si conta da 1
        BigDecimal[] incassi = new BigDecimal[8];
        java.util.Arrays.fill(incassi, BigDecimal.ZERO);

        for (ScontrinoRepository.RigaCassa riga : this.scontrinoRepository.cassaDellAnno(id, anno)) {
            /* DayOfWeek.getValue(): 1 = lunedi ... 7 = domenica, senza margine
               di interpretazione. */
            int giorno = riga.getDataOra().getDayOfWeek().getValue();
            conteggi[giorno]++;
            incassi[giorno] = incassi[giorno].add(valoreOZero(riga.getTotale()));
        }

        List<IncassoGiornoDto> giorni = new ArrayList<>(7);
        for (DayOfWeek giorno : DayOfWeek.values()) {
            int indice = giorno.getValue();
            giorni.add(new IncassoGiornoDto(indice, conteggi[indice], incassi[indice]));
        }
        return giorni;
    }

    /* Ventiquattro voci, comprese le ore in cui il locale e' chiuso: sono
       quelle che fanno vedere quanto sono staccate le due gobbe. */
    @Transactional(readOnly = true)
    public List<AffluenzaOraDto> affluenzaPerOra(Authentication autenticazione, int anno) {
        Long id = locale(autenticazione).getId();

        Map<Integer, Long> perOra = new HashMap<>();
        for (ScontrinoRepository.AggregatoOra riga : this.scontrinoRepository.perOra(id, anno)) {
            perOra.put(riga.getOra(), riga.getNumero());
        }

        List<AffluenzaOraDto> ore = new ArrayList<>(24);
        for (int ora = 0; ora < 24; ora++) {
            ore.add(new AffluenzaOraDto(ora, perOra.getOrDefault(ora, 0L)));
        }
        return ore;
    }

    /* Cinque voci, da 1 a 5 stelle. Senza parametro anno: le recensioni sono
       il giudizio corrente del locale, non un fatto di cassa. */
    @Transactional(readOnly = true)
    public VotiRecensioniDto votiRecensioni(Authentication autenticazione) {
        Long id = locale(autenticazione).getId();

        Map<Integer, Long> perVoto = new HashMap<>();
        for (RecensioneRepository.AggregatoVoto riga : this.recensioneRepository.distribuzioneVoti(id)) {
            perVoto.put(riga.getVoto(), riga.getQuantita());
        }

        List<VotoQuantitaDto> distribuzione = new ArrayList<>(5);
        for (int voto = 1; voto <= 5; voto++) {
            distribuzione.add(new VotoQuantitaDto(voto, perVoto.getOrDefault(voto, 0L)));
        }

        return new VotiRecensioniDto(
                this.recensioneRepository.mediaVoti(id),
                this.recensioneRepository.countByRistoranteId(id),
                distribuzione);
    }

    /* ---------- utilita' ---------- */

    /* Il locale di chi sta chiamando. Un metodo privato di una riga, ma e' il
       cardine di tutto: e' qui che "il ristorante" smette di essere una cosa
       che il client puo' scegliere e diventa una conseguenza di chi e'.
       Se l'account non gestisce nessun locale esce una
       ResourceNotFoundException, che ApiExceptionHandler trasforma in 404. */
    private Ristorante locale(Authentication autenticazione) {
        return this.ristoranteService.ristoranteDelGestore(autenticazione);
    }

    /* Le somme su un insieme vuoto tornano null dal database. Sui soldi il
       nulla non e' un'informazione utile: zero incassato e' zero. */
    private static BigDecimal valoreOZero(BigDecimal valore) {
        return valore == null ? BigDecimal.ZERO : valore;
    }
}
