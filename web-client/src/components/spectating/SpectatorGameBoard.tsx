import { useGameStore } from '@/store/gameStore.ts'
import { SpectatorContext } from '../../contexts/SpectatorContext'
import { GameBoard } from '../game/GameBoard'
import { CombatArrows } from '../combat/CombatArrows'
import { DecisionSourceThumbnail } from '../ui/DecisionSourceThumbnail'
import { SeatPortrait, useHasSeatPortrait } from '../game/SeatPortrait'
import { SpectatorGameOverBanner } from './SpectatorGameOverBanner'
import type { SpectatorDecisionStatus } from '@/types'

// ============================================================================
// SpectatorGameBoard - Read-only view of a game in progress
// ============================================================================
//
// SPECTATOR vs PLAYER MODE DISTINCTION:
//
// When a user plays a game:
//   - GameBoard renders with full interactivity
//   - Player sees their own hand (face up)
//   - Player can click cards to play them, declare attackers/blockers, etc.
//   - Action menu, pass button, and targeting UI are visible
//
// When a user spectates a game:
//   - SpectatorGameBoard wraps GameBoard in spectator mode
//   - Both hands are shown face-down (card backs in fan layout)
//   - All interaction is disabled (read-only view)
//   - No action menu, pass button, or targeting UI
//   - Phase indicator shows actual player names instead of "Your Turn"
//   - A header bar shows "Spectating {Player1} vs {Player2}" with back button
//
// This is implemented via:
//   1. SpectatorContext - provides spectator state to child components
//   2. spectatorMode prop on GameBoard - disables all interactions
//   3. topOffset prop - accounts for the spectator header bar
//
// The server sends a masked game state where both hands contain hidden cards,
// ensuring spectators cannot see any private information.
// ============================================================================

/** Height of the spectator header bar in pixels */
const HEADER_HEIGHT = 55

/** The HUD orbs' 1v1 colours — the board puts player 1 at the bottom (blue) and player 2 on top (orange). */
const PLAYER_RING = 'rgba(74, 154, 234, 0.8)'
const OPPONENT_RING = 'rgba(255, 158, 70, 0.85)'

/**
 * SpectatorGameBoard - Wrapper component for spectating a game.
 *
 * This component:
 * - Shows a loading state while connecting to the match
 * - Provides SpectatorContext for child components to detect spectator mode
 * - Renders the standard GameBoard with spectatorMode enabled
 * - Displays a header with match info and a back button
 */
export function SpectatorGameBoard() {
  const spectatingState = useGameStore((state) => state.spectatingState)
  const stopSpectating = useGameStore((state) => state.stopSpectating)
  const spectatorBottomSeatId = useGameStore((state) => state.spectatorBottomSeatId)
  const setSpectatorBottomSeat = useGameStore((state) => state.setSpectatorBottomSeat)

  // Not spectating - render nothing
  if (!spectatingState) return null

  // Multiplayer pods: the header gains a seat-switcher that cycles which seat's
  // perspective anchors the bottom half (the rail + board strip carry the rest).
  const players = spectatingState.gameState?.players ?? []
  const isMultiPod = players.length > 2
  const bottomSeatId = spectatorBottomSeatId ?? spectatingState.player1Id
  const bottomSeat = players.find((p) => p.playerId === bottomSeatId) ?? players[0] ?? null
  const cycleSeat = () => {
    if (players.length === 0 || !bottomSeat) return
    const idx = players.findIndex((p) => p.playerId === bottomSeat.playerId)
    const next = players[(idx + 1) % players.length]
    if (next) setSpectatorBottomSeat(next.playerId)
  }

  // Loading state - waiting for server to send game state
  if (!spectatingState.gameState) {
    return (
      <SpectatorLoadingView
        player1Name={spectatingState.player1Name}
        player2Name={spectatingState.player2Name}
        onBack={stopSpectating}
      />
    )
  }

  // Render the game board in spectator mode
  return (
    <SpectatorContext.Provider
      value={{
        isSpectating: true,
        player1Id: spectatingState.player1Id,
        player2Id: spectatingState.player2Id,
        player1Name: spectatingState.player1Name,
        player2Name: spectatingState.player2Name,
      }}
    >
      <div style={styles.container}>
        <SpectatorHeader
          player1Name={spectatingState.player1Name}
          player2Name={spectatingState.player2Name}
          seatIds={isMultiPod ? players.map((p) => p.playerId) : [spectatingState.player1Id, spectatingState.player2Id]}
          onBack={stopSpectating}
          {...(isMultiPod
            ? {
                matchupOverride: players.map((p) => p.name).join(' · '),
                seatSwitcher: { bottomName: bottomSeat?.name ?? '?', onCycle: cycleSeat },
              }
            : {})}
        />
        <div style={styles.gameBoardContainer}>
          <GameBoard spectatorMode topOffset={HEADER_HEIGHT} />
          {/* The game ended: the final board stays up under the result until the spectator
              leaves (or the banner's countdown takes them back to the overview). */}
          {spectatingState.gameOver && (
            <SpectatorGameOverBanner
              key={spectatingState.gameSessionId}
              gameOver={spectatingState.gameOver}
              seats={players}
              onBack={stopSpectating}
            />
          )}
        </div>
        {/* Decision indicator for spectators */}
        {spectatingState.decisionStatus && !spectatingState.gameOver && (
          <SpectatorDecisionIndicator
            decisionStatus={spectatingState.decisionStatus}
          />
        )}
      </div>
      {/* Combat arrows rendered at top level to ensure visibility above all containers. Once the
          game is over they would draw across the result banner, and the fight they show is done. */}
      {!spectatingState.gameOver && <CombatArrows />}
    </SpectatorContext.Provider>
  )
}

