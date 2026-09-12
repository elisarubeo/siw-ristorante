# Parte REST + React — requisiti e guida operativa

> **Documento superato per la scelta della funzionalità.** Qui sotto (§2) si
> consigliavano le *recensioni*; la funzionalità scelta sono invece le
> **statistiche del ristoratore**. Il contratto da realizzare, i file da
> scrivere e l'ordine di lavoro stanno in
> [`api-statistiche.md`](api-statistiche.md), che è il documento operativo.
>
> Questo resta utile per due cose, che valgono uguali qualunque sia la
> funzionalità: le **trappole già pagate** nel progetto gemello (§6) e i
> **concetti React da far vedere** (§7). Le §2, §3 e §5 parlano di recensioni
> e non valgono più.

Documento di passaggio scritto a partire dal progetto gemello già completato
(`~/siwprojects/siw-festival`, branch `main`, ultimo commit "frontend recensioni").
Serve a chi lavora su `siw-ristorante` per sapere **cosa va consegnato**, **come è
stato risolto nell'altro progetto** e **quali trappole sono già state pagate**.

Avvertenza: i riferimenti a paragrafi della traccia (§4.1, §13) sono quelli della
traccia *festival*. La traccia del ristorante va riletta per confermare quale
funzionalità deve passare a REST/React e se le statistiche sono bonus anche lì.

---

## 1. Che cosa chiede il professore (sostanza della parte REST/React)

1. **Una porzione dell'applicazione servita come SPA React**, non in Thymeleaf.
   Il resto del sito resta Thymeleaf: i due mondi convivono nella stessa
   applicazione Spring Boot (un processo, un JAR, un repository).
2. **API REST JSON** per quella porzione: `@RestController` separati, DTO in
   ingresso e in uscita, codici HTTP corretti (200/201/204/400/401/403/404/409/500),
   validazione Bean Validation sul corpo della richiesta.
3. **Autenticazione stateless con JWT** sulla parte REST, *in aggiunta* al
   form login con sessione della parte Thymeleaf. Stesso database di credenziali,
   stesso BCrypt, due catene di filtri distinte.
4. **Letture pubbliche, scritture autenticate**, con controllo di proprietà
   lato server (solo l'autore modifica/elimina la propria recensione).
5. **Uso esplicito dei concetti React visti a lezione** (vedi §7): `useState`,
   `useEffect`, `useContext`, componenti controllati, props, `key`, React Router
   con rotte annidate e rotta protetta, chiamate HTTP con axios.
6. **Documentazione automatica delle API con Swagger/OpenAPI** (springdoc),
   con il pulsante *Authorize* per incollare il JWT.
7. Il build React finisce **dentro le risorse statiche di Spring Boot**, così
   la consegna è un solo progetto Maven.

Tutto questo, nel festival, riguarda **le recensioni dei film**: si arriva da un
link nella pagina Thymeleaf del film a `/reviews/movies/{id}`, dove la SPA mostra
elenco recensioni, statistiche e form di inserimento/modifica/eliminazione.

---

## 2. Quale funzionalità scegliere in `siw-ristorante`

Le candidate, in ordine di convenienza:

| Candidata | Pro | Contro |
|---|---|---|
| **Recensioni di un ristorante** (consigliata) | Modello, repository, service e regole già esistono (`Recensione`, `RecensioneRepository`, `RecensioneService`); è esattamente il caso già risolto nel gemello, quindi il lavoro è quasi solo di traduzione | Va deciso se la versione Thymeleaf (`templates/ristoranti/recensioni.html`, `recensioni/form.html`) resta o viene sostituita dalla SPA |
| Prenotazioni del cliente ("Le mie prenotazioni") | Più ricca (date, stati, conflitti) | Molte più regole di dominio da riesprimere in REST; più superficie di errore |
| Ordinazione al tavolo | Interattiva, bella da mostrare | La più complessa: righe, totali, stati |

**Raccomandazione: le recensioni.** Il modello è già gemello di quello del
festival — `Recensione(id, titolo, voto 1..5, testo, data, ristorante, user)`,
un solo voto per utente per ristorante, `mediaVoti` già calcolata dal database.
Se si sceglie questa, la decisione da prendere è una sola: la pagina Thymeleaf
delle recensioni diventa un link alla SPA (come nel festival) oppure resta in
sola lettura. Nel festival la SPA è l'unico punto in cui si scrive una recensione.

Differenze di modello da tenere presenti rispetto al festival:
- la recensione ha **anche un titolo** (max 30) e il testo è opzionale (max 200) →
  il DTO di richiesta ha tre campi, non due;
- i ruoli sono **`DEFAULT`, `ADMIN`, `RISTORATORE`** (non "USER"): il claim `role`
  del token e le `hasAuthority(...)` usano queste stringhe;
- package base **`it.uniroma3.siw_ristorante`** (con underscore): serve esatto
  nell'annotazione `@RestControllerAdvice(basePackages = ...)`;
- `RecensioneRepository` ha già le query giuste, incluse quelle che filtrano per
  autore nella query stessa (`findByIdAndRistoranteIdAndUserId`): ottimo, ma
  attenzione — per l'API conviene distinguere **404 (non esiste)** da **403 (non è
  tua)**, quindi caricare per id e confrontare l'autore nel service;
