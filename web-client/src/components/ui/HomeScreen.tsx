/**
 * Landing screen — before any game exists.
 *
 * Two layouts, by connection state:
 *
 * - **Not yet connected, or a created game waiting for its opponent** — the centred glass card:
 *   name entry, the Learn callout for a first-time visitor, the invite code.
 * - **Connected** — the play hub over the card art: a top bar (Deckbuilder, Replays, Sets, Learn, Help and
 *   the account), a "jump back in" row (a lobby still open from before a reload, saved setups, an
 *   invite-code field), and the {@link PlayHub} mode catalogue with its launch panel. Open lobbies,
 *   live games and the account callouts sit beside the catalogue and give way to the panel.
 *
 * What is playable is declarative (`lobby/playModes.ts` over `lobby/modeMatrix.ts`); this file only
 * lays the screen out and turns a recipe into lobby-creation messages via `useApplyRecipe`.
 */
import { useState, useEffect, useCallback, type ReactNode } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useGameStore } from '@/store/gameStore.ts'
import type { TournamentFormat } from '@/types'
import { randomBackground } from '@/utils/background.ts'
import { PreferencesButton } from '@/components/preferences/PreferencesButton'
import { ReplayViewer, type GameSummary } from '../admin/ReplayViewer'
import type { ReplayData } from '@/replay/reconstructSnapshots.ts'
import { labelForFormat } from '@/utils/deckLegality'
import { useAuthStore } from '@/store/authStore'
import { useConnectName } from '@/store/useConnectName'
import { AuthWidget } from '@/components/auth/AuthWidget'
import { LoginModal } from '@/components/auth/LoginModal'
import { DeckMigrationPrompt } from '@/components/auth/DeckMigrationPrompt'
import { AccountBenefitsCallout } from '@/components/auth/AccountBenefitsCallout'
import { LearnCallout } from '@/components/learn/LearnCallout'
import { WhatsNew } from '@/components/whatsNew/WhatsNew'
import { ArgentumMark } from './ArgentumMark'
import { FullscreenButton } from './FullscreenButton'
import { PlayHub } from './PlayHub'
import { SetupRail } from './SetupRail'
import { FindOpponentPanel } from '../matchmaking/FindOpponentPanel'
import { useApplyRecipe } from '../lobby/useApplyRecipe'
import { loadLobbyId, clearLobbyId } from '@/store/slices/shared'
import styles from './GameUI.module.css'
import home from './Home.module.css'
import welcome from './Welcome.module.css'

/** Community invite, also linked from the contributing guide's "get help" section. */
const DISCORD_INVITE_URL = 'https://discord.com/invite/dy6eSRPWzu'

/** Public source repository for the engine and web client. */
const GITHUB_REPOSITORY_URL = 'https://github.com/wingedsheep/argentum-engine'

/** The maker's portfolio site — a byline in the credits, not a community link. */
const MAKER_PORTFOLIO_URL = 'https://wingedsheep.com'

/**
 * The contributing guide is a static page under `web-client/public/`, not an SPA route — it must be
 * reached with a plain `<a href>` full navigation, never a react-router `Link`. Trailing slash so
 * nginx serves the directory index directly instead of leaning on the SPA fallback.
 */
const CONTRIBUTING_GUIDE_URL = '/contribute/'

interface PublicTournamentSummary {
  lobbyId: string
  state: string
  playerCount: number
  maxPlayers: number
  format: TournamentFormat
  setNames: string[]
  boosterCount: number
  gamesPerMatch: number
  deckFormat?: string | null
}

interface PublicQuickGameSummary {
  lobbyId: string
  playerCount: number
  maxPlayers: number
  setCode: string | null
  hostName: string | null
  format?: string | null
}

type PublicLobbyEntry =
  | ({ kind: 'tournament' } & PublicTournamentSummary)
  | ({ kind: 'quickGame' } & PublicQuickGameSummary)

interface LiveQuickGameSummary {
  gameSessionId: string
  player1Name: string
  player2Name: string
  player1Life: number
  player2Life: number
}

interface LiveTournamentMatchSummary {
  gameSessionId: string
  lobbyId: string
  round: number
  player1Name: string
  player2Name: string
  player1Life: number
  player2Life: number
}

type LiveGameEntry =
  | ({ kind: 'tournament' } & LiveTournamentMatchSummary)
  | ({ kind: 'quickGame' } & LiveQuickGameSummary)

/**
 * Home screen shown before a game starts — and the router into the lobby / tournament overlays.
 */
