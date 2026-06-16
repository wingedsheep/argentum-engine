import { StrictMode, Suspense, lazy } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import './styles/variables.css'
import './styles/responsive.css'
import 'mana-font/css/mana.min.css'
import 'keyrune/css/keyrune.min.css'
import { initAnalytics } from './utils/analytics'

const App = lazy(() => import('./App'))
const TournamentEntryPage = lazy(() =>
  import('./components/tournament/TournamentEntryPage').then(({ TournamentEntryPage }) => ({ default: TournamentEntryPage }))
)
const JoinLobbyPage = lazy(() =>
  import('./components/lobby/JoinLobbyPage').then(({ JoinLobbyPage }) => ({ default: JoinLobbyPage }))
)
const AdminPage = lazy(() =>
  import('./components/admin/AdminPage').then(({ AdminPage }) => ({ default: AdminPage }))
)
const ReplayPage = lazy(() =>
  import('./components/replay/ReplayPage').then(({ ReplayPage }) => ({ default: ReplayPage }))
)
const DeckbuilderPage = lazy(() =>
  import('./components/deckbuilder/DeckbuilderPage').then(({ DeckbuilderPage }) => ({ default: DeckbuilderPage }))
)
const ScenarioBuilderPage = lazy(() =>
  import('./components/scenario/ScenarioBuilderPage').then(({ ScenarioBuilderPage }) => ({ default: ScenarioBuilderPage }))
)
const LlmTournamentPage = lazy(() =>
  import('./components/llmTournament/LlmTournamentPage').then(({ LlmTournamentPage }) => ({ default: LlmTournamentPage }))
)
const SetCompletionPage = lazy(() =>
  import('./components/setCompletion/SetCompletionPage').then(({ SetCompletionPage }) => ({ default: SetCompletionPage }))
)

initAnalytics()

const rootElement = document.getElementById('root')
if (!rootElement) {
  throw new Error('Root element not found')
}

createRoot(rootElement).render(
  <StrictMode>
    <BrowserRouter>
      <Suspense fallback={null}>
        <Routes>
          <Route path="/tournament/:lobbyId" element={<TournamentEntryPage />} />
          <Route path="/join/:lobbyId" element={<JoinLobbyPage />} />
          <Route path="/replay/:gameId" element={<ReplayPage />} />
          <Route path="/admin" element={<AdminPage />} />
          <Route path="/deckbuilder" element={<DeckbuilderPage />} />
          <Route path="/deckbuilder/:deckId" element={<DeckbuilderPage />} />
          <Route path="/scenario" element={<ScenarioBuilderPage />} />
          <Route path="/set-completion" element={<SetCompletionPage />} />
          <Route path="/llm-tournament" element={<LlmTournamentPage />} />
          <Route path="/llm-tournament/:id" element={<LlmTournamentPage />} />
          <Route path="*" element={<App />} />
        </Routes>
      </Suspense>
    </BrowserRouter>
  </StrictMode>
)
