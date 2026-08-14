/**
 * Landing screen — the centred glass card you see before any game exists.
 *
 * Three labelled tiers instead of one `Quick Game | Tournament` toggle:
 *
 * - **PLAY** — the {@link PlayWizard}'s three questions, a join-code row, and a Continue chip when a
 *   lobby is still live from a previous page load.
 * - **BUILD & BROWSE** — deckbuilder, replays and set completion. The account pages (`/stats`,
 *   `/friends`, `/profile`) live on the {@link AuthWidget} in the top bar instead, next to who you
 *   are signed in as.
 * - **LAB** — debugging and content tools, dev builds only; the tier does not render otherwise.
 *
 * What is playable is declarative (`lobby/modeMatrix.ts`) and the wizard only renders it; this file
 * only knows how to turn a finished selection into lobby-creation messages.
 */
import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useGameStore } from '@/store/gameStore.ts'
import type { TournamentFormat } from '@/types'
import { randomBackground } from '@/utils/background.ts'
import { ReplayViewer, type GameSummary } from '../admin/ReplayViewer'
import type { ReplayData } from '@/replay/reconstructSnapshots.ts'
import { labelForFormat } from '@/utils/deckLegality'
import { useAuthStore } from '@/store/authStore'
import { useConnectName } from '@/store/useConnectName'
import { AuthWidget } from '@/components/auth/AuthWidget'
import { LoginModal } from '@/components/auth/LoginModal'
import { DeckMigrationPrompt } from '@/components/auth/DeckMigrationPrompt'
import { AccountBenefitsCallout } from '@/components/auth/AccountBenefitsCallout'
import { FullscreenButton } from './FullscreenButton'
import { PlayWizard } from './PlayWizard'
import { SetupRail } from './SetupRail'
import type { Selection } from '../lobby/modeMatrix'
import { recipeFromSelection } from '../lobby/lobbyRecipe'
import { useApplyRecipe } from '../lobby/useApplyRecipe'
import { loadLobbyId, clearLobbyId } from '@/store/slices/shared'
import styles from './GameUI.module.css'

/** Community invite, also linked from the contributing guide's "get help" section. */
const DISCORD_INVITE_URL = 'https://discord.com/invite/dy6eSRPWzu'

