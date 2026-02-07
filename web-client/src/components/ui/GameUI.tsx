import { useState, useEffect } from 'react'
import { useGameStore, type LobbyState, type TournamentState } from '../../store/gameStore'
import styles from './GameUI.module.css'

const backgroundModules = import.meta.glob('../../assets/backgrounds/*.jpeg', { eager: true, query: '?url', import: 'default' }) as Record<string, string>
const backgroundUrls = Object.values(backgroundModules)

function pickBackground(): string {
  const HOUR_MS = 60 * 60 * 1000
  const stored = localStorage.getItem('argentum-bg')
  if (stored) {
    const { index, timestamp } = JSON.parse(stored)
    if (Date.now() - timestamp < HOUR_MS && backgroundUrls[index]) {
      return backgroundUrls[index]
    }
  }
  const index = Math.floor(Math.random() * backgroundUrls.length)
  localStorage.setItem('argentum-bg', JSON.stringify({ index, timestamp: Date.now() }))
  return backgroundUrls[index]
}

const randomBackground = pickBackground()

type GameMode = 'normal' | 'tournament'

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

  // Don't show connection overlay if actively building deck (but show during 'waiting' phase)
  // Exception: always show if tournamentState exists (for TournamentOverlay)
  if (deckBuildingState && deckBuildingState.phase !== 'waiting' && !tournamentState) return null

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
  const connect = useGameStore((state) => state.connect)
  const createGame = useGameStore((state) => state.createGame)
  const joinGame = useGameStore((state) => state.joinGame)
  const createTournamentLobby = useGameStore((state) => state.createTournamentLobby)
  const joinLobby = useGameStore((state) => state.joinLobby)
  const lobbyState = useGameStore((state) => state.lobbyState)
  const [joinSessionId, setJoinSessionId] = useState('')
  const [gameMode, setGameMode] = useState<GameMode>('normal')
  const [playerName, setPlayerName] = useState(() => sessionStorage.getItem('argentum-player-name') || '')

  const [nameConfirmed, setNameConfirmed] = useState(() => !!sessionStorage.getItem('argentum-player-name'))

  const confirmName = () => {
    if (playerName.trim()) {
      sessionStorage.setItem('argentum-player-name', playerName.trim())
      setNameConfirmed(true)
      connect(playerName.trim())
    }
  }

  // Empty deck triggers server-side random deck generation from Portal set
  const randomDeck = {}

  const handleCreate = () => {
    if (gameMode === 'tournament') {
      // Create lobby with default settings - host can change in lobby
      createTournamentLobby(['POR'], 'SEALED')
    } else {
      createGame(randomDeck)
    }
  }

  const handleJoin = () => {
    if (joinSessionId.trim()) {
      if (gameMode === 'tournament') {
        joinLobby(joinSessionId.trim())
      } else {
        joinGame(joinSessionId.trim(), randomDeck)
      }
    }
  }

  // Show tournament UI if we're in a tournament (even without lobbyState)
  const tournamentState = useGameStore((state) => state.tournamentState)
  if (tournamentState) {
    return <TournamentOverlay tournamentState={tournamentState} />
  }

  // Show lobby UI if we're in a lobby
  if (lobbyState) {
    return <LobbyOverlay lobbyState={lobbyState} />
  }

  return (
    <div className={styles.connectionOverlay} style={{ backgroundImage: `url(${randomBackground})` }}>
      <FullscreenButton />
      <div className={styles.contentBackdrop}>
        <h1 className={styles.title}>Argentum Engine</h1>

        {error && (
          <p className={styles.errorMessage}>Error: {error}</p>
        )}

        {!nameConfirmed && (
          <div className={styles.inputGroup}>
            <label className={styles.inputLabel}>Enter your name</label>
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
                Play with a randomly generated deck for quick 1v1 matches.
              </p>
            )}
            {gameMode === 'tournament' && (
              <p className={styles.modeDescription}>
                Create a lobby for Sealed or Draft. Configure format and set after creating.
              </p>
            )}

            <button
              onClick={handleCreate}
              className={gameMode === 'tournament' ? styles.tournamentButton : styles.primaryButton}
            >
              {gameMode === 'tournament' ? 'Create Lobby' : 'Create Game'}
            </button>

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
                placeholder={gameMode === 'tournament' ? 'Enter Lobby ID' : 'Enter Session ID'}
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
          </div>
        )}

        {sessionId && (
          <WaitingForOpponent sessionId={sessionId} />
        )}
      </div>
    </div>
  )
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
 * Lobby overlay for sealed lobbies.
 */
