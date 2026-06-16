import { useState, useEffect, useCallback, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useGameStore, type LobbyState, type TournamentState, type FfaState } from '@/store/gameStore.ts'
import type { SealedCardInfo, TournamentFormat } from '@/types'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { teamColor } from '@/styles/seatColors'
import { ManaCost } from './ManaSymbols'
import { SetIcon } from './SetIcon'
import { randomBackground } from '@/utils/background.ts'
import { ReplayViewer, type GameSummary } from '../admin/ReplayViewer'
import type { ReplayData } from '@/replay/reconstructSnapshots.ts'
import { QuickGameLobbyOverlay } from './QuickGameLobbyOverlay'
import { useDeckLibrary, buildDraftedDeckSave, type SavedDeckEntry } from '@/store/deckLibrary'
import { DeckPicker } from './DeckPicker'
import { BanListEditor } from './BanListEditor'
import { SetPickerModal } from './SetPickerModal'
import { JoinQrModal } from './JoinQrModal'
import { buildJoinUrl } from '@/utils/joinLink'
import { labelForFormat } from '@/utils/deckLegality'
import styles from './GameUI.module.css'

type GameMode = 'normal' | 'tournament'

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
 * Connection/lobby UI - shown when not in a game.
 * Combat mode and game UI are handled in App.tsx and GameBoard.tsx.
 */
export function GameUI() {
  const connectionStatus = useGameStore((state) => state.connectionStatus)
  const sessionId = useGameStore((state) => state.sessionId)
  const lastError = useGameStore((state) => state.lastError)
  const deckBuildingState = useGameStore((state) => state.deckBuildingState)
  const tournamentState = useGameStore((state) => state.tournamentState)
  const ffaState = useGameStore((state) => state.ffaState)
  const quickGameLobbyState = useGameStore((state) => state.quickGameLobbyState)

  // Don't show connection overlay if actively building deck (but show during 'waiting' phase)
  // Exception: always show if tournamentState/ffaState exists (for the standings overlays)
  if (deckBuildingState && deckBuildingState.phase !== 'waiting' && !tournamentState && !ffaState) return null

  // Quick-game lobby is its own dedicated overlay (deck picker lives inside it).
  if (quickGameLobbyState && !sessionId) return <QuickGameLobbyOverlay />

  return (
    <ConnectionOverlay
      status={connectionStatus}
      sessionId={sessionId}
      error={lastError?.message}
    />
  )
}

/**
 * Connection overlay shown before game starts.
 */