export function HomeScreen({
  status,
  sessionId,
  error,
}: {
  status: string
  sessionId: string | null
  error: string | undefined
}) {
  const navigate = useNavigate()
  const connect = useGameStore((state) => state.connect)
  const aiEnabled = useGameStore((state) => state.aiEnabled)
  const joinQuickGameLobby = useGameStore((state) => state.joinQuickGameLobby)
  const applyRecipe = useApplyRecipe()
  const lobbyState = useGameStore((state) => state.lobbyState)
  const [joinSessionId, setJoinSessionId] = useState('')
  const [playerName, setPlayerName] = useState(() => localStorage.getItem('argentum-player-name') || '')

  // The name we already have (stored, or the signed-in account's) versus one typed here just now.
  const { name: connectName, resolving: nameResolving } = useConnectName()
  const [nameConfirmed, setNameConfirmed] = useState(false)
  const [loginOpen, setLoginOpen] = useState(false)
  const [showReplays, setShowReplays] = useState(false)
  const [publicLobbies, setPublicLobbies] = useState<PublicLobbyEntry[]>([])
  const [publicLobbiesError, setPublicLobbiesError] = useState<string | null>(null)
  const [liveGames, setLiveGames] = useState<LiveGameEntry[]>([])
  // A join requested before the socket was up (name entry, or a public-lobby row clicked while
  // disconnected), replayed once we're connected. Kind-agnostic: the quick-game join handler
  // delegates to the tournament handler when the code belongs to one.
  const [pendingJoinCode, setPendingJoinCode] = useState<string | null>(null)
  // Read once at first render, before `connect` gets a chance to clear it: the lobby this browser
  // was in before the page reloaded. Surfaced as the Continue chip — a mid-lobby refresh used to
  // land you back here with no indication that a lobby was still waiting for you.
  const [resumableLobbyId, setResumableLobbyId] = useState<string | null>(() => loadLobbyId())
  const onlinePlayers = useGameStore((state) => state.onlinePlayers)
  const spectateGame = useGameStore((state) => state.spectateGame)
  const setPendingSpectateGameId = useGameStore((state) => state.setPendingSpectateGameId)
  // Server config + session are bootstrapped by `useConnectName` above, so the AuthWidget knows
  // whether to show at all.
  const authStatus = useAuthStore((state) => state.status)
  const accountsEnabled = useAuthStore((state) => state.accountsEnabled)

  const confirmName = () => {
    if (playerName.trim()) {
      localStorage.setItem('argentum-player-name', playerName.trim())
      setNameConfirmed(true)
      if (joinSessionId.trim()) setPendingJoinCode(joinSessionId.trim())
      connect(playerName.trim())
    }
  }

  const handleJoin = () => {
    if (joinSessionId.trim()) {
      // Unified join: send to QuickGameLobbyHandler, which delegates to the tournament
      // handler if the code happens to be a tournament lobby. The home-screen Join field
      // doesn't care which kind of lobby is behind a code.
      joinQuickGameLobby(joinSessionId.trim())
    }
  }

  // Replay a join that was queued while disconnected.
  useEffect(() => {
    if (!pendingJoinCode || status !== 'connected') return
    setPendingJoinCode(null)
    joinQuickGameLobby(pendingJoinCode)
  }, [pendingJoinCode, status, joinQuickGameLobby])

  useEffect(() => {
    if (sessionId || lobbyState) {
      setPublicLobbies([])
      setLiveGames([])
      return
    }

    let cancelled = false
    const loadPublicLobbies = async () => {
      try {
        const [tournamentsRes, quickGamesRes, liveQuickRes, liveTournRes] = await Promise.all([
          fetch('/api/tournaments/public'),
          fetch('/api/quick-games/public'),
          fetch('/api/quick-games/live'),
          fetch('/api/tournaments/live'),
        ])
        if (!tournamentsRes.ok) throw new Error(`Tournaments: ${tournamentsRes.status}`)
        if (!quickGamesRes.ok) throw new Error(`Quick games: ${quickGamesRes.status}`)
        const tournaments = await tournamentsRes.json() as PublicTournamentSummary[]
        const quickGames = await quickGamesRes.json() as PublicQuickGameSummary[]
        const liveQuick = liveQuickRes.ok ? await liveQuickRes.json() as LiveQuickGameSummary[] : []
        const liveTourn = liveTournRes.ok ? await liveTournRes.json() as LiveTournamentMatchSummary[] : []
        if (!cancelled) {
          const merged: PublicLobbyEntry[] = [
            ...quickGames.map((q) => ({ kind: 'quickGame' as const, ...q })),
            ...tournaments.map((t) => ({ kind: 'tournament' as const, ...t })),
          ]
          const live: LiveGameEntry[] = [
            ...liveQuick.map((g) => ({ kind: 'quickGame' as const, ...g })),
            ...liveTourn.map((m) => ({ kind: 'tournament' as const, ...m })),
          ]
          setPublicLobbies(merged)
          setLiveGames(live)
          setPublicLobbiesError(null)
        }
      } catch {
        if (!cancelled) {
          setPublicLobbies([])
          setLiveGames([])
          setPublicLobbiesError('Could not load public lobbies.')
        }
      }
    }

    void loadPublicLobbies()
    const interval = window.setInterval(loadPublicLobbies, 10_000)
    return () => {
      cancelled = true
      window.clearInterval(interval)
    }
  }, [sessionId, lobbyState])

  // Bootstrap the online-players count via REST so the badge appears before the
  // user has a WebSocket session. Once connected, the server pushes
  // OnlinePlayersCount on every connect/disconnect (see ConnectionHandler).
  useEffect(() => {
    if (sessionId || lobbyState || onlinePlayers !== null) return
    let cancelled = false
    fetch('/api/players/online')
      .then((res) => (res.ok ? res.json() as Promise<{ count: number }> : null))
      .then((data) => {
        if (!cancelled && data) useGameStore.setState({ onlinePlayers: data.count })
      })
      .catch(() => { /* ignore — WS push will populate */ })
    return () => { cancelled = true }
  }, [sessionId, lobbyState, onlinePlayers])

  const fetchPlayerGames = useCallback(async (): Promise<GameSummary[]> => {
    const token = localStorage.getItem('argentum-token')
    if (!token) throw new Error('No player token')
    const res = await fetch('/api/replays', {
      headers: { 'X-Player-Token': token },
    })
    if (!res.ok) throw new Error(`Server error: ${res.status}`)
    return await res.json() as GameSummary[]
  }, [])

  const fetchPlayerReplay = useCallback(async (gameId: string): Promise<ReplayData> => {
    const token = localStorage.getItem('argentum-token')
    if (!token) throw new Error('No player token')
    const res = await fetch(`/api/replays/${gameId}`, {
      headers: { 'X-Player-Token': token },
    })
    if (!res.ok) throw new Error(`Failed to load replay: ${res.status}`)
    return await res.json() as ReplayData
  }, [])

  // Lobby, tournament standings and FFA standings are routed in `GameUI.tsx`, which never mounts
  // this screen while any of them is live.

  // Show replay viewer overlay
  if (showReplays) {
    return (
      <ReplayViewer
        fetchGames={fetchPlayerGames}
        fetchReplay={fetchPlayerReplay}
        onBack={() => setShowReplays(false)}
      />
    )
  }

  const showPublicLobbies = !sessionId && !lobbyState && (publicLobbies.length > 0 || publicLobbiesError || (onlinePlayers ?? 0) > 0)
  const showLiveGames = !sessionId && !lobbyState && liveGames.length > 0
  // Only the lobby panels; the account widget rides the top bar, so an empty server no longer
  // reserves a 320px rail (and its mirror gutter) for a single row of sign-in pills.
  const showSideRail = showPublicLobbies || showLiveGames

  const handleSpectate = (gameSessionId: string) => {
    if (status === 'connected') {
      spectateGame(gameSessionId)
      return
    }
    const name = connectName ?? playerName.trim()
    if (!name) return
    localStorage.setItem('argentum-player-name', name)
    setPendingSpectateGameId(gameSessionId)
    setNameConfirmed(true)
    connect(name)
  }

  const joinPublicLobby = (entry: PublicLobbyEntry) => {
    setJoinSessionId(entry.lobbyId)
    if (status === 'connected') {
      // QuickGameLobbyHandler routes by lobby kind — works for both.
      joinQuickGameLobby(entry.lobbyId)
    } else {
      const name = connectName ?? playerName.trim()
      if (!name) return
      localStorage.setItem('argentum-player-name', name)
      setPendingJoinCode(entry.lobbyId)
      setNameConfirmed(true)
      connect(name)
    }
  }

  const sideLists = (
    <>
      {showPublicLobbies && (
        <PublicLobbyList
          lobbies={publicLobbies}
          error={publicLobbiesError}
          onlinePlayers={onlinePlayers}
          onJoin={joinPublicLobby}
        />
      )}
      {showLiveGames && (
        <LiveGameList
          games={liveGames}
          onSpectate={handleSpectate}
          disabled={!connectName && !playerName.trim() && status !== 'connected'}
        />
      )}
    </>
  )

  // Connected and not in a game: the mode catalogue. Everything before that — name entry, the
  // connection in flight, a created game waiting for its opponent — keeps the centred glass card.
  if (status === 'connected' && !sessionId) {
    return (
      <div
        className={`${styles.connectionOverlay} ${home.overlay}`}
        style={{ backgroundImage: `url(${randomBackground})` }}
      >
        <div className={home.artTint} aria-hidden />
        <header className={home.topBar}>
          <Link to="/" className={home.brand} aria-label="Argentum — home" onClick={() => setShowReplays(false)}>
            <BrandMark />
            Argentum
          </Link>
          <nav className={home.nav} aria-label="Main">
            <button
              type="button"
              className={home.navItem}
              onClick={() => navigate('/deckbuilder')}
              title="Build and save decks, and search every card"
            >
              Deckbuilder
            </button>
            <button type="button" className={home.navItem} onClick={() => setShowReplays(true)}>Replays</button>
            {/* "Which cards of a set can I actually play with?" is a deckbuilding question. */}
            <button type="button" className={home.navItem} onClick={() => navigate('/set-completion')}>Sets</button>
            <button type="button" className={home.navItem} onClick={() => navigate('/learn')}>Learn</button>
            <button
              type="button"
              className={home.navItem}
              onClick={() => navigate('/help')}
              title="How Argentum works — modes, priority, shortcuts"
            >
              Help
            </button>
          </nav>
          <div className={home.topBarEnd}>
            <WhatsNew compact />
            <PreferencesButton />
            <FullscreenButton compact />
            <AuthWidget />
          </div>
        </header>

        <main className={home.main}>
          {error && <p className={home.error}>Error: {error}</p>}

          <div className={home.quickRow}>
            {resumableLobbyId && (
              <div className={home.resume}>
                <div className={home.resumeText}>
                  <span className={home.resumeLabel}>Lobby still open</span>
                  <span className={home.resumeCode}>{resumableLobbyId}</span>
                </div>
                <button
                  type="button"
                  className={home.resumeButton}
                  onClick={() => joinQuickGameLobby(resumableLobbyId)}
                >
                  Rejoin
                </button>
                <button
                  type="button"
                  className={home.resumeDismiss}
                  aria-label="Dismiss"
                  title="I'm done with that lobby"
                  onClick={() => { clearLobbyId(); setResumableLobbyId(null) }}
                >
                  ×
                </button>
              </div>
            )}
            {/* Absent until you have played something: a returning player gets one click, a
                first-time player sees only the catalogue. */}
            <SetupRail onLaunch={applyRecipe} />
          </div>

          <PlayHub
            aiEnabled={aiEnabled}
            onLaunch={(recipe) => applyRecipe(recipe)}
            // Not a mode: someone with a code has had every question answered for them.
            headerAction={
              <form
                className={home.joinCard}
                onSubmit={(e) => { e.preventDefault(); handleJoin() }}
              >
                <label htmlFor="join-code" className={home.joinLabel}>Have a code?</label>
                <input
                  id="join-code"
                  type="text"
                  value={joinSessionId}
                  onChange={(e) => setJoinSessionId(e.target.value)}
                  placeholder="Invite code"
                  autoComplete="off"
                  className={home.joinInput}
                />
                <button type="submit" disabled={!joinSessionId.trim()} className={home.joinButton} data-testid="join-code-submit">
                  Join
                </button>
              </form>
            }
            aside={
              <>
                <FindOpponentPanel onSignIn={() => setLoginOpen(true)} />
                {sideLists}
                <LearnCallout variant="tier" />
                <AccountBenefitsCallout onCreateAccount={() => setLoginOpen(true)} />
                <DeckMigrationPrompt />
              </>
            }
          />

        </main>

        <HomeFooter>
          {/* Dev builds only: every entry point drives `/api/dev/*`, which exists only when the
              server runs with GAME_DEV_ENDPOINTS_ENABLED. The *routes* stay open either way — a
              replay's "share as scenario" link is a real `/scenario?s=` deep link. */}
          {import.meta.env.DEV && (
            <nav className={home.lab} aria-label="Lab">
              <span className={home.labLabel}>Lab</span>
              <button type="button" onClick={() => navigate('/scenario')} className={home.labLink}>Scenario Builder</button>
              <button type="button" onClick={() => navigate('/llm-tournament')} className={home.labLink}>LLM Tournament</button>
              {/* Bot-vs-bot with nobody in a seat — the way to watch the engine AI play. */}
              <button type="button" onClick={() => navigate('/ai-sandbox')} className={home.labLink}>AI Sandbox</button>
            </nav>
          )}
        </HomeFooter>
        <LoginModal open={loginOpen} onClose={() => setLoginOpen(false)} />
      </div>
    )
  }

  // Before the hub: name entry, the connection in flight, or a created game waiting for its
  // opponent. The hub's frame (top bar, footer) around one centred glass card.
  const askingForName = !nameConfirmed && !connectName && !nameResolving
  const connecting = !askingForName && !sessionId && !error
  return (
    <div
      className={`${styles.connectionOverlay} ${home.overlay}`}
      style={{ backgroundImage: `url(${randomBackground})` }}
    >
      <div className={home.artTint} aria-hidden />
      <header className={home.topBar}>
        <Link to="/" className={home.brand} aria-label="Argentum — home">
          <BrandMark />
          Argentum
        </Link>
        <nav className={home.nav} aria-label="Main">
          <button type="button" className={home.navItem} onClick={() => navigate('/deckbuilder')}>Deckbuilder</button>
          <button type="button" className={home.navItem} onClick={() => navigate('/set-completion')}>Sets</button>
          <button type="button" className={home.navItem} onClick={() => navigate('/learn')}>Learn</button>
          <button
            type="button"
            className={home.navItem}
            onClick={() => navigate('/help')}
            title="How Argentum works — modes, priority, shortcuts"
          >
            Help
          </button>
        </nav>
        <div className={home.topBarEnd}>
          <WhatsNew compact />
          <PreferencesButton />
          <FullscreenButton compact />
          <AuthWidget />
        </div>
      </header>

      <main className={welcome.main}>
        <div className={welcome.layout} data-side={showSideRail}>
          <div className={welcome.card}>
            <div className={welcome.heading}>
              <h1 className={welcome.title}>
                {sessionId ? 'Game created' : joinSessionId ? 'Join the table' : 'Welcome to Argentum'}
              </h1>
              {askingForName && !sessionId && (
                <p className={welcome.lede}>Play Magic in your browser — against friends, strangers or the AI.</p>
              )}
            </div>

            {error && <p className={welcome.error}>Error: {error}</p>}

            {/* Only a visitor with no name at all is asked for one. A signed-in account already has a
                display name, and the server would overwrite anything typed here with it. Held back
                while the account check is in flight so the prompt can't flash and vanish. */}
            {askingForName && (
              <>
                {/* The one screen a first-time visitor sees. Someone who has never played Magic
                    needs the course before they need a name, so it comes first — unless they arrived
                    with an invite code or a game token, in which case they are here to join a
                    table, not to learn. */}
                {!joinSessionId && !new URLSearchParams(window.location.search).has('token') && (
                  <LearnCallout variant="arrival" />
                )}
                <div className={welcome.form}>
                  <label className={welcome.label} htmlFor="player-name">
                    {joinSessionId ? 'Your name, to join' : 'What should we call you?'}
                  </label>
                  <div className={welcome.inputRow}>
                    <input
                      id="player-name"
                      type="text"
                      value={playerName}
                      onChange={(e) => setPlayerName(e.target.value)}
                      onKeyDown={(e) => { if (e.key === 'Enter') confirmName() }}
                      placeholder="Your name"
                      autoFocus
                      maxLength={20}
                      className={welcome.input}
                    />
                    <button
                      type="button"
                      onClick={confirmName}
                      disabled={!playerName.trim()}
                      className={welcome.primary}
                    >
                      Continue
                    </button>
                  </div>
                </div>
                {accountsEnabled && authStatus !== 'authenticated' && (
                  <p className={welcome.nudge}>
                    Playing as a guest.{' '}
                    <button
                      type="button"
                      onClick={() => setLoginOpen(true)}
                      className={welcome.nudgeButton}
                    >
                      Create a free account
                    </button>{' '}
                    — one magic link, no password — to save decks across devices, add friends, play
                    ranked, track your stats, and rewatch your games.
                  </p>
                )}
              </>
            )}

            {connecting && (
              <div className={welcome.status} role="status">
                <span className={welcome.spinner} aria-hidden />
                Connecting…
              </div>
            )}

            {sessionId && (
              <WaitingForOpponent sessionId={sessionId} />
            )}

            <span className={welcome.commit}>{__COMMIT_HASH__}</span>
          </div>

          {showSideRail && (
            <div className={welcome.side}>
              {sideLists}
            </div>
          )}
        </div>
      </main>

      <HomeFooter />
      <LoginModal open={loginOpen} onClose={() => setLoginOpen(false)} />
    </div>
  )
}

