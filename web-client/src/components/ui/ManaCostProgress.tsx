/**
 * Mana cost display with paid/unpaid progress indicators.
 * Shows which parts of a spell's mana cost are covered by the current mana pool.
 */

import type { ClientManaPool, ClientRestrictedManaEntry } from '@/types'
import { ManaSymbol } from './ManaSymbols'

interface ManaCostProgressProps {
  /** Mana cost string, e.g. "{1}{B}{B}" */
  cost: string | null
  /** Current floating mana pool */
  manaPool: ClientManaPool
  /**
   * Restricted ("spend this mana only to …") mana the server flagged eligible for *this* action
   * (`LegalActionInfo.eligibleRestrictedMana`). Counted as spendable, so a cast payable with
   * Ashling, Rimebound's MV4+ mana doesn't render as unpaid.
   */
  eligibleRestrictedMana?: readonly ClientRestrictedManaEntry[]
  /** Symbol size in pixels */
  size?: number
  /** Gap between symbols */
  gap?: number
}

/** Pip letters, in the order the pool is consumed. */
const PIPS = ['W', 'U', 'B', 'R', 'G', 'C'] as const

interface SymbolStatus {
  symbol: string
  paid: boolean
}

/**
 * Greedily assign mana pool to cost symbols.
 * Colored symbols are matched first, then generic costs use remaining pool.
 */
function computeProgress(
  symbols: string[],
  pool: ClientManaPool,
  eligibleRestricted: readonly ClientRestrictedManaEntry[],
): SymbolStatus[] {
  const available: Record<string, number> = {
    W: pool.white,
    U: pool.blue,
    B: pool.black,
    R: pool.red,
    G: pool.green,
    C: pool.colorless,
  }
  for (const entry of eligibleRestricted) {
    const pip = entry.color ?? 'C'
    if (pip in available) available[pip]!++
  }

  const result: SymbolStatus[] = symbols.map((s) => ({ symbol: s, paid: false }))

  // Pass 1: Match colored/colorless symbols
  for (const entry of result) {
    if (available[entry.symbol] !== undefined && available[entry.symbol]! > 0) {
      available[entry.symbol]!--
      entry.paid = true
    }
  }

  // Pass 2: Match generic (numeric) symbols with any remaining mana
  let totalRemaining = PIPS.reduce((sum, pip) => sum + available[pip]!, 0)

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
export function ManaCostProgress({
  cost,
  manaPool,
  eligibleRestrictedMana = [],
  size = 16,
  gap = 2,
}: ManaCostProgressProps) {
  if (!cost) return null

  const matches = cost.match(/\{([^}]+)\}/g)
  if (!matches || matches.length === 0) return null

  const symbols = matches.map((m) => m.slice(1, -1))
  const progress = computeProgress(symbols, manaPool, eligibleRestrictedMana)

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