function ConnectionOverlay({
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
  const createTournamentLobby = useGameStore((state) => state.createTournamentLobby)
  const createQuickGameLobby = useGameStore((state) => state.createQuickGameLobby)
  const joinQuickGameLobby = useGameStore((state) => state.joinQuickGameLobby)
  const setPendingTournamentId = useGameStore((state) => state.setPendingTournamentId)
  const lobbyState = useGameStore((state) => state.lobbyState)
  const [joinSessionId, setJoinSessionId] = useState('')
  const [gameMode, setGameMode] = useState<GameMode>('normal')
  const [playerName, setPlayerName] = useState(() => localStorage.getItem('argentum-player-name') || '')

  const [nameConfirmed, setNameConfirmed] = useState(() => !!localStorage.getItem('argentum-player-name'))
  const [showReplays, setShowReplays] = useState(false)
  const [publicLobbies, setPublicLobbies] = useState<PublicLobbyEntry[]>([])
  const [publicLobbiesError, setPublicLobbiesError] = useState<string | null>(null)
  const [liveGames, setLiveGames] = useState<LiveGameEntry[]>([])
  const onlinePlayers = useGameStore((state) => state.onlinePlayers)
  const spectateGame = useGameStore((state) => state.spectateGame)
  const setPendingSpectateGameId = useGameStore((state) => state.setPendingSpectateGameId)

  const confirmName = () => {
    if (playerName.trim()) {
      localStorage.setItem('argentum-player-name', playerName.trim())
      setNameConfirmed(true)
      if (gameMode === 'tournament' && joinSessionId.trim()) {
        setPendingTournamentId(joinSessionId.trim())
      }
      connect(playerName.trim())
    }
  }

  const handleCreate = () => {
    if (gameMode === 'tournament') {
      // Create lobby with default settings - host can change in lobby
      createTournamentLobby(['ECL'], 'SEALED')
    } else {
      // Quick games go through a real lobby; deck/format/set selection (including the Momir Basic
      // custom format) all live inside it.
      createQuickGameLobby(false)
    }
  }

  const handlePlayVsAi = () => {
    createQuickGameLobby(true)
  }

  const handleJoin = () => {
    if (joinSessionId.trim()) {
      // Unified join: send to QuickGameLobbyHandler, which delegates to the tournament
      // handler if the code happens to be a tournament lobby. The home-screen Join field
      // doesn't care which kind of lobby is behind a code.
      joinQuickGameLobby(joinSessionId.trim())
    }
  }

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

  // Show tournament UI if we're in a tournament (even without lobbyState)
  const tournamentState = useGameStore((state) => state.tournamentState)
  const ffaState = useGameStore((state) => state.ffaState)
  if (tournamentState) {
    return <TournamentOverlay tournamentState={tournamentState} />
  }

  // Show Free-for-All standings UI if the pod has started a game
  if (ffaState) {
    return <FreeForAllOverlay ffaState={ffaState} />
  }

  // Show lobby UI if we're in a lobby
  if (lobbyState) {
    return <LobbyOverlay lobbyState={lobbyState} />
  }

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

  const handleSpectate = (gameSessionId: string) => {
    if (status === 'connected') {
      spectateGame(gameSessionId)
      return
    }
    if (!playerName.trim()) return
    localStorage.setItem('argentum-player-name', playerName.trim())
    setPendingSpectateGameId(gameSessionId)
    setNameConfirmed(true)
    connect(playerName.trim())
  }

  return (
    <div className={styles.connectionOverlay} style={{ backgroundImage: `url(${randomBackground})` }}>
      <FullscreenButton />
      <div className={styles.landingLayout}>
        <div className={styles.contentBackdrop}>
          <h1 className={styles.title}>Argentum Engine</h1>
          <span className={styles.commitHash}>{__COMMIT_HASH__}</span>

          {error && (
            <p className={styles.errorMessage}>Error: {error}</p>
          )}

          {!nameConfirmed && (
            <div className={styles.inputGroup}>
              <label className={styles.inputLabel}>{gameMode === 'tournament' && joinSessionId ? 'Enter your name to join' : 'Enter your name'}</label>
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
            </div>
          )}

          {status === 'connected' && !sessionId && (
            <div className={styles.inputGroup}>
              {/* Game Mode Toggle */}
              <div className={styles.modeToggle}>
                <ModeButton
                  label="Quick Game"
                  active={gameMode === 'normal'}
                  onClick={() => setGameMode('normal')}
                  title="Play with a random deck"
                />
                <ModeButton
                  label="Tournament"
                  active={gameMode === 'tournament'}
                  onClick={() => setGameMode('tournament')}
                  title="Sealed or Draft with up to 8 players"
                />
              </div>

              {/* Game mode description */}
              {gameMode === 'normal' && (
                <p className={styles.modeDescription}>
                  Pick a deck inside the lobby, then play 1v1 with a friend or against the AI.
                </p>
              )}
              {gameMode === 'tournament' && (
                <p className={styles.modeDescription}>
                  Create a lobby for Sealed or Draft. Configure format and set after creating.
                </p>
              )}

              {gameMode !== 'tournament' ? (
                <div className={styles.createButtonRow}>
                  <button
                    onClick={handleCreate}
                    className={styles.primaryButton}
                  >
                    Create Quick Game
                  </button>
                  {aiEnabled && (
                    <button
                      onClick={handlePlayVsAi}
                      className={styles.aiButton}
                    >
                      Play vs AI
                    </button>
                  )}
                </div>
              ) : (
                <button
                  onClick={handleCreate}
                  className={styles.tournamentButton}
                >
                  Create Lobby
                </button>
              )}

              <div className={styles.divider}>
                <div className={styles.dividerLine} />
                <span className={styles.dividerText}>or join existing</span>
                <div className={styles.dividerLine} />
              </div>

              <div className={styles.joinRow}>
                <input
                  type="text"
                  value={joinSessionId}
                  onChange={(e) => setJoinSessionId(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && handleJoin()}
                  placeholder="Enter Lobby Code"
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

              <div className={styles.secondaryButtonRow}>
                <button
                  onClick={() => navigate('/deckbuilder')}
                  className={styles.secondaryButton}
                >
                  Deckbuilder
                </button>
                <button
                  onClick={() => navigate('/scenario')}
                  className={styles.secondaryButton}
                >
                  Scenario Builder
                </button>
                <button
                  onClick={() => navigate('/set-completion')}
                  className={styles.secondaryButton}
                >
                  Set Completion
                </button>
                {import.meta.env.DEV && (
                  <button
                    onClick={() => navigate('/llm-tournament')}
                    className={styles.secondaryButton}
                  >
                    LLM Tournament
                  </button>
                )}
                <button
                  onClick={() => setShowReplays(true)}
                  className={styles.secondaryButton}
                >
                  Game Replays
                </button>
              </div>
            </div>
          )}

          {sessionId && (
            <WaitingForOpponent sessionId={sessionId} />
          )}
        </div>

        {(showPublicLobbies || showLiveGames) && (
          <div className={styles.sidePanelStack}>
            {showPublicLobbies && (
              <PublicLobbyList
                lobbies={publicLobbies}
                error={publicLobbiesError}
                onlinePlayers={onlinePlayers}
                onJoin={(entry) => {
                  setJoinSessionId(entry.lobbyId)
                  if (entry.kind === 'tournament') setGameMode('tournament')
                  if (status === 'connected') {
                    // QuickGameLobbyHandler routes by lobby kind — works for both.
                    joinQuickGameLobby(entry.lobbyId)
                  } else if (playerName.trim()) {
                    localStorage.setItem('argentum-player-name', playerName.trim())
                    // pendingTournamentId triggers a JoinLobby on connect — works only for tournaments.
                    if (entry.kind === 'tournament') setPendingTournamentId(entry.lobbyId)
                    setNameConfirmed(true)
                    connect(playerName.trim())
                  }
                }}
              />
            )}
            {showLiveGames && (
              <LiveGameList
                games={liveGames}
                onSpectate={handleSpectate}
                disabled={!playerName.trim() && status !== 'connected'}
              />
            )}
          </div>
        )}
      </div>
      <div className={styles.attribution}>
        <span>
          Card images via <a href="https://scryfall.com" target="_blank" rel="noopener noreferrer" className={styles.attributionLink}>Scryfall</a>
          {' · '}
          Mana symbols by <a href="https://mana.andrewgioia.com" target="_blank" rel="noopener noreferrer" className={styles.attributionLink}>Mana Font</a> (SIL OFL 1.1 / MIT)
        </span>
        <span className={styles.attributionDisclaimer}>
          Fan-made project. Not affiliated with, endorsed, or sponsored by Wizards of the Coast. Magic: The Gathering is © Wizards of the Coast LLC.
        </span>
      </div>
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
  const lifeSummary = `${game.player1Life} / ${game.player2Life} life`
  if (game.kind === 'tournament') {
    return `Tournament · Round ${game.round} · ${lifeSummary}`
  }
  return `Quick Game · ${lifeSummary}`
}

function publicLobbyName(entry: PublicLobbyEntry): string {
  if (entry.kind === 'tournament') {
    if (entry.format === 'PREMADE_DECKS') return 'Premade Decks Tournament'
    return entry.setNames.join(' + ') || 'Tournament'
  }
  return entry.hostName ? `${entry.hostName}'s Quick Game` : 'Quick Game'
}

function publicLobbyMeta(entry: PublicLobbyEntry): string {
  if (entry.kind === 'tournament') {
    if (entry.format === 'PREMADE_DECKS') {
      const parts = ['Premade Decks']
      if (entry.deckFormat) parts.push(labelForFormat(entry.deckFormat))
      parts.push(`${entry.playerCount}/${entry.maxPlayers} players`)
      if (entry.gamesPerMatch > 1) parts.push(`${entry.gamesPerMatch} games per matchup`)
      return parts.join(' · ')
    }
    const base = `${formatTournamentFormat(entry.format)} · ${entry.boosterCount} ${entry.format === 'DRAFT' ? 'packs' : 'boosters'} · ${entry.playerCount}/${entry.maxPlayers} players`
    return entry.gamesPerMatch > 1 ? `${base} · ${entry.gamesPerMatch} games per matchup` : base
  }
  const parts = ['Quick Game']
  if (entry.setCode) parts.push(entry.setCode)
  if (entry.format) parts.push(labelForFormat(entry.format))
  parts.push(`${entry.playerCount}/${entry.maxPlayers} players`)
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
 * Mode toggle button.
 */
function ModeButton({
  label,
  active,
  onClick,
  title,
}: {
  label: string
  active: boolean
  onClick: () => void
  title?: string
}) {
  return (
    <button
      onClick={onClick}
      title={title}
      className={`${styles.modeButton} ${active ? styles.modeButtonActive : ''}`}
    >
      {label}
    </button>
  )
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

/**
 * Get the fixed booster count for grid draft based on player count.
 * Targets 18 grids per draft (the canonical number).
 */
/**
 * Lobby overlay for sealed lobbies.
 */
function LobbyOverlay({
  lobbyState,
}: {
  lobbyState: LobbyState
}) {
  const startLobby = useGameStore((state) => state.startLobby)
  const leaveLobby = useGameStore((state) => state.leaveLobby)
  const addAiToLobby = useGameStore((state) => state.addAiToLobby)
  const removeAiFromLobby = useGameStore((state) => state.removeAiFromLobby)
  const aiEnabled = useGameStore((state) => state.aiEnabled)
  const updateLobbySettings = useGameStore((state) => state.updateLobbySettings)
  const tournamentState = useGameStore((state) => state.tournamentState)
  const ffaState = useGameStore((state) => state.ffaState)
  const [copied, setCopied] = useState(false)
  const [showSetPicker, setShowSetPicker] = useState(false)

  // Show tournament standings when tournament is active
  if (tournamentState) {
    return <TournamentOverlay tournamentState={tournamentState} />
  }

  // Show Free-for-All standings once the pod has started a game
  if (ffaState) {
    return <FreeForAllOverlay ffaState={ffaState} />
  }

  const isWaiting = lobbyState.state === 'WAITING_FOR_PLAYERS'
  const format = lobbyState.settings.format
  const isDraft = format === 'DRAFT'
  const isWinston = format === 'WINSTON_DRAFT'
  const isGridDraft = format === 'GRID_DRAFT'
  const isSealed = format === 'SEALED'
  const isCommanderDraft = format === 'COMMANDER_DRAFT'
  const isCommanderSealed = format === 'COMMANDER_SEALED'
  const isAnyCommander = isCommanderDraft || isCommanderSealed
  const isPremade = format === 'PREMADE_DECKS'
  const isFfa = lobbyState.settings.gameMode === 'FREE_FOR_ALL'
  // Two-Headed Giant (CR 810): the pod mode that runs two teams of two off the same draft/sealed
  // build. Exactly four players; combat/attack rules are fixed (no per-creature attack picker).
  const is2hg = lobbyState.settings.gameMode === 'TWO_HEADED_GIANT'
  // Team vs. Team (CR 808): two even teams (2v2 / 3v3 / 4v4). Like 2HG but nothing is shared —
  // each player keeps their own life and turn, and is eliminated individually.
  const isTeamVsTeam = lobbyState.settings.gameMode === 'TEAM_VS_TEAM'
  // Any team mode shares the random/manual team-assignment controls below.
  const isTeamGame = is2hg || isTeamVsTeam
  // Both team modes split the pod into exactly two even teams; team size follows the player count.
  const teamSize = Math.max(1, Math.floor(lobbyState.players.length / 2))
  // Team setup: random by default, or host-assigned (playerId -> team, defaulting to join order).
  const randomTeams = lobbyState.settings.randomTeams ?? true
  const teamAssignments = lobbyState.settings.teamAssignments ?? {}
  const playerTeam = (playerId: string, index: number): number =>
    teamAssignments[playerId] ?? Math.floor(index / teamSize)
  // Manual teams must be an even split into two equal teams (the server otherwise re-balances).
  const manualTeamsBalanced = lobbyState.players.length >= 4 && lobbyState.players.length % 2 === 0 &&
    [0, 1].every(t => lobbyState.players.filter((p, i) => playerTeam(p.playerId, i) === t).length === teamSize)
  // Move one player to the other team, sending the full explicit assignment for every seat.
  const togglePlayerTeam = (playerId: string, index: number) => {
    const flipped = playerTeam(playerId, index) === 0 ? 1 : 0
    const next: Record<string, number> = {}
    lobbyState.players.forEach((p, i) => {
      next[p.playerId] = p.playerId === playerId ? flipped : playerTeam(p.playerId, i)
    })
    updateLobbySettings({ teamAssignments: next })
  }
  // "Draft-shape" — anything that hands packs around at pick time. Commander Draft fits the
  // shape (same per-pick UI / timer / pack-passing) so it inherits Draft-only settings.
  const isAnyDraft = isDraft || isWinston || isGridDraft || isCommanderDraft
  const isAnySealed = isSealed || isCommanderSealed
  const hasSelectedSets = lobbyState.settings.setCodes.length > 0
  const playerCount = lobbyState.players.length
  const canSwitchToNormalDraft = playerCount <= 8
  const canSwitchToWinston = playerCount <= 2
  const canSwitchToGrid = playerCount <= 4
  // Commander Draft/Sealed are 1v1 for the foreseeable future (multiplayer commander is a
  // separate project — see backlog/commander-format.md Phase 3).
  const canSwitchToCommander = playerCount <= 2
  const playerCheck = isWinston ? playerCount === 2
    : isGridDraft ? playerCount >= 2 && playerCount <= 4
    : isAnyCommander ? playerCount === 2
    : is2hg ? playerCount === 4
    : isTeamVsTeam ? playerCount >= 4 && playerCount % 2 === 0
    : playerCount >= 2
  // Premade format: no boosters generated, so set selection is optional. We do require every
  // connected player to have submitted a deck before the host can start.
  const allConnectedDecksSubmitted = lobbyState.players
    .filter((p) => p.isConnected)
    .every((p) => p.deckSubmitted)
  const canStart = isPremade
    ? playerCheck && allConnectedDecksSubmitted
    : playerCheck && hasSelectedSets

  const copyLobbyId = () => {
    navigator.clipboard.writeText(lobbyState.lobbyId)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  // Unified set picker: every set (complete + incomplete) lives behind one searchable modal
  // (`SetPickerModal`, shared with the Quick Game lobby). The lobby itself only shows the
  // *selected* sets as compact chips, so its footprint stays small and stable no matter how many
  // sets exist in total or how many a host picks.
  type AvailableSet = typeof lobbyState.settings.availableSets[number]
  const allSets = lobbyState.settings.availableSets
  const selectedSets = lobbyState.settings.setCodes
    .map((code) => allSets.find((s) => s.code === code))
    .filter((s): s is AvailableSet => s != null)

  const toggleSet = (code: string) => {
    const isSelected = lobbyState.settings.setCodes.includes(code)
    const newCodes = isSelected
      ? lobbyState.settings.setCodes.filter((c) => c !== code)
      : [...lobbyState.settings.setCodes, code]
    updateLobbySettings({ setCodes: newCodes })
  }

  return (
    <div className={styles.lobbyOverlay} style={{ backgroundImage: `url(${randomBackground})` }}>
      <FullscreenButton />
      <div className={styles.lobbyContent}>
        {/* Header */}
        <div className={styles.lobbyHeader}>
          <div className={`${styles.lobbyFormat} ${isAnyDraft ? styles.lobbyFormatDraft : styles.lobbyFormatSealed}`}>
            {isGridDraft ? 'Grid'
              : isWinston ? 'Winston'
              : isCommanderDraft ? 'Commander Draft'
              : isCommanderSealed ? 'Commander Sealed'
              : isDraft ? 'Draft'
              : isPremade ? 'Premade'
              : 'Sealed'}
          </div>
          <h1 className={styles.lobbyTitle}>
            {isPremade
              ? (is2hg ? 'Premade Decks Two-Headed Giant' : isTeamVsTeam ? 'Premade Decks Team vs. Team' : isFfa ? 'Premade Decks Free-for-All' : 'Premade Decks Tournament')
              : (lobbyState.settings.setNames.join(' + ') || 'Lobby')}
          </h1>
          <p className={styles.lobbySubtitle}>
            {(() => {
              const distText = lobbyState.settings.setCodes.length > 1 && Object.keys(lobbyState.settings.boosterDistribution).length > 0
                ? Object.entries(lobbyState.settings.boosterDistribution).map(([code, count]) => {
                  const idx = lobbyState.settings.setCodes.indexOf(code)
                  const name = idx >= 0 ? (lobbyState.settings.setNames[idx] ?? code) : code
                  return `${count} ${name}`
                }).join(' + ')
                : null
              const presetLabel = lobbyState.settings.commanderPreset === 'COMMANDER' ? 'Commander 30 life' : 'Brawl 25 life'
              if (isGridDraft) return `Grid Draft · ${lobbyState.settings.boosterCount} boosters · ${lobbyState.settings.pickTimeSeconds}s per pick`
              if (isWinston) return `Winston Draft · ${distText ?? `${lobbyState.settings.boosterCount} boosters`} · ${lobbyState.settings.pickTimeSeconds}s per turn`
              if (isCommanderDraft) return `${distText ?? `${lobbyState.settings.boosterCount} packs`} · ${lobbyState.settings.pickTimeSeconds}s per pick${lobbyState.settings.picksPerRound === 2 ? ' · Pick 2' : ''} · ${presetLabel}`
              if (isCommanderSealed) return `${distText ?? `${lobbyState.settings.boosterCount} packs`} · ${presetLabel}`
              if (isDraft) return `${distText ?? `${lobbyState.settings.boosterCount} packs`} · ${lobbyState.settings.pickTimeSeconds}s per pick${lobbyState.settings.picksPerRound === 2 ? ' · Pick 2' : ''}`
              if (isPremade) return 'Premade Decks · bring your own ≥40-card deck'
              return distText ?? `${lobbyState.settings.boosterCount} boosters per player`
            })()}
            {!isFfa && !isTeamGame && (lobbyState.settings.gamesPerMatch ?? 1) > 1 && ` · ${lobbyState.settings.gamesPerMatch} games per matchup`}
            {isFfa && ' · Free-for-All'}
            {is2hg && ' · Two-Headed Giant'}
            {isTeamVsTeam && ' · Team vs. Team'}
          </p>
        </div>

        {/* Invite code + scannable QR to pull another device straight into the lobby */}
        <div style={{ alignSelf: 'stretch', display: 'flex', alignItems: 'stretch', gap: 8 }}>
          <div
            onClick={copyLobbyId}
            className={`${styles.inviteBox} ${copied ? styles.inviteBoxCopied : ''}`}
            style={{ flex: 1, marginBottom: 0, justifyContent: 'space-between' }}
          >
            <div>
              <div style={{ color: 'var(--text-disabled)', fontSize: 10, textTransform: 'uppercase', letterSpacing: '0.08em', marginBottom: 3 }}>
                Invite Code
              </div>
              <div className={styles.inviteCode} data-testid="invite-code">
                {lobbyState.lobbyId}
              </div>
            </div>
            <span className={`${styles.inviteCopyLabel} ${copied ? styles.inviteCopyLabelCopied : ''}`} style={{ flexShrink: 0, marginLeft: 12 }}>
              {copied ? 'Copied!' : 'Copy'}
            </span>
          </div>
          <JoinQrModal url={buildJoinUrl(lobbyState.lobbyId)} />
        </div>

        {/* Settings (host only) */}
        {isWaiting && lobbyState.isHost && (
          <div className={styles.settingsPanel}>
            {/* Format selection — top row picks the pool type; variant sub-row picks the shape. */}
            <div className={styles.settingsRow}>
              <span className={styles.settingsLabel}>Format</span>
              <div className={styles.settingsButtons}>
                <button
                  onClick={() => { if (!isAnySealed) updateLobbySettings({ format: 'SEALED' }) }}
                  className={`${styles.settingsButton} ${isAnySealed ? styles.settingsButtonActive : ''}`}
                >
                  Sealed
                </button>
                <button
                  onClick={() => { if (!isAnyDraft) updateLobbySettings({ format: 'DRAFT' }) }}
                  className={`${styles.settingsButton} ${isAnyDraft ? `${styles.settingsButtonActive} ${styles.settingsButtonDraft}` : ''}`}
                >
                  Draft
                </button>
                <button
                  onClick={() => updateLobbySettings({ format: 'PREMADE_DECKS' })}
                  className={`${styles.settingsButton} ${isPremade ? styles.settingsButtonActive : ''}`}
                  title="Players bring their own pre-built decks (saved or pasted)"
                >
                  Premade
                </button>
              </div>
            </div>
            {/* Mode axis (orthogonal to format): bracket of 1v1 matches vs one multiplayer game */}
            <div className={styles.settingsRow}>
              <span className={styles.settingsLabel}>Mode</span>
              <div className={styles.variantGroup}>
                <div className={styles.settingsButtons}>
                  <button
                    onClick={() => updateLobbySettings({ gameMode: 'TOURNAMENT' })}
                    className={`${styles.settingsButton} ${!isFfa && !isTeamGame ? styles.settingsButtonActive : ''}`}
                    title="Round-robin bracket of 1v1 matches"
                  >
                    Tournament
                  </button>
                  <button
                    onClick={() => playerCount <= 6 && updateLobbySettings({ gameMode: 'FREE_FOR_ALL' })}
                    disabled={playerCount > 6}
                    className={`${styles.settingsButton} ${isFfa ? styles.settingsButtonActive : ''}`}
                    title="One multiplayer game — everyone at the same table (2-6 players)"
                  >
                    Free-for-All
                  </button>
                  <button
                    onClick={() => playerCount <= 4 && updateLobbySettings({ gameMode: 'TWO_HEADED_GIANT' })}
                    disabled={playerCount > 4}
                    className={`${styles.settingsButton} ${is2hg ? styles.settingsButtonActive : ''}`}
                    title="2v2 teams — draft or seal, then play one team game (exactly 4 players)"
                  >
                    Two-Headed Giant
                  </button>
                  <button
                    onClick={() => playerCount <= 8 && updateLobbySettings({ gameMode: 'TEAM_VS_TEAM' })}
                    disabled={playerCount > 8}
                    className={`${styles.settingsButton} ${isTeamVsTeam ? styles.settingsButtonActive : ''}`}
                    title="Two even teams — 2v2, 3v3, or 4v4. Own life and own turns; last team standing wins."
                  >
                    Team vs. Team
                  </button>
                </div>
                {isFfa && (
                  <div className={styles.variantCaption}>
                    One game, everyone at the same table (2-6 players). Last player standing wins.
                  </div>
                )}
                {is2hg && (
                  <div className={styles.variantCaption}>
                    Four players in two teams of two. Each team shares one 30-life total, takes turns
                    together, and attacks and blocks as one. Last team standing wins.
                  </div>
                )}
                {isTeamVsTeam && (
                  <div className={styles.variantCaption}>
                    An even pod (4/6/8) split into two teams — 2v2, 3v3, or 4v4. Each player keeps their
                    own 20 life and their own turn; players are knocked out one at a time. The last team
                    with anyone standing wins.
                  </div>
                )}
              </div>
            </div>
            {/* Team setup (2HG — CR 810; Team vs. Team — CR 808): random teams each game, or host-picked teams. */}
            {isTeamGame && (
              <div className={styles.settingsRow}>
                <span className={styles.settingsLabel}>Teams</span>
                <div className={styles.variantGroup}>
                  <div className={styles.settingsButtons}>
                    <button
                      onClick={() => updateLobbySettings({ randomTeams: true })}
                      className={`${styles.settingsButton} ${randomTeams ? styles.settingsButtonActive : ''}`}
                      title="Shuffle the players into two even teams when the game starts (re-rolled each game)"
                    >
                      Random
                    </button>
                    <button
                      onClick={() => updateLobbySettings({ randomTeams: false })}
                      className={`${styles.settingsButton} ${!randomTeams ? styles.settingsButtonActive : ''}`}
                      title="Set the teams by hand — click each player's team chip below"
                    >
                      Choose teams
                    </button>
                  </div>
                  <div className={styles.variantCaption}>
                    {randomTeams
                      ? 'Teams are randomised at game start, fresh every game.'
                      : manualTeamsBalanced
                        ? 'Click a player’s team chip below to move them between teams.'
                        : `Click each player’s team chip below — each team needs exactly ${teamSize} player${teamSize === 1 ? '' : 's'}.`}
                  </div>
                </div>
              </div>
            )}
            {/* Free-for-All attack rule (CR 802/803) — only relevant once 3+ players share one table */}
            {isFfa && (
              <div className={styles.settingsRow}>
                <span className={styles.settingsLabel}>Attack</span>
                <div className={styles.variantGroup}>
                  <div className={styles.settingsButtons}>
                    {([
                      ['MULTIPLE', 'Any opponent', 'Each creature may attack any opponent (CR 802)'],
                      ['LEFT', 'Left only', 'Each creature may attack only the player to your left (CR 803)'],
                      ['RIGHT', 'Right only', 'Each creature may attack only the player to your right (CR 803)'],
                    ] as const).map(([mode, label, title]) => (
                      <button
                        key={mode}
                        onClick={() => updateLobbySettings({ attackMode: mode })}
                        className={`${styles.settingsButton} ${(lobbyState.settings.attackMode ?? 'MULTIPLE') === mode ? styles.settingsButtonActive : ''}`}
                        title={title}
                      >
                        {label}
                      </button>
                    ))}
                  </div>
                  <div className={styles.variantCaption}>
                    Who each creature may attack. "Left"/"right" follow the seating order.
                  </div>
                </div>
              </div>
            )}
            {isAnySealed && (() => {
              const caption = isCommanderSealed
                ? 'Open Commander-shaped packs and build a 60-card deck around a commander from your pool. 1v1.'
                : 'Open 6 boosters and build a 40-card deck.'
              return (
                <div className={styles.settingsRow}>
                  <span className={styles.settingsLabel}>Variant</span>
                  <div className={styles.variantGroup}>
                    <div className={styles.settingsButtons}>
                      <button
                        onClick={() => updateLobbySettings({ format: 'SEALED' })}
                        className={`${styles.settingsButton} ${isSealed ? styles.settingsButtonActive : ''}`}
                      >
                        Standard
                      </button>
                      <button
                        onClick={() => canSwitchToCommander && updateLobbySettings({ format: 'COMMANDER_SEALED' })}
                        disabled={!canSwitchToCommander}
                        className={`${styles.settingsButton} ${isCommanderSealed ? styles.settingsButtonActive : ''}`}
                      >
                        Commander
                      </button>
                    </div>
                    <div className={styles.variantCaption}>{caption}</div>
                  </div>
                </div>
              )
            })()}
            {isAnyDraft && (() => {
              const caption = isCommanderDraft
                ? 'Commander-shaped 20-card packs; pick a commander from your pool. 1v1.'
                : isWinston ? 'Pick from 3 face-down piles. 2 players.'
                : isGridDraft ? 'Pick a row or column from a 3×3 grid. 2-4 players.'
                : 'Pass packs around the table. 3-8 players.'
              return (
                <div className={styles.settingsRow}>
                  <span className={styles.settingsLabel}>Variant</span>
                  <div className={styles.variantGroup}>
                    <div className={styles.settingsButtons}>
                      <button
                        onClick={() => canSwitchToNormalDraft && updateLobbySettings({ format: 'DRAFT' })}
                        disabled={!canSwitchToNormalDraft}
                        className={`${styles.settingsButton} ${isDraft ? `${styles.settingsButtonActive} ${styles.settingsButtonDraft}` : ''}`}
                      >
                        Normal
                      </button>
                      <button
                        onClick={() => canSwitchToWinston && updateLobbySettings({ format: 'WINSTON_DRAFT' })}
                        disabled={!canSwitchToWinston}
                        className={`${styles.settingsButton} ${isWinston ? `${styles.settingsButtonActive} ${styles.settingsButtonDraft}` : ''}`}
                      >
                        Winston
                      </button>
                      <button
                        onClick={() => canSwitchToGrid && updateLobbySettings({ format: 'GRID_DRAFT' })}
                        disabled={!canSwitchToGrid}
                        className={`${styles.settingsButton} ${isGridDraft ? `${styles.settingsButtonActive} ${styles.settingsButtonDraft}` : ''}`}
                      >
                        Grid
                      </button>
                      <button
                        onClick={() => canSwitchToCommander && updateLobbySettings({ format: 'COMMANDER_DRAFT' })}
                        disabled={!canSwitchToCommander}
                        className={`${styles.settingsButton} ${isCommanderDraft ? `${styles.settingsButtonActive} ${styles.settingsButtonDraft}` : ''}`}
                      >
                        Commander
                      </button>
                    </div>
                    <div className={styles.variantCaption}>{caption}</div>
                  </div>
                </div>
              )
            })()}
            {/* Set selection — selected sets shown as chips; the full searchable browser is a modal.
                Skipped for Premade Decks since no boosters are generated. */}
            {!isPremade && (
            <div className={styles.settingsRow} style={{ alignItems: 'flex-start' }}>
              <span className={styles.settingsLabel} style={{ paddingTop: 7 }}>Sets</span>
              <div className={styles.setSelection}>
                {selectedSets.length > 0 ? (
                  <div className={styles.setChips}>
                    {selectedSets.map((set) => (
                      <span
                        key={set.code}
                        className={`${styles.setChip} ${isAnyDraft ? styles.setChipDraft : ''} ${set.partial ? styles.setChipPartial : ''}`}
                        title={set.partial ? `${set.name} — partial (reduced card pool)` : set.name}
                      >
                        <SetIcon code={set.code} className={styles.setChipIcon} />
                        <span className={styles.setChipName}>{set.name}</span>
                        <button
                          type="button"
                          className={styles.setChipRemove}
                          aria-label={`Remove ${set.name}`}
                          onClick={() => toggleSet(set.code)}
                        >×</button>
                      </span>
                    ))}
                  </div>
                ) : (
                  <span className={styles.setSelectionEmpty}>No sets selected yet</span>
                )}
                <button
                  type="button"
                  onClick={() => setShowSetPicker(true)}
                  className={styles.addSetsButton}
                >
                  + Add sets
                </button>
              </div>
            </div>
            )}
            {/* Chaos boosters toggle — only meaningful with >1 set selected and a booster-based format. */}
            {!isPremade && !isGridDraft && lobbyState.settings.setCodes.length > 1 && (
              <div className={styles.settingsRow}>
                <span className={styles.settingsLabel}>Booster mix</span>
                <div className={styles.variantGroup}>
                  <div className={styles.settingsButtons}>
                    <button
                      onClick={() => updateLobbySettings({ chaosBoosters: false })}
                      className={`${styles.settingsButton} ${!lobbyState.settings.chaosBoosters ? styles.settingsButtonActive : ''}`}
                    >
                      Per set
                    </button>
                    <button
                      onClick={() => updateLobbySettings({ chaosBoosters: true })}
                      className={`${styles.settingsButton} ${lobbyState.settings.chaosBoosters ? styles.settingsButtonActive : ''}`}
                    >
                      Chaos
                    </button>
                  </div>
                  <div className={styles.variantCaption}>
                    {lobbyState.settings.chaosBoosters
                      ? 'Each booster mixes cards from all selected sets.'
                      : 'Each booster contains cards from a single set.'}
                  </div>
                </div>
              </div>
            )}
            {/* Booster ban list — host excludes named cards from generated boosters. Not for Premade. */}
            {!isPremade && (
              <BanListEditor
                setCodes={lobbyState.settings.setCodes}
                bannedCardNames={lobbyState.settings.bannedCardNames ?? []}
                onChange={(names) => updateLobbySettings({ bannedCardNames: names })}
              />
            )}
            {/* Boosters setting - for Sealed and Winston (Grid uses fixed counts) */}
            {(isSealed || isWinston || isCommanderSealed) && lobbyState.settings.setCodes.length > 1 && !lobbyState.settings.chaosBoosters && (
              <div className={styles.settingsRow} style={{ flexDirection: 'column', alignItems: 'stretch', gap: 8 }}>
                <span className={styles.settingsLabel}>{isWinston ? 'Boosters (total)' : 'Boosters per player'}</span>
                <div className={styles.boosterDistribution}>
                  {lobbyState.settings.setCodes.map((code) => {
                    const setName = lobbyState.settings.setNames[lobbyState.settings.setCodes.indexOf(code)] ?? code
                    const dist = lobbyState.settings.boosterDistribution
                    const count = dist[code] ?? 0
                    const total = Object.values(dist).reduce((a, b) => a + b, 0)
                    return (
                      <div key={code} className={styles.boosterDistributionRow}>
                        <span className={styles.boosterDistributionSetName}>{setName}</span>
                        <div className={styles.boosterDistributionControls}>
                          <button
                            className={styles.boosterDistributionBtn}
                            disabled={count <= 0}
                            onClick={() => {
                              const newDist = { ...dist, [code]: count - 1 }
                              updateLobbySettings({ boosterDistribution: newDist, boosterCount: total - 1 })
                            }}
                          >-</button>
                          <span className={styles.boosterDistributionCount}>{count}</span>
                          <button
                            className={styles.boosterDistributionBtn}
                            disabled={total >= 16}
                            onClick={() => {
                              const newDist = { ...dist, [code]: count + 1 }
                              updateLobbySettings({ boosterDistribution: newDist, boosterCount: total + 1 })
                            }}
                          >+</button>
                        </div>
                      </div>
                    )
                  })}
                  <div className={styles.boosterDistributionTotal}>
                    <span style={{ flex: 1 }}>Total</span>
                    <span className={styles.boosterDistributionTotalCount}>
                      {Object.values(lobbyState.settings.boosterDistribution).reduce((a, b) => a + b, 0)} boosters
                    </span>
                  </div>
                </div>
              </div>
            )}
            {(isSealed || isWinston || isCommanderSealed) && (lobbyState.settings.setCodes.length <= 1 || lobbyState.settings.chaosBoosters) && (
              <div className={styles.settingsRow}>
                <span className={styles.settingsLabel}>{isWinston ? 'Total boosters' : 'Boosters per player'}</span>
                <select
                  value={lobbyState.settings.boosterCount}
                  onChange={(e) => updateLobbySettings({ boosterCount: Number(e.target.value) })}
                  className={styles.settingsSelect}
                >
                  {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16].map((n) => (
                    <option key={n} value={n}>{n}</option>
                  ))}
                </select>
              </div>
            )}
            {/* Packs per player - for Draft and Commander Draft */}
            {(isDraft || isCommanderDraft) && lobbyState.settings.setCodes.length > 1 && !lobbyState.settings.chaosBoosters && (
              <div className={styles.settingsRow} style={{ flexDirection: 'column', alignItems: 'stretch', gap: 8 }}>
                <span className={styles.settingsLabel}>Packs per player</span>
                <div className={styles.boosterDistribution}>
                  {lobbyState.settings.setCodes.map((code) => {
                    const setName = lobbyState.settings.setNames[lobbyState.settings.setCodes.indexOf(code)] ?? code
                    const dist = lobbyState.settings.boosterDistribution
                    const count = dist[code] ?? 0
                    const total = Object.values(dist).reduce((a, b) => a + b, 0)
                    return (
                      <div key={code} className={styles.boosterDistributionRow}>
                        <span className={styles.boosterDistributionSetName}>{setName}</span>
                        <div className={styles.boosterDistributionControls}>
                          <button
                            className={styles.boosterDistributionBtn}
                            disabled={count <= 0}
                            onClick={() => {
                              const newDist = { ...dist, [code]: count - 1 }
                              updateLobbySettings({ boosterDistribution: newDist, boosterCount: total - 1 })
                            }}
                          >-</button>
                          <span className={styles.boosterDistributionCount}>{count}</span>
                          <button
                            className={styles.boosterDistributionBtn}
                            disabled={total >= 6}
                            onClick={() => {
                              const newDist = { ...dist, [code]: count + 1 }
                              updateLobbySettings({ boosterDistribution: newDist, boosterCount: total + 1 })
                            }}
                          >+</button>
                        </div>
                      </div>
                    )
                  })}
                  <div className={styles.boosterDistributionTotal}>
                    <span style={{ flex: 1 }}>Total</span>
                    <span className={styles.boosterDistributionTotalCount}>
                      {Object.values(lobbyState.settings.boosterDistribution).reduce((a, b) => a + b, 0)} packs
                    </span>
                  </div>
                </div>
              </div>
            )}
            {(isDraft || isCommanderDraft) && (lobbyState.settings.setCodes.length <= 1 || lobbyState.settings.chaosBoosters) && (
              <div className={styles.settingsRow}>
                <span className={styles.settingsLabel}>Packs per player</span>
                <select
                  value={lobbyState.settings.boosterCount}
                  onChange={(e) => updateLobbySettings({ boosterCount: Number(e.target.value) })}
                  className={styles.settingsSelect}
                >
                  {[1, 2, 3, 4, 5, 6].map((n) => (
                    <option key={n} value={n}>{n}</option>
                  ))}
                </select>
              </div>
            )}
            {/* Timer setting - for Draft and Winston */}
            {(isAnyDraft) && (
              <div className={styles.settingsRow}>
                <span className={styles.settingsLabel}>{isWinston ? 'Turn timer (seconds)' : 'Pick timer (seconds)'}</span>
                <select
                  value={lobbyState.settings.pickTimeSeconds}
                  onChange={(e) => updateLobbySettings({ pickTimeSeconds: Number(e.target.value) })}
                  className={styles.settingsSelect}
                >
                  {[30, 45, 60, 90, 120].map((n) => (
                    <option key={n} value={n}>{n}s</option>
                  ))}
                </select>
              </div>
            )}
            {/* Pick 2 mode - for Draft and Commander Draft */}
            {(isDraft || isCommanderDraft) && (
              <div className={styles.settingsRow}>
                <span className={styles.settingsLabel}>Cards per pick</span>
                <div className={styles.settingsButtons}>
                  <button
                    onClick={() => updateLobbySettings({ picksPerRound: 1 })}
                    className={`${styles.settingsButton} ${lobbyState.settings.picksPerRound === 1 ? `${styles.settingsButtonActive} ${styles.settingsButtonDraft}` : ''}`}
                  >
                    1
                  </button>
                  <button
                    onClick={() => updateLobbySettings({ picksPerRound: 2 })}
                    className={`${styles.settingsButton} ${lobbyState.settings.picksPerRound === 2 ? `${styles.settingsButtonActive} ${styles.settingsButtonDraft}` : ''}`}
                  >
                    2
                  </button>
                </div>
              </div>
            )}
            {/* Commander preset + Brawl knobs — only for Commander Draft / Sealed */}
            {isAnyCommander && (
              <>
                <div className={styles.settingsRow}>
                  <span className={styles.settingsLabel}>Preset</span>
                  <div className={styles.settingsButtons}>
                    <button
                      onClick={() => updateLobbySettings({ commanderPreset: 'BRAWL' })}
                      className={`${styles.settingsButton} ${lobbyState.settings.commanderPreset === 'BRAWL' ? styles.settingsButtonActive : ''}`}
                      title="Paper Brawl shape — 25 starting life, 16 commander damage"
                    >
                      Brawl (25/16)
                    </button>
                    <button
                      onClick={() => updateLobbySettings({ commanderPreset: 'COMMANDER' })}
                      className={`${styles.settingsButton} ${lobbyState.settings.commanderPreset === 'COMMANDER' ? styles.settingsButtonActive : ''}`}
                      title="Closer to Commander Legends — 30 life, 21 commander damage"
                    >
                      Commander (30/21)
                    </button>
                  </div>
                </div>
                <div className={styles.settingsRow}>
                  <span className={styles.settingsLabel}>Min deck size</span>
                  <select
                    value={lobbyState.settings.deckSizeMin}
                    onChange={(e) => updateLobbySettings({ deckSizeMin: Number(e.target.value) })}
                    className={styles.settingsSelect}
                  >
                    {[40, 50, 60, 75, 100].map((n) => (
                      <option key={n} value={n}>{n}</option>
                    ))}
                  </select>
                </div>
                <div className={styles.settingsRow}>
                  <span className={styles.settingsLabel}>Singleton</span>
                  <div className={styles.settingsButtons}>
                    <button
                      onClick={() => updateLobbySettings({ allowDuplicates: true })}
                      className={`${styles.settingsButton} ${lobbyState.settings.allowDuplicates ? styles.settingsButtonActive : ''}`}
                      title="Allow multiple copies of the same card (drafted Commander default)"
                    >
                      Duplicates OK
                    </button>
                    <button
                      onClick={() => updateLobbySettings({ allowDuplicates: false })}
                      className={`${styles.settingsButton} ${!lobbyState.settings.allowDuplicates ? styles.settingsButtonActive : ''}`}
                      title="Paper-Commander singleton — max 1 of any non-basic card"
                    >
                      Singleton
                    </button>
                  </div>
                </div>
              </>
            )}
            {!isFfa && (
              <div className={styles.settingsRow}>
                <span className={styles.settingsLabel}>Games per matchup</span>
                <select
                  value={lobbyState.settings.gamesPerMatch ?? 1}
                  onChange={(e) => updateLobbySettings({ gamesPerMatch: Number(e.target.value) })}
                  className={styles.settingsSelect}
                >
                  {[1, 2, 3, 4, 5].map((n) => (
                    <option key={n} value={n}>{n}</option>
                  ))}
                </select>
              </div>
            )}
            <div className={styles.settingsRow}>
              <span className={styles.settingsLabel}>Visibility</span>
              <div className={styles.settingsButtons}>
                <button
                  onClick={() => updateLobbySettings({ isPublic: false })}
                  className={`${styles.settingsButton} ${!lobbyState.settings.isPublic ? styles.settingsButtonActive : ''}`}
                >
                  Private
                </button>
                <button
                  onClick={() => updateLobbySettings({ isPublic: true })}
                  className={`${styles.settingsButton} ${lobbyState.settings.isPublic ? styles.settingsButtonActive : ''}`}
                >
                  Public
                </button>
              </div>
            </div>
            <div className={styles.settingsRow}>
              <span className={styles.settingsLabel} title="Lets players use Suggest Pick and Auto-build during this event">
                AI assistance
              </span>
              <div className={styles.settingsButtons}>
                <button
                  onClick={() => updateLobbySettings({ aiAssistEnabled: false })}
                  className={`${styles.settingsButton} ${!lobbyState.settings.aiAssistEnabled ? styles.settingsButtonActive : ''}`}
                >
                  Off
                </button>
                <button
                  onClick={() => updateLobbySettings({ aiAssistEnabled: true })}
                  className={`${styles.settingsButton} ${lobbyState.settings.aiAssistEnabled ? styles.settingsButtonActive : ''}`}
                >
                  On
                </button>
              </div>
            </div>
            {isPremade && (
              <div className={styles.settingsRow}>
                <span className={styles.settingsLabel}>Deck format</span>
                <select
                  value={lobbyState.settings.deckFormat ?? ''}
                  onChange={(e) =>
                    updateLobbySettings({ deckFormat: (e.target.value || '') as never })
                  }
                  className={styles.settingsSelect}
                  title="Restrict submitted decks to a constructed format. None = no restriction."
                >
                  <option value="">No restriction</option>
                  <option value="STANDARD">Standard</option>
                  <option value="PIONEER">Pioneer</option>
                  <option value="MODERN">Modern</option>
                  <option value="PAUPER">Pauper</option>
                  <option value="LEGACY">Legacy</option>
                  <option value="VINTAGE">Vintage</option>
                  <option value="COMMANDER">Commander</option>
                  <option value="BRAWL">Brawl</option>
                  <option value="STANDARD_BRAWL">Standard Brawl</option>
                  <option value="PREMODERN">Premodern</option>
                </select>
              </div>
            )}
          </div>
        )}

        {/* Premade Decks: every player picks their own deck right here in the lobby. */}
        {isWaiting && isPremade && (
          <PremadeDeckPickerPanel lobbyState={lobbyState} />
        )}

        {/* Player list */}
        <div className={styles.playerListPanel}>
          <div className={styles.playerListHeader}>
            <span className={styles.playerListTitle}>Players</span>
            <span className={styles.playerCount}>
              {lobbyState.players.length} / {isWinston ? 2 : isGridDraft ? 4 : (lobbyState.settings.maxPlayers || 8)}
            </span>
          </div>
          {lobbyState.players.map((player, i) => (
            <div
              key={player.playerId}
              className={styles.playerRow}
              style={{ borderBottom: i < lobbyState.players.length - 1 ? undefined : 'none' }}
            >
              <div className={styles.playerInfo}>
                <div className={`${styles.statusDot} ${!player.isConnected ? styles.statusDotOffline : styles.statusDotOnline}`} />
                <span className={styles.playerName}>
                  {player.playerName}
                </span>
                {/* Team-game (2HG / Team vs. Team) team chip. Random mode: a neutral chip (teams
                    decided at game start). Manual mode: the assigned team, clickable for the host to
                    reassign. */}
                {isTeamGame && randomTeams && (
                  <span
                    style={{
                      fontSize: 10,
                      fontWeight: 800,
                      letterSpacing: '0.05em',
                      textTransform: 'uppercase',
                      color: 'rgba(226, 232, 240, 0.7)',
                      border: '1px solid rgba(148, 163, 184, 0.45)',
                      background: 'rgba(148, 163, 184, 0.12)',
                      borderRadius: 4,
                      padding: '1px 6px',
                    }}
                  >
                    Random
                  </span>
                )}
                {isTeamGame && !randomTeams && (() => {
                  const team = playerTeam(player.playerId, i)
                  const c = teamColor(team)
                  const chipStyle = {
                    fontSize: 10,
                    fontWeight: 800,
                    letterSpacing: '0.05em',
                    textTransform: 'uppercase' as const,
                    color: c.bright,
                    border: `1px solid ${c.base}`,
                    background: c.soft,
                    borderRadius: 4,
                    padding: '1px 6px',
                  }
                  const hostCanEdit = isWaiting && lobbyState.isHost
                  return hostCanEdit ? (
                    <button
                      onClick={() => togglePlayerTeam(player.playerId, i)}
                      style={{ ...chipStyle, cursor: 'pointer' }}
                      title="Click to move this player to the other team"
                    >
                      Team {team + 1}
                    </button>
                  ) : (
                    <span style={chipStyle}>Team {team + 1}</span>
                  )
                })()}
                {player.isHost && (
                  <span className={styles.hostBadge}>Host</span>
                )}
              </div>
              <div className={styles.playerActions}>
                <span className={`${styles.playerStatus} ${!player.isConnected
                  ? styles.playerStatusDisconnected
                  : player.deckSubmitted
                    ? styles.playerStatusReady
                    : styles.playerStatusJoined
                  }`}>
                  {!player.isConnected
                    ? 'Disconnected'
                    : player.deckSubmitted
                      ? 'Deck Ready'
                      : 'Joined'}
                </span>
                {isWaiting && lobbyState.isHost && player.isAi && (
                  <button
                    onClick={() => removeAiFromLobby(player.playerId)}
                    className={styles.removeAiButton}
                    title="Remove AI player"
                  >
                    ×
                  </button>
                )}
              </div>
            </div>
          ))}
          {lobbyState.players.length === 0 && (
            <div className={styles.emptyPlayerList}>
              Waiting for players to join...
            </div>
          )}
          {isWaiting && lobbyState.isHost && aiEnabled && !isFfa && playerCount < (isWinston ? 2 : isGridDraft ? 4 : (lobbyState.settings.maxPlayers || 8)) && (
            <button onClick={addAiToLobby} className={styles.addAiButton}>
              + Add AI Player
            </button>
          )}
        </div>

        {/* Actions */}
        <div className={styles.actionsRow}>
          {isWaiting && lobbyState.isHost && (
            <button
              onClick={startLobby}
              disabled={!canStart}
              title={
                isPremade
                  ? lobbyState.players.length < 2
                    ? 'Need at least 2 players'
                    : !allConnectedDecksSubmitted
                      ? 'All connected players must submit a deck first'
                      : undefined
                  : !hasSelectedSets
                    ? 'Select at least one set'
                    : isWinston && lobbyState.players.length !== 2
                      ? 'Winston Draft requires exactly 2 players'
                      : lobbyState.players.length < 2
                        ? 'Need at least 2 players'
                        : undefined
              }
              className={styles.startButton}
            >
              {isAnyDraft ? 'Start Draft' : isPremade ? (isFfa ? 'Start Game' : 'Start Tournament') : 'Start Game'}
            </button>
          )}
          <button onClick={leaveLobby} className={styles.leaveButton}>
            Leave
          </button>
        </div>

        {isWaiting && !lobbyState.isHost && (
          <p className={styles.waitingHint}>
            Waiting for host to start the game...
          </p>
        )}
      </div>

      {showSetPicker && (
        <SetPickerModal
          sets={allSets}
          selectedCodes={lobbyState.settings.setCodes}
          onToggleSet={toggleSet}
          onClose={() => setShowSetPicker(false)}
        />
      )}
    </div>
  )
}

/**
 * Embedded deck picker for the Premade Decks tournament format. Each player picks
 * (saved/example/paste) and submits their deck right in the lobby; the host can only
 * start once everybody has submitted.
 */
function PremadeDeckPickerPanel({ lobbyState }: { lobbyState: LobbyState }) {
  const submitLobbyDeck = useGameStore((s) => s.submitLobbyDeck)
  const unsubmitLobbyDeck = useGameStore((s) => s.unsubmitLobbyDeck)
  const playerId = useGameStore((s) => s.playerId)

  const me = lobbyState.players.find((p) => p.playerId === playerId)
  const hasSubmitted = !!me?.deckSubmitted

  const [pendingDeck, setPendingDeck] = useState<Record<string, number>>({})
  const [pendingCommander, setPendingCommander] = useState<string | null>(null)
  const [isValid, setIsValid] = useState(false)

  const handleDeckChange = useCallback(
    (deck: Record<string, number>, commander?: string | null) => {
      setPendingDeck(deck)
      setPendingCommander(commander ?? null)
    },
    [],
  )

  if (hasSubmitted) {
    return (
      <div className={styles.deckSubmittedCard} role="status">
        <div className={styles.deckSubmittedIcon} aria-hidden>✓</div>
        <div className={styles.deckSubmittedBody}>
          <span className={styles.deckSubmittedTitle}>Deck submitted</span>
          <span className={styles.deckSubmittedSubtitle}>
            Waiting for the host to start the tournament.
          </span>
        </div>
        <button onClick={unsubmitLobbyDeck} className={styles.deckSubmittedEditButton}>
          Edit deck
        </button>
      </div>
    )
  }

  const deckFormat = lobbyState.settings.deckFormat
  const isCommanderShape =
    deckFormat === 'COMMANDER' || deckFormat === 'BRAWL' || deckFormat === 'STANDARD_BRAWL'
  const totalCards = Object.values(pendingDeck).reduce((a, b) => a + b, 0)
  const needsCommander = isCommanderShape && !pendingCommander
  const canSubmit = isValid && totalCards >= 40 && !needsCommander

  const submitTitle = !canSubmit
    ? needsCommander
      ? 'Pick a deck with a designated commander to play this format'
      : 'Pick a valid deck of at least 40 cards'
    : undefined

  return (
    <div className={styles.settingsPanel}>
      <div className={styles.settingsRow} style={{ alignItems: 'flex-start', flexDirection: 'column', gap: 12 }}>
        <span className={styles.settingsLabel}>Your Deck</span>
        {deckFormat && (
          <span className={styles.formatRestrictionNotice}>
            <span className={styles.formatRestrictionBadge}>{labelForFormat(deckFormat)}</span>
            <span>Only cards legal in this format will be accepted.</span>
          </span>
        )}
        <DeckPicker
          tabs={['saved', 'examples', 'paste']}
          onDeckChange={handleDeckChange}
          onValidityChange={setIsValid}
          format={deckFormat ?? null}
        />
        <button
          onClick={() => submitLobbyDeck(pendingDeck, isCommanderShape ? pendingCommander : null)}
          disabled={!canSubmit}
          title={submitTitle}
          className={styles.startButton}
        >
          Submit Deck
        </button>
      </div>
    </div>
  )
}

/**
 * Tournament overlay showing standings between rounds.
 */
interface HoveredStanding {
  playerId: string
  playerName: string
  wins: number
  losses: number
  draws: number
  points: number
  gamesWon: number
  gamesLost: number
  lifeDifferential: number | undefined
  tiebreakerReason: string | null | undefined
  rect: DOMRect
}

/**
 * Free-for-All pod overlay — the FFA-mode counterpart of [TournamentOverlay]. Shown between
 * games: standings of the last game (placement order), a "Play Again" ready loop, and the
 * usual share/replays/deck/leave toolbar. During a game the board renders instead.
 */
function FreeForAllOverlay({ ffaState }: { ffaState: FfaState }) {
  const playerId = useGameStore((state) => state.playerId)
  const lobbyState = useGameStore((state) => state.lobbyState)
  const deckBuildingState = useGameStore((state) => state.deckBuildingState)
  const readyForNextRound = useGameStore((state) => state.readyForNextRound)
  const unsubmitDeck = useGameStore((state) => state.unsubmitDeck)
  const leaveTournament = useGameStore((state) => state.leaveTournament)
  const spectateGame = useGameStore((state) => state.spectateGame)
  const [linkCopied, setLinkCopied] = useState(false)
  const [showReplays, setShowReplays] = useState(false)
  const [confirmLeave, setConfirmLeave] = useState(false)

  const shareLink = `${window.location.origin}/tournament/${ffaState.lobbyId}`
  const copyShareLink = () => {
    navigator.clipboard.writeText(shareLink)
    setLinkCopied(true)
    setTimeout(() => setLinkCopied(false), 2000)
  }

  const fetchPodGames = useCallback(async (): Promise<GameSummary[]> => {
    const token = localStorage.getItem('argentum-token')
    if (!token) throw new Error('No player token')
    const res = await fetch(`/api/replays/tournament/${ffaState.lobbyId}`, {
      headers: { 'X-Player-Token': token },
    })
    if (!res.ok) throw new Error(`Server error: ${res.status}`)
    return await res.json() as GameSummary[]
  }, [ffaState.lobbyId])

  const fetchPodReplay = useCallback(async (gameId: string): Promise<ReplayData> => {
    const token = localStorage.getItem('argentum-token')
    if (!token) throw new Error('No player token')
    const res = await fetch(`/api/replays/${gameId}?lobbyId=${ffaState.lobbyId}`, {
      headers: { 'X-Player-Token': token },
    })
    if (!res.ok) throw new Error(`Failed to load replay: ${res.status}`)
    return await res.json() as ReplayData
  }, [ffaState.lobbyId])

  if (showReplays) {
    return (
      <ReplayViewer
        fetchGames={fetchPodGames}
        fetchReplay={fetchPodReplay}
        onBack={() => setShowReplays(false)}
      />
    )
  }

  const isSpectator = !playerId || !lobbyState?.players.some((p) => p.playerId === playerId)
  const gameInProgress = ffaState.currentGameSessionId != null
  const isPlayerReady = playerId ? ffaState.readyPlayerIds.includes(playerId) : false
  const readyCount = ffaState.readyPlayerIds.length
  const totalPlayers = lobbyState?.players.filter((p) => p.isConnected).length
    ?? ffaState.standings?.length ?? 0

  return (
    <div className={styles.tournamentOverlay}>
      {/* ── Header ── */}
      <div className={styles.trnHeader}>
        <div className={styles.trnHeaderTop}>
          <h1 className={styles.trnTitle}>Free-for-All</h1>
          <span className={styles.trnRound}>
            {gameInProgress
              ? `Game ${ffaState.gameNumber} in progress`
              : ffaState.gamesPlayed > 0
                ? `After game ${ffaState.gamesPlayed}`
                : 'Waiting to start'}
          </span>
        </div>
        <div className={styles.trnToolbar}>
          <button onClick={copyShareLink} className={styles.trnToolbarBtn}>
            {linkCopied ? 'Copied!' : 'Share Link'}
          </button>
          {ffaState.gamesPlayed > 0 && (
            <button onClick={() => setShowReplays(true)} className={styles.trnToolbarBtn}>
              Replays
            </button>
          )}
          {gameInProgress && isSpectator && ffaState.currentGameSessionId && (
            <button
              onClick={() => spectateGame(ffaState.currentGameSessionId!)}
              className={styles.trnToolbarBtn}
            >
              Watch Game
            </button>
          )}
        </div>
      </div>

      {/* ── Action zone: play again / waiting ── */}
      {!isSpectator && !gameInProgress && (
        <div className={styles.trnActionZone}>
          <div className={styles.trnReadyRow}>
            <button
              onClick={readyForNextRound}
              disabled={isPlayerReady}
              className={styles.readyButton}
            >
              {isPlayerReady ? '✓ Ready' : ffaState.gamesPlayed > 0 ? 'Play Again' : 'Ready'}
            </button>
            {!isPlayerReady && deckBuildingState && (
              <button onClick={unsubmitDeck} className={styles.editDeckButton}>
                Edit Deck
              </button>
            )}
            <span className={styles.readyCount}>
              {readyCount}/{totalPlayers} ready
            </span>
          </div>
        </div>
      )}
      {gameInProgress && !isSpectator && (
        <div className={`${styles.statusBoxWaiting} ${styles.trnSection}`}>
          Game in progress...
        </div>
      )}

      {/* ── Standings (placement order of the last game) ── */}
      {ffaState.standings && (
        <div className={`${styles.standingsTable} ${styles.trnSection}`}>
          <table className={styles.standingsTableInner}>
            <thead className={styles.standingsHeader}>
              <tr>
                <th className={styles.standingsTh}>#</th>
                <th className={styles.standingsThLeft}>Player</th>
                <th className={styles.standingsTh}>Result</th>
              </tr>
            </thead>
            <tbody>
              {ffaState.standings.map((standing) => {
                const isMe = standing.playerId === playerId
                return (
                  <tr
                    key={standing.playerId}
                    className={`${styles.standingsRow} ${isMe ? styles.standingsRowMe : ''}`}
                  >
                    <td className={`${styles.standingsTd} ${styles.standingsRank} ${
                      standing.placement === 1 ? styles.standingsRankFirst :
                      standing.placement === 2 ? styles.standingsRankSecond :
                      standing.placement === 3 ? styles.standingsRankThird : ''
                    }`}>
                      {standing.placement}
                    </td>
                    <td className={styles.standingsTdLeft} style={{ fontWeight: isMe ? 600 : 400 }}>
                      <span className={styles.standingsPlayerName} title={standing.playerName}>
                        {standing.playerName}
                      </span>
                      {isMe && <span className={styles.meIndicator}>(you)</span>}
                    </td>
                    <td className={styles.standingsTd}>
                      {standing.placement === 1 ? 'Winner' : `${ordinal(standing.placement)} place`}
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        </div>
      )}
      {!ffaState.standings && !gameInProgress && (
        <div className={`${styles.statusBoxWaiting} ${styles.trnSection}`}>
          Waiting for all players to be ready...
        </div>
      )}

      {/* ── Leave ── */}
      <div className={styles.trnSection} style={{ display: 'flex', justifyContent: 'center', gap: 8 }}>
        {!confirmLeave ? (
          <button onClick={() => setConfirmLeave(true)} className={styles.leaveButton}>
            Leave
          </button>
        ) : (
          <>
            <button onClick={leaveTournament} className={styles.leaveButton}>
              Confirm Leave
            </button>
            <button onClick={() => setConfirmLeave(false)} className={styles.trnToolbarBtn}>
              Cancel
            </button>
          </>
        )}
      </div>
    </div>
  )
}

function ordinal(n: number): string {
  if (n % 100 >= 11 && n % 100 <= 13) return `${n}th`
  switch (n % 10) {
    case 1: return `${n}st`
    case 2: return `${n}nd`
    case 3: return `${n}rd`
    default: return `${n}th`
  }
}

function TournamentOverlay({
  tournamentState,
}: {
  tournamentState: TournamentState
}) {
  const playerId = useGameStore((state) => state.playerId)
  const spectateGame = useGameStore((state) => state.spectateGame)
  const readyForNextRound = useGameStore((state) => state.readyForNextRound)
  const addExtraRound = useGameStore((state) => state.addExtraRound)
  const leaveTournament = useGameStore((state) => state.leaveTournament)
  const unsubmitDeck = useGameStore((state) => state.unsubmitDeck)
  const lobbyState = useGameStore((state) => state.lobbyState)
  const deckBuildingState = useGameStore((state) => state.deckBuildingState)
  const disconnectedPlayers = useGameStore((state) => state.disconnectedPlayers)
  const addDisconnectTime = useGameStore((state) => state.addDisconnectTime)
  const kickPlayer = useGameStore((state) => state.kickPlayer)
  const [hoveredStanding, setHoveredStanding] = useState<HoveredStanding | null>(null)
  const [linkCopied, setLinkCopied] = useState(false)
  const [showDeckViewer, setShowDeckViewer] = useState(false)
  const [confirmLeave, setConfirmLeave] = useState(false)
  const [showReplays, setShowReplays] = useState(false)
  const [deckSavedAt, setDeckSavedAt] = useState<number | null>(null)
  const [saveDeckDialog, setSaveDeckDialog] = useState<{ name: string } | null>(null)
  const hydrateDeckLibrary = useDeckLibrary((s) => s.hydrate)
  const saveDeckToLibrary = useDeckLibrary((s) => s.saveDeck)
  useEffect(() => { hydrateDeckLibrary() }, [hydrateDeckLibrary])

  const buildDeckSave = (): { cards: Record<string, number>; entries: SavedDeckEntry[] | undefined } | null => {
    if (!deckBuildingState) return null
    const built = buildDraftedDeckSave(
      deckBuildingState.deck,
      deckBuildingState.landCounts,
      [...deckBuildingState.cardPool, ...deckBuildingState.basicLands],
    )
    return Object.keys(built.cards).length === 0 ? null : built
  }

  const openSaveDeckDialog = () => {
    if (!deckBuildingState) return
    const setNames = lobbyState?.settings.setNames?.join(' + ')
    const stamp = new Date().toLocaleDateString()
    const defaultName = setNames ? `${setNames} draft – ${stamp}` : `Drafted deck – ${stamp}`
    setSaveDeckDialog({ name: defaultName })
  }

  const confirmSaveDeck = () => {
    if (!saveDeckDialog) return
    const built = buildDeckSave()
    if (!built) return
    const name = saveDeckDialog.name.trim() || `Drafted deck – ${new Date().toLocaleDateString()}`
    saveDeckToLibrary({ name, cards: built.cards, ...(built.entries ? { entries: built.entries } : {}) })
    setSaveDeckDialog(null)
    setDeckSavedAt(Date.now())
    setTimeout(() => setDeckSavedAt(null), 2000)
  }
  // Tick every second to update disconnect countdown timers
  const [, setTick] = useState(0)
  const hasDisconnected = Object.keys(disconnectedPlayers).length > 0
  useEffect(() => {
    if (!hasDisconnected) return
    const timer = setInterval(() => setTick((t) => t + 1), 1000)
    return () => clearInterval(timer)
  }, [hasDisconnected])

  const shareLink = `${window.location.origin}/tournament/${tournamentState.lobbyId}`
  const copyShareLink = () => {
    navigator.clipboard.writeText(shareLink)
    setLinkCopied(true)
    setTimeout(() => setLinkCopied(false), 2000)
  }

  // Spectators are not in the standings list
  const isSpectator = !playerId || !tournamentState.standings.some(s => s.playerId === playerId)

  // Check if we're waiting for players to ready up (before first game OR between rounds)
  const isWaitingForReady = (
    // Before first round (round 0)
    tournamentState.currentRound === 0 ||
    // Between rounds (has results, no active match)
    (tournamentState.lastRoundResults !== null && !tournamentState.currentMatchGameSessionId)
  ) && !tournamentState.isComplete

  // Check if current player is ready
  const isPlayerReady = playerId ? tournamentState.readyPlayerIds.includes(playerId) : false
  const readyCount = tournamentState.readyPlayerIds.length
  const totalPlayers = tournamentState.standings.filter(s => s.isConnected).length

  // Auto-ready when player has a bye - no need for manual confirmation
  // Exception: Don't auto-ready before first round (round 0) so player can still edit deck
  useEffect(() => {
    if (!isSpectator && isWaitingForReady && tournamentState.nextRoundHasBye && !isPlayerReady && tournamentState.currentRound > 0) {
      readyForNextRound()
    }
  }, [isSpectator, isWaitingForReady, tournamentState.nextRoundHasBye, isPlayerReady, tournamentState.currentRound, readyForNextRound])

  const fetchTournamentGames = useCallback(async (): Promise<GameSummary[]> => {
    const token = localStorage.getItem('argentum-token')
    if (!token) throw new Error('No player token')
    const res = await fetch(`/api/replays/tournament/${tournamentState.lobbyId}`, {
      headers: { 'X-Player-Token': token },
    })
    if (!res.ok) throw new Error(`Server error: ${res.status}`)
    return await res.json() as GameSummary[]
  }, [tournamentState.lobbyId])

  const fetchTournamentReplay = useCallback(async (gameId: string): Promise<ReplayData> => {
    const token = localStorage.getItem('argentum-token')
    if (!token) throw new Error('No player token')
    const res = await fetch(`/api/replays/${gameId}?lobbyId=${tournamentState.lobbyId}`, {
      headers: { 'X-Player-Token': token },
    })
    if (!res.ok) throw new Error(`Failed to load replay: ${res.status}`)
    return await res.json() as ReplayData
  }, [tournamentState.lobbyId])

  if (showReplays) {
    return (
      <ReplayViewer
        fetchGames={fetchTournamentGames}
        fetchReplay={fetchTournamentReplay}
        onBack={() => setShowReplays(false)}
      />
    )
  }

  const roundLabel = !tournamentState.isComplete
    ? isWaitingForReady
      ? `Round ${tournamentState.currentRound + 1} of ${tournamentState.totalRounds}`
      : `Round ${tournamentState.currentRound} of ${tournamentState.totalRounds}`
    : null

  return (
    <div className={styles.tournamentOverlay}>
      {/* ── Header: title + round + toolbar ── */}
      <div className={styles.trnHeader}>
        <div className={styles.trnHeaderTop}>
          <h1 className={styles.trnTitle}>
            {tournamentState.isComplete ? 'Tournament Complete' : 'Standings'}
          </h1>
          {roundLabel && <span className={styles.trnRound}>{roundLabel}</span>}
        </div>
        <div className={styles.trnToolbar}>
          <button onClick={copyShareLink} className={styles.trnToolbarBtn}>
            {linkCopied ? 'Copied!' : 'Share Link'}
          </button>
          {tournamentState.lastRoundResults && (
            <button onClick={() => setShowReplays(true)} className={styles.trnToolbarBtn}>
              Replays
            </button>
          )}
          {!isSpectator && deckBuildingState && (
            <>
              <button
                onClick={openSaveDeckDialog}
                className={styles.trnToolbarBtn}
                style={{
                  background: deckSavedAt ? 'rgba(108, 192, 74, 0.2)' : 'var(--accent-primary, #6aa3ff)',
                  borderColor: deckSavedAt ? 'rgba(108, 192, 74, 0.5)' : 'var(--accent-primary, #6aa3ff)',
                  color: '#fff',
                  fontWeight: 600,
                }}
                title="Save this drafted deck to your local My Decks library"
              >
                {deckSavedAt ? 'Saved ✓' : 'Save Deck'}
              </button>
              <button onClick={() => setShowDeckViewer(true)} className={styles.trnToolbarBtn}>
                View Deck
              </button>
            </>
          )}
        </div>
      </div>

      {/* ── Action zone: next match / ready / bye ── */}
      {!isSpectator && !tournamentState.isComplete && (
        <div className={styles.trnActionZone}>
          {/* Next opponent */}
          {isWaitingForReady && !tournamentState.nextRoundHasBye && (
            <div className={styles.statusBoxMatch}>
              <div className={styles.statusBoxMatchLabel}>Next Match</div>
              <div className={styles.statusBoxMatchOpponent}>
                {tournamentState.nextOpponentName
                  ? `vs ${tournamentState.nextOpponentName}`
                  : 'Waiting for matchup...'}
              </div>
            </div>
          )}

          {/* BYE status */}
          {((isWaitingForReady && tournamentState.nextRoundHasBye) || (tournamentState.isBye && !isWaitingForReady)) && (
            <div className={styles.statusBoxBye}>
              <span className={styles.byeIcon}>&#x2713;</span>
              <span>Sitting out this round</span>
            </div>
          )}

          {/* Ready button row */}
          {isWaitingForReady && (
            <div className={styles.trnReadyRow}>
              <button
                onClick={readyForNextRound}
                disabled={isPlayerReady}
                className={styles.readyButton}
              >
                {isPlayerReady ? '✓ Ready' : 'Ready for Next Round'}
              </button>
              {tournamentState.currentRound === 0 && !isPlayerReady && (
                <button onClick={unsubmitDeck} className={styles.editDeckButton}>
                  Edit Deck
                </button>
              )}
              <span className={styles.readyCount}>
                {readyCount}/{totalPlayers} ready
              </span>
            </div>
          )}

          {/* Waiting for others */}
          {!isWaitingForReady && !tournamentState.isBye && !tournamentState.currentMatchGameSessionId && (
            <div className={styles.statusBoxWaiting}>
              Waiting for other matches to complete...
            </div>
          )}
        </div>
      )}

      {/* ── Disconnected players ── */}
      {Object.keys(disconnectedPlayers).length > 0 && (
        <div className={`${styles.disconnectedBanner} ${styles.trnSection}`}>
          {Object.entries(disconnectedPlayers).map(([pid, info]) => {
            const elapsed = Math.floor((Date.now() - info.disconnectedAt) / 1000)
            const remaining = Math.max(0, info.secondsRemaining - elapsed)
            const mins = Math.floor(remaining / 60)
            const secs = remaining % 60
            const canKick = elapsed >= 120
            return (
              <div key={pid} className={styles.disconnectedPlayer}>
                <span className={styles.disconnectedName}>{info.playerName} disconnected</span>
                <span className={styles.disconnectedTimer}>{mins}:{secs.toString().padStart(2, '0')}</span>
                {!isSpectator && (
                  <>
                    <button className={styles.addTimeButton} onClick={() => addDisconnectTime(pid)}>
                      +1 min
                    </button>
                    {canKick && (
                      <button className={styles.kickButton} onClick={() => kickPlayer(pid)}>
                        Kick
                      </button>
                    )}
                  </>
                )}
              </div>
            )
          })}
        </div>
      )}

      {/* ── Standings table ── */}
      <div className={`${styles.standingsTable} ${styles.trnSection}`}>
        <table className={styles.standingsTableInner}>
          <thead className={styles.standingsHeader}>
            <tr>
              <th className={styles.standingsTh}>#</th>
              <th className={styles.standingsThLeft}>Player</th>
              <th className={styles.standingsTh}>Record</th>
              <th className={styles.standingsTh}>Pts</th>
              <th className={styles.standingsTh} title="Game Win Rate">GWR</th>
              {isWaitingForReady && <th className={styles.standingsTh}>Ready</th>}
            </tr>
          </thead>
          <tbody>
            {tournamentState.standings.map((standing, index) => {
              const isMe = standing.playerId === playerId
              const isReady = tournamentState.readyPlayerIds.includes(standing.playerId)
              const displayRank = standing.rank ?? index + 1

              const gamesWon = standing.gamesWon ?? 0
              const gamesLost = standing.gamesLost ?? 0
              const totalGames = gamesWon + gamesLost
              const winRate = totalGames > 0 ? ((gamesWon / totalGames) * 100).toFixed(0) : '-'

              const handleMouseEnter = (e: React.MouseEvent<HTMLTableRowElement>) => {
                const rect = e.currentTarget.getBoundingClientRect()
                setHoveredStanding({
                  playerId: standing.playerId,
                  playerName: standing.playerName,
                  wins: standing.wins,
                  losses: standing.losses,
                  draws: standing.draws,
                  points: standing.points,
                  gamesWon: gamesWon,
                  gamesLost: gamesLost,
                  lifeDifferential: standing.lifeDifferential,
                  tiebreakerReason: standing.tiebreakerReason,
                  rect,
                })
              }

              return (
                <tr
                  key={standing.playerId}
                  className={`${styles.standingsRow} ${isMe ? styles.standingsRowMe : ''}`}
                  onMouseEnter={handleMouseEnter}
                  onMouseLeave={() => setHoveredStanding(null)}
                >
                  <td className={`${styles.standingsTd} ${styles.standingsRank} ${displayRank === 1 ? styles.standingsRankFirst :
                    displayRank === 2 ? styles.standingsRankSecond :
                      displayRank === 3 ? styles.standingsRankThird : ''
                    }`}>
                    {displayRank}
                    {standing.tiebreakerReason === 'TIED' && (
                      <span className={styles.tiedIndicator}>*</span>
                    )}
                  </td>
                  <td className={styles.standingsTdLeft} style={{ fontWeight: isMe ? 600 : 400 }}>
                    <span className={styles.standingsPlayerName} title={standing.playerName}>
                      {standing.playerName}
                    </span>
                    {isMe && <span className={styles.meIndicator}>(you)</span>}
                    {!standing.isConnected && (
                      <span className={styles.disconnectedIndicator}>DC</span>
                    )}
                  </td>
                  <td className={`${styles.standingsTd} ${styles.standingsRecord}`}>
                    <span className={styles.standingsWins}>{standing.wins}</span>
                    {'-'}
                    <span className={styles.standingsLosses}>{standing.losses}</span>
                    {'-'}
                    <span className={styles.standingsDraws}>{standing.draws}</span>
                  </td>
                  <td className={`${styles.standingsTd} ${styles.standingsPoints}`}>{standing.points}</td>
                  <td className={`${styles.standingsTd} ${styles.standingsGwr}`}>
                    {totalGames > 0 ? `${winRate}%` : '-'}
                  </td>
                  {isWaitingForReady && (
                    <td className={`${styles.standingsTd} ${styles.standingsReady}`} style={{ color: isReady ? 'var(--color-success-light)' : 'var(--text-disabled)' }}>
                      {standing.isConnected ? (isReady ? '✓' : '···') : '-'}
                    </td>
                  )}
                </tr>
              )
            })}
          </tbody>
        </table>

        {/* Instant hover tooltip */}
        {hoveredStanding && (
          <div
            className={styles.standingsTooltip}
            style={{
              top: hoveredStanding.rect.top + hoveredStanding.rect.height / 2,
              left: hoveredStanding.rect.right + 12,
              transform: 'translateY(-50%)',
            }}
          >
            <div className={styles.tooltipName}>{hoveredStanding.playerName}</div>
            <div className={styles.tooltipStat}>
              <span className={styles.tooltipStatLabel}>Match Record</span>
              <span className={styles.tooltipStatValue}>
                {hoveredStanding.wins}W-{hoveredStanding.losses}L-{hoveredStanding.draws}D
              </span>
            </div>
            <div className={styles.tooltipStat}>
              <span className={styles.tooltipStatLabel}>Points</span>
              <span className={styles.tooltipStatValue}>{hoveredStanding.points}</span>
            </div>
            {(hoveredStanding.gamesWon + hoveredStanding.gamesLost) > 0 && (
              <div className={styles.tooltipStat}>
                <span className={styles.tooltipStatLabel}>Game Record</span>
                <span className={styles.tooltipStatValue}>
                  {hoveredStanding.gamesWon}-{hoveredStanding.gamesLost} (
                  {((hoveredStanding.gamesWon / (hoveredStanding.gamesWon + hoveredStanding.gamesLost)) * 100).toFixed(0)}%)
                </span>
              </div>
            )}
            {hoveredStanding.lifeDifferential !== undefined && (
              <div className={styles.tooltipStat}>
                <span className={styles.tooltipStatLabel}>Life Diff</span>
                <span className={styles.tooltipStatValue}>
                  {hoveredStanding.lifeDifferential >= 0 ? '+' : ''}{hoveredStanding.lifeDifferential}
                </span>
              </div>
            )}
            {hoveredStanding.tiebreakerReason && hoveredStanding.tiebreakerReason !== 'TIED' && (
              <div className={styles.tooltipTiebreaker}>
                {hoveredStanding.tiebreakerReason === 'HEAD_TO_HEAD'
                  ? 'Ranked by head-to-head'
                  : hoveredStanding.tiebreakerReason === 'H2H_GAMES'
                    ? 'Ranked by H2H games'
                    : hoveredStanding.tiebreakerReason === 'LIFE_DIFF'
                      ? 'Ranked by life diff'
                      : null}
              </div>
            )}
            {hoveredStanding.tiebreakerReason === 'TIED' && (
              <div className={styles.tooltipTiebreaker}>Tied</div>
            )}
          </div>
        )}
      </div>

      {/* ── Live matches ── */}
      {!tournamentState.currentMatchGameSessionId && tournamentState.activeMatches && tournamentState.activeMatches.length > 0 && (
        <div className={`${styles.matchesSection} ${styles.trnSection}`}>
          <h3 className={styles.matchesSectionTitle}>
            Live Matches
          </h3>
          <div className={styles.matchesList}>
            {tournamentState.activeMatches.map((match) => (
              <button
                key={match.gameSessionId}
                onClick={() => spectateGame(match.gameSessionId)}
                className={styles.matchButton}
              >
                <div className={styles.matchPlayers}>
                  <span className={styles.matchPlayerName}>{match.player1Name}</span>
                  <span className={styles.matchVs}>vs</span>
                  <span className={styles.matchPlayerName}>{match.player2Name}</span>
                </div>
                <div className={styles.matchScore}>
                  <span className={match.player1Life <= 5 ? styles.matchLifeLow : styles.matchLifeHigh}>
                    {match.player1Life}
                  </span>
                  <span className={styles.matchScoreDash}>-</span>
                  <span className={match.player2Life <= 5 ? styles.matchLifeLow : styles.matchLifeHigh}>
                    {match.player2Life}
                  </span>
                  <span className={styles.matchWatch}>▶ Watch</span>
                </div>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* ── Last round results ── */}
      {tournamentState.lastRoundResults && (
        <div className={`${styles.resultsSection} ${styles.trnSection}`}>
          <h3 className={styles.resultsSectionTitle}>
            Round {tournamentState.currentRound} Results
          </h3>
          {tournamentState.lastRoundResults.map((result, i) => (
            <div key={i} className={styles.resultRow}>
              <span>{result.player1Name}</span>
              <span className={styles.resultOutcome}>
                {result.isBye
                  ? 'BYE'
                  : result.isDraw
                    ? 'Draw'
                    : `Winner: ${result.winnerId === result.player1Id ? result.player1Name : result.player2Name}`}
              </span>
              <span>{result.isBye ? '' : result.player2Name}</span>
            </div>
          ))}
        </div>
      )}

      {/* ── Footer actions ── */}
      <div className={styles.trnFooter}>
        {tournamentState.isComplete ? (
          <>
            {lobbyState?.isHost && (
              <button onClick={addExtraRound} className={styles.readyButton}>
                Add Round
              </button>
            )}
            <button onClick={leaveTournament} className={styles.returnButton}>
              Return to Menu
            </button>
          </>
        ) : (
          <button
            onClick={() => {
              if (confirmLeave) {
                leaveTournament()
              } else {
                setConfirmLeave(true)
                setTimeout(() => setConfirmLeave(false), 3000)
              }
            }}
            className={confirmLeave ? styles.leaveButtonConfirm : styles.leaveButton}
          >
            {confirmLeave ? 'Confirm Leave?' : 'Leave Tournament'}
          </button>
        )}
      </div>

      {/* Deck Viewer Modal */}
      {showDeckViewer && deckBuildingState && (
        <DeckViewerModal
          deckBuildingState={deckBuildingState}
          onClose={() => setShowDeckViewer(false)}
        />
      )}

      {/* Save Deck dialog */}
      {saveDeckDialog && (
        <SaveDeckDialog
          name={saveDeckDialog.name}
          onNameChange={(name) => setSaveDeckDialog({ name })}
          onCancel={() => setSaveDeckDialog(null)}
          onConfirm={confirmSaveDeck}
        />
      )}
    </div>
  )
}

function SaveDeckDialog({
  name,
  onNameChange,
  onConfirm,
  onCancel,
}: {
  name: string
  onNameChange: (name: string) => void
  onConfirm: () => void
  onCancel: () => void
}) {
  const inputRef = useRef<HTMLInputElement>(null)
  useEffect(() => {
    // Focus + select on mount so the user can immediately overtype the suggested name.
    inputRef.current?.focus()
    inputRef.current?.select()
  }, [])
  return (
    <div className={styles.deckViewerBackdrop} onClick={onCancel}>
      <div
        className={styles.deckViewerPanel}
        onClick={(e) => e.stopPropagation()}
        style={{ maxWidth: 480 }}
      >
        <div className={styles.deckViewerHeader}>
          <h3 className={styles.deckViewerTitle}>Save Deck</h3>
          <button className={styles.deckViewerClose} onClick={onCancel}>
            &#x2715;
          </button>
        </div>
        <div style={{ padding: 'var(--space-4) var(--space-5)', display: 'flex', flexDirection: 'column', gap: 'var(--space-3)' }}>
          <label style={{ fontSize: 'var(--font-sm)', color: 'var(--text-faint)' }}>
            Deck name
          </label>
          <input
            ref={inputRef}
            type="text"
            value={name}
            onChange={(e) => onNameChange(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter') onConfirm()
              if (e.key === 'Escape') onCancel()
            }}
            placeholder="Deck name"
            style={{
              background: 'rgba(0, 0, 0, 0.3)',
              border: '1px solid rgba(255, 255, 255, 0.12)',
              borderRadius: 'var(--radius-sm)',
              padding: 'var(--space-2) var(--space-3)',
              color: 'var(--text-primary)',
              fontSize: '0.95rem',
            }}
          />
          <div style={{ display: 'flex', gap: 'var(--space-2)', justifyContent: 'flex-end' }}>
            <button
              type="button"
              onClick={onCancel}
              className={styles.leaveButton}
            >
              Cancel
            </button>
            <button
              type="button"
              onClick={onConfirm}
              disabled={!name.trim()}
              className={styles.startButton}
            >
              Save to My Decks
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

/**
 * Read-only deck viewer modal for viewing submitted deck during tournament.
 */
function DeckViewerModal({
  deckBuildingState,
  onClose,
}: {
  deckBuildingState: { cardPool: readonly SealedCardInfo[]; basicLands: readonly SealedCardInfo[]; deck: readonly string[]; landCounts: Record<string, number> }
  onClose: () => void
}) {
  const [hoveredCard, setHoveredCard] = useState<SealedCardInfo | null>(null)
  const [hoverPos, setHoverPos] = useState<{ x: number; y: number } | null>(null)
  const hydrateDeckLibrary = useDeckLibrary((s) => s.hydrate)
  const saveDeck = useDeckLibrary((s) => s.saveDeck)
  useEffect(() => { hydrateDeckLibrary() }, [hydrateDeckLibrary])
  const defaultDeckName = `Drafted ${new Date().toLocaleDateString()} ${new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}`
  const [saveName, setSaveName] = useState(defaultDeckName)
  const [savedAt, setSavedAt] = useState<number | null>(null)

  const handleHover = (card: SealedCardInfo | null, e?: React.MouseEvent) => {
    setHoveredCard(card)
    if (card && e) {
      setHoverPos({ x: e.clientX, y: e.clientY })
    } else {
      setHoverPos(null)
    }
  }

  const cardInfoMap = new Map<string, SealedCardInfo>()
  for (const card of deckBuildingState.cardPool) {
    cardInfoMap.set(card.name, card)
  }
  for (const land of deckBuildingState.basicLands) {
    cardInfoMap.set(land.name, land)
  }

  // Count cards in deck
  const deckCounts = new Map<string, number>()
  for (const name of deckBuildingState.deck) {
    deckCounts.set(name, (deckCounts.get(name) ?? 0) + 1)
  }

  // Calculate CMC for a card
  const getCmc = (card: SealedCardInfo): number => {
    if (!card.manaCost) return 0
    let cmc = 0
    for (const match of card.manaCost.matchAll(/\{([^}]+)\}/g)) {
      const sym = match[1] ?? ''
      const num = parseInt(sym, 10)
      if (!isNaN(num)) cmc += num
      else if (sym !== 'X') cmc += 1
    }
    return cmc
  }

  // Group cards by CMC
  const grouped = new Map<number, { card: SealedCardInfo; count: number }[]>()
  for (const [name, count] of deckCounts) {
    const card = cardInfoMap.get(name)
    if (!card) continue
    const cmc = getCmc(card)
    if (!grouped.has(cmc)) grouped.set(cmc, [])
    grouped.get(cmc)!.push({ card, count })
  }
  // Sort groups by CMC, cards within by name
  const sortedGroups = [...grouped.entries()].sort((a, b) => a[0] - b[0])
  for (const [, cards] of sortedGroups) {
    cards.sort((a, b) => a.card.name.localeCompare(b.card.name))
  }

  // Calculate stats
  const totalLands = Object.values(deckBuildingState.landCounts).reduce((s, n) => s + n, 0)
  const totalSpellCards = deckBuildingState.deck.length
  let creatures = 0
  let nonCreatureSpells = 0
  for (const [name, count] of deckCounts) {
    const card = cardInfoMap.get(name)
    if (!card) continue
    if (card.typeLine.toLowerCase().includes('creature')) creatures += count
    else nonCreatureSpells += count
  }
  const totalCards = totalSpellCards + totalLands

  return (
    <div className={styles.deckViewerBackdrop} onClick={onClose}>
      <div className={styles.deckViewerPanel} onClick={(e) => e.stopPropagation()}>
        <div className={styles.deckViewerHeader}>
          <h3 className={styles.deckViewerTitle}>Your Deck ({totalCards})</h3>
          <button className={styles.deckViewerClose} onClick={onClose}>
            &#x2715;
          </button>
        </div>
        <div className={styles.deckViewerBody}>
          {/* Stats */}
          <div className={styles.deckViewerStats}>
            <div className={styles.deckViewerStat}>
              <span className={styles.deckViewerStatValue}>{creatures}</span>
              <span className={styles.deckViewerStatLabel}>Creatures</span>
            </div>
            <div className={styles.deckViewerStat}>
              <span className={styles.deckViewerStatValue}>{nonCreatureSpells}</span>
              <span className={styles.deckViewerStatLabel}>Spells</span>
            </div>
            <div className={styles.deckViewerStat}>
              <span className={styles.deckViewerStatValue}>{totalLands}</span>
              <span className={styles.deckViewerStatLabel}>Lands</span>
            </div>
          </div>

          {/* Card list grouped by CMC */}
          {sortedGroups.map(([cmc, cards]) => (
            <div key={cmc} className={styles.deckViewerGroup}>
              <div className={styles.deckViewerGroupHeader}>
                {cmc === 0 ? 'CMC 0' : `CMC ${cmc}`} ({cards.reduce((s, c) => s + c.count, 0)})
              </div>
              {cards.map(({ card, count }) => (
                <div
                  key={card.name}
                  className={styles.deckViewerRow}
                  onMouseEnter={(e) => handleHover(card, e)}
                  onMouseMove={(e) => handleHover(card, e)}
                  onMouseLeave={() => handleHover(null)}
                >
                  <span className={styles.deckViewerCount}>{count}</span>
                  <span className={styles.deckViewerCardName}>{card.name}</span>
                  <span className={styles.deckViewerManaCost}>
                    {card.manaCost
                      ? <ManaCost cost={card.manaCost} size={12} />
                      : <span style={{ color: '#666', fontSize: 10 }}>({getCmc(card)})</span>}
                  </span>
                </div>
              ))}
            </div>
          ))}

          {/* Basic lands */}
          {totalLands > 0 && (
            <div className={styles.deckViewerLandsSection}>
              <div className={styles.deckViewerGroupHeader}>
                Lands ({totalLands})
              </div>
              {Object.entries(deckBuildingState.landCounts)
                .filter(([, count]) => count > 0)
                .map(([name, count]) => {
                  const landCard = cardInfoMap.get(name)
                  return (
                    <div
                      key={name}
                      className={styles.deckViewerLandRow}
                      onMouseEnter={(e) => landCard && handleHover(landCard, e)}
                      onMouseMove={(e) => landCard && handleHover(landCard, e)}
                      onMouseLeave={() => handleHover(null)}
                    >
                      <span className={styles.deckViewerCount}>{count}</span>
                      <span className={styles.deckViewerLandName}>{name}</span>
                    </div>
                  )
                })}
            </div>
          )}
        </div>
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 8,
            padding: 'var(--space-3) var(--space-5)',
            borderTop: '1px solid rgba(255, 255, 255, 0.08)',
          }}
        >
          <input
            type="text"
            value={saveName}
            onChange={(e) => setSaveName(e.target.value)}
            placeholder="Deck name"
            style={{
              flex: 1,
              background: 'rgba(0, 0, 0, 0.3)',
              border: '1px solid rgba(255, 255, 255, 0.1)',
              borderRadius: 'var(--radius-sm)',
              padding: 'var(--space-1) var(--space-2)',
              color: 'var(--text-primary)',
              fontSize: '0.85rem',
            }}
          />
          <button
            type="button"
            onClick={() => {
              // Combine non-basic deck cards with basic-land counts, pinning each to the
              // printing it was drafted as so the saved deck keeps the exact art/printing.
              const { cards, entries } = buildDraftedDeckSave(
                deckBuildingState.deck,
                deckBuildingState.landCounts,
                [...deckBuildingState.cardPool, ...deckBuildingState.basicLands],
              )
              saveDeck({ name: saveName.trim() || defaultDeckName, cards, ...(entries ? { entries } : {}) })
              setSavedAt(Date.now())
              setTimeout(() => setSavedAt(null), 2000)
            }}
            disabled={totalCards === 0}
            className={styles.startButton}
            style={{ flexShrink: 0 }}
          >
            {savedAt ? 'Saved ✓' : 'Save to My Decks'}
          </button>
        </div>
      </div>

      {/* Card image preview on hover */}
      {hoveredCard && <DeckViewerCardPreview card={hoveredCard} pos={hoverPos} />}
    </div>
  )
}

/**
 * Card image preview that follows the cursor, shown when hovering cards in the deck viewer.
 */
function DeckViewerCardPreview({ card, pos }: { card: SealedCardInfo; pos: { x: number; y: number } | null }) {
  const imageUrl = getCardImageUrl(card.name, card.imageUri, 'large')
  const previewWidth = 250
  const previewHeight = Math.round(previewWidth * 1.4)

  let top = 80
  let left = 20
  if (pos) {
    const margin = 20
    if (pos.x + previewWidth + margin + 20 < window.innerWidth) {
      left = pos.x + margin
    } else {
      left = pos.x - previewWidth - margin
    }
    top = Math.max(10, Math.min(pos.y - previewHeight / 2, window.innerHeight - previewHeight - 10))
  }

  return (
    <div
      style={{
        position: 'fixed',
        top,
        left,
        pointerEvents: 'none',
        zIndex: 1002,
        transition: 'top 0.05s, left 0.05s',
      }}
    >
      <div
        style={{
          width: previewWidth,
          height: previewHeight,
          borderRadius: 12,
          overflow: 'hidden',
          boxShadow: '0 8px 32px rgba(0, 0, 0, 0.8)',
        }}
      >
        <img
          src={imageUrl}
          alt={card.name}
          style={{
            width: '100%',
            height: '100%',
            objectFit: 'cover',
          }}
        />
      </div>
    </div>
  )
}

/**
 * Fullscreen toggle button.
 */
function FullscreenButton() {
  const [isFullscreen, setIsFullscreen] = useState(false)

  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(!!document.fullscreenElement)
    }
    document.addEventListener('fullscreenchange', handleFullscreenChange)
    return () => document.removeEventListener('fullscreenchange', handleFullscreenChange)
  }, [])

  const toggleFullscreen = async () => {
    try {
      if (!document.fullscreenElement) {
        await document.documentElement.requestFullscreen()
      } else {
        await document.exitFullscreen()
      }
    } catch (err) {
      console.error('Fullscreen error:', err)
    }
  }

  return (
    <button
      onClick={toggleFullscreen}
      className={styles.fullscreenButton}
      title={isFullscreen ? 'Exit fullscreen (Esc)' : 'Enter fullscreen'}
    >
      {isFullscreen ? '⛶ Exit' : '⛶ Fullscreen'}
    </button>
  )
}
