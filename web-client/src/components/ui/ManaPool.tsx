import type { ClientManaPool } from '../../types'
import { totalMana, isManaPoolEmpty } from '../../types'

interface ManaPoolProps {
  manaPool: ClientManaPool
}

/**
 * Mana pool display showing available mana by color.
 */
export function ManaPool({ manaPool }: ManaPoolProps) {
  if (isManaPoolEmpty(manaPool)) {
    return null
  }

  return (
    <div
      style={{
        backgroundColor: 'rgba(0, 0, 0, 0.8)',
        borderRadius: 8,
        padding: '8px 16px',
        display: 'flex',
        alignItems: 'center',
        gap: 12,
        pointerEvents: 'auto',
      }}
    >
      <span style={{ color: '#888', fontSize: 12 }}>Mana:</span>

      <div style={{ display: 'flex', gap: 8 }}>
        {manaPool.white > 0 && (
          <ManaOrb color="#f9faf4" symbol="W" count={manaPool.white} />
        )}
        {manaPool.blue > 0 && (
          <ManaOrb color="#0e68ab" symbol="U" count={manaPool.blue} />
        )}
        {manaPool.black > 0 && (
          <ManaOrb color="#150b00" symbol="B" count={manaPool.black} textColor="#ccc" />
        )}
        {manaPool.red > 0 && (
          <ManaOrb color="#d3202a" symbol="R" count={manaPool.red} />
        )}
        {manaPool.green > 0 && (
          <ManaOrb color="#00733e" symbol="G" count={manaPool.green} />
        )}
        {manaPool.colorless > 0 && (
          <ManaOrb color="#9e9e9e" symbol="C" count={manaPool.colorless} />
        )}
      </div>

      <span
        style={{
          color: '#888',
          fontSize: 14,
          marginLeft: 8,
        }}
      >
        ({totalMana(manaPool)})
      </span>
    </div>
  )
}

interface ManaOrbProps {
  color: string
  symbol: string
  count: number
  textColor?: string
}

/**
 * Individual mana orb with count.
 */
function ManaOrb({ color, symbol, count, textColor = '#000' }: ManaOrbProps) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 2,
      }}
    >
      <div
        style={{
          width: 24,
          height: 24,
          borderRadius: '50%',
          backgroundColor: color,
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          border: '2px solid rgba(255, 255, 255, 0.3)',
          boxShadow: `0 0 8px ${color}`,
        }}
      >
        <span
          style={{
            fontSize: 12,
            fontWeight: 'bold',
            color: textColor,
          }}
        >
          {symbol}
        </span>
      </div>
      {count > 1 && (
        <span
          style={{
            fontSize: 14,
            color: '#fff',
            fontWeight: 'bold',
          }}
        >
          ×{count}
        </span>
      )}
    </div>
  )
}