/** The Argentum mark beside the wordmark. */
function BrandMark() {
  return <ArgentumMark size={36} className={home.brandMark} />
}

/**
 * One glass bar, styled like the top bar's nav: the credits and fan-content disclaimer on the left,
 * the community links on the right, and `children` (the dev-only Lab links) between them. On a
 * desktop it is a single row — anything taller would push the hub into scrolling on a laptop.
 */
function HomeFooter({ children }: { children?: ReactNode }) {
  return (
    <footer className={home.footer}>
      <div className={home.footerBar}>
        <p className={home.credits}>
          <span>
            Made by <a href={MAKER_PORTFOLIO_URL} target="_blank" rel="noopener noreferrer">wingedsheep</a>
            <span className={home.creditsDot} aria-hidden>·</span>
            Card images via <a href="https://scryfall.com" target="_blank" rel="noopener noreferrer">Scryfall</a>
            <span className={home.creditsDot} aria-hidden>·</span>
            Mana symbols by <a href="https://mana.andrewgioia.com" target="_blank" rel="noopener noreferrer">Mana Font</a>
          </span>
          <span className={home.disclaimer}>
            Fan-made; not affiliated with or endorsed by Wizards of the Coast. Magic: The Gathering is © Wizards of the Coast LLC.
          </span>
        </p>
        {children}
        <nav className={home.community} aria-label="Community">
          <a href={DISCORD_INVITE_URL} target="_blank" rel="noopener noreferrer" className={home.communityLink} data-brand="discord">
            <DiscordIcon />
            Discord
          </a>
          <a href={GITHUB_REPOSITORY_URL} target="_blank" rel="noopener noreferrer" className={home.communityLink}>
            <GitHubIcon />
            GitHub
          </a>
          <a href={CONTRIBUTING_GUIDE_URL} target="_blank" rel="noopener noreferrer" className={home.communityLink}>
            <GuideIcon />
            Help build it
          </a>
        </nav>
      </div>
    </footer>
  )
}

