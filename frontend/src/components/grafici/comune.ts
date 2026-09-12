/* Quello che tutti i grafici hanno in comune: colori, assi, tooltip.
   Sta qui e non copiato cinque volte perche' un grafico con gli assi di un
   grigio diverso dagli altri si nota subito, e correggerlo in cinque file e'
   il modo migliore per dimenticarne uno. */

/* UN SOLO COLORE per i dati, il terracotta del sito.
   Non e' pigrizia: una tavolozza di colori diversi serve a distinguere piu'
   serie DENTRO lo stesso grafico, e qui ogni grafico ha una serie sola. Dare
   un colore diverso a ogni pannello suggerirebbe una differenza di
   significato che non c'e', e costringerebbe a verificare che i colori
   restino distinguibili anche a chi non distingue il rosso dal verde. */
export const COLORE_DATI = '#b4532a'

export const COLORE_GRIGLIA = '#e7ded2'
export const COLORE_TESTO = '#6d635c'
export const COLORE_SUPERFICIE = '#ffffff'

/* Le barre non riempiono mai la loro fetta di asse: l'aria fra una e l'altra
   e' quello che le fa leggere come oggetti separati. */
export const SPESSORE_BARRA = 24

/* Angoli arrotondati solo dalla parte del valore, squadrati sulla linea di
   base: la barra cresce da li', e arrotondarla anche sotto la staccherebbe
   dall'asse. */
export const CIMA_ARROTONDATA: [number, number, number, number] = [4, 4, 0, 0]
export const DESTRA_ARROTONDATA: [number, number, number, number] = [0, 4, 4, 0]

/* Assi volutamente sbiaditi: la linea e i trattini non sono dati, e l'unica
   cosa che deve risaltare in un grafico sono i dati. */
export const ASSE = {
    tick: { fill: COLORE_TESTO, fontSize: 12 },
    tickLine: false,
    axisLine: false,
} as const

/* Il margine in alto non e' decorativo: l'etichetta del valore massimo si
   disegna SOPRA la barra piu' alta, e senza spazio finirebbe mangiata dal
   bordo del grafico. */
export const MARGINE = { top: 20, right: 8, bottom: 0, left: 0 }

/* Il velo grigio che segue il puntatore sulle barre: chiarisce quale fetta di
   asse si sta leggendo, senza coprire la barra. */
export const CURSORE = { fill: 'rgba(43, 37, 33, .04)' }
