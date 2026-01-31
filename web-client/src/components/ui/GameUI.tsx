import { useState, useEffect } from 'react'
import { useGameStore, type LobbyState, type TournamentState } from '../../store/gameStore'
import { useResponsive, type ResponsiveSizes } from '../../hooks/useResponsive'

type GameMode = 'normal' | 'sealed'

// Available sets for sealed play
const AVAILABLE_SETS = [
  { code: 'POR', name: 'Portal' },
]

/**
 * Connection/lobby UI - shown when not in a game.
 * Combat mode and game UI are handled in App.tsx and GameBoard.tsx.
 */
export function GameUI() {
  const connectionStatus = useGameStore((state) => state.connectionStatus)
  const sessionId = useGameStore((state) => state.sessionId)
  const lastError = useGameStore((state) => state.lastError)
  const deckBuildingState = useGameStore((state) => state.deckBuildingState)

  // Don't show connection overlay if actively building deck (but show during 'waiting' phase)
  if (deckBuildingState && deckBuildingState.phase !== 'waiting') return null

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
  const createSealedLobby = useGameStore((state) => state.createSealedLobby)
  const joinLobby = useGameStore((state) => state.joinLobby)
  const lobbyState = useGameStore((state) => state.lobbyState)
  const [joinSessionId, setJoinSessionId] = useState('')
  const [gameMode, setGameMode] = useState<GameMode>('normal')
  const [selectedSet, setSelectedSet] = useState(AVAILABLE_SETS[0]?.code ?? 'POR')
  const [playerName, setPlayerName] = useState(() => sessionStorage.getItem('argentum-player-name') || '')

  const [nameConfirmed, setNameConfirmed] = useState(() => !!sessionStorage.getItem('argentum-player-name'))

  const confirmName = () => {
    if (playerName.trim()) {
      sessionStorage.setItem('argentum-player-name', playerName.trim())
      setNameConfirmed(true)
      connect(playerName.trim())
    }
  }
  const responsive = useResponsive()

  // Empty deck triggers server-side random deck generation from Portal set
  const randomDeck = {}

  const handleCreate = () => {
    if (gameMode === 'sealed') {
      createSealedLobby(selectedSet)
    } else {
      createGame(randomDeck)
    }
  }

  const handleJoin = () => {
    if (joinSessionId.trim()) {
      if (gameMode === 'sealed') {
        joinLobby(joinSessionId.trim())
      } else {
        joinGame(joinSessionId.trim(), randomDeck)
      }
    }
  }

  // Show tournament UI if we're in a tournament (even without lobbyState)
  const tournamentState = useGameStore((state) => state.tournamentState)
  if (tournamentState) {
    return <TournamentOverlay tournamentState={tournamentState} responsive={responsive} />
  }

  // Show lobby UI if we're in a lobby
  if (lobbyState) {
    return <LobbyOverlay lobbyState={lobbyState} responsive={responsive} />
  }

  return (
    <div
      style={{
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.9)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        color: 'white',
        gap: responsive.isMobile ? 16 : 24,
        padding: responsive.containerPadding,
        pointerEvents: 'auto',
      }}
    >
      <FullscreenButton responsive={responsive} />
      <h1 style={{ margin: 0, fontSize: responsive.fontSize.xlarge }}>Argentum Engine</h1>

      {error && (
        <p style={{ color: '#ff4444', fontSize: responsive.fontSize.normal }}>Error: {error}</p>
      )}

      {!nameConfirmed && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12, alignItems: 'center', width: '100%', maxWidth: 400 }}>
          <label style={{ color: '#888', fontSize: responsive.fontSize.normal }}>Enter your name</label>
          <input
            type="text"
            value={playerName}
            onChange={(e) => setPlayerName(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') confirmName() }}
            placeholder="Your name"
            autoFocus
            maxLength={20}
            style={{
              padding: responsive.isMobile ? '10px 12px' : '12px 16px',
              fontSize: responsive.fontSize.large,
              backgroundColor: '#222',
              color: 'white',
              border: '1px solid #444',
              borderRadius: 8,
              textAlign: 'center',
              width: '100%',
              maxWidth: 250,
            }}
          />
          <button
            onClick={confirmName}
            disabled={!playerName.trim()}
            style={{
              padding: responsive.isMobile ? '10px 20px' : '12px 24px',
              fontSize: responsive.fontSize.large,
              backgroundColor: playerName.trim() ? '#0066cc' : '#333',
              color: 'white',
              border: 'none',
              borderRadius: 8,
              cursor: playerName.trim() ? 'pointer' : 'not-allowed',
            }}
          >
            Continue
          </button>
        </div>
      )}

      {status === 'connected' && !sessionId && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: responsive.isMobile ? 16 : 24, alignItems: 'center', width: '100%', maxWidth: 400 }}>
          {/* Game Mode Toggle */}
          <div
            style={{
              display: 'flex',
              backgroundColor: '#222',
              borderRadius: 8,
              padding: 4,
              gap: 4,
            }}
          >
            <ModeButton
              label="Quick Game"
              active={gameMode === 'normal'}
              onClick={() => setGameMode('normal')}
              responsive={responsive}
              title="Play with a random deck"
            />
            <ModeButton
              label="Sealed Lobby"
              active={gameMode === 'sealed'}
              onClick={() => setGameMode('sealed')}
              responsive={responsive}
            />
          </div>

          {/* Set selector (only shown for sealed mode) */}
          {gameMode === 'sealed' && (
            <div
              style={{
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                gap: 8,
                width: '100%',
              }}
            >
              <label style={{ color: '#888', fontSize: responsive.fontSize.small }}>
                Select Set:
              </label>
              <select
                value={selectedSet}
                onChange={(e) => setSelectedSet(e.target.value)}
                style={{
                  padding: responsive.isMobile ? '10px 12px' : '12px 16px',
                  fontSize: responsive.fontSize.normal,
                  backgroundColor: '#222',
                  color: 'white',
                  border: '1px solid #444',
                  borderRadius: 8,
                  cursor: 'pointer',
                  width: '100%',
                  maxWidth: 200,
                }}
              >
                {AVAILABLE_SETS.map((set) => (
                  <option key={set.code} value={set.code}>
                    {set.name} ({set.code})
                  </option>
                ))}
              </select>
              <p style={{ color: '#666', fontSize: responsive.fontSize.small, margin: 0, textAlign: 'center' }}>
                Create a lobby for up to 8 players, build decks, then play a round-robin tournament
              </p>
            </div>
          )}

          <button
            onClick={handleCreate}
            style={{
              padding: responsive.isMobile ? '10px 20px' : '12px 24px',
              fontSize: responsive.fontSize.large,
              backgroundColor: gameMode === 'sealed' ? '#e65100' : '#0066cc',
              color: 'white',
              border: 'none',
              borderRadius: 8,
              cursor: 'pointer',
              width: '100%',
              maxWidth: 200,
            }}
          >
            {gameMode === 'sealed' ? 'Create Lobby' : 'Create Game'}
          </button>

          <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#666' }}>
            <div style={{ width: 40, height: 1, backgroundColor: '#666' }} />
            <span style={{ fontSize: responsive.fontSize.small }}>or join existing</span>
            <div style={{ width: 40, height: 1, backgroundColor: '#666' }} />
          </div>

          <div style={{ display: 'flex', gap: 8, flexDirection: responsive.isMobile ? 'column' : 'row', width: '100%' }}>
            <input
              type="text"
              value={joinSessionId}
              onChange={(e) => setJoinSessionId(e.target.value)}
              onKeyDown={(e) => e.key === 'Enter' && handleJoin()}
              placeholder={gameMode === 'sealed' ? 'Enter Lobby ID' : 'Enter Session ID'}
              style={{
                padding: responsive.isMobile ? '10px 12px' : '12px 16px',
                fontSize: responsive.fontSize.normal,
                backgroundColor: '#222',
                color: 'white',
                border: '1px solid #444',
                borderRadius: 8,
                flex: 1,
                minWidth: 0,
              }}
            />
            <button
              onClick={handleJoin}
              disabled={!joinSessionId.trim()}
              style={{
                padding: responsive.isMobile ? '10px 20px' : '12px 24px',
                fontSize: responsive.fontSize.large,
                backgroundColor: joinSessionId.trim() ? '#00aa44' : '#333',
                color: 'white',
                border: 'none',
                borderRadius: 8,
                cursor: joinSessionId.trim() ? 'pointer' : 'not-allowed',
                whiteSpace: 'nowrap',
              }}
            >
              Join
            </button>
          </div>
        </div>
      )}

      {sessionId && (
        <WaitingForOpponent sessionId={sessionId} responsive={responsive} />
      )}
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
  responsive,
  title,
}: {
  label: string
  active: boolean
  onClick: () => void
  responsive: ReturnType<typeof useResponsive>
  title?: string
}) {
  return (
    <button
      onClick={onClick}
      title={title}
      style={{
        padding: responsive.isMobile ? '8px 12px' : '10px 16px',
        fontSize: responsive.fontSize.normal,
        backgroundColor: active ? '#4fc3f7' : 'transparent',
        color: active ? '#000' : '#888',
        border: 'none',
        borderRadius: 6,
        cursor: 'pointer',
        fontWeight: active ? 600 : 400,
        transition: 'all 0.15s',
      }}
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
  responsive,
}: {
  sessionId: string
  responsive: ReturnType<typeof useResponsive>
}) {
  const cancelGame = useGameStore((state) => state.cancelGame)
  const [copied, setCopied] = useState(false)

  const copySessionId = () => {
    navigator.clipboard.writeText(sessionId)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div style={{ textAlign: 'center' }}>
      <p style={{ fontSize: responsive.fontSize.normal }}>Game Created!</p>
      <div
        onClick={copySessionId}
        style={{
          display: 'inline-flex',
          alignItems: 'center',
          gap: 12,
          padding: '12px 16px',
          backgroundColor: copied ? 'rgba(79, 195, 247, 0.08)' : 'rgba(255, 255, 255, 0.03)',
          border: copied ? '1px solid rgba(79, 195, 247, 0.3)' : '1px solid rgba(255, 255, 255, 0.08)',
          borderRadius: 10,
          cursor: 'pointer',
          transition: 'all 0.2s',
          marginBottom: 12,
        }}
      >
        <div style={{
          fontFamily: 'monospace',
          fontSize: responsive.isMobile ? 15 : 18,
          color: '#ddd',
          fontWeight: 500,
          letterSpacing: '0.04em',
        }}>
          {sessionId}
        </div>
        <span style={{
          color: copied ? '#4fc3f7' : '#555',
          fontSize: 12,
          transition: 'color 0.2s',
        }}>
          {copied ? 'Copied!' : 'Copy'}
        </span>
      </div>
      <p style={{ color: '#888', fontSize: responsive.fontSize.normal }}>
        Waiting for opponent to join...
      </p>
      <button
        onClick={cancelGame}
        style={{
          marginTop: 20,
          padding: '8px 16px',
          fontSize: responsive.fontSize.normal,
          backgroundColor: '#c0392b',
          color: 'white',
          border: 'none',
          borderRadius: 4,
          cursor: 'pointer',
        }}
      >
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
  responsive,
}: {
  lobbyState: LobbyState
  responsive: ReturnType<typeof useResponsive>
}) {
  const startSealedLobby = useGameStore((state) => state.startSealedLobby)
  const leaveLobby = useGameStore((state) => state.leaveLobby)
  const updateLobbySettings = useGameStore((state) => state.updateLobbySettings)
  const tournamentState = useGameStore((state) => state.tournamentState)
  const [copied, setCopied] = useState(false)

  // Show tournament standings when tournament is active
  if (tournamentState) {
    return <TournamentOverlay tournamentState={tournamentState} responsive={responsive} />
  }

  const isWaiting = lobbyState.state === 'WAITING_FOR_PLAYERS'
  const canStart = lobbyState.players.length >= 2

  const copyLobbyId = () => {
    navigator.clipboard.writeText(lobbyState.lobbyId)
    setCopied(true)
    setTimeout(() => setCopied(false), 2000)
  }

  return (
    <div
      style={{
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        background: 'linear-gradient(135deg, #0a0a12 0%, #12101e 50%, #0a0a12 100%)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        color: 'white',
        padding: responsive.containerPadding,
      }}
    >
      <FullscreenButton responsive={responsive} />
      <div style={{
        width: '100%',
        maxWidth: 500,
        display: 'flex',
        flexDirection: 'column',
        gap: 16,
      }}>
        {/* Header */}
        <div style={{ textAlign: 'center', marginBottom: 8 }}>
          <div style={{
            fontSize: 11,
            fontWeight: 600,
            color: '#e65100',
            textTransform: 'uppercase',
            letterSpacing: '0.12em',
            marginBottom: 8,
          }}>
            Sealed Draft
          </div>
          <h1 style={{
            margin: '0 0 6px 0',
            fontSize: responsive.isMobile ? 26 : 32,
            fontWeight: 700,
            letterSpacing: '-0.02em',
            background: 'linear-gradient(180deg, #fff 0%, #aaa 100%)',
            WebkitBackgroundClip: 'text',
            WebkitTextFillColor: 'transparent',
          }}>
            {lobbyState.settings.setName || 'Sealed Lobby'}
          </h1>
          <p style={{ color: '#555', fontSize: responsive.fontSize.small, margin: 0 }}>
            {lobbyState.settings.boosterCount} boosters per player
            {(lobbyState.settings.gamesPerMatch ?? 1) > 1 && ` · ${lobbyState.settings.gamesPerMatch} games per matchup`}
          </p>
        </div>

        {/* Invite code */}
        <div
          onClick={copyLobbyId}
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '12px 16px',
            backgroundColor: copied ? 'rgba(79, 195, 247, 0.08)' : 'rgba(255, 255, 255, 0.03)',
            border: copied ? '1px solid rgba(79, 195, 247, 0.3)' : '1px solid rgba(255, 255, 255, 0.08)',
            borderRadius: 10,
            cursor: 'pointer',
            transition: 'all 0.2s',
          }}
        >
          <div>
            <div style={{ color: '#666', fontSize: 10, textTransform: 'uppercase', letterSpacing: '0.08em', marginBottom: 3 }}>
              Invite Code
            </div>
            <div style={{
              fontFamily: 'monospace',
              fontSize: responsive.isMobile ? 15 : 18,
              color: '#ddd',
              fontWeight: 500,
              letterSpacing: '0.04em',
            }}>
              {lobbyState.lobbyId}
            </div>
          </div>
          <span style={{
            color: copied ? '#4fc3f7' : '#555',
            fontSize: 12,
            flexShrink: 0,
            marginLeft: 12,
            transition: 'color 0.2s',
          }}>
            {copied ? 'Copied!' : 'Copy'}
          </span>
        </div>

        {/* Settings (host only) */}
        {isWaiting && lobbyState.isHost && (
          <div style={{
            display: 'flex',
            flexDirection: 'column',
            gap: 0,
            backgroundColor: 'rgba(255, 255, 255, 0.03)',
            border: '1px solid rgba(255, 255, 255, 0.08)',
            borderRadius: 10,
            overflow: 'hidden',
          }}>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '12px 16px',
              borderBottom: '1px solid rgba(255, 255, 255, 0.04)',
            }}>
              <span style={{ color: '#888', fontSize: responsive.fontSize.small }}>
                Boosters per player
              </span>
              <select
                value={lobbyState.settings.boosterCount}
                onChange={(e) => updateLobbySettings({ boosterCount: Number(e.target.value) })}
                style={{
                  padding: '5px 10px',
                  fontSize: responsive.fontSize.small,
                  backgroundColor: '#1a1a24',
                  color: 'white',
                  border: '1px solid rgba(255, 255, 255, 0.1)',
                  borderRadius: 6,
                  cursor: 'pointer',
                  outline: 'none',
                }}
              >
                {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map((n) => (
                  <option key={n} value={n}>{n}</option>
                ))}
              </select>
            </div>
            <div style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'space-between',
              padding: '12px 16px',
            }}>
              <span style={{ color: '#888', fontSize: responsive.fontSize.small }}>
                Games per matchup
              </span>
              <select
                value={lobbyState.settings.gamesPerMatch ?? 1}
                onChange={(e) => updateLobbySettings({ gamesPerMatch: Number(e.target.value) })}
                style={{
                  padding: '5px 10px',
                  fontSize: responsive.fontSize.small,
                  backgroundColor: '#1a1a24',
                  color: 'white',
                  border: '1px solid rgba(255, 255, 255, 0.1)',
                  borderRadius: 6,
                  cursor: 'pointer',
                  outline: 'none',
                }}
              >
                {[1, 2, 3, 4, 5].map((n) => (
                  <option key={n} value={n}>{n}</option>
                ))}
              </select>
            </div>
          </div>
        )}

        {/* Player list */}
        <div style={{
          backgroundColor: 'rgba(255, 255, 255, 0.03)',
          border: '1px solid rgba(255, 255, 255, 0.08)',
          borderRadius: 10,
          overflow: 'hidden',
        }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '12px 16px',
            borderBottom: '1px solid rgba(255, 255, 255, 0.06)',
          }}>
            <span style={{ fontSize: responsive.fontSize.normal, fontWeight: 600, color: '#ccc' }}>Players</span>
            <span style={{ color: '#444', fontSize: responsive.fontSize.small }}>
              {lobbyState.players.length} / {lobbyState.settings.maxPlayers || 8}
            </span>
          </div>
          {lobbyState.players.map((player, i) => (
            <div
              key={player.playerId}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '11px 16px',
                borderBottom: i < lobbyState.players.length - 1 ? '1px solid rgba(255, 255, 255, 0.04)' : 'none',
                transition: 'background-color 0.15s',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <div style={{
                  width: 8,
                  height: 8,
                  borderRadius: '50%',
                  backgroundColor: !player.isConnected ? '#ff4444' : '#00cc44',
                  boxShadow: !player.isConnected ? '0 0 6px rgba(255, 68, 68, 0.4)' : '0 0 6px rgba(0, 204, 68, 0.4)',
                  flexShrink: 0,
                }} />
                <span style={{ fontSize: responsive.fontSize.normal, color: '#ddd' }}>
                  {player.playerName}
                </span>
                {player.isHost && (
                  <span style={{
                    fontSize: 9,
                    fontWeight: 700,
                    color: '#e65100',
                    backgroundColor: 'rgba(230, 81, 0, 0.12)',
                    padding: '2px 7px',
                    borderRadius: 4,
                    textTransform: 'uppercase',
                    letterSpacing: '0.06em',
                  }}>
                    Host
                  </span>
                )}
              </div>
              <span style={{
                fontSize: responsive.fontSize.small,
                color: !player.isConnected ? '#ff4444' : player.deckSubmitted ? '#4caf50' : '#555',
                fontWeight: player.deckSubmitted ? 500 : 400,
              }}>
                {!player.isConnected
                  ? 'Disconnected'
                  : player.deckSubmitted
                    ? 'Deck Ready'
                    : 'Joined'}
              </span>
            </div>
          ))}
          {lobbyState.players.length === 0 && (
            <div style={{ padding: '24px 16px', textAlign: 'center', color: '#333', fontSize: responsive.fontSize.small }}>
              Waiting for players to join...
            </div>
          )}
        </div>

        {/* Actions */}
        <div style={{ display: 'flex', gap: 10, marginTop: 4 }}>
          {isWaiting && lobbyState.isHost && (
            <button
              onClick={startSealedLobby}
              disabled={!canStart}
              style={{
                flex: 1,
                padding: '13px 20px',
                fontSize: responsive.fontSize.normal,
                fontWeight: 600,
                background: canStart
                  ? 'linear-gradient(180deg, #e65100 0%, #bf4300 100%)'
                  : '#1a1a24',
                color: canStart ? 'white' : '#444',
                border: canStart ? 'none' : '1px solid rgba(255, 255, 255, 0.08)',
                borderRadius: 10,
                cursor: canStart ? 'pointer' : 'not-allowed',
                transition: 'all 0.15s',
                boxShadow: canStart ? '0 2px 12px rgba(230, 81, 0, 0.3)' : 'none',
              }}
            >
              Start Game
            </button>
          )}
          <button
            onClick={leaveLobby}
            style={{
              padding: '13px 20px',
              fontSize: responsive.fontSize.normal,
              backgroundColor: 'transparent',
              color: '#666',
              border: '1px solid rgba(255, 255, 255, 0.08)',
              borderRadius: 10,
              cursor: 'pointer',
              transition: 'all 0.15s',
            }}
          >
            Leave
          </button>
        </div>

        {isWaiting && !lobbyState.isHost && (
          <p style={{ color: '#444', fontSize: responsive.fontSize.small, textAlign: 'center', margin: 0 }}>
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
function TournamentOverlay({
  tournamentState,
  responsive,
}: {
  tournamentState: TournamentState
  responsive: ReturnType<typeof useResponsive>
}) {
  const playerId = useGameStore((state) => state.playerId)
  const spectateGame = useGameStore((state) => state.spectateGame)
  const readyForNextRound = useGameStore((state) => state.readyForNextRound)

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
  useEffect(() => {
    if (isWaitingForReady && tournamentState.nextRoundHasBye && !isPlayerReady) {
      readyForNextRound()
    }
  }, [isWaitingForReady, tournamentState.nextRoundHasBye, isPlayerReady, readyForNextRound])

  return (
    <div
      style={{
        position: 'absolute',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.9)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        color: 'white',
        gap: 16,
        padding: responsive.containerPadding,
        overflow: 'auto',
      }}
    >
      <h1 style={{ margin: 0, fontSize: responsive.fontSize.xlarge }}>
        {tournamentState.isComplete ? 'Tournament Complete' : 'Tournament Standings'}
      </h1>

      {!tournamentState.isComplete && (
        <p style={{ color: '#888', fontSize: responsive.fontSize.normal }}>
          {isWaitingForReady
            ? `Starting Round ${tournamentState.currentRound + 1} of ${tournamentState.totalRounds}`
            : `Round ${tournamentState.currentRound} of ${tournamentState.totalRounds}`}
        </p>
      )}

      {/* Next opponent info */}
      {isWaitingForReady && (
        <div
          style={{
            backgroundColor: '#1a2a4e',
            padding: '16px 24px',
            borderRadius: 8,
            fontSize: responsive.fontSize.normal,
            textAlign: 'center',
            border: '1px solid #3a4a6e',
          }}
        >
          <div style={{ color: '#888', fontSize: responsive.fontSize.small, marginBottom: 4 }}>
            Round {tournamentState.currentRound + 1}
          </div>
          <div style={{ fontWeight: 600, fontSize: responsive.fontSize.large }}>
            {tournamentState.nextRoundHasBye
              ? 'You have a BYE'
              : tournamentState.nextOpponentName
                ? `vs ${tournamentState.nextOpponentName}`
                : 'Waiting for matchup...'}
          </div>
        </div>
      )}

      {tournamentState.isBye && !isWaitingForReady && (
        <div
          style={{
            backgroundColor: '#1a472a',
            padding: '12px 24px',
            borderRadius: 8,
            fontSize: responsive.fontSize.normal,
            marginBottom: 8,
          }}
        >
          You have a BYE this round
        </div>
      )}

      {/* Ready for next round button and status */}
      {isWaitingForReady && (
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 12 }}>
          <button
            onClick={readyForNextRound}
            disabled={isPlayerReady}
            style={{
              padding: '14px 32px',
              fontSize: responsive.fontSize.large,
              fontWeight: 600,
              backgroundColor: isPlayerReady ? '#1a472a' : '#e65100',
              color: 'white',
              border: isPlayerReady ? '2px solid #2a6a3a' : 'none',
              borderRadius: 10,
              cursor: isPlayerReady ? 'default' : 'pointer',
              transition: 'all 0.2s',
              boxShadow: isPlayerReady ? 'none' : '0 2px 12px rgba(230, 81, 0, 0.3)',
            }}
          >
            {isPlayerReady ? '✓ Ready' : 'Ready for Next Round'}
          </button>
          <p style={{ color: '#888', fontSize: responsive.fontSize.small, margin: 0 }}>
            {readyCount} of {totalPlayers} players ready
          </p>
        </div>
      )}

      {/* Show "waiting for others" message when in active round but match is done */}
      {!isWaitingForReady && !tournamentState.isBye && !tournamentState.currentMatchGameSessionId && !tournamentState.isComplete && (
        <div
          style={{
            backgroundColor: '#2a2a4e',
            padding: '12px 24px',
            borderRadius: 8,
            fontSize: responsive.fontSize.normal,
            marginBottom: 8,
          }}
        >
          Waiting for other matches to complete...
        </div>
      )}

      {/* Active matches for spectating - show for any player waiting (bye or game finished) */}
      {!tournamentState.currentMatchGameSessionId && tournamentState.activeMatches && tournamentState.activeMatches.length > 0 && (
        <div style={{ width: '100%', maxWidth: 500 }}>
          <h3 style={{ margin: '0 0 8px 0', fontSize: responsive.fontSize.normal, color: '#888' }}>
            Live Matches - Click to Watch
          </h3>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {tournamentState.activeMatches.map((match) => (
              <button
                key={match.gameSessionId}
                onClick={() => spectateGame(match.gameSessionId)}
                style={{
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  padding: '12px 16px',
                  backgroundColor: '#1a1a2e',
                  border: '1px solid #333',
                  borderRadius: 8,
                  color: 'white',
                  cursor: 'pointer',
                  transition: 'background-color 0.2s',
                }}
                onMouseEnter={(e) => (e.currentTarget.style.backgroundColor = '#2a2a4e')}
                onMouseLeave={(e) => (e.currentTarget.style.backgroundColor = '#1a1a2e')}
              >
                <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                  <span style={{ fontWeight: 600 }}>{match.player1Name}</span>
                  <span style={{ color: '#888' }}>vs</span>
                  <span style={{ fontWeight: 600 }}>{match.player2Name}</span>
                </div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 14 }}>
                  <span style={{ color: match.player1Life <= 5 ? '#ff4444' : '#4caf50' }}>
                    {match.player1Life}
                  </span>
                  <span style={{ color: '#666' }}>-</span>
                  <span style={{ color: match.player2Life <= 5 ? '#ff4444' : '#4caf50' }}>
                    {match.player2Life}
                  </span>
                  <span style={{ color: '#4fc3f7', marginLeft: 8 }}>▶ Watch</span>
                </div>
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Standings table */}
      <div
        style={{
          width: '100%',
          maxWidth: 500,
          backgroundColor: '#111',
          borderRadius: 8,
          overflow: 'hidden',
        }}
      >
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ backgroundColor: '#222' }}>
              <th style={thStyle}>#</th>
              <th style={{ ...thStyle, textAlign: 'left' }}>Player</th>
              <th style={thStyle}>W</th>
              <th style={thStyle}>L</th>
              <th style={thStyle}>D</th>
              <th style={thStyle}>Pts</th>
              {isWaitingForReady && <th style={thStyle}>Ready</th>}
            </tr>
          </thead>
          <tbody>
            {tournamentState.standings.map((standing, index) => {
              const isMe = standing.playerId === playerId
              const isReady = tournamentState.readyPlayerIds.includes(standing.playerId)
              return (
                <tr
                  key={standing.playerId}
                  style={{
                    backgroundColor: isMe ? '#1a1a3a' : 'transparent',
                    borderBottom: '1px solid #222',
                  }}
                >
                  <td style={tdStyle}>{index + 1}</td>
                  <td style={{ ...tdStyle, textAlign: 'left', fontWeight: isMe ? 600 : 400 }}>
                    {standing.playerName}
                    {isMe && <span style={{ color: '#4fc3f7', marginLeft: 6 }}>(you)</span>}
                    {!standing.isConnected && (
                      <span style={{ color: '#ff4444', marginLeft: 6, fontSize: 12 }}>DC</span>
                    )}
                  </td>
                  <td style={{ ...tdStyle, color: '#00cc44' }}>{standing.wins}</td>
                  <td style={{ ...tdStyle, color: '#ff4444' }}>{standing.losses}</td>
                  <td style={tdStyle}>{standing.draws}</td>
                  <td style={{ ...tdStyle, fontWeight: 600 }}>{standing.points}</td>
                  {isWaitingForReady && (
                    <td style={{ ...tdStyle, color: isReady ? '#4caf50' : '#666' }}>
                      {standing.isConnected ? (isReady ? '✓' : '...') : '-'}
                    </td>
                  )}
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      {/* Last round results */}
      {tournamentState.lastRoundResults && (
        <div style={{ width: '100%', maxWidth: 500 }}>
          <h3 style={{ margin: '0 0 8px 0', fontSize: responsive.fontSize.normal, color: '#888' }}>
            Round {tournamentState.currentRound} Results
          </h3>
          {tournamentState.lastRoundResults.map((result, i) => (
            <div
              key={i}
              style={{
                display: 'flex',
                justifyContent: 'space-between',
                padding: '6px 12px',
                backgroundColor: '#111',
                borderRadius: 4,
                marginBottom: 4,
                fontSize: responsive.fontSize.small,
              }}
            >
              <span>{result.player1Name}</span>
              <span style={{ color: '#888' }}>
                {result.isBye
                  ? 'BYE'
                  : result.isDraw
                    ? 'Draw'
                    : `Winner: ${result.winnerId === result.player1Name ? result.player1Name : result.player2Name}`}
              </span>
              <span>{result.isBye ? '' : result.player2Name}</span>
            </div>
          ))}
        </div>
      )}

      {tournamentState.isComplete && (
        <button
          onClick={() => window.location.reload()}
          style={{
            padding: '12px 24px',
            fontSize: responsive.fontSize.large,
            backgroundColor: '#e65100',
            color: 'white',
            border: 'none',
            borderRadius: 8,
            cursor: 'pointer',
          }}
        >
          Return to Menu
        </button>
      )}
    </div>
  )
}

const thStyle: React.CSSProperties = {
  padding: '10px 12px',
  textAlign: 'center',
  fontSize: 14,
  fontWeight: 600,
  color: '#888',
}

const tdStyle: React.CSSProperties = {
  padding: '10px 12px',
  textAlign: 'center',
  fontSize: 14,
}

/**
 * Fullscreen toggle button.
 */
function FullscreenButton({ responsive }: { responsive: ResponsiveSizes }) {
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
      style={{
        position: 'absolute',
        top: responsive.isMobile ? 8 : 12,
        left: responsive.isMobile ? 8 : 12,
        zIndex: 100,
        padding: responsive.isMobile ? '6px 10px' : '8px 14px',
        fontSize: responsive.fontSize.small,
        backgroundColor: 'rgba(255, 255, 255, 0.1)',
        color: '#888',
        border: '1px solid #444',
        borderRadius: 6,
        cursor: 'pointer',
        display: 'flex',
        alignItems: 'center',
        gap: 4,
      }}
      title={isFullscreen ? 'Exit fullscreen (Esc)' : 'Enter fullscreen'}
    >
      {isFullscreen ? '⛶ Exit' : '⛶ Fullscreen'}
    </button>
  )
}
