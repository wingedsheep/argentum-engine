import { useState } from 'react'
import { useGameStore, type LobbyState, type TournamentState } from '../../store/gameStore'
import { useResponsive } from '../../hooks/useResponsive'

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
              label="Normal Game"
              active={gameMode === 'normal'}
              onClick={() => setGameMode('normal')}
              responsive={responsive}
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
}: {
  label: string
  active: boolean
  onClick: () => void
  responsive: ReturnType<typeof useResponsive>
}) {
  return (
    <button
      onClick={onClick}
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
  return (
    <div style={{ textAlign: 'center' }}>
      <p style={{ fontSize: responsive.fontSize.normal }}>Game Created!</p>
      <p style={{ fontSize: responsive.isMobile ? 16 : 24, fontFamily: 'monospace', wordBreak: 'break-all' }}>
        {sessionId}
      </p>
      <p style={{ color: '#888', fontSize: responsive.fontSize.normal }}>
        Waiting for opponent to join...
      </p>
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
        backgroundColor: '#0a0a0f',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        color: 'white',
        padding: responsive.containerPadding,
      }}
    >
      <div style={{
        width: '100%',
        maxWidth: 480,
        display: 'flex',
        flexDirection: 'column',
        gap: 20,
      }}>
        {/* Header */}
        <div style={{ textAlign: 'center' }}>
          <h1 style={{
            margin: '0 0 4px 0',
            fontSize: responsive.fontSize.xlarge,
            fontWeight: 700,
            letterSpacing: '-0.02em',
          }}>
            Sealed Lobby
          </h1>
          <p style={{ color: '#666', fontSize: responsive.fontSize.small, margin: 0 }}>
            {lobbyState.settings.setName} &middot; {lobbyState.settings.boosterCount} boosters per player
          </p>
        </div>

        {/* Invite code */}
        <div
          onClick={copyLobbyId}
          style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '10px 16px',
            backgroundColor: '#161620',
            border: '1px solid #2a2a3a',
            borderRadius: 8,
            cursor: 'pointer',
            transition: 'border-color 0.15s',
          }}
        >
          <div>
            <div style={{ color: '#555', fontSize: 11, textTransform: 'uppercase', letterSpacing: '0.05em', marginBottom: 2 }}>
              Lobby Code
            </div>
            <div style={{ fontFamily: 'monospace', fontSize: responsive.isMobile ? 14 : 16, color: '#ccc' }}>
              {lobbyState.lobbyId}
            </div>
          </div>
          <span style={{ color: copied ? '#4fc3f7' : '#555', fontSize: 12, flexShrink: 0, marginLeft: 12 }}>
            {copied ? 'Copied' : 'Click to copy'}
          </span>
        </div>

        {/* Settings (host only) */}
        {isWaiting && lobbyState.isHost && (
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '10px 16px',
            backgroundColor: '#161620',
            border: '1px solid #2a2a3a',
            borderRadius: 8,
          }}>
            <span style={{ color: '#888', fontSize: responsive.fontSize.small }}>
              Boosters per player
            </span>
            <select
              value={lobbyState.settings.boosterCount}
              onChange={(e) => updateLobbySettings({ boosterCount: Number(e.target.value) })}
              style={{
                padding: '4px 8px',
                fontSize: responsive.fontSize.small,
                backgroundColor: '#222',
                color: 'white',
                border: '1px solid #3a3a4a',
                borderRadius: 4,
                cursor: 'pointer',
              }}
            >
              {[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12].map((n) => (
                <option key={n} value={n}>{n}</option>
              ))}
            </select>
          </div>
        )}

        {/* Player list */}
        <div style={{
          backgroundColor: '#161620',
          border: '1px solid #2a2a3a',
          borderRadius: 8,
          overflow: 'hidden',
        }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            padding: '10px 16px',
            borderBottom: '1px solid #2a2a3a',
          }}>
            <span style={{ fontSize: responsive.fontSize.normal, fontWeight: 600 }}>Players</span>
            <span style={{ color: '#555', fontSize: responsive.fontSize.small }}>
              {lobbyState.players.length} / 8
            </span>
          </div>
          {lobbyState.players.map((player, i) => (
            <div
              key={player.playerId}
              style={{
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
                padding: '10px 16px',
                borderBottom: i < lobbyState.players.length - 1 ? '1px solid #1e1e2a' : 'none',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <div style={{
                  width: 8,
                  height: 8,
                  borderRadius: '50%',
                  backgroundColor: !player.isConnected ? '#ff4444' : '#00cc44',
                  flexShrink: 0,
                }} />
                <span style={{ fontSize: responsive.fontSize.normal }}>
                  {player.playerName}
                </span>
                {player.isHost && (
                  <span style={{
                    fontSize: 10,
                    fontWeight: 600,
                    color: '#e65100',
                    backgroundColor: 'rgba(230, 81, 0, 0.15)',
                    padding: '2px 6px',
                    borderRadius: 4,
                    textTransform: 'uppercase',
                    letterSpacing: '0.04em',
                  }}>
                    Host
                  </span>
                )}
              </div>
              <span style={{ fontSize: responsive.fontSize.small, color: '#555' }}>
                {!player.isConnected
                  ? 'Disconnected'
                  : player.deckSubmitted
                    ? 'Deck Ready'
                    : 'Joined'}
              </span>
            </div>
          ))}
          {lobbyState.players.length === 0 && (
            <div style={{ padding: '20px 16px', textAlign: 'center', color: '#444', fontSize: responsive.fontSize.small }}>
              No players yet
            </div>
          )}
        </div>

        {/* Actions */}
        <div style={{ display: 'flex', gap: 10 }}>
          {isWaiting && lobbyState.isHost && (
            <button
              onClick={startSealedLobby}
              disabled={!canStart}
              style={{
                flex: 1,
                padding: '12px 20px',
                fontSize: responsive.fontSize.normal,
                fontWeight: 600,
                backgroundColor: canStart ? '#e65100' : '#1a1a24',
                color: canStart ? 'white' : '#444',
                border: canStart ? 'none' : '1px solid #2a2a3a',
                borderRadius: 8,
                cursor: canStart ? 'pointer' : 'not-allowed',
                transition: 'background-color 0.15s',
              }}
            >
              Start Game
            </button>
          )}
          <button
            onClick={leaveLobby}
            style={{
              padding: '12px 20px',
              fontSize: responsive.fontSize.normal,
              backgroundColor: 'transparent',
              color: '#888',
              border: '1px solid #2a2a3a',
              borderRadius: 8,
              cursor: 'pointer',
              transition: 'border-color 0.15s',
            }}
          >
            Leave
          </button>
        </div>

        {isWaiting && !lobbyState.isHost && (
          <p style={{ color: '#555', fontSize: responsive.fontSize.small, textAlign: 'center', margin: 0 }}>
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
          Round {tournamentState.currentRound} of {tournamentState.totalRounds}
        </p>
      )}

      {tournamentState.isBye && (
        <div
          style={{
            backgroundColor: '#1a472a',
            padding: '12px 24px',
            borderRadius: 8,
            fontSize: responsive.fontSize.normal,
          }}
        >
          You have a BYE this round (auto-win)
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
            </tr>
          </thead>
          <tbody>
            {tournamentState.standings.map((standing, index) => {
              const isMe = standing.playerId === playerId
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
