import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'

export default function RottaProtetta() {
    const { autenticato } = useAuth()
    const posizione = useLocation()

    if (!autenticato) {
        return <Navigate to="/login" replace state={{ da: posizione.pathname }} />
    }

    return <Outlet />
}
