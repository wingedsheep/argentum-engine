import { useGameStore } from '@/store/gameStore.ts'

/**
 * Displays an indicator when the opponent is making a decision.
 * Shows the type of decision and the source card name if available.
 */
export function OpponentDecisionIndicator() {
  const opponentDecisionStatus = useGameStore((s) => s.opponentDecisionStatus)
  const opponentName = useGameStore((s) => s.opponentName)
  // The status carries the deciding seat's id — name it directly (with N players
  // "the opponent" is ambiguous; in 2-player this resolves to the same name).
  const decidingName = useGameStore((s) => {
    const id = s.opponentDecisionStatus?.playerId
    if (!id) return null
    return s.gameState?.players.find((p) => p.playerId === id)?.name ?? null
  })

  if (!opponentDecisionStatus) return null

  return (
    <div style={styles.container}>
      <div style={styles.spinner} />
      <div>
        <div style={styles.text}>
          {decidingName ?? opponentName ?? 'Opponent'} is {opponentDecisionStatus.displayText.toLowerCase()}
        </div>
        {opponentDecisionStatus.sourceName && (
          <div style={styles.sourceText}>
            ({opponentDecisionStatus.sourceName})
          </div>
        )}
      </div>
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  container: {
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
  spinner: {
    width: 16,
    height: 16,
    border: '2px solid #333',
    borderTopColor: '#ffc107',
    borderRadius: '50%',
    animation: 'spin 1s linear infinite',
  },
  text: {
    color: '#ffc107',
    fontWeight: 500,
  },
  sourceText: {
    color: '#888',
    fontSize: '0.85em',
  },
}
