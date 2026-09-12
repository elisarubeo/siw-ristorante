# Statistiche del ristoratore — contratto REST

> **Stato: fatto e verificato.** Backend e frontend sono scritti e provati
> insieme sull'applicazione in esecuzione. Questo documento non è più una lista
> di cose da fare: è la descrizione di come funziona, e resta il posto dove
> guardare quando un numero non torna.

Questo documento **è il contratto** fra i `@RestController` e la SPA React in
`frontend/`. Il frontend dà per buona ogni forma JSON descritta qui: cambiando
il nome di un campo in Java va cambiato anche in `frontend/src/types/index.ts`,
altrimenti TypeScript non se ne accorge — i tipi descrivono ciò che *ci si
aspetta* di ricevere, non ciò che arriva davvero, e il valore diventa
`undefined` a tempo di esecuzione.

---

## 1. Le due metà dell'applicazione

| | Thymeleaf (già fatto) | REST + React (questa parte) |
|---|---|---|
| Indirizzi | tutto tranne `/api` e `/statistiche` | `/api/**` (dati) e `/statistiche/**` (la SPA) |
| Autenticazione | form login, sessione, cookie `JSESSIONID` | JWT nell'header `Authorization`, **stateless** |
| CSRF | attivo (ci sono i cookie) | disattivato (niente cookie ⇒ nessun attacco CSRF possibile) |
| Errori | pagine `error/404.html`, `error/500.html` | JSON, sempre della stessa forma |
| Catena di filtri | `@Order(2)`, quella che c'è già | `@Order(1)`, nuova, con `securityMatcher("/api/**")` |

Una sola applicazione Spring Boot, un solo database, due catene di filtri che
non si incontrano mai: `securityMatcher("/api/**")` fa sì che le richieste a
`/api` non arrivino nemmeno alla catena web.

---

## 2. Chi può leggere che cosa

Le statistiche sono **dati di cassa**: non sono pubbliche. A differenza delle
recensioni, qui non esistono letture aperte a tutti.

- `POST /api/auth/login` → pubblico.
- tutto il resto sotto `/api/statistiche/**` → richiede un token valido con
  ruolo `RISTORATORE`.

**Il ristorante non compare mai nell'indirizzo.** Non c'è
`/api/statistiche/{ristoranteId}/...`: il locale è quello del gestore
autenticato, ricavato dal token. È la stessa regola di
`RistoranteService.ristoranteDelGestore(authentication)` già usata da
`/mio-ristorante`. Conseguenza: non esiste un numero da cambiare nell'URL per
sbirciare l'incasso di un altro locale — il problema non si pone invece di
essere risolto con un controllo.

Se l'account autenticato non gestisce nessun ristorante → **404**
(`ResourceNotFoundException`, come già fa `ristoranteDelGestore`).

---

## 3. Codici di stato

Una convenzione sola, applicata ovunque, così React ha un solo punto da leggere:

| Codice | Quando | Cosa fa il frontend |
|---|---|---|
| 200 | tutto bene | disegna |
| 400 | corpo della richiesta non valido (Bean Validation) | mostra `message` e i `fieldErrors` |
| 401 | token assente, scaduto o malformato | svuota `localStorage` e manda a `/statistiche/login` |
| 403 | autenticato ma il ruolo non basta (un cliente che chiama `/api/statistiche`) | mostra "non hai i permessi" |
| 404 | l'account non gestisce nessun ristorante | mostra il messaggio |
| 500 | imprevisto | messaggio generico; lo stack trace resta nei log |

⚠️ **401 e 403 vanno scritti a mano.** Senza `exceptionHandling` sulla catena
`/api/**`, una richiesta senza token riceve **403**, non 401: non esistendo un
meccanismo di login su quella catena, Spring Security ripiega sul 403. Servono
un `authenticationEntryPoint` (→ 401) e un `accessDeniedHandler` (→ 403) che
scrivono il JSON direttamente sulla `HttpServletResponse`: a quel punto
`@RestControllerAdvice` non è ancora entrato in gioco, perché l'eccezione nasce
nei filtri, prima dei controller.

### Corpo di errore — sempre questa forma

```json
{ "message": "Credenziali non valide." }
```

```json
{ "message": "Dati non validi.", "fieldErrors": { "username": "non può essere vuoto" } }
```