// ============================================================================
// Sub-components
// ============================================================================

/**
 * Loading view shown while connecting to a spectated match.
 */
function SpectatorLoadingView({
  player1Name,
  player2Name,
  onBack,
}: {
  player1Name: string
  player2Name: string
  onBack: () => void
}) {
  return (
    <div style={styles.container}>
      <div style={styles.loadingContent}>
        <div style={styles.spinner} />
        <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
        <h2 style={styles.loadingTitle}>Connecting to match...</h2>
        <p style={styles.loadingSubtitle}>
          {player1Name} vs {player2Name}
        </p>
        <button onClick={onBack} style={styles.backButton}>
          Back to Overview
        </button>
      </div>
    </div>
  )
}

/**
 * Header bar for spectator mode with match info and back button.
 */
function SpectatorHeader({
  player1Name,
  player2Name,
  seatIds,
  onBack,
  matchupOverride,
  seatSwitcher,
}: {
  player1Name: string
  player2Name: string
  /** The seats in matchup order: their portraits flank "A vs B" (a pod's lead the roster). */
  seatIds: readonly (string | null)[]
  onBack: () => void
  /** Multiplayer pods: full roster text instead of "A vs B". */
  matchupOverride?: string
  /** Multiplayer pods: cycle which seat's perspective anchors the bottom half. */
  seatSwitcher?: { bottomName: string; onCycle: () => void }
}) {
  return (
    <div style={styles.header}>
      <button onClick={onBack} style={styles.backButton}>
        Back to Overview
      </button>
      {/* 1v1: each player's face on their side of "A vs B", like a fight card. A pod's faces
          overlap ahead of the roster line instead. Seats without a portrait drop out. */}
      <div style={styles.headerCenter}>
        {matchupOverride ? (
          <span style={styles.headerFaces}>
            {seatIds.map((id, i) => (
              <SeatPortrait key={id ?? i} playerId={id} size={34} ringColor="#0a0a12" ringWidth={2} style={{ marginLeft: i === 0 ? 0 : -9, zIndex: seatIds.length - i }} />
            ))}
          </span>
        ) : (
          <SeatPortrait playerId={seatIds[0]} size={36} ringColor={PLAYER_RING} ringWidth={2} />
        )}
        <div style={styles.headerTitle}>
          <div style={styles.spectatingLabel}>Spectating</div>
          <div style={styles.matchupText}>
            {matchupOverride ?? `${player1Name} vs ${player2Name}`}
          </div>
        </div>
        {!matchupOverride && <SeatPortrait playerId={seatIds[1]} size={36} ringColor={OPPONENT_RING} ringWidth={2} />}
      </div>
      {seatSwitcher ? (
        <button
          onClick={seatSwitcher.onCycle}
          title="Cycle which player's view anchors the bottom of the board"
          style={{ ...styles.backButton, whiteSpace: 'nowrap' }}
        >
          View: {seatSwitcher.bottomName} ⟳
        </button>
      ) : (
        /* Spacer to balance the back button */
        <div style={styles.headerSpacer} />
      )}
    </div>
  )
}

/**
 * Decision indicator shown when a player is making a choice.
 * For spectators, this shows which player is deciding.
 */
function SpectatorDecisionIndicator({
  decisionStatus,
}: {
  decisionStatus: SpectatorDecisionStatus
}) {
  // The deciding player's face, with the spinner as a ring around it — "who is thinking" at a
  // glance. No portrait: the plain spinner.
  const hasPortrait = useHasSeatPortrait(decisionStatus.playerId)
  return (
    <div style={styles.decisionIndicator}>
      {hasPortrait ? (
        <span style={styles.decisionFace}>
          <SeatPortrait playerId={decisionStatus.playerId} size={30} ringColor="transparent" ringWidth={0} />
          <span aria-hidden style={styles.decisionFaceRing} />
        </span>
      ) : (
        <div style={styles.decisionSpinner} />
      )}
      <DecisionSourceThumbnail sourceId={decisionStatus.sourceId} />
      <div>
        <div style={styles.decisionText}>
          {decisionStatus.playerName} is {decisionStatus.displayText.toLowerCase()}
        </div>
        {decisionStatus.sourceName && (
          <div style={styles.decisionSourceText}>
            ({decisionStatus.sourceName})
          </div>
        )}
      </div>
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  )
}

