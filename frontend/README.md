# Statistiche — la parte REST/React di siw-ristorante

Applicazione a pagina singola (SPA) che mostra al ristoratore l'andamento del
suo locale: incassi, conti chiusi, piatti più venduti, giorni e ore di
maggiore affluenza, giudizio dei clienti.

Non è un progetto a parte: vive dentro `siw-ristorante`, consuma le API REST
di quella stessa applicazione Spring Boot e, una volta buildata, finisce fra le
sue risorse statiche. La consegna resta un solo progetto Maven.

**Il contratto con il backend è in [`../docs/api-statistiche.md`](../docs/api-statistiche.md).**
Prima di toccare un tipo in `src/types/index.ts`, leggi quel documento.

---

## Come si lavora

```bash
npm install          # una volta sola
npm run dev          # http://localhost:5173/statistiche/
```

Si sviluppa su `:5173`, non su `:8080`: il dev server ricarica il componente
modificato senza perdere lo stato della pagina. Le chiamate a `/api` le inoltra
a Spring Boot da sé (il proxy in `vite.config.ts`).

### Senza backend

Finché i `@RestController` non esistono, `VITE_FINTO=1` in `.env.development`
fa rispondere i servizi da `src/services/datiFinti.ts`, con un ritardo
simulato. Si entra con **borgo / borgo**.

Quando il backend risponde: `VITE_FINTO=0` e si riavvia `npm run dev`. È il
momento in cui si scoprono le differenze fra il documento del contratto e il
codice vero — se un numero sparisce dalla pagina, il nome del campo nel JSON
non corrisponde a quello in `src/types/index.ts`.

### Per la consegna

```bash
npm run build        # scrive in ../src/main/resources/static/statistiche/
```

Poi `./mvnw spring-boot:run` e si apre `http://localhost:8080/statistiche/`.
**Va provato anche il refresh su una rotta profonda** (`/statistiche/login`):
è il caso che si rompe per primo, e serve il resource handler descritto nel
documento del contratto.

---

## Com'è fatto dentro

```
src/
  main.tsx                 punto d'ingresso: AuthProvider, Router, le rotte
  stile.css                foglio di stile unico, gli stessi colori del sito
  types/index.ts           le forme che arrivano dall'API (ricalcano i DTO Java)
  services/
    api.ts                 istanza axios, interceptor, traduzione degli errori
    authService.ts         login
    statisticheService.ts  una funzione per endpoint
    datiFinti.ts           i dati di prova per VITE_FINTO=1
  context/AuthContext.tsx  chi è collegato, token in localStorage, useAuth()
  hooks/useRisorsa.ts      carica → attesa / dati / errore, con cleanup
  components/
    Layout.tsx             barra e piede, con <Outlet/> in mezzo
    RottaProtetta.tsx      manda al login chi non è entrato
    Pannello.tsx           il riquadro di un grafico e i suoi tre stati
    Indicatore.tsx         un numero con la sua etichetta
    TabellaDati.tsx        i numeri del grafico, in chiaro
    grafici/               comune.ts (colori e assi), Tooltip.tsx, i 5 grafici
  pages/
    LoginPage.tsx
    StatistichePage.tsx    tiene lo stato e lo passa ai grafici
    NonTrovataPage.tsx
  utils/formato.ts         euro, numeri, mesi e giorni in italiano
```

Due divisioni reggono tutto il resto:

- **i servizi non sono i componenti.** Nessun componente sa che esiste axios:
  chiama una funzione di `services/` e riceve dati o un'eccezione con un
  messaggio già leggibile. È la stessa separazione che nel backend c'è fra
  controller e service.
- **i grafici non chiamano l'API.** Ricevono i dati dalle props e li disegnano.
  Lo stato lo tiene `StatistichePage`, che è l'unica a sapere che i dati
  arrivano da qualche parte.

### Scelte sui grafici

- **Mai due scale verticali nello stesso grafico.** Conti chiusi ed euro sono
  due misure diverse: sovrapposte su due assi indipendenti, la loro posizione
  relativa dipende da dove si fanno partire gli assi e non significa niente.
  Sono due grafici impilati che condividono i mesi.
- **Un colore solo.** Una tavolozza serve a distinguere più serie dentro lo
  stesso grafico, e qui ogni grafico ha una serie sola: colori diversi
  suggerirebbero una differenza che non c'è.
- **Un'etichetta sola per grafico**, sul valore massimo. Un numero sopra ogni
  barra è un muro di cifre che nessuno legge.
- **I buchi si riempiono lato server.** I mesi senza incassi, i giorni a zero e
  i voti che nessuno ha dato arrivano già nella risposta. Un grafico a cui
  manca agosto non mostra agosto vuoto: salda luglio a settembre e mente.
- **Ogni grafico ha sotto i suoi numeri**, in una tabella apribile: un grafico
  è muto per chi usa un lettore di schermo, e il valore esatto di una barra non
  si legge comunque a occhio.
