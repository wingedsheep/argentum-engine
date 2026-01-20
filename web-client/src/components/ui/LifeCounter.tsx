import type { EntityId } from '../../types'

interface LifeCounterProps {
  playerId: EntityId // Used for click handling in future
  name: string
  life: number
  poisonCounters: number
  isOpponent: boolean
}

/**
 * Life total display for a player.
 */
export function LifeCounter({
  name,
  life,
  poisonCounters,
  isOpponent,
}: LifeCounterProps) {
  // Determine life color based on value
  const getLifeColor = () => {
    if (life <= 0) return '#ff0000'
    if (life <= 5) return '#ff6600'
    if (life <= 10) return '#ffcc00'
    return '#ffffff'
  }

  return (
    <div
      style={{
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        borderRadius: 8,
        padding: '8px 16px',
        display: 'flex',
        flexDirection: 'column',
        alignItems: isOpponent ? 'flex-start' : 'flex-start',
        minWidth: 120,
        pointerEvents: 'auto',
      }}
    >
      {/* Player name */}
      <span
        style={{
          color: '#888',
          fontSize: 12,
          marginBottom: 4,
        }}
      >
        {isOpponent ? 'Opponent' : 'You'}: {name}
      </span>

      {/* Life total */}
      <div
        style={{
          display: 'flex',
          alignItems: 'center',
          gap: 8,
        }}
      >
        <span
          style={{
            fontSize: 32,
            fontWeight: 'bold',
            color: getLifeColor(),
            fontVariantNumeric: 'tabular-nums',
          }}
        >
          {life}
        </span>

        {/* Heart icon */}
        <span style={{ fontSize: 20 }}>❤️</span>
      </div>

      {/* Poison counters (if any) */}
      {poisonCounters > 0 && (
        <div
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 4,
            marginTop: 4,
            color: '#00ff88',
          }}
        >
          <span style={{ fontSize: 16 }}>☠️</span>
          <span style={{ fontSize: 14 }}>{poisonCounters}/10</span>
        </div>
      )}
    </div>
  )
}