- manca la distribuzione dei voti: se si vogliono le statistiche serve una query
  `GROUP BY r.voto`.

---

## 3. Contratto API da realizzare (modello festival, nomi da adattare)

Base path `/api`. Tutto JSON, anche gli errori.

| Metodo | Percorso | Auth | Risposta |
|---|---|---|---|
| POST | `/api/auth/login` | no | 200 `{token, username, userId, role}` · 401 se credenziali errate |
| GET | `/api/ristoranti/{id}/recensioni` | no | 200 lista di `RecensioneDto` |
| GET | `/api/ristoranti/{id}/recensioni/stats` | no | 200 `{mediaVoti, totale, distribuzione}` |
| POST | `/api/ristoranti/{id}/recensioni` | sì | 201 + header `Location` + corpo `RecensioneDto` · 409 se l'utente ha già recensito |
| PUT | `/api/recensioni/{id}` | sì (autore) | 200 `RecensioneDto` · 403 se non è sua |
| DELETE | `/api/recensioni/{id}` | sì (autore) | 204 senza corpo · 403 se non è sua |

Corpo di errore **sempre della stessa forma** (`ApiError`):
```json
{ "message": "Dati non validi.", "fieldErrors": { "voto": "Il voto minimo è 1" } }
```
`fieldErrors` presente solo sui 400 di validazione (`@JsonInclude(NON_NULL)`).
Un'unica forma = un unico punto da leggere in React: `error.response.data.message`.

Convenzioni di stato usate nel gemello, da replicare:
- **401** = non autenticato (token assente/scaduto) → React manda al login;
- **403** = autenticato ma non autorizzato (recensione di un altro);
- **409** = richiesta ben formata ma impossibile nello stato attuale (doppia recensione);
- **400** = validazione del corpo;
- **500** = messaggio generico all'utente, stack trace nei log.

---

## 4. File da creare lato backend (12 file nel gemello)

Con il ruolo di ciascuno. I percorsi sono relativi a `src/main/java/it/uniroma3/siw_ristorante/`.

**DTO** (`dto/`) — record Java, mai entità sull'API:
- `LoginRequest(username, password)` con `@NotBlank`;
- `LoginResponse(token, username, userId, role)` — `userId` **nel corpo, non nel token**;
- `ApiError(message, fieldErrors)` con costruttore di comodo a un argomento;
- `RecensioneRequest(titolo, voto, testo)` — solo i campi che l'utente scrive:
  id, data e autore li decide il server;
- `RecensioneDto(id, titolo, voto, testo, data, autoreId, autoreNome, autoreCognome)`
  con un factory `from(Recensione)`;
- `RecensioneStatsDto(mediaVoti, totale, distribuzione)` — `Double` e `Map<Integer,Long>`.

**Servizi e sicurezza** (`service/`, `config/`):
- `JwtService` — genera e verifica il token (HMAC-SHA256, subject = username,
  claim `role`, scadenza); `extractUsername`/`extractRole` restituiscono `null`
  su token non valido invece di lanciare;
- `JwtAuthenticationFilter extends OncePerRequestFilter` — legge
  `Authorization: Bearer <token>`, popola il `SecurityContext`, **non blocca
  nulla**: decidere è compito della catena;