/**
 * Discord's own wordmark glyph, from simple-icons (CC0) — the brand mark is what makes the link
 * recognisable at footer size, where a generic chat bubble would not be.
 */
function DiscordIcon() {
  return (
    <svg className={styles.communityIcon} viewBox="0 0 24 24" fill="currentColor" aria-hidden focusable="false">
      <path d="M20.317 4.3698a19.7913 19.7913 0 00-4.8851-1.5152.0741.0741 0 00-.0785.0371c-.211.3753-.4447.8648-.6083 1.2495-1.8447-.2762-3.68-.2762-5.4868 0-.1636-.3933-.4058-.8742-.6177-1.2495a.077.077 0 00-.0785-.037 19.7363 19.7363 0 00-4.8852 1.515.0699.0699 0 00-.0321.0277C.5334 9.0458-.319 13.5799.0992 18.0578a.0824.0824 0 00.0312.0561c2.0528 1.5076 4.0413 2.4228 5.9929 3.0294a.0777.0777 0 00.0842-.0276c.4616-.6304.8731-1.2952 1.226-1.9942a.076.076 0 00-.0416-.1057c-.6528-.2476-1.2743-.5495-1.8722-.8923a.077.077 0 01-.0076-.1277c.1258-.0943.2517-.1923.3718-.2914a.0743.0743 0 01.0776-.0105c3.9278 1.7933 8.18 1.7933 12.0614 0a.0739.0739 0 01.0785.0095c.1202.099.246.1981.3728.2924a.077.077 0 01-.0066.1276 12.2986 12.2986 0 01-1.873.8914.0766.0766 0 00-.0407.1067c.3604.698.7719 1.3628 1.225 1.9932a.076.076 0 00.0842.0286c1.961-.6067 3.9495-1.5219 6.0023-3.0294a.077.077 0 00.0313-.0552c.5004-5.177-.8382-9.6739-3.5485-13.6604a.061.061 0 00-.0312-.0286zM8.02 15.3312c-1.1825 0-2.1569-1.0857-2.1569-2.419 0-1.3332.9555-2.4189 2.157-2.4189 1.2108 0 2.1757 1.0952 2.1568 2.419 0 1.3332-.9555 2.4189-2.1569 2.4189zm7.9748 0c-1.1825 0-2.1569-1.0857-2.1569-2.419 0-1.3332.9554-2.4189 2.1569-2.4189 1.2108 0 2.1757 1.0952 2.1568 2.419 0 1.3332-.946 2.4189-2.1568 2.4189Z" />
    </svg>
  )
}

