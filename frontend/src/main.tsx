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

createRoot(document.getElementById('root')!).render(
    <StrictMode>
        <AuthProvider>
            <BrowserRouter basename="/statistiche">
                <Routes>
                    <Route element={<Layout />}>
                        <Route path="/login" element={<LoginPage />} />

                        <Route element={<RottaProtetta />}>
                            <Route path="/" element={<StatistichePage />} />
                        </Route>

                        <Route path="*" element={<NonTrovataPage />} />
                    </Route>
                </Routes>
            </BrowserRouter>
        </AuthProvider>
    </StrictMode>,
)