- `SecurityConfiguration` — da **estendere**: oggi c'è una sola catena `@Order(2)`.
  Va aggiunta una catena `@Order(1)` con `securityMatcher("/api/**")`;
- `OpenApiConfig` — titolo + `SecurityScheme` bearer (è ciò che fa comparire
  *Authorize* in Swagger UI);
- `config/ReactAppConfig` + `controller/ReactAppController` — servono la SPA
  buildata (vedi §6, trappola 4).

**Controller REST** (`api/` — package **nuovo e separato** da `controller/`):
- `AuthRestController` — `POST /api/auth/login`; usa l'`AuthenticationManager`,
  non verifica la password a mano;
- `RecensioneRestController` — i cinque endpoint; prende `Authentication` come
  parametro del metodo per sapere chi sta scrivendo.

**Gestione errori** (`exception/`):
- `ApiExceptionHandler` con `@RestControllerAdvice(basePackages = "it.uniroma3.siw_ristorante.api")`
  e `@Order(HIGHEST_PRECEDENCE)`.

**Modifiche a file esistenti**: `pom.xml` (jjwt + springdoc), `application.properties`
(segreto e scadenza JWT, path Swagger), `SecurityConfiguration`, e il template
Thymeleaf del ristorante per aggiungere il link alla SPA.

### Dipendenze Maven (versioni già verificate con Spring Boot 4.1)
```xml
<properties>
  <springdoc.version>3.1.0</springdoc.version> <!-- la 2.x è per Boot 3: non funziona -->
  <jjwt.version>0.12.6</jjwt.version>
</properties>
<!-- springdoc-openapi-starter-webmvc-ui (compile) -->
<!-- jjwt-api (compile) + jjwt-impl (runtime) + jjwt-jackson (runtime) -->
```
`spring-boot-starter-validation` e `spring-boot-starter-security` ci sono già.

### application.properties da aggiungere
```properties
app.jwt.secret=<stringa di almeno 32 caratteri, altrimenti l'app non parte>
app.jwt.expiration-ms=86400000
springdoc.swagger-ui.path=/swagger-ui.html
springdoc.swagger-ui.operationsSorter=method
```

### Le due catene di sicurezza, in breve
```java
@Bean @Order(1)
SecurityFilterChain apiFilterChain(HttpSecurity http, JwtAuthenticationFilter jwt) {
  http.securityMatcher("/api/**");            // solo /api passa da qui
  http.cors(Customizer.withDefaults());       // con un bean CorsConfigurationSource
  http.csrf(c -> c.disable());                // niente cookie ⇒ niente CSRF possibile
  http.sessionManagement(s -> s.sessionCreationPolicy(STATELESS));
  http.authorizeHttpRequests(a -> {
    a.requestMatchers(POST, "/api/auth/login").permitAll();
    a.requestMatchers(GET, "/api/**").permitAll();   // letture pubbliche
    a.anyRequest().authenticated();
  });
  http.addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class);
  http.exceptionHandling(e -> { /* 401 e 403 in JSON, vedi trappola 2 */ });
  return http.build();
}
```
La catena Thymeleaf esistente diventa `@Order(2)` (già lo è) e deve inoltre
lasciare pubblici: i percorsi della SPA (`/recensioni-app/**` o come la si chiama),
`/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs`, `/v3/api-docs/**`.
Nella catena web **non** servono più regole su `/api/**`: quelle richieste non ci
arrivano mai.

---

## 5. File da creare lato frontend (progetto Vite in `frontend/`)

Struttura del gemello, 14 file:
```
frontend/
  vite.config.ts          base + outDir dentro static/ + proxy /api
  src/main.tsx            ThemeProvider, CssBaseline, AuthProvider, BrowserRouter(basename), Routes
  src/theme/index.ts       tema MUI (palette, tipografia)
  src/types/index.ts       interfacce TS che ricalcano i DTO Java
  src/services/api.ts      istanza axios + interceptor richiesta/risposta + messaggioErrore()
  src/services/authService.ts
  src/services/recensioneService.ts   una funzione per endpoint, try/catch, errore → stringa
  src/context/AuthContext.tsx   token/username/userId/role in stato + localStorage, useAuth()
  src/components/Layout.tsx     AppBar + <Outlet />
  src/components/PrivateRoute.tsx  layout route con <Navigate to="/login" replace />
  src/components/RecensioneCard.tsx    presentazionale, props + callback
  src/components/RecensioneFormDialog.tsx  Dialog MUI, form controllato, crea E modifica
  src/components/RecensioneStats.tsx   presentazionale puro (media, barre per voto)
  src/pages/{HomePage,LoginPage,RistoranteRecensioniPage,NotFoundPage}.tsx
```

