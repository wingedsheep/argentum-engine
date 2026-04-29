import type { ClientManaPool, ClientRestrictedManaEntry } from '@/types'
import { totalMana, isManaPoolEmpty } from '@/types'
import { ManaSymbol } from './ManaSymbols'

interface ManaPoolProps {
  manaPool: ClientManaPool
}

/**
 * Mana pool display showing available mana by color. Restricted mana (mana with
 * spending restrictions, e.g. "spend only on spells with mana value 4 or greater")
 * is rendered separately with a dashed outline and a hover tooltip showing the
 * restriction text.
 */
export function ManaPool({ manaPool }: ManaPoolProps) {
  if (isManaPoolEmpty(manaPool)) {
    return null
  }

  const restricted = manaPool.restrictedMana ?? []

  return (
    <div
      style={{
        backgroundColor: 'rgba(0, 0, 0, 0.85)',
        border: '1px solid rgba(255, 215, 0, 0.5)',
        borderRadius: 8,
        padding: '8px 16px',
        display: 'flex',
        alignItems: 'center',
        gap: 12,
        pointerEvents: 'auto',
        boxShadow: '0 0 8px rgba(255, 215, 0, 0.2)',
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
        {restricted.length > 0 && (
          <RestrictedManaOrbs entries={restricted} />
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

interface RestrictedManaOrbsProps {
  entries: ReadonlyArray<ClientRestrictedManaEntry>
}

/**
 * Render restricted-mana entries grouped by (color, restriction) pair so two
 * entries with the same color and restriction collapse into a single orb with
 * a count badge. Each orb has a dashed outline and a `title` tooltip showing
 * the restriction description.
 */
function RestrictedManaOrbs({ entries }: RestrictedManaOrbsProps) {
  const grouped = new Map<string, { color: string | null; restriction: string; count: number }>()
  for (const entry of entries) {
    const key = `${entry.color ?? 'C'}|${entry.restrictionDescription}`
    const existing = grouped.get(key)
    if (existing) {
      existing.count += 1
    } else {
      grouped.set(key, {
        color: entry.color,
        restriction: entry.restrictionDescription,
        count: 1,
      })
    }
  }

  return (
    <>
      {Array.from(grouped.values()).map((group, index) => (
        <div
          key={index}
          title={group.restriction}
          style={{
            display: 'flex',
            alignItems: 'center',
            gap: 2,
            padding: '2px 4px',
            borderRadius: 4,
            border: '1px dashed rgba(255, 215, 0, 0.6)',
          }}
        >
          <ManaSymbol symbol={group.color ?? 'C'} size={24} />
          {group.count > 1 && (
            <span
              style={{
                fontSize: 14,
                color: '#fff',
                fontWeight: 'bold',
              }}
            >
              ×{group.count}
            </span>
          )}
          <span
            style={{
              fontSize: 11,
              color: '#ffd700',
              fontWeight: 'bold',
              marginLeft: 2,
            }}
          >
            *
          </span>
        </div>
      ))}
    </>
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