function LobbyOverlay({
  lobbyState,
}: {
  lobbyState: LobbyState
}) {
  const startLobby = useGameStore((state) => state.startLobby)
  const leaveLobby = useGameStore((state) => state.leaveLobby)
  const updateLobbySettings = useGameStore((state) => state.updateLobbySettings)
  const tournamentState = useGameStore((state) => state.tournamentState)
  const [copied, setCopied] = useState(false)

  // Show tournament standings when tournament is active
  if (tournamentState) {
    return <TournamentOverlay tournamentState={tournamentState} />
  }

  const isWaiting = lobbyState.state === 'WAITING_FOR_PLAYERS'
  const isDraft = lobbyState.settings.format === 'DRAFT'
  const hasSelectedSets = lobbyState.settings.setCodes.length > 0
  const canStart = lobbyState.players.length >= 2 && hasSelectedSets

  const copyLobbyId = () => {
    navigator.clipboard.writeText(lobbyState.lobbyId)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div className={styles.lobbyOverlay} style={{ backgroundImage: `url(${randomBackground})` }}>
      <FullscreenButton />
      <div className={styles.lobbyContent}>
        {/* Header */}
        <div className={styles.lobbyHeader}>
          <div className={`${styles.lobbyFormat} ${isDraft ? styles.lobbyFormatDraft : styles.lobbyFormatSealed}`}>
            {isDraft ? 'Draft' : 'Sealed'}
          </div>
          <h1 className={styles.lobbyTitle}>
            {lobbyState.settings.setNames.join(' + ') || 'Lobby'}
          </h1>
          <p className={styles.lobbySubtitle}>
            {isDraft
              ? `${lobbyState.settings.boosterCount} packs · ${lobbyState.settings.pickTimeSeconds}s per pick${lobbyState.settings.picksPerRound === 2 ? ' · Pick 2' : ''}`
              : `${lobbyState.settings.boosterCount} boosters per player`}
            {(lobbyState.settings.gamesPerMatch ?? 1) > 1 && ` · ${lobbyState.settings.gamesPerMatch} games per matchup`}
          </p>
        </div>

        {/* Invite code */}
        <div
          onClick={copyLobbyId}
          className={`${styles.inviteBox} ${copied ? styles.inviteBoxCopied : ''}`}
          style={{ alignSelf: 'stretch', justifyContent: 'space-between' }}
        >
          <div>
            <div style={{ color: 'var(--text-disabled)', fontSize: 10, textTransform: 'uppercase', letterSpacing: '0.08em', marginBottom: 3 }}>
              Invite Code
            </div>
            <div className={styles.inviteCode}>
              {lobbyState.lobbyId}
            </div>
          </div>
          <span className={`${styles.inviteCopyLabel} ${copied ? styles.inviteCopyLabelCopied : ''}`} style={{ flexShrink: 0, marginLeft: 12 }}>
            {copied ? 'Copied!' : 'Copy'}
          </span>
        </div>

        {/* Settings (host only) */}
        {isWaiting && lobbyState.isHost && (
          <div className={styles.settingsPanel}>
            {/* Format selection */}
            <div className={styles.settingsRow}>
              <span className={styles.settingsLabel}>Format</span>
              <div className={styles.settingsButtons}>
                <button
                  onClick={() => updateLobbySettings({ format: 'SEALED' })}
                  className={`${styles.settingsButton} ${!isDraft ? styles.settingsButtonActive : ''}`}
                >
                  Sealed
                </button>
                <button
                  onClick={() => updateLobbySettings({ format: 'DRAFT' })}
                  className={`${styles.settingsButton} ${isDraft ? `${styles.settingsButtonActive} ${styles.settingsButtonDraft}` : ''}`}
                >
                  Draft
                </button>
              </div>
            </div>
            {/* Set selection */}
            <div className={styles.settingsRow}>
              <span className={styles.settingsLabel}>Sets</span>
              <div className={styles.settingsButtons} style={{ flexWrap: 'wrap', justifyContent: 'flex-end' }}>
                {lobbyState.settings.availableSets.map((set) => {
                  const isSelected = lobbyState.settings.setCodes.includes(set.code)
                  return (
                    <button
                      key={set.code}
                      onClick={() => {
                        const newCodes = isSelected
                          ? lobbyState.settings.setCodes.filter(c => c !== set.code)
                          : [...lobbyState.settings.setCodes, set.code]
                        updateLobbySettings({ setCodes: newCodes })
                      }}
                      className={`${styles.settingsButton} ${isSelected ? (isDraft ? `${styles.settingsButtonActive} ${styles.settingsButtonDraft}` : styles.settingsButtonActive) : ''}`}
                    >
                      {set.name}{set.incomplete ? ' (Incomplete)' : ''}
                    </button>
                  )
                })}
              </div>
            </div>
            {/* Boosters setting - only for Sealed */}
            {!isDraft && (
              <div className={styles.settingsRow}>
                <span className={styles.settingsLabel}>Boosters per player</span>
                <select
                  value={lobbyState.settings.boosterCount}
                  onChange={(e) => updateLobbySettings({ boosterCount: Number(e.target.value) })}
                  className={styles.settingsSelect}
                >
                  {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map((n) => (
                    <option key={n} value={n}>{n}</option>
                  ))}
                </select>
              </div>
            )}
            {/* Packs per player - only for Draft */}
            {isDraft && (
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
            {/* Pick timer setting - only for Draft */}
            {isDraft && (
              <div className={styles.settingsRow}>
                <span className={styles.settingsLabel}>Pick timer (seconds)</span>
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
            {/* Pick 2 mode - only for Draft */}
            {isDraft && (
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
          </div>
        )}

        {/* Player list */}
        <div className={styles.playerListPanel}>
          <div className={styles.playerListHeader}>
            <span className={styles.playerListTitle}>Players</span>
            <span className={styles.playerCount}>
              {lobbyState.players.length} / {lobbyState.settings.maxPlayers || 8}
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
                {player.isHost && (
                  <span className={styles.hostBadge}>Host</span>
                )}
              </div>
              <span className={`${styles.playerStatus} ${
                !player.isConnected
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
            </div>
          ))}
          {lobbyState.players.length === 0 && (
            <div className={styles.emptyPlayerList}>
              Waiting for players to join...
            </div>
          )}
        </div>

        {/* Actions */}
        <div className={styles.actionsRow}>
          {isWaiting && lobbyState.isHost && (
            <button
              onClick={startLobby}
              disabled={!canStart}
              title={
                !hasSelectedSets
                  ? 'Select at least one set'
                  : lobbyState.players.length < 2
                    ? 'Need at least 2 players'
                    : undefined
              }
              className={styles.startButton}
            >
              {isDraft ? 'Start Draft' : 'Start Game'}
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

function TournamentOverlay({
  tournamentState,
}: {
  tournamentState: TournamentState
}) {
  const playerId = useGameStore((state) => state.playerId)
  const spectateGame = useGameStore((state) => state.spectateGame)
  const readyForNextRound = useGameStore((state) => state.readyForNextRound)
  const leaveTournament = useGameStore((state) => state.leaveTournament)
  const unsubmitDeck = useGameStore((state) => state.unsubmitDeck)
  const [hoveredStanding, setHoveredStanding] = useState<HoveredStanding | null>(null)

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
    if (isWaitingForReady && tournamentState.nextRoundHasBye && !isPlayerReady && tournamentState.currentRound > 0) {
      readyForNextRound()
    }
  }, [isWaitingForReady, tournamentState.nextRoundHasBye, isPlayerReady, tournamentState.currentRound, readyForNextRound])

  return (
    <div className={styles.tournamentOverlay}>
      <h1 className={styles.title}>
        {tournamentState.isComplete ? 'Tournament Complete' : 'Tournament Standings'}
      </h1>

      {!tournamentState.isComplete && (
        <p className={styles.waitingSubtitle}>
          {isWaitingForReady
            ? `Starting Round ${tournamentState.currentRound + 1} of ${tournamentState.totalRounds}`
            : `Round ${tournamentState.currentRound} of ${tournamentState.totalRounds}`}
        </p>
      )}

      {/* Next opponent info or BYE status */}
      {isWaitingForReady && !tournamentState.nextRoundHasBye && (
        <div className={styles.statusBoxMatch}>
          <div className={styles.statusBoxMatchLabel}>
            Round {tournamentState.currentRound + 1}
          </div>
          <div className={styles.statusBoxMatchOpponent}>
            {tournamentState.nextOpponentName
              ? `vs ${tournamentState.nextOpponentName}`
              : 'Waiting for matchup...'}
          </div>
        </div>
      )}

      {/* BYE status - shown when waiting for ready with bye, or during active round with bye */}
      {((isWaitingForReady && tournamentState.nextRoundHasBye) || (tournamentState.isBye && !isWaitingForReady)) && (
        <div className={styles.statusBoxBye}>
          <span className={styles.byeIcon}>&#x2713;</span>
          <span>Sitting out this round</span>
        </div>
      )}

      {/* Ready for next round button and status */}
      {isWaitingForReady && (
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12 }}>
          <div style={{ display: 'flex', gap: 12 }}>
            <button
              onClick={readyForNextRound}
              disabled={isPlayerReady}
              className={styles.readyButton}
            >
              {isPlayerReady ? '✓ Ready' : 'Ready for Next Round'}
            </button>
            {/* Show Edit Deck button before first round starts (can go back to deck building) */}
            {tournamentState.currentRound === 0 && !isPlayerReady && (
              <button
                onClick={unsubmitDeck}
                className={styles.editDeckButton}
              >
                Edit Deck
              </button>
            )}
          </div>
          <p className={styles.readyCount}>
            {readyCount} of {totalPlayers} players ready
          </p>
        </div>
      )}

      {/* Show "waiting for others" message when in active round but match is done */}
      {!isWaitingForReady && !tournamentState.isBye && !tournamentState.currentMatchGameSessionId && !tournamentState.isComplete && (
        <div className={styles.statusBoxWaiting}>
          Waiting for other matches to complete...
        </div>
      )}

      {/* Active matches for spectating - show for any player waiting (bye or game finished) */}
      {!tournamentState.currentMatchGameSessionId && tournamentState.activeMatches && tournamentState.activeMatches.length > 0 && (
        <div className={styles.matchesSection}>
          <h3 className={styles.matchesSectionTitle}>
            Live Matches - Click to Watch
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

      {/* Standings table */}
      <div className={styles.standingsTable}>
        <table className={styles.standingsTableInner}>
          <thead className={styles.standingsHeader}>
            <tr>
              <th className={styles.standingsTh}>#</th>
              <th className={styles.standingsThLeft}>Player</th>
              <th className={styles.standingsTh}>W</th>
              <th className={styles.standingsTh}>L</th>
              <th className={styles.standingsTh}>D</th>
              <th className={styles.standingsTh}>Pts</th>
              <th className={styles.standingsTh} title="Game Win Rate">GWR</th>
              {isWaitingForReady && <th className={styles.standingsTh}>Ready</th>}
            </tr>
          </thead>
          <tbody>
            {tournamentState.standings.map((standing, index) => {
              const isMe = standing.playerId === playerId
              const isReady = tournamentState.readyPlayerIds.includes(standing.playerId)
              // Use server-provided rank or fall back to index+1
              const displayRank = standing.rank ?? index + 1

              // Calculate game win rate
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
                  <td className={`${styles.standingsTd} ${styles.standingsRank} ${
                    displayRank === 1 ? styles.standingsRankFirst :
                    displayRank === 2 ? styles.standingsRankSecond :
                    displayRank === 3 ? styles.standingsRankThird : ''
                  }`}>
                    {displayRank}
                    {standing.tiebreakerReason === 'TIED' && (
                      <span className={styles.tiedIndicator}>*</span>
                    )}
                  </td>
                  <td className={styles.standingsTdLeft} style={{ fontWeight: isMe ? 600 : 400 }}>
                    {standing.playerName}
                    {isMe && <span className={styles.meIndicator}>(you)</span>}
                    {!standing.isConnected && (
                      <span className={styles.disconnectedIndicator}>DC</span>
                    )}
                  </td>
                  <td className={`${styles.standingsTd} ${styles.standingsWins}`}>{standing.wins}</td>
                  <td className={`${styles.standingsTd} ${styles.standingsLosses}`}>{standing.losses}</td>
                  <td className={`${styles.standingsTd} ${styles.standingsDraws}`}>{standing.draws}</td>
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

      {/* Last round results */}
      {tournamentState.lastRoundResults && (
        <div className={styles.resultsSection}>
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

      {/* Leave/Return button */}
      <button
        onClick={leaveTournament}
        className={tournamentState.isComplete ? styles.returnButton : styles.leaveButton}
        style={{ marginTop: tournamentState.isComplete ? 0 : 8 }}
      >
        {tournamentState.isComplete ? 'Return to Menu' : 'Leave Tournament'}
      </button>
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