Stack esatto del gemello (funziona, conviene copiarlo):
`react` 19, `react-dom`, `react-router-dom` 7, `axios`, `@mui/material`,
`@mui/icons-material`, `@emotion/react`, `@emotion/styled`; dev: `vite` 8,
`@vitejs/plugin-react`, `typescript`, `@types/*`, `oxlint`.

```bash
cd ~/siwprojects/siw-ristorante
npm create vite@latest frontend -- --template react-ts
cd frontend
npm install
npm install axios react-router-dom @mui/material @emotion/react @emotion/styled @mui/icons-material
```

### I tre file che reggono tutto

**`vite.config.ts`**
```ts
base: '/recensioni-app/',                                  // = basename del Router
build: { outDir: '../src/main/resources/static/recensioni-app', emptyOutDir: true },
server: { port: 5173, proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } } }
```

**`src/services/api.ts`** — `baseURL: '/api'` (percorso *relativo*: in sviluppo lo
prende il proxy, in produzione punta da sé al server che ha servito la pagina).
Interceptor di richiesta: allega `Bearer <token>` da `localStorage`. Interceptor di
risposta: su **401** svuota `localStorage` e rimanda al login, con una guardia per
non entrare in loop se è il login stesso a fallire.

**`src/context/AuthContext.tsx`** — `token`, `username`, `userId`, `role`,
`isAuthenticated`, `isAdmin`, `login()`, `logout()`. Doppia scrittura
`localStorage` + stato React (il primo sopravvive al refresh, il secondo fa
ridisegnare i componenti) e un `useEffect([])` di *token recovery* al primo render.

---

## 6. Trappole già pagate nel gemello (questa è la parte che fa risparmiare ore)

1. **CORS va configurato come bean `CorsConfigurationSource` + `.cors(...)` sulla
   catena**, non come `WebMvcConfigurer`: Spring Security gira *prima* di MVC,
   quindi i preflight `OPTIONS` prenderebbero 401 senza mai arrivare a MVC.
   Fra gli `allowedHeaders` serve **`Authorization`**, altrimenti il token viene
   scartato. In sviluppo, usando il proxy di Vite, CORS non entra in gioco —
   configurarlo comunque per quando si chiama `:8080` direttamente.
2. **Senza `exceptionHandling`, una richiesta senza token riceve 403, non 401.**
   Non esistendo un meccanismo di login su quella catena, Spring ripiega sul 403.
   La differenza conta: React sul 401 manda al login, il 403 significa "non ti è
   permesso". Si scrive l'`authenticationEntryPoint` e l'`accessDeniedHandler`
   a mano sulla `HttpServletResponse`, con la stessa forma JSON di `ApiError`
   (lì `ApiExceptionHandler` non è ancora entrato in gioco).
3. **Serve un gestore di eccezioni separato per i controller REST.** Il
   `GlobalExceptionHandler` esistente restituisce *nomi di viste* Thymeleaf: su
   una chiamata `/api` axios riceverebbe HTML al posto del JSON. Per questo i
   `@RestController` stanno in un package **`api`** disgiunto da `controller`,
   e i due advice si selezionano per `basePackages`.
4. **Refresh su una rotta React = 404.** Il routing è lato client: ricaricando
   `/recensioni-app/ristoranti/3` il browser chiede davvero quell'indirizzo.
   La soluzione *non* è un controller che inoltra tutto a `index.html` —
   intercetterebbe anche `assets/index-abc.js` restituendo HTML al posto del JS.
   Si registra un `ResourceHandler` con un `PathResourceResolver` che prima
   verifica se il file esiste davvero e solo altrimenti ripiega su `index.html`
   (attenzione al path vuoto e a quello che finisce con `/`), più un
   `@GetMapping({"/recensioni-app", "/recensioni-app/"})` che fa
   `forward:/recensioni-app/index.html` per la sola radice.