/** GitHub's mark, kept inline so the landing page needs no icon dependency or extra asset request. */
function GitHubIcon() {
  return (
    <svg className={styles.communityIcon} viewBox="0 0 24 24" fill="currentColor" aria-hidden focusable="false">
      <path d="M12 .7a11.5 11.5 0 00-3.64 22.41c.58.1.79-.25.79-.56v-2.23c-3.22.7-3.9-1.37-3.9-1.37-.52-1.34-1.28-1.7-1.28-1.7-1.05-.72.08-.7.08-.7 1.16.08 1.77 1.19 1.77 1.19 1.03 1.77 2.7 1.26 3.36.96.1-.75.4-1.26.73-1.55-2.57-.29-5.27-1.28-5.27-5.68 0-1.25.45-2.28 1.18-3.08-.12-.29-.51-1.46.11-3.04 0 0 .97-.31 3.16 1.18a10.95 10.95 0 015.75 0c2.2-1.49 3.16-1.18 3.16-1.18.63 1.58.23 2.75.12 3.04.73.8 1.17 1.83 1.17 3.08 0 4.42-2.71 5.38-5.29 5.67.42.36.79 1.07.79 2.16v3.23c0 .31.21.67.8.56A11.5 11.5 0 0012 .7z" />
    </svg>
  )
}

