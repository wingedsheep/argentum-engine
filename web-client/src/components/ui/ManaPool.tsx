import type { ClientManaPool } from '../../types'
import { totalMana, isManaPoolEmpty } from '../../types'
import { ManaSymbol } from './ManaSymbols'

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
          <ManaOrb symbol="W" count={manaPool.white} />
        )}
        {manaPool.blue > 0 && (
          <ManaOrb symbol="U" count={manaPool.blue} />
        )}
        {manaPool.black > 0 && (
          <ManaOrb symbol="B" count={manaPool.black} />
        )}
        {manaPool.red > 0 && (
          <ManaOrb symbol="R" count={manaPool.red} />
        )}
        {manaPool.green > 0 && (
          <ManaOrb symbol="G" count={manaPool.green} />
        )}
        {manaPool.colorless > 0 && (
          <ManaOrb symbol="C" count={manaPool.colorless} />
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
  symbol: string
  count: number
}

/**
 * Individual mana orb with count using the actual mana symbol asset.
 */
function ManaOrb({ symbol, count }: ManaOrbProps) {
  return (
    <div
      style={{
        display: 'flex',
        alignItems: 'center',
        gap: 2,
      }}
    >
      <ManaSymbol symbol={symbol} size={24} />
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