5. **Tre valori devono combaciare**: `base` in `vite.config.ts`, `basename` di
   `BrowserRouter`, e il percorso del resource handler. Se divergono, i file JS
   danno 404 o le rotte non risolvono.
6. **`@NotNull` è indispensabile accanto a `@Min`/`@Max`**: per Bean Validation
   un `null` è *valido* per `@Min`. E il campo va dichiarato `Integer`, non `int`:
   con un primitivo il campo mancante diventa `0` e `@NotNull` non scatta mai.
7. **Mai restituire l'entità**: Jackson seguirebbe `Recensione → Ristorante →
   recensioni → ...` all'infinito e, fuori dalla transazione, troverebbe i proxy
   LAZY non inizializzati. Da qui i DTO. Il factory `from(...)` che naviga le
   relazioni LAZY va chiamato **dentro** un metodo `@Transactional`.
8. **`JOIN FETCH` sull'autore nella query della lista**, altrimenti una query per
   ogni recensione (N+1). Sempre un `ORDER BY` esplicito, altrimenti l'ordine
   delle righe non è garantito.
9. **Nel DTO va `autoreId`, non lo username**: `User` non ha riferimenti verso
   `Credentials` (la relazione è a senso unico), quindi risalire allo username
   costerebbe una query per recensione. React confronta `autoreId` con lo
   `userId` ricevuto al login per decidere dove mostrare Modifica/Elimina — che
   resta **cortesia visiva**: il controllo che conta è nel service.
10. **Il filtro JWT non deve bloccare**: se il token manca o è invalido, si lascia
    passare la richiesta come anonima. È così che le GET pubbliche funzionano
    senza token. `OncePerRequestFilter` per non rieseguirlo sui forward interni
    (es. verso `/error`).
11. **Il ruolo nel token è quello di `Credentials`** (`DEFAULT`/`ADMIN`/`RISTORATORE`),
    senza prefisso `ROLE_`, coerente con le `hasAuthority(...)` usate nel resto
    dell'app. Le authorities si ricostruiscono dal claim, senza interrogare il
    database: è il senso dello stateless.
12. **Segreto JWT ≥ 32 caratteri** (256 bit per HMAC-SHA256), altrimenti jjwt
    rifiuta la chiave **all'avvio**. Nel token solo username e ruolo: il payload
    è Base64URL, leggibile da chiunque — la firma garantisce integrità, non
    riservatezza.
13. **Doppia difesa sul "una sola recensione per utente"**: vincolo
    `@UniqueConstraint` sulla tabella *e* controllo nel service. Se due richieste
    arrivano insieme il controllo applicativo può passarle entrambe e il vincolo
    ne blocca una: la `DataIntegrityViolationException` va tradotta nello stesso
    409 del controllo applicativo, così il frontend vede un comportamento solo.
    (In `siw-ristorante` il vincolo DB su `(user_id, ristorante_id)` oggi **non
    c'è**: va aggiunto.)
14. **springdoc: nella catena web vanno permessi tutti e tre i percorsi**
    (`/swagger-ui.html`, `/swagger-ui/**`, `/v3/api-docs/**`), altrimenti
    l'interfaccia si apre vuota.
15. **React**: il `Dialog` resta montato anche da chiuso → lo stato del form va
    risincronizzato in un `useEffect([open, initial])`, altrimenti riaprendolo si
    vedono i valori precedenti. Mai mutare gli array di stato (sempre nuova
    referenza). `key` obbligatoria nelle liste. Nel `useEffect` di caricamento,
    flag `annullato` nella funzione di cleanup per non scrivere stato su un
    componente smontato.
16. **La media è `Double`, non `double`**: senza recensioni la query di
    aggregazione restituisce `null`. Lato React `null` significa "nessun dato",
    e va mostrato come tale, non come 0,0. La distribuzione dei voti: il
    `GROUP BY` restituisce righe solo per i voti presenti, riempire gli assenti
    con 0 è compito del service, così il frontend riceve sempre 5 voci.

---

## 7. Concetti React da far vedere (sono quelli delle slide)

Nel gemello ciascuno ha un posto preciso; conviene mantenerlo, è la mappa che
serve all'orale:
- `useState` → campi del form, lista, caricamento, errore (`LoginPage`, `FormDialog`);
- **componenti controllati** → `value` + `onChange`, il valore vive nello stato
  non nel DOM (slide 14, lez. 2);
- `useEffect` → caricamento al mount con dipendenza `[id]`, cleanup, token recovery;
- `useContext` → `AuthContext`: l'utente collegato serve in punti lontani
  dell'albero (navbar, form, pulsante Elimina), passarlo di prop in prop sarebbe
  ingestibile (slide 32, lez. 1). L'`AuthProvider` avvolge il Router, non una rotta;
- **props e callback** → `RecensioneCard` e `RecensioneStats` sono presentazionali:
  ricevono dati, non chiamano l'API; il genitore tiene lo stato (*lifting*);
- **React Router** → rotte annidate con `<Outlet />` nel `Layout`, `PrivateRoute`
  come layout route con `<Navigate replace />`, `useParams`, `useNavigate`,
  ritorno alla pagina di origine dopo il login (slide 15, lez. 3);
- **axios** → istanza condivisa, interceptor, livello `services/` separato dai
  componenti (la stessa separazione che nel backend c'è fra controller e service);
- **MUI** → tema centralizzato, `Dialog`, `Rating`, `Alert`, `LinearProgress`.

Attenzione a un punto di merito: la pagina di lettura **non** va dentro
`PrivateRoute` (le recensioni sono pubbliche). È il form a comparire o no a
seconda che ci sia un utente: tre casi da gestire — non autenticato, autenticato
senza recensione, ha già recensito.

---

## 8. Ordine di lavoro consigliato

1. `pom.xml` + `application.properties` (jjwt, springdoc, segreto JWT) → l'app parte.
2. `JwtService` + `JwtAuthenticationFilter` + seconda catena in `SecurityConfiguration`
   + `CorsConfigurationSource`.
3. DTO + `AuthRestController` → provare il login da Swagger e ottenere un token.
4. `ApiExceptionHandler` (prima dei controller, così gli errori sono già JSON).
5. Metodi nel `RecensioneService` che restituiscono DTO (`findByRistorante`,
   `create`, `update`, `delete`, `stats`) + query mancanti nel repository.
6. `RecensioneRestController` → tutti gli endpoint provati da Swagger con
   *Authorize*: qui il backend è finito e si verifica senza React.
7. Progetto Vite: `vite.config.ts`, `types`, `api.ts`, `AuthContext`, `Layout`,
   `LoginPage` → login funzionante su `:5173`.
8. Pagina delle recensioni, `Card`, `FormDialog`, `Stats`.
9. `ReactAppConfig` + `ReactAppController`, `npm run build`, verifica su
   `:8080/recensioni-app/...` **compreso il refresh su una rotta profonda**.
10. Link dalla pagina Thymeleaf del ristorante alla SPA, e dalla SPA il ritorno
    al sito (link HTML normale, non `<Link>` di React Router: si esce dall'app).

Comandi:
```bash
./mvnw spring-boot:run            # backend su :8080
cd frontend && npm run dev        # dev server su :5173, proxy su /api
cd frontend && npm run build      # build dentro src/main/resources/static/...
# Swagger: http://localhost:8080/swagger-ui.html
```
Durante lo sviluppo si lavora su `:5173`; per la consegna conta che funzioni il
build servito da `:8080`. Il contenuto di `static/recensioni-app/` è generato:
decidere consapevolmente se committarlo (nel festival **è committato**, così il
progetto gira clonando solo il repo Maven senza `npm install`).

---

## 9. Domande d'orale che questa parte invita

Vale la pena saper rispondere a queste, sono le stesse scelte commentate nel codice:
- Perché due catene di filtri e non una? Cosa fa `securityMatcher` e cosa `@Order`?
- Perché si può disabilitare CSRF sulla catena REST e non su quella Thymeleaf?
- Perché stateless? Cosa c'è dentro un JWT, cosa garantisce la firma, cosa **non**
  garantisce? Perché `userId` non sta nel token?
- Perché DTO e non entità sull'API? Cosa succede serializzando un'entità con
  relazioni LAZY e bidirezionali?
- Differenza fra 401, 403, 404 e 409 in questi endpoint.
- Perché il controllo di proprietà sta nel service e non nel frontend?
- Come fa il refresh su una rotta React a non dare 404?
- Perché `useContext` per l'autenticazione e non props?
- Dove vengono validati i dati, e perché in due posti (client e server)?