/**
 * A wrench, not a card-with-a-plus: contributions are bug fixes, UX work and rules corrections as
 * much as new cards, and the icon should not imply the narrower of the two.
 */
function GuideIcon() {
  return (
    <svg
      className={styles.communityIcon}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.9"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden
      focusable="false"
    >
      <path d="M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91a6 6 0 0 1 7.94-7.94l-3.76 3.76z" />
    </svg>
  )
}

function PublicLobbyList({
  lobbies,
  error,
  onlinePlayers,
  onJoin,
}: {
  lobbies: PublicLobbyEntry[]
  error: string | null
  onlinePlayers: number | null
  onJoin: (entry: PublicLobbyEntry) => void
}) {
  if (lobbies.length === 0 && !error && (onlinePlayers ?? 0) === 0) return null

  return (
    <div className={styles.publicTournamentPanel}>
      <div className={styles.publicTournamentHeader}>
        <span className={styles.publicTournamentTitle}>Public Lobbies</span>
        <div className={styles.publicTournamentHeaderRight}>
          {onlinePlayers !== null && onlinePlayers > 0 && (
            <span className={styles.onlinePlayersBadge}>
              <span className={styles.onlinePlayersDot} />
              {onlinePlayers} online
            </span>
          )}
          {lobbies.length > 0 && (
            <span className={styles.publicTournamentCount}>{lobbies.length}</span>
          )}
        </div>
      </div>
      {lobbies.length === 0 && !error ? (
        <p className={styles.publicTournamentEmpty}>No public lobbies right now.</p>
      ) : error && lobbies.length === 0 ? (
        <p className={styles.publicTournamentEmpty}>{error}</p>
      ) : (
        lobbies.map((entry) => (
          <div key={`${entry.kind}-${entry.lobbyId}`} className={styles.publicTournamentRow}>
            <div className={styles.publicTournamentInfo}>
              <span className={styles.publicTournamentName}>{publicLobbyName(entry)}</span>
              <span className={styles.publicTournamentMeta}>{publicLobbyMeta(entry)}</span>
            </div>
            <button onClick={() => onJoin(entry)} className={styles.publicTournamentJoinButton}>
              Join
            </button>
          </div>
        ))
      )}
    </div>
  )
}