`fieldErrors` compare solo nei 400 di validazione: nel record `ApiError` va
annotato `@JsonInclude(JsonInclude.Include.NON_NULL)` perché negli altri casi
sparisca dal JSON invece di apparire come `null`.

Il frontend legge sempre e solo `error.response.data.message`.

---

## 4. Gli endpoint

Base path `/api`. Tutti i `GET` qui sotto vogliono
`Authorization: Bearer <token>`.

### 4.1 `POST /api/auth/login` — pubblico

Richiesta:
```json
{ "username": "borgo", "password": "borgo" }
```

Risposta **200**:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "borgo",
  "userId": 4,
  "role": "RISTORATORE"
}
```

**401** se le credenziali sono sbagliate (`BadCredentialsException`) o se
l'account è disabilitato — ricorda che `enabled` è calcolato: il gestore di un
ristorante disattivato non deve entrare nemmeno da qui.

Note:
- usa l'`AuthenticationManager`, non confrontare la password a mano: così vale
  la stessa `UserDetailsService` (e lo stesso `COALESCE(r.attivo, true)`) del
  form login;
- `userId` sta **nel corpo, non nel token**: nel token va solo ciò che serve a
  ricostruire l'autenticazione a ogni richiesta (username e ruolo). React usa
  `userId` per l'interfaccia, non per autorizzare;
- il `role` è quello di `Credentials` (`DEFAULT` / `ADMIN` / `RISTORATORE`),
  **senza prefisso `ROLE_`**, coerente con le `hasAuthority(...)` usate nel
  resto dell'applicazione.

Il frontend accetta il login di qualsiasi ruolo, ma se il ruolo non è
`RISTORATORE` mostra subito che le statistiche non sono per quell'account,
senza nemmeno chiamare gli altri endpoint.

---

### 4.2 `GET /api/statistiche/contesto`

Il primo endpoint che il frontend chiama: gli serve per sapere di che locale
sta parlando e **quali anni ha senso mostrare** nel menu a tendina.

```json
{
  "ristoranteId": 1,
  "nome": "Osteria del Borgo",
  "indirizzo": "Via dei Coronari 12, Roma",
  "anniDisponibili": [2026, 2025]
}
```

- `anniDisponibili`: gli anni in cui esiste almeno uno scontrino, **dal più
  recente**. Array vuoto se il locale non ha mai chiuso un conto: in quel caso
  la SPA mostra "nessun dato" e non chiama nient'altro.
- Query: `select distinct year(s.dataOra) from Scontrino s where s.ristorante.id = ?1 order by 1 desc`.

---

### 4.3 `GET /api/statistiche/riepilogo?anno=2026`

I numeri in cima alla pagina.

```json
{
  "anno": 2026,
  "numeroScontrini": 184,
  "incassoTotale": 6420.50,
  "scontrinoMedio": 34.89,
  "pietanzeVendute": 512,
  "durataMediaMinuti": 87,
  "incassoAnnoPrecedente": 4210.00,
  "variazionePercentuale": 52.5,
  "mediaVoti": 4.3,
  "numeroRecensioni": 12
}
```

| Campo | Tipo Java | Come si calcola | Quando è `null` |
|---|---|---|---|
| `anno` | `int` | l'anno richiesto | mai |
| `numeroScontrini` | `long` | `count(s)` | mai (0) |
| `incassoTotale` | `BigDecimal` | `sum(s.totale)` | mai — **se la query dà `null`, restituisci `BigDecimal.ZERO`** |
| `scontrinoMedio` | `BigDecimal` | `incassoTotale / numeroScontrini`, 2 decimali, `RoundingMode.HALF_UP` | mai (0 se non ci sono scontrini) |
| `pietanzeVendute` | `long` | `sum(r.quantita)` sulle righe degli scontrini dell'anno | mai (0) |
| `durataMediaMinuti` | `Integer` | media di `apertura → dataOra` in minuti, arrotondata | **sì**, se nessuno scontrino dell'anno ha `apertura` |
| `incassoAnnoPrecedente` | `BigDecimal` | stesso calcolo su `anno - 1` | **sì**, se quell'anno non ha scontrini |
| `variazionePercentuale` | `Double` | `(totale − precedente) / precedente × 100` | **sì**, se il precedente è `null` o zero (dividere per zero non ha risposta) |
| `mediaVoti` | `Double` | `RecensioneRepository.mediaVoti(id)` — esiste già | **sì**, se non ci sono recensioni |
| `numeroRecensioni` | `long` | `count` delle recensioni del locale | mai (0) |

`durataMediaMinuti`: `apertura` è ammessa nulla (gli scontrini più vecchi della
funzionalità non ce l'hanno), quindi la media va calcolata **solo sulle righe
che ce l'hanno**, non trattando i null come zero.

`mediaVoti` e `numeroRecensioni` non dipendono dall'anno: sono il giudizio del
locale oggi. Restano uguali cambiando anno, ed è voluto.

> **Attenzione al tipo.** `Double` e non `double`, `Integer` e non `int`: le
> query di aggregazione su un insieme vuoto restituiscono `null`, e un
> primitivo lo trasformerebbe in `0` — cioè in un'informazione falsa. Lato
> React `null` significa "nessun dato" e si disegna come "—", non come `0,0`.

---

### 4.4 `GET /api/statistiche/scontrini-per-mese?anno=2026`

Il grafico principale: quanti conti si sono chiusi e quanto si è incassato in
ogni mese dell'anno.

```json
[
  { "mese": 1,  "numeroScontrini": 12, "incasso": 410.00 },
  { "mese": 2,  "numeroScontrini": 9,  "incasso": 318.50 },
  ...
  { "mese": 12, "numeroScontrini": 0,  "incasso": 0.00 }
]
```

- **Sempre 12 elementi**, `mese` da 1 a 12, in ordine. I mesi senza scontrini
  vanno riempiti con zeri **dal service**: il `group by` restituisce righe solo
  per i mesi che hanno dati, e un grafico a cui mancano i mesi vuoti mente
  (agosto assente non è agosto a zero: è un buco che il grafico chiude
  saldando luglio a settembre).
- Raggruppa con `year(s.dataOra)` / `month(s.dataOra)` e ordina esplicitamente.

---

### 4.5 `GET /api/statistiche/piatti-piu-ordinati?anno=2026&limite=8`

```json
[
  { "piattoId": 1, "nomePiatto": "Carbonara",    "quantita": 142, "incasso": 1917.00 },
  { "piattoId": 3, "nomePiatto": "Amatriciana",  "quantita": 118, "incasso": 1475.00 }
]
```

- `limite`: facoltativo, **predefinito 8**, massimo consigliato 20. Il
  frontend chiede 8.
- Ordinato per `quantita` decrescente; a parità, per `incasso` decrescente.
- Si legge da `RigaScontrino`, non da `RigaOrdinazione`: le ordinazioni
  spariscono alla chiusura del conto, le righe dello scontrino restano.
- `nomePiatto` è la **copia** salvata nella riga, non un join su `Piatto`:
  quel piatto potrebbe essere stato eliminato dal menu. Raggruppa per
  `r.piattoId` e prendi `max(r.nomePiatto)` come etichetta — se il piatto è
  stato rinominato nel tempo, raggruppare anche per nome lo spezzerebbe in due
  voci che sono lo stesso piatto.
- `incasso` = `sum(r.prezzoUnitario * r.quantita)`.
- Array vuoto se l'anno non ha righe: non è un 404.

---

### 4.6 `GET /api/statistiche/incasso-per-giorno?anno=2026`

In che giorni della settimana lavora il locale.

```json
[
  { "giorno": 1, "numeroScontrini": 20, "incasso": 700.00 },
  ...
  { "giorno": 7, "numeroScontrini": 48, "incasso": 1720.00 }
]
```

- **Sempre 7 elementi**, `giorno` da **1 = lunedì** a **7 = domenica** (la
  convenzione ISO, la stessa di `DayOfWeek.getValue()`). I giorni senza
  scontrini a zero.
- ⚠️ Non usare direttamente la funzione del database: in PostgreSQL
  `extract(dow from ...)` conta **0 = domenica**. Il modo pulito è leggere le
  date e raggruppare in Java con `dataOra.getDayOfWeek().getValue()`, oppure
  convertire esplicitamente. Se i numeri risultano ruotati di un giorno,
  l'errore è qui.

---

### 4.7 `GET /api/statistiche/affluenza-per-ora?anno=2026`

A che ora si riempie la sala.

```json
[
  { "ora": 0,  "numeroScontrini": 0 },
  ...
  { "ora": 13, "numeroScontrini": 64 },
  ...
  { "ora": 23, "numeroScontrini": 3 }
]
```

- **Sempre 24 elementi**, `ora` da 0 a 23.
- L'ora è quella in cui i clienti **si sono seduti**, non quella in cui hanno
  pagato: usa `coalesce(s.apertura, s.dataOra)`, così gli scontrini vecchi
  senza `apertura` contribuiscono con l'ora del pagamento invece di sparire.

---

### 4.8 `GET /api/statistiche/voti-recensioni`

```json
{
  "mediaVoti": 4.33,
  "totale": 12,
  "distribuzione": [
    { "voto": 1, "quantita": 0 },
    { "voto": 2, "quantita": 1 },
    { "voto": 3, "quantita": 1 },
    { "voto": 4, "quantita": 6 },
    { "voto": 5, "quantita": 4 }
  ]
}
```

- **Sempre 5 elementi** in `distribuzione`, da 1 a 5: il `group by r.voto`
  restituisce righe solo per i voti presenti, riempire gli assenti è compito
  del service.
- `mediaVoti` è `Double` e vale `null` senza recensioni.
- Nessun parametro `anno`: le recensioni sono il giudizio corrente del locale,
  non un fatto di cassa. Serve una query nuova:
  `select r.voto, count(r) from Recensione r where r.ristorante.id = ?1 group by r.voto`.

---

## 5. I file, e che cosa fa ciascuno

Percorsi relativi a `src/main/java/it/uniroma3/siw_ristorante/`.
I package `api/` e `dto/` hanno un `package-info.java` che spiega perché stanno
separati da `controller/` e `model/`.

```
dto/
  LoginRequest.java          record(String username, String password) — @NotBlank su entrambi
  LoginResponse.java         record(String token, String username, Long userId, String role)
  ApiError.java              record(String message, Map<String,String> fieldErrors) + @JsonInclude(NON_NULL)
  ContestoDto.java           §4.2
  RiepilogoDto.java          §4.3
  ScontriniMeseDto.java      §4.4
  PiattoVendutoDto.java      §4.5
  IncassoGiornoDto.java      §4.6
  AffluenzaOraDto.java       §4.7
  VotiRecensioniDto.java     §4.8  (contiene la lista di VotoQuantitaDto)
  VotoQuantitaDto.java       record(int voto, long quantita)

