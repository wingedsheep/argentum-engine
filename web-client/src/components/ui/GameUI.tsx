import { useState, useEffect, useCallback } from 'react'
import { useGameStore, type LobbyState, type TournamentState } from '../../store/gameStore'
import type { SealedCardInfo } from '../../types'
import { getCardImageUrl } from '../../utils/cardImages'
import { ManaCost } from './ManaSymbols'
import { randomBackground } from '../../utils/background'
import { ReplayViewer, type GameSummary, type SpectatorStateUpdate } from '../admin/ReplayViewer'
import styles from './GameUI.module.css'

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
  const [playerName, setPlayerName] = useState(() => localStorage.getItem('argentum-player-name') || '')

  const [nameConfirmed, setNameConfirmed] = useState(() => !!localStorage.getItem('argentum-player-name'))
  const [showReplays, setShowReplays] = useState(false)

  const confirmName = () => {
    if (playerName.trim()) {
      localStorage.setItem('argentum-player-name', playerName.trim())
      setNameConfirmed(true)
      connect(playerName.trim())
    }
  }

  // Empty deck triggers server-side random deck generation from Portal set
  const randomDeck = {}

  const handleCreate = () => {
    if (gameMode === 'tournament') {
      // Create lobby with default settings - host can change in lobby
      createTournamentLobby(['ONS', 'SCG'], 'SEALED')
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

  const fetchPlayerGames = useCallback(async (): Promise<GameSummary[]> => {
    const token = localStorage.getItem('argentum-token')
    if (!token) throw new Error('No player token')
    const res = await fetch('/api/replays', {
      headers: { 'X-Player-Token': token },
    })
    if (!res.ok) throw new Error(`Server error: ${res.status}`)
    return await res.json() as GameSummary[]
  }, [])

  const fetchPlayerReplay = useCallback(async (gameId: string): Promise<SpectatorStateUpdate[]> => {
    const token = localStorage.getItem('argentum-token')
    if (!token) throw new Error('No player token')
    const res = await fetch(`/api/replays/${gameId}`, {
      headers: { 'X-Player-Token': token },
    })
    if (!res.ok) throw new Error(`Failed to load replay: ${res.status}`)
    return await res.json() as SpectatorStateUpdate[]
  }, [])

  // Show tournament UI if we're in a tournament (even without lobbyState)
  const tournamentState = useGameStore((state) => state.tournamentState)
  if (tournamentState) {
    return <TournamentOverlay tournamentState={tournamentState} />
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

  return (
    <div className={styles.connectionOverlay} style={{ backgroundImage: `url(${randomBackground})` }}>
      <FullscreenButton />
      <div className={styles.contentBackdrop}>
        <h1 className={styles.title}>Argentum Engine</h1>
        <span className={styles.commitHash}>{__COMMIT_HASH__}</span>

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

            <button
              onClick={() => setShowReplays(true)}
              className={styles.replayLinkButton}
            >
              Game Replays
            </button>
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
            <div className={styles.inviteCode} data-testid="invite-code">
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
            {/* Set selection — grouped by block */}
            <div className={styles.settingsRow} style={{ alignItems: 'flex-start' }}>
              <span className={styles.settingsLabel} style={{ paddingTop: 6 }}>Sets</span>
              <div className={styles.setSelectionGrid}>
                {(() => {
                  const sets = lobbyState.settings.availableSets
                  // Group sets: blocks first (preserving order), then ungrouped sets
                  const blockOrder: string[] = []
                  const blockSets = new Map<string, typeof sets[number][]>()
                  const ungrouped: typeof sets[number][] = []
                  for (const set of sets) {
                    if (set.block) {
                      if (!blockSets.has(set.block)) {
                        blockOrder.push(set.block)
                        blockSets.set(set.block, [])
                      }
                      blockSets.get(set.block)!.push(set)
                    } else {
                      ungrouped.push(set)
                    }
                  }
                  const renderSetButton = (set: typeof sets[number]) => {
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
                        {set.name}{set.incomplete ? ' (unfinished)' : ''}
                      </button>
                    )
                  }
                  return (
                    <>
                      {ungrouped.map(renderSetButton)}
                      {blockOrder.map((blockName) => (
                        <div key={blockName} className={styles.blockGroup}>
                          <span className={styles.blockLabel}>{blockName} Block</span>
                          <div className={styles.blockSets}>
                            {blockSets.get(blockName)!.map(renderSetButton)}
                          </div>
                        </div>
                      ))}
                    </>
                  )
                })()}
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
                  {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16].map((n) => (
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
  const deckBuildingState = useGameStore((state) => state.deckBuildingState)
  const disconnectedPlayers = useGameStore((state) => state.disconnectedPlayers)
  const addDisconnectTime = useGameStore((state) => state.addDisconnectTime)
  const kickPlayer = useGameStore((state) => state.kickPlayer)
  const [hoveredStanding, setHoveredStanding] = useState<HoveredStanding | null>(null)
  const [linkCopied, setLinkCopied] = useState(false)
  const [showDeckViewer, setShowDeckViewer] = useState(false)
  const [confirmLeave, setConfirmLeave] = useState(false)
  const [showReplays, setShowReplays] = useState(false)
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

  const fetchTournamentReplay = useCallback(async (gameId: string): Promise<SpectatorStateUpdate[]> => {
    const token = localStorage.getItem('argentum-token')
    if (!token) throw new Error('No player token')
    const res = await fetch(`/api/replays/${gameId}?lobbyId=${tournamentState.lobbyId}`, {
      headers: { 'X-Player-Token': token },
    })
    if (!res.ok) throw new Error(`Failed to load replay: ${res.status}`)
    return await res.json() as SpectatorStateUpdate[]
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
          {!isSpectator && deckBuildingState && (tournamentState.currentRound > 0 || isPlayerReady) && (
            <button onClick={() => setShowDeckViewer(true)} className={styles.trnToolbarBtn}>
              View Deck
            </button>
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
          <button onClick={leaveTournament} className={styles.returnButton}>
            Return to Menu
          </button>
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