function LiveGameList({
  games,
  onSpectate,
  disabled,
}: {
  games: LiveGameEntry[]
  onSpectate: (gameSessionId: string) => void
  disabled: boolean
}) {
  return (
    <div className={styles.publicTournamentPanel}>
      <div className={styles.publicTournamentHeader}>
        <span className={styles.publicTournamentTitle}>Live Games</span>
        <div className={styles.publicTournamentHeaderRight}>
          <span className={styles.liveBadge}>
            <span className={styles.liveDot} />
            Live
          </span>
          <span className={styles.publicTournamentCount}>{games.length}</span>
        </div>
      </div>
      {games.map((game) => (
        <div key={`${game.kind}-${game.gameSessionId}`} className={styles.publicTournamentRow}>
          <div className={styles.publicTournamentInfo}>
            <span className={styles.publicTournamentName}>
              {game.player1Name} vs {game.player2Name}
            </span>
            <span className={styles.publicTournamentMeta}>{liveGameMeta(game)}</span>
          </div>
          <button
            onClick={() => onSpectate(game.gameSessionId)}
            disabled={disabled}
            className={styles.spectateButton}
          >
            Spectate
          </button>
        </div>
      ))}
    </div>
  )
}

function liveGameMeta(game: LiveGameEntry): string {
  const lifeSummary = nbsp(`${game.player1Life} / ${game.player2Life} life`)
  if (game.kind === 'tournament') {
    return `Tournament · ${nbsp(`Round ${game.round}`)} · ${lifeSummary}`
  }
  return `${nbsp('Quick Game')} · ${lifeSummary}`
}