service/
  JwtService.java            genera e verifica il token; extractUsername/extractRole
                             restituiscono null su token non valido invece di lanciare
  StatisticheService.java    tutte le aggregazioni; @Transactional(readOnly = true);
                             è qui che si riempiono i mesi/giorni/ore/voti mancanti

config/
  JwtAuthenticationFilter.java   extends OncePerRequestFilter
  SecurityConfiguration.java     ← da MODIFICARE: aggiungere la catena @Order(1)
  OpenApiConfig.java             titolo + SecurityScheme bearer (fa comparire
                                 il pulsante "Authorize" in Swagger UI)
  ReactAppConfig.java            serve la SPA buildata, vedi §6

api/
  AuthRestController.java        POST /api/auth/login
  StatisticheRestController.java i sei GET
  ReactAppController.java        forward della sola radice /statistiche

exception/
  ApiExceptionHandler.java       @RestControllerAdvice(basePackages = "it.uniroma3.siw_ristorante.api")
                                 + @Order(Ordered.HIGHEST_PRECEDENCE)

repository/
  ScontrinoRepository.java       ← da ESTENDERE: le @Query di aggregazione
  RecensioneRepository.java      ← da ESTENDERE: distribuzione dei voti, count
```

### Due trappole di versione, già pagate

1. **Spring Boot 4 usa Jackson 3, il cui package è `tools.jackson`**, non
   `com.fasterxml.jackson`. Sul classpath c'è anche la 2.x, tirata dentro da
   jjwt e springdoc: importare `com.fasterxml.jackson.databind.ObjectMapper`
   compila benissimo e poi all'avvio dà *"required a bean of type ObjectMapper
   that could not be found"*, perché il bean di Spring è dell'altro tipo.
   (Le **annotazioni** invece restano `com.fasterxml.jackson.annotation`: il
   `@JsonInclude(NON_NULL)` di `ApiError` funziona, verificato — un errore
   senza `fieldErrors` non ha proprio la chiave.)
2. **Il progetto è su Java 17**: niente `Math.clamp`, che è del 21.

### La catena di sicurezza da aggiungere

```java
@Bean
@Order(1)
SecurityFilterChain apiFilterChain(HttpSecurity http, JwtAuthenticationFilter jwtFilter) throws Exception {
    http.securityMatcher("/api/**");                   // solo /api passa di qui
    http.csrf(csrf -> csrf.disable());                 // niente cookie ⇒ niente CSRF
    http.cors(Customizer.withDefaults());              // serve un bean CorsConfigurationSource
    http.sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.authorizeHttpRequests(auth -> {
        auth.requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll();
        auth.requestMatchers("/api/statistiche/**").hasAuthority(Credentials.RISTORATORE_ROLE);
        auth.anyRequest().authenticated();
    });
    http.addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    http.exceptionHandling(e -> {
        e.authenticationEntryPoint(...);   // 401 in JSON
        e.accessDeniedHandler(...);        // 403 in JSON
    });
    return http.build();
}
```

E nella catena web esistente (`@Order(2)`) vanno resi pubblici:

```java
authorize.requestMatchers("/statistiche", "/statistiche/**").permitAll();
authorize.requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**").permitAll();
```

Il `permitAll` su `/statistiche/**` riguarda **solo i file della SPA** (HTML,
JS, CSS): sono codice che gira nel browser, non dati. I dati stanno dietro
`/api/statistiche/**`, che richiede il token. Senza questo `permitAll`,
`anyRequest().authenticated()` manderebbe la SPA al form login di Thymeleaf.

Swagger UI serve anche a te: da lì provi tutti gli endpoint con "Authorize"
prima ancora di aprire React.

---

## 6. Servire la SPA (e non prendere 404 al refresh)

Il build di Vite finisce in `src/main/resources/static/statistiche/`, che Spring
Boot serve già da solo: `/statistiche/index.html` e `/statistiche/assets/*.js`
funzionano senza scrivere niente. Mancano due cose:

1. **`/statistiche` senza barra finale** → un `@GetMapping({"/statistiche", "/statistiche/"})`
   che fa `return "forward:/statistiche/index.html";`
2. **Il refresh su una rotta profonda.** `/statistiche/login` non è un file: il
   routing è lato client, ma ricaricando la pagina il browser chiede davvero
   quell'indirizzo al server. La soluzione **non** è inoltrare tutto a
   `index.html`: intercetterebbe anche `assets/index-abc123.js`, restituendo
   HTML al posto del JavaScript e rompendo l'applicazione in un modo che in
   console sembra un errore di sintassi. Si registra un `ResourceHandler` con
   un `PathResourceResolver` che **prima verifica se il file esiste davvero** e
   solo in caso contrario ripiega su `index.html`.

### Tre valori che devono combaciare

Se divergono, o i file JS danno 404 o le rotte non risolvono:

| Dove | Valore |
|---|---|
| `frontend/vite.config.ts` → `base` | `/statistiche/` |
| `frontend/src/main.tsx` → `<BrowserRouter basename>` | `/statistiche` |
| `ReactAppConfig` → `addResourceHandler` | `/statistiche/**` → `classpath:/static/statistiche/` |

---

## 7. L'interruttore dei dati finti

`frontend/src/services/api.ts` legge `VITE_FINTO`. **Adesso vale `0`**: le
chiamate vanno davvero a `/api`.

Con `VITE_FINTO=1` i servizi non toccano la rete e rispondono da
`frontend/src/services/datiFinti.ts`, con un ritardo simulato. Serviva a
scrivere l'interfaccia prima che il backend esistesse; resta utile per lavorare
sui grafici con Spring spento. Le variabili si leggono all'avvio, quindi dopo
averlo cambiato va riavviato `npm run dev`.

Credenziali per la prova: **`borgo` / `borgo`** (il gestore dell'Osteria del
Borgo, il locale con più dati). `nino` e `verde` hanno meno scontrini, e
servono a vedere come reggono i grafici con pochi numeri.

---

## 8. Dati di prova

`import.sql` contiene ora circa 14 mesi di scontrini generati, con:

- **stagionalità**: più clienti in primavera ed estate, dicembre in ripresa,
  gennaio e novembre fiacchi — così il grafico per mese ha una forma, non un
  rumore piatto;
- **settimana**: venerdì e sabato pieni, lunedì e martedì scarichi;
- **fasce orarie**: due gobbe, pranzo (12–14) e cena (19–22);
- **piatti**: i primi del menu vendono più dei dolci, così la classifica ha
  una gerarchia visibile.

Le date sono **relative a `CURRENT_DATE`**, come il resto del file: con
`ddl-auto=create` il database si ricrea a ogni avvio e i dati restano sempre
"degli ultimi quattordici mesi", a qualsiasi data tu li guardi. Per questo gli
anni disponibili sono due (l'anno in corso e il precedente) e il confronto
`variazionePercentuale` ha sempre qualcosa da confrontare.

Gli altri due locali hanno meno dati apposta: `nino` vede una dashboard più
povera e `verde` quasi vuota. Serve a provare che i grafici reggano anche con
pochi numeri, che è il caso in cui di solito si rompono.

---

## 9. Come si avvia e come si verifica

```bash
# backend: sulla 8081 perche' su questa macchina la 8080 e' occupata
./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081

# frontend, solo per svilupparlo: dev server con ricaricamento a caldo
cd frontend && npm run dev          # http://localhost:5173/statistiche/

# frontend, per la consegna: finisce dentro le risorse statiche di Spring
cd frontend && npm run build
```

- L'applicazione completa: **http://localhost:8081/statistiche/** — è il build,
  servito da Spring Boot, cioè esattamente quello che vede chi corregge.
- Le API una per una: **http://localhost:8081/swagger-ui.html** — `POST
  /api/auth/login` con `borgo` / `borgo`, si copia il campo `token`, lo si
  incolla in *Authorize*, e da lì tutti gli endpoint rispondono.
- Il collegamento dal sito: da "Il mio ristorante" → pulsante **Statistiche**.

Se si avvia sulla 8080, vanno rimessi a 8080 anche `VITE_BACKEND` in
`frontend/.env.development` (serve solo al dev server: il build non usa il
proxy, chiama `/api` sulla stessa origine da cui è arrivata la pagina).

### Che cosa è stato verificato davvero

Sull'applicazione in esecuzione, non a occhio sul codice:

| | |
|---|---|
| login `borgo`/`borgo` | 200, token a tre pezzi, ruolo `RISTORATORE` |
| password sbagliata | 401 con `{"message": ...}` e **senza** la chiave `fieldErrors` |
| username e password vuoti | 400 con `fieldErrors` sui due campi |
| chiamata senza token | **401**, non 403 (è il blocco `exceptionHandling`) |
| token di un cliente (`elisa`, ruolo `DEFAULT`) | 403 |
| token con firma inventata | 401 |
| `scontrini-per-mese` | sempre 12 voci, anche i mesi a zero |
| `incasso-per-giorno` | sempre 7 voci, `1` = lunedì |
| `affluenza-per-ora` | sempre 24 voci |
| `voti-recensioni` | sempre 5 voci, anche i voti che nessuno ha dato |
| `piatti-piu-ordinati?limite=9999` | non sfonda: il service riporta il limite entro 20 |
| `/statistiche`, `/statistiche/`, `/statistiche/login`, rotte inventate | 200 con la pagina React |
| `/statistiche/assets/*.js` | resta `text/javascript`, non diventa HTML |
| la SPA sui dati veri | 5 pannelli, nessun errore in console, il cambio anno ricarica solo il cruscotto, F5 non butta fuori |

## 10. Domande d'orale che questa parte invita

- Perché due catene di filtri e non una? Cosa fa `securityMatcher`, cosa `@Order`?
- Perché si può disabilitare CSRF sulla catena REST e non su quella Thymeleaf?
- Cosa c'è dentro un JWT? Cosa garantisce la firma, cosa **non** garantisce?
  Perché `userId` non sta nel token?
- Perché DTO e non entità? Cosa succede serializzando un'entità con relazioni
  LAZY e bidirezionali?
- Differenza fra 401 e 403, e perché senza `exceptionHandling` si ottiene 403
  anche quando manca il token.
- Perché il ristorante non compare nell'indirizzo delle statistiche?
- Perché le aggregazioni tornano `Double` e non `double`?
- Perché i mesi vuoti li riempie il server e non il grafico?
- Come fa il refresh su una rotta React a non dare 404, e perché non basta
  inoltrare tutto a `index.html`?
- Perché `useContext` per l'autenticazione e non le props?