/** Public source repository for the engine and web client. */
const GITHUB_REPOSITORY_URL = 'https://github.com/wingedsheep/argentum-engine'

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

  /**
   * Create the lobby a completed wizard selection describes.
   *
   * A selection is the thinnest possible recipe — three answers and no settings — so the wizard and
   * a saved setup take the same path out of this screen. That is what stopped this function
   * hardcoding `['ECL'], 6, 45, false`: the values now come from the recipe, and a wizard-made draft
   * lobby opens on no sets rather than on one nobody picked.
   */
  const launch = (selection: Selection) => applyRecipe(recipeFromSelection(selection))

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

  return (
    <div className={styles.connectionOverlay} style={{ backgroundImage: `url(${randomBackground})` }}>
      {/* One flow row across the top: viewport controls left, account right. See `.landingTopBar`. */}
      <div className={styles.landingTopBar}>
        <div className={styles.landingTopBarControls}>
          <FullscreenButton />
          <button
            type="button"
            onClick={() => navigate('/help')}
            className={styles.fullscreenButton}
            title="How Argentum works — modes, priority, shortcuts"
          >
            ? Help
          </button>
        </div>
        <AuthWidget />
      </div>
      <div className={styles.landingLayout}>
        {/* Mirrors the side rail's width so the glass card stays viewport-centred rather than
            centred-minus-the-rail. Collapsed below 1480px, where that symmetry costs more width
            than the card can spare — see `.landingGutter`. */}
        {showSideRail && <div className={styles.landingGutter} aria-hidden="true" />}
        <div className={styles.contentBackdrop}>
          <h1 className={styles.title}>Argentum Engine</h1>
          <span className={styles.commitHash}>{__COMMIT_HASH__}</span>

          {error && (
            <p className={styles.errorMessage}>Error: {error}</p>
          )}

          {/* Only a visitor with no name at all is asked for one. A signed-in account already has a
              display name, and the server would overwrite anything typed here with it. Held back
              while the account check is in flight so the prompt can't flash and vanish. */}
          {!nameConfirmed && !connectName && !nameResolving && (
            <div className={styles.inputGroup}>
              <label className={styles.inputLabel}>{joinSessionId ? 'Enter your name to join' : 'Enter your name'}</label>
              <input
                type="text"
                value={playerName}
                onChange={(e) => setPlayerName(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') confirmName() }}
                placeholder="Your name"
                autoFocus
                maxLength={20}
                className={styles.textInput}
              />
              <button
                onClick={confirmName}
                disabled={!playerName.trim()}
                className={styles.primaryButton}
              >
                Continue
              </button>
              {accountsEnabled && authStatus !== 'authenticated' && (
                <p className={styles.accountNudge}>
                  Playing as a guest.{' '}
                  <button
                    type="button"
                    onClick={() => setLoginOpen(true)}
                    className={styles.accountNudgeButton}
                  >
                    Create a free account
                  </button>{' '}
                  — one magic link, no password — to save decks across devices, add friends, play
                  ranked, track your stats, and rewatch your games.
                </p>
              )}
            </div>
          )}

          {status === 'connected' && !sessionId && (
            <div className={styles.homeTiers}>
              {/* ── PLAY ─────────────────────────────────────────────── */}
              <section className={styles.homeTier}>
                <SectionHeading label="Play" />
                {/* Above the wizard, and absent until you have played something: a returning player
                    gets one click, a first-time player gets the three questions unchanged. */}
                <SetupRail onLaunch={applyRecipe} />
                <PlayWizard aiEnabled={aiEnabled} onLaunch={launch} />

                {/* Not a step. Someone who has a code has had the three questions answered for them,
                    so the join row stays visible throughout rather than hiding behind step 1. */}
                <div className={styles.joinRow}>
                  <input
                    type="text"
                    value={joinSessionId}
                    onChange={(e) => setJoinSessionId(e.target.value)}
                    onKeyDown={(e) => e.key === 'Enter' && handleJoin()}
                    placeholder="Been invited? Paste the code here"
                    className={styles.sessionInput}
                  />
                  <button
                    onClick={handleJoin}
                    disabled={!joinSessionId.trim()}
                    className={styles.joinButton}
                  >
                    Join
                  </button>
                </div>

                {resumableLobbyId && (
                  <div className={styles.continueChip}>
                    <button
                      type="button"
                      className={styles.continueChipButton}
                      onClick={() => joinQuickGameLobby(resumableLobbyId)}
                    >
                      Continue → lobby <span className={styles.continueChipCode}>{resumableLobbyId}</span>
                    </button>
                    <button
                      type="button"
                      className={styles.continueChipDismiss}
                      aria-label="Dismiss"
                      title="I'm done with that lobby"
                      onClick={() => { clearLobbyId(); setResumableLobbyId(null) }}
                    >
                      ×
                    </button>
                  </div>
                )}

                <AccountBenefitsCallout onCreateAccount={() => setLoginOpen(true)} />
                <DeckMigrationPrompt />
              </section>

              {/* ── BUILD & BROWSE ───────────────────────────────────────
                  Only what every visitor can use. Stats, Friends and Profile were here too, and
                  they are all account-scoped and all already reachable from the AuthWidget in the
                  side rail — two routes to the same three pages, one of which is right above. */}
              <section className={styles.homeTier}>
                <SectionHeading label="Build & Browse" />
                <div className={styles.secondaryButtonRow}>
                  <button onClick={() => navigate('/deckbuilder')} className={styles.secondaryButton}>
                    Deckbuilder
                  </button>
                  <button onClick={() => setShowReplays(true)} className={styles.secondaryButton}>
                    Replays
                  </button>
                  {/* "Which cards of a set can I actually play with?" is a deckbuilding question, not
                      a debugging one — it sat under LAB, behind an "advanced" caption that told
                      players it wasn't for them. */}
                  <button onClick={() => navigate('/set-completion')} className={styles.secondaryButton}>
                    Set Completion
                  </button>
                </div>
              </section>

              {/* ── LAB ──────────────────────────────────────────────────
                  Dev builds only, and the whole tier goes with it. Every entry point drives
                  `/api/dev/*`, which exists only when the server runs with GAME_DEV_ENDPOINTS_ENABLED
                  — in a production build they lead somewhere that cannot work, and with Set
                  Completion moved out there is nothing left in the tier to justify rendering it.
                  The *routes* stay open either way: a replay's "share as scenario" link is a real
                  `/scenario?s=` deep link, and gating the route would break it. */}
              {import.meta.env.DEV && (
                <section className={styles.homeTier}>
                  <SectionHeading label="Lab" hint="dev builds only" />
                  <div className={styles.secondaryButtonRow}>
                    <button onClick={() => navigate('/scenario')} className={styles.secondaryButton}>
                      Scenario Builder
                    </button>
                    <button onClick={() => navigate('/llm-tournament')} className={styles.secondaryButton}>
                      LLM Tournament
                    </button>
                    {/* Bot-vs-bot with nobody in a seat — the way to watch the engine AI play and
                        see where it goes wrong. */}
                    <button onClick={() => navigate('/ai-sandbox')} className={styles.secondaryButton}>
                      AI Sandbox
                    </button>
                  </div>
                  <p className={styles.tierCaption}>
                    Debugging and content tools, not part of normal play.
                  </p>
                </section>
              )}
            </div>
          )}

          {sessionId && (
            <WaitingForOpponent sessionId={sessionId} />
          )}
        </div>

        {showSideRail && (
          <div className={styles.sidePanelStack}>
            {showPublicLobbies && (
              <PublicLobbyList
                lobbies={publicLobbies}
                error={publicLobbiesError}
                onlinePlayers={onlinePlayers}
                onJoin={(entry) => {
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
                }}
              />
            )}
            {showLiveGames && (
              <LiveGameList
                games={liveGames}
                onSpectate={handleSpectate}
                disabled={!connectName && !playerName.trim() && status !== 'connected'}
              />
            )}
          </div>
        )}
      </div>
      <div className={styles.attribution}>
        <span className={styles.communityLinks}>
          <a href={DISCORD_INVITE_URL} target="_blank" rel="noopener noreferrer" className={styles.communityLink}>
            <DiscordIcon />
            Discord
          </a>
          <a href={GITHUB_REPOSITORY_URL} target="_blank" rel="noopener noreferrer" className={styles.communityLink}>
            <GitHubIcon />
            GitHub
          </a>
          <a href={CONTRIBUTING_GUIDE_URL} target="_blank" rel="noopener noreferrer" className={styles.communityLink}>
            <GuideIcon />
            Help build it
          </a>
        </span>
        <span>
          Card images via <a href="https://scryfall.com" target="_blank" rel="noopener noreferrer" className={styles.attributionLink}>Scryfall</a>
          {' · '}
          Mana symbols by <a href="https://mana.andrewgioia.com" target="_blank" rel="noopener noreferrer" className={styles.attributionLink}>Mana Font</a> (SIL OFL 1.1 / MIT)
        </span>
        <span className={styles.attributionDisclaimer}>
          Fan-made project. Not affiliated with, endorsed, or sponsored by Wizards of the Coast. Magic: The Gathering is © Wizards of the Coast LLC.
        </span>
      </div>
      <LoginModal open={loginOpen} onClose={() => setLoginOpen(false)} />
    </div>
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

/** Rule-and-label heading separating the landing screen's tiers. */
function SectionHeading({ label, hint }: { label: string; hint?: string }) {
  return (
    <div className={styles.tierHeading}>
      <span className={styles.tierHeadingLabel}>
        {label}
        {hint && <span className={styles.tierHeadingHint}>{hint}</span>}
      </span>
      <span className={styles.tierHeadingRule} />
    </div>
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
    <div className={styles.waitingSection}>
      <p className={styles.waitingTitle}>Game Created!</p>
      <div
        onClick={copySessionId}
        className={`${styles.inviteBox} ${copied ? styles.inviteBoxCopied : ''}`}
      >
        <div className={styles.inviteCode}>
          {sessionId}
        </div>
        <span className={`${styles.inviteCopyLabel} ${copied ? styles.inviteCopyLabelCopied : ''}`}>
          {copied ? 'Copied!' : 'Copy'}
        </span>
      </div>
      <p className={styles.waitingSubtitle}>
        Waiting for opponent to join...
      </p>
      <button onClick={cancelGame} className={styles.cancelButton}>
        Cancel Game
      </button>
    </div>
  )
}
