import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import './styles/variables.css'
import './styles/responsive.css'
import App from './App'
import { TournamentEntryPage } from './components/tournament/TournamentEntryPage'
import { AdminPage } from './components/admin/AdminPage'
import { initAnalytics } from './utils/analytics'

initAnalytics()

const rootElement = document.getElementById('root')
if (!rootElement) {
  throw new Error('Root element not found')
}

createRoot(rootElement).render(
  <StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/tournament/:lobbyId" element={<TournamentEntryPage />} />
        <Route path="/admin" element={<AdminPage />} />
        <Route path="*" element={<App />} />
      </Routes>
    </BrowserRouter>
  </StrictMode>
)