// ============================================================================
// Styles
// ============================================================================

const styles: Record<string, React.CSSProperties> = {
  // Container that fills the viewport
  container: {
    position: 'fixed',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: '#0a0a12',
    display: 'flex',
    flexDirection: 'column',
    zIndex: 1500,
  },

  // Header bar at the top of the spectator view
  header: {
    display: 'flex',
    alignItems: 'center',
    padding: '10px 16px',
    gap: 12,
    borderBottom: '1px solid rgba(255, 255, 255, 0.08)',
    backgroundColor: 'rgba(10, 12, 20, 0.92)',
    backdropFilter: 'var(--chrome-blur)',
    WebkitBackdropFilter: 'var(--chrome-blur)',
    flexShrink: 0,
    zIndex: 1600,
  },

  headerCenter: {
    flex: 1,
    minWidth: 0,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    gap: 14,
  },

  headerTitle: {
    textAlign: 'center',
    minWidth: 0,
  },

  headerFaces: {
    display: 'flex',
    alignItems: 'center',
    flexShrink: 0,
  },

  spectatingLabel: {
    color: '#f2b45c',
    fontSize: 11,
    fontWeight: 600,
    textTransform: 'uppercase',
    letterSpacing: '0.12em',
  },

  matchupText: {
    color: '#eef0f6',
    fontFamily: 'var(--font-display)',
    fontSize: 17,
    fontWeight: 600,
    marginTop: 2,
    overflow: 'hidden',
    textOverflow: 'ellipsis',
    whiteSpace: 'nowrap',
  },

  // Balances the back button so the matchup stays centred — but not at a phone's expense.
  headerSpacer: {
    width: 'min(120px, 12vw)',
    flexShrink: 0,
  },

  // Back button used in header and loading view
  backButton: {
    padding: '8px 16px',
    fontSize: 13,
    backgroundColor: 'var(--chrome-bg)',
    color: 'var(--chrome-text)',
    border: '1px solid var(--chrome-border)',
    borderRadius: 'var(--chrome-radius)',
    cursor: 'pointer',
    flexShrink: 0,
  },

  // Container for the GameBoard
  gameBoardContainer: {
    flex: 1,
    position: 'relative',
    overflow: 'hidden',
  },

  // Loading view styles
  loadingContent: {
    textAlign: 'center',
    color: 'white',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    height: '100%',
  },

  loadingTitle: {
    margin: '16px 0 8px 0',
    fontFamily: 'var(--font-display)',
    fontSize: 24,
    fontWeight: 600,
  },

  loadingSubtitle: {
    color: '#9aa3b8',
    margin: 0,
    fontSize: 14,
  },

  spinner: {
    width: 34,
    height: 34,
    border: '3px solid rgba(255, 255, 255, 0.12)',
    borderTopColor: '#f2b45c',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
  },

  // Decision indicator styles
  decisionIndicator: {
    position: 'absolute',
    top: 80,
    left: '50%',
    transform: 'translateX(-50%)',
    backgroundColor: 'var(--chrome-panel-bg)',
    border: '1px solid rgba(242, 180, 92, 0.5)',
    borderRadius: 12,
    backdropFilter: 'var(--chrome-blur)',
    WebkitBackdropFilter: 'var(--chrome-blur)',
    boxShadow: '0 10px 30px rgba(0, 0, 0, 0.45)',
    padding: '10px 18px',
    display: 'flex',
    alignItems: 'center',
    gap: 12,
    zIndex: 100,
    pointerEvents: 'none',
  },

  decisionSpinner: {
    width: 16,
    height: 16,
    border: '2px solid rgba(255, 255, 255, 0.12)',
    borderTopColor: '#f2b45c',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
  },

  decisionFace: {
    position: 'relative',
    display: 'inline-flex',
    flexShrink: 0,
  },

  decisionFaceRing: {
    position: 'absolute',
    inset: -4,
    borderRadius: '50%',
    border: '2px solid rgba(255, 255, 255, 0.12)',
    borderTopColor: '#f2b45c',
    borderRightColor: '#f2b45c',
    animation: 'spin 1.2s linear infinite',
  },

  decisionText: {
    color: '#f6c27a',
    fontWeight: 500,
  },

  decisionSourceText: {
    color: '#9aa3b8',
    fontSize: '0.85em',
  },
}
