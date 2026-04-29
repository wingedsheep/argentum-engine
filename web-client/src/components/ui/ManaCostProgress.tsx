/**
 * Mana cost display with paid/unpaid progress indicators.
 * Shows which parts of a spell's mana cost are covered by the current mana pool.
 */

import type { ClientManaPool } from '@/types'
import { ManaSymbol } from './ManaSymbols'

interface ManaCostProgressProps {
  /** Mana cost string, e.g. "{1}{B}{B}" */
  cost: string | null
  /** Current floating mana pool */
  manaPool: ClientManaPool
  /** Symbol size in pixels */
  size?: number
  /** Gap between symbols */
  gap?: number
}

/** Numeric mana pool keys (excludes the restrictedMana list field). */
type NumericPoolKey = 'white' | 'blue' | 'black' | 'red' | 'green' | 'colorless'

/** Mana color codes mapped to pool keys */
const COLOR_TO_POOL_KEY: Record<string, NumericPoolKey> = {
  W: 'white',
  U: 'blue',
  B: 'black',
  R: 'red',
  G: 'green',
  C: 'colorless',
}

interface SymbolStatus {
  symbol: string
  paid: boolean
}

/**
 * Greedily assign mana pool to cost symbols.
 * Colored symbols are matched first, then generic costs use remaining pool.
 */
function computeProgress(symbols: string[], pool: ClientManaPool): SymbolStatus[] {
  // Work with a mutable copy of the pool
  const remaining = { ...pool }

  const result: SymbolStatus[] = symbols.map((s) => ({ symbol: s, paid: false }))

  // Pass 1: Match colored/colorless symbols
  for (const entry of result) {
    const poolKey = COLOR_TO_POOL_KEY[entry.symbol]
    if (poolKey && remaining[poolKey] > 0) {
      remaining[poolKey]--
      entry.paid = true
    }
  }

  // Pass 2: Match generic (numeric) symbols with any remaining mana
  let totalRemaining =
    remaining.white + remaining.blue + remaining.black + remaining.red + remaining.green + remaining.colorless

  for (const entry of result) {
    if (entry.paid) continue
    const num = parseInt(entry.symbol, 10)
    if (!isNaN(num)) {
      // Generic cost — need `num` total mana from any source
      if (totalRemaining >= num) {
        totalRemaining -= num
        entry.paid = true
      }
    }
  }

  return result
}

/**
 * Renders a mana cost string with visual indicators for which symbols
 * are already payable from the current mana pool.
 */
export function ManaCostProgress({ cost, manaPool, size = 16, gap = 2 }: ManaCostProgressProps) {
  if (!cost) return null

  const matches = cost.match(/\{([^}]+)\}/g)
  if (!matches || matches.length === 0) return null

  const symbols = matches.map((m) => m.slice(1, -1))
  const progress = computeProgress(symbols, manaPool)

  return (
    <span style={{ display: 'inline-flex', alignItems: 'center', gap }}>
      {progress.map((entry, i) => (
        <span
          key={i}
          style={{
            opacity: entry.paid ? 1 : 0.35,
            filter: entry.paid ? 'none' : 'grayscale(60%)',
            transition: 'opacity 0.15s, filter 0.15s',
          }}
        >
          <ManaSymbol symbol={entry.symbol} size={size} />
        </span>
      ))}
    </span>
  )
}
