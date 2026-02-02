import { useGameStore } from '../../store/gameStore'
import { SpectatorContext } from '../../contexts/SpectatorContext'
import { GameBoard } from '../game/GameBoard'
import { CombatArrows } from '../combat/CombatArrows'
import type { SpectatorDecisionStatus } from '../../types'

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

  // Not spectating - render nothing
  if (!spectatingState) return null

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
          onBack={stopSpectating}
        />
        <div style={styles.gameBoardContainer}>
          <GameBoard spectatorMode topOffset={HEADER_HEIGHT} />
        </div>
        {/* Decision indicator for spectators */}
        {spectatingState.decisionStatus && (
          <SpectatorDecisionIndicator
            decisionStatus={spectatingState.decisionStatus}
          />
        )}
      </div>
      {/* Combat arrows rendered at top level to ensure visibility above all containers */}
      <CombatArrows />
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
  onBack,
}: {
  player1Name: string
  player2Name: string
  onBack: () => void
}) {
  return (
    <div style={styles.header}>
      <button onClick={onBack} style={styles.backButton}>
        Back to Overview
      </button>
      <div style={styles.headerCenter}>
        <div style={styles.spectatingLabel}>Spectating</div>
        <div style={styles.matchupText}>
          {player1Name} vs {player2Name}
        </div>
      </div>
      {/* Spacer to balance the back button */}
      <div style={styles.headerSpacer} />
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
  return (
    <div style={styles.decisionIndicator}>
      <div style={styles.decisionSpinner} />
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
    borderBottom: '1px solid #1a1a25',
    backgroundColor: '#0d0d15',
    flexShrink: 0,
    zIndex: 1600,
  },

  headerCenter: {
    textAlign: 'center',
    flex: 1,
  },

  spectatingLabel: {
    color: '#888',
    fontSize: 12,
    textTransform: 'uppercase',
    letterSpacing: '0.1em',
  },

  matchupText: {
    color: '#aaa',
    fontSize: 14,
    marginTop: 4,
  },

  headerSpacer: {
    width: 120,
  },

  // Back button used in header and loading view
  backButton: {
    padding: '8px 16px',
    fontSize: 13,
    backgroundColor: 'transparent',
    color: '#888',
    border: '1px solid #333',
    borderRadius: 6,
    cursor: 'pointer',
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
    fontSize: 20,
  },

  loadingSubtitle: {
    color: '#666',
    margin: 0,
    fontSize: 14,
  },

  spinner: {
    width: 40,
    height: 40,
    border: '3px solid #333',
    borderTopColor: '#888',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
  },

  // Decision indicator styles
  decisionIndicator: {
    position: 'absolute',
    top: 80,
    left: '50%',
    transform: 'translateX(-50%)',
    backgroundColor: 'rgba(0, 0, 0, 0.9)',
    border: '1px solid #ffc107',
    borderRadius: 8,
    padding: '10px 20px',
    display: 'flex',
    alignItems: 'center',
    gap: 12,
    zIndex: 100,
    pointerEvents: 'none',
  },

  decisionSpinner: {
    width: 16,
    height: 16,
    border: '2px solid #333',
    borderTopColor: '#ffc107',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
  },

  decisionText: {
    color: '#ffc107',
    fontWeight: 500,
  },

  decisionSourceText: {
    color: '#888',
    fontSize: '0.85em',
  },
}