function publicLobbyName(entry: PublicLobbyEntry): string {
  if (entry.kind === 'tournament') {
    if (entry.format === 'PREMADE_DECKS') return 'Premade Decks Tournament'
    return entry.setNames.join(' + ') || 'Tournament'
  }
  return entry.hostName ? `${entry.hostName}'s Quick Game` : 'Quick Game'
}

/**
 * Non-breaking spaces inside each fact, so the rail's narrow column only ever wraps at a `·` —
 * "1/2" and "players" on separate lines read as two facts rather than one.
 */
function nbsp(text: string): string {
  return text.replace(/ /g, ' ')
}

function publicLobbyMeta(entry: PublicLobbyEntry): string {
  const seats = nbsp(`${entry.playerCount}/${entry.maxPlayers} players`)
  if (entry.kind === 'tournament') {
    const series = entry.gamesPerMatch > 1 ? nbsp(`${entry.gamesPerMatch} games per matchup`) : null
    if (entry.format === 'PREMADE_DECKS') {
      const parts = [nbsp('Premade Decks')]
      if (entry.deckFormat) parts.push(nbsp(labelForFormat(entry.deckFormat)))
      parts.push(seats)
      if (series) parts.push(series)
      return parts.join(' · ')
    }
    const packs = nbsp(`${entry.boosterCount} ${entry.format === 'DRAFT' ? 'packs' : 'boosters'}`)
    const parts = [nbsp(formatTournamentFormat(entry.format)), packs, seats]
    if (series) parts.push(series)
    return parts.join(' · ')
  }
  const parts = [nbsp('Quick Game')]
  if (entry.setCode) parts.push(entry.setCode)
  if (entry.format) parts.push(nbsp(labelForFormat(entry.format)))
  parts.push(seats)
  return parts.join(' · ')
}

function formatTournamentFormat(format: PublicTournamentSummary['format']): string {
  switch (format) {
    case 'WINSTON_DRAFT':
      return 'Winston Draft'
    case 'GRID_DRAFT':
      return 'Grid Draft'
    case 'DRAFT':
      return 'Draft'
    case 'COMMANDER_DRAFT':
      return 'Commander Draft'
    case 'SEALED':
      return 'Sealed'
    case 'COMMANDER_SEALED':
      return 'Commander Sealed'
    case 'PREMADE_DECKS':
      return 'Premade Decks'
  }
}

/**
 * Waiting for opponent display.
 */
function WaitingForOpponent({
  sessionId,
}: {
  sessionId: string
}) {
  const cancelGame = useGameStore((state) => state.cancelGame)
  const [copied, setCopied] = useState(false)

  const copySessionId = () => {
    navigator.clipboard.writeText(sessionId)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className={welcome.waiting}>
      <button
        type="button"
        onClick={copySessionId}
        className={welcome.inviteButton}
        data-copied={copied}
      >
        <span className={welcome.inviteText}>
          <span className={welcome.inviteLabel}>Invite code — send it to your opponent</span>
          <span className={welcome.inviteCode}>{sessionId}</span>
        </span>
        <span className={welcome.inviteAction}>{copied ? 'Copied!' : 'Copy'}</span>
      </button>
      <div className={welcome.status} role="status">
        <span className={welcome.spinner} aria-hidden />
        Waiting for your opponent to join…
      </div>
      <button type="button" onClick={cancelGame} className={welcome.secondary}>
        Cancel game
      </button>
    </div>
  )
}
