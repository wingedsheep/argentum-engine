import { StrictMode, Suspense, lazy } from 'react'
import { createRoot } from 'react-dom/client'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import './styles/variables.css'
import './styles/base.css'
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
const AiSandboxPage = lazy(() =>
  import('./components/aiSandbox/AiSandboxPage').then(({ AiSandboxPage }) => ({ default: AiSandboxPage }))
)
const SetCompletionPage = lazy(() =>
  import('./components/setCompletion/SetCompletionPage').then(({ SetCompletionPage }) => ({ default: SetCompletionPage }))
)
const LoginVerifyPage = lazy(() =>
  import('./pages/LoginVerifyPage').then(({ LoginVerifyPage }) => ({ default: LoginVerifyPage }))
)
const ProfilePage = lazy(() =>
  import('./pages/ProfilePage').then(({ ProfilePage }) => ({ default: ProfilePage }))
)
const PreferencesPage = lazy(() =>
  import('./pages/PreferencesPage').then(({ PreferencesPage }) => ({ default: PreferencesPage }))
)
const FriendsPage = lazy(() =>
  import('./pages/FriendsPage').then(({ FriendsPage }) => ({ default: FriendsPage }))
)
const MessagesPage = lazy(() =>
  import('./pages/MessagesPage').then(({ MessagesPage }) => ({ default: MessagesPage }))
)
const StatsPage = lazy(() =>
  import('./pages/StatsPage').then(({ StatsPage }) => ({ default: StatsPage }))
)
const LearnPage = lazy(() =>
  import('./pages/LearnPage').then(({ LearnPage }) => ({ default: LearnPage }))
)
const HelpPage = lazy(() =>
  import('./pages/HelpPage').then(({ HelpPage }) => ({ default: HelpPage }))
)
const PublicProfilePage = lazy(() =>
  import('./pages/PublicProfilePage').then(({ PublicProfilePage }) => ({ default: PublicProfilePage }))
)

// Beside the router, not inside `App`: a search keeps running on every route (see the component).
const MatchmakingLayer = lazy(() => import('./components/matchmaking/MatchmakingLayer'))
// Same reason: reports the current page to the server (admin Live overview) on every route.
const ActivityReporter = lazy(() => import('./components/shared/ActivityReporter'))

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
          <Route path="/login/verify" element={<LoginVerifyPage />} />
          <Route path="/profile" element={<ProfilePage />} />
          <Route path="/preferences" element={<PreferencesPage />} />
          <Route path="/stats" element={<StatsPage />} />
          <Route path="/u/:userId" element={<PublicProfilePage />} />
          <Route path="/friends" element={<FriendsPage />} />
          <Route path="/messages" element={<MessagesPage />} />
          <Route path="/messages/:accountId" element={<MessagesPage />} />
          <Route path="/help" element={<HelpPage />} />
          <Route path="/help/:section" element={<HelpPage />} />
          <Route path="/learn" element={<LearnPage />} />
          <Route path="/learn/:missionId" element={<LearnPage />} />
          <Route path="/llm-tournament" element={<LlmTournamentPage />} />
          <Route path="/llm-tournament/:id" element={<LlmTournamentPage />} />
          <Route path="/ai-sandbox" element={<AiSandboxPage />} />
          <Route path="/ai-sandbox/:lobbyId" element={<AiSandboxPage />} />
          <Route path="*" element={<App />} />
        </Routes>
      </Suspense>
      <Suspense fallback={null}>
        <MatchmakingLayer />
        <ActivityReporter />
      </Suspense>
    </BrowserRouter>
  </StrictMode>
)
