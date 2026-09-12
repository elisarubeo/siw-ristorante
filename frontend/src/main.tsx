import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import Layout from './components/Layout'
import RottaProtetta from './components/RottaProtetta'
import LoginPage from './pages/LoginPage'
import NonTrovataPage from './pages/NonTrovataPage'
import StatistichePage from './pages/StatistichePage'
import './stile.css'

/* Il punto di ingresso dell'applicazione React.

   L'ordine degli involucri non e' casuale: l'AuthProvider sta FUORI dal
   Router, non dentro una rotta. L'utente collegato serve a tutte le pagine,
   compresa quella di login (che deve sapere se e' gia' entrato), e uno stato
   dentro una rotta si azzererebbe a ogni cambio di pagina. */

createRoot(document.getElementById('root')!).render(
    /* StrictMode e' solo per lo sviluppo: disegna ogni componente due volte
       per far emergere gli effetti collaterali scritti male (una richiesta
       senza cleanup si vede subito, perche' parte due volte). Nel build non
       fa niente. */
    <StrictMode>
        <AuthProvider>
            {/* basename: l'applicazione non vive alla radice del sito ma sotto
                /statistiche, e il Router deve saperlo per non cercare le
                rotte a partire da "/". Deve combaciare con `base` in
                vite.config.ts e con il resource handler lato Spring. */}
            <BrowserRouter basename="/statistiche">
                <Routes>
                    {/* Rotte annidate: <Layout> e' la cornice, e disegna le
                        figlie dentro il suo <Outlet />. Non ha un percorso
                        suo, quindi non compare nell'indirizzo. */}
                    <Route element={<Layout />}>
                        <Route path="/login" element={<LoginPage />} />

                        {/* Anche RottaProtetta e' una rotta senza percorso:
                            avvolge quelle che vogliono un utente collegato,
                            invece di far ripetere il controllo a ognuna. */}
                        <Route element={<RottaProtetta />}>
                            <Route path="/" element={<StatistichePage />} />
                        </Route>

                        {/* "*" e' l'ultima e prende tutto quello che non ha
                            trovato posto sopra. */}
                        <Route path="*" element={<NonTrovataPage />} />
                    </Route>
                </Routes>
            </BrowserRouter>
        </AuthProvider>
    </StrictMode>,
)
