import { useMemo } from 'react'
import { useGameStore, ConvokeCreatureSelection } from '@/store/gameStore.ts'
import { Color, ColorSymbols } from '@/types/enums'
import { useViewingPlayer } from '@/store/selectors'
import type { ClientManaPool } from '@/types/gameState'
import { ManaSymbol } from './ManaSymbols'

/**
 * Convert a backend Color enum name (e.g. "WHITE") to the matching mana-pip letter
 * ("W") used inside parsed cost strings. Returns the input unchanged if it's already
 * a letter or an unrecognised value.
 */
function toPipLetter(color: string): string {
  return ColorSymbols[color as Color] ?? color
}

/**
 * Parse a mana cost string into individual symbols.
 * e.g., "{2}{W}{W}" -> ["2", "W", "W"]
 */
function parseManaCost(manaCost: string): string[] {
  const symbols: string[] = []
  const regex = /\{([^}]+)\}/g
  let match
  while ((match = regex.exec(manaCost)) !== null) {
    symbols.push(match[1]!)
  }
  return symbols
}

/**
 * True if [symbol] is a hybrid pip (e.g. "W/U") whose halves include [color].
 * Per CR 107.4e a hybrid mana symbol is a colored mana symbol of both component colors.
 */
function hybridCoversColor(symbol: string, color: string): boolean {
  if (!symbol.includes('/')) return false
  const halves = symbol.split('/')
  return halves.includes(color)
}

/**
 * Calculate remaining mana cost after applying convoke selections.
 *
 * Per CR 702.51a, convoke pays a colored mana pip by tapping a creature of that color.
 * Hybrid symbols (CR 107.4e) are colored symbols of both component colors, so a creature
 * paying with one of the hybrid's colors covers the pip. Prefer exact colored matches
 * before hybrids so we don't waste a hybrid pip on a plain colored requirement.
 */
function calculateRemainingCost(
  originalSymbols: string[],
  selectedCreatures: ConvokeCreatureSelection[]
): string[] {
  const remaining = [...originalSymbols]

  for (const creature of selectedCreatures) {
    if (creature.payingColor) {
      const pip = toPipLetter(creature.payingColor)
      const exactIndex = remaining.indexOf(pip)
      if (exactIndex >= 0) {
        remaining.splice(exactIndex, 1)
        continue
      }
      const hybridIndex = remaining.findIndex(s => hybridCoversColor(s, pip))
      if (hybridIndex >= 0) {
        remaining.splice(hybridIndex, 1)
      }
    } else {
      const genericIndex = remaining.findIndex(s => /^\d+$/.test(s))
      if (genericIndex >= 0) {
        const genericValue = parseInt(remaining[genericIndex]!, 10)
        if (genericValue > 1) {
          remaining[genericIndex] = String(genericValue - 1)
        } else {
          remaining.splice(genericIndex, 1)
        }
      }
    }
  }

  return remaining
}

/**
 * Subtract the player's floating mana from [symbols], paying exact-color pips first,
 * then hybrid pips, then generic. Returns the symbols still owed after the pool is
 * applied. Mirrors the order the server's autoPay uses so the affordability check
 * matches the actual payment.
 */
function applyManaPool(symbols: string[], pool: ClientManaPool | undefined): string[] {
  if (!pool) return symbols
  const remaining = [...symbols]
  const available: Record<string, number> = {
    W: pool.white,
    U: pool.blue,
    B: pool.black,
    R: pool.red,
    G: pool.green,
    C: pool.colorless,
  }

  for (const pip of ['W', 'U', 'B', 'R', 'G', 'C']) {
    while (available[pip]! > 0) {
      const idx = remaining.indexOf(pip)
      if (idx < 0) break
      remaining.splice(idx, 1)
      available[pip]!--
    }
  }

  for (const pip of ['W', 'U', 'B', 'R', 'G']) {
    while (available[pip]! > 0) {
      const idx = remaining.findIndex(s => hybridCoversColor(s, pip))
      if (idx < 0) break
      remaining.splice(idx, 1)
      available[pip]!--
    }
  }

  let generic = available.W! + available.U! + available.B! + available.R! + available.G! + available.C!
  while (generic > 0) {
    const idx = remaining.findIndex(s => /^\d+$/.test(s))
    if (idx < 0) break
    const value = parseInt(remaining[idx]!, 10)
    if (value > 1) {
      remaining[idx] = String(value - 1)
    } else {
      remaining.splice(idx, 1)
    }
    generic--
  }

  return remaining
}

/**
 * Calculate the total mana value of remaining cost symbols.
 * Generic symbols count as their numeric value, colored symbols count as 1 each.
 */
function totalManaNeeded(symbols: string[]): number {
  let total = 0
  for (const s of symbols) {
    const num = parseInt(s, 10)
    total += isNaN(num) ? 1 : num
  }
  return total
}

/**
 * Calculate the total mana available from mana sources.
 */
function totalManaAvailable(sources: readonly { manaAmount?: number }[] | undefined | null): number {
  if (!sources) return 0
  let total = 0
  for (const s of sources) {
    total += s.manaAmount ?? 1
  }
  return total
}

/**
 * Compact floating HUD bar for convoke selection.
 * Shows mana cost progress and confirm/cancel buttons while
 * creatures are selected directly on the battlefield.
 */
export function ConvokeSelector() {
  const convokeSelectionState = useGameStore((state) => state.convokeSelectionState)
  const cancelConvokeSelection = useGameStore((state) => state.cancelConvokeSelection)
  const confirmConvokeSelection = useGameStore((state) => state.confirmConvokeSelection)
  const viewingPlayer = useViewingPlayer()
  const manaPool = viewingPlayer?.manaPool

  const originalSymbols = useMemo(() => {
    if (!convokeSelectionState) return []
    return parseManaCost(convokeSelectionState.manaCost)
  }, [convokeSelectionState?.manaCost])

  const remainingSymbols = useMemo(() => {
    if (!convokeSelectionState) return []
    return calculateRemainingCost(originalSymbols, convokeSelectionState.selectedCreatures)
  }, [originalSymbols, convokeSelectionState?.selectedCreatures])

  const symbolsAfterPool = useMemo(
    () => applyManaPool(remainingSymbols, manaPool),
    [remainingSymbols, manaPool]
  )

  if (!convokeSelectionState) return null

  const { cardName, selectedCreatures, actionInfo } = convokeSelectionState
  const isAbility = actionInfo.action.type === 'ActivateAbility'

  const manaNeeded = totalManaNeeded(symbolsAfterPool)
  const manaFromSources = totalManaAvailable(actionInfo.availableManaSources)
  const canAfford = manaNeeded <= manaFromSources

  return (
    <div style={styles.bar}>
      <span style={styles.label}>
        {isAbility ? 'Tap creatures for' : 'Convoke'} <strong>{cardName}</strong>
      </span>
      <span style={styles.divider} />
      <span style={styles.costLabel}>Cost:</span>
      <div style={styles.manaSymbols}>
        {originalSymbols.map((symbol, i) => (
          <ManaSymbol key={i} symbol={symbol} size={18} />
        ))}
      </div>
      <span style={styles.arrow}>→</span>
      <div style={styles.manaSymbols}>
        {remainingSymbols.length > 0 ? (
          remainingSymbols.map((symbol, i) => (
            <ManaSymbol key={i} symbol={symbol} size={18} />
          ))
        ) : (
          <span style={styles.freeCast}>Free!</span>
        )}
      </div>
      <span style={styles.count}>
        ({selectedCreatures.length} tapped)
      </span>
      <span style={styles.divider} />
      <button onClick={cancelConvokeSelection} style={styles.cancelButton}>
        Cancel
      </button>
      <button
        onClick={canAfford ? confirmConvokeSelection : undefined}
        style={canAfford ? styles.confirmButton : styles.confirmButtonDisabled}
      >
        {isAbility ? 'Activate' : 'Cast'}
      </button>
    </div>
  )
}

const styles: Record<string, React.CSSProperties> = {
  bar: {
    position: 'absolute',
    bottom: 12,
    left: '50%',
    transform: 'translateX(-50%)',
    display: 'flex',
    alignItems: 'center',
    gap: 10,
    padding: '10px 20px',
    backgroundColor: 'rgba(20, 20, 40, 0.95)',
    border: '2px solid #4a4a6a',
    borderRadius: 10,
    boxShadow: '0 4px 20px rgba(0, 0, 0, 0.6)',
    zIndex: 1500,
    whiteSpace: 'nowrap',
  },
  label: {
    color: '#ccc',
    fontSize: 14,
  },
  divider: {
    width: 1,
    height: 20,
    backgroundColor: '#4a4a6a',
  },
  costLabel: {
    color: '#888',
    fontSize: 13,
  },
  manaSymbols: {
    display: 'flex',
    alignItems: 'center',
    gap: 3,
  },
  arrow: {
    color: '#666',
    fontSize: 14,
  },
  freeCast: {
    color: '#4caf50',
    fontWeight: 'bold',
    fontSize: 13,
  },
  count: {
    color: '#666',
    fontSize: 12,
  },
  cancelButton: {
    padding: '6px 14px',
    fontSize: 13,
    backgroundColor: '#444',
    color: '#fff',
    border: 'none',
    borderRadius: 6,
    cursor: 'pointer',
  },
  confirmButton: {
    padding: '6px 14px',
    fontSize: 13,
    backgroundColor: '#0066cc',
    color: '#fff',
    border: 'none',
    borderRadius: 6,
    cursor: 'pointer',
  },
  confirmButtonDisabled: {
    padding: '6px 14px',
    fontSize: 13,
    backgroundColor: '#333',
    color: '#666',
    border: 'none',
    borderRadius: 6,
    cursor: 'not-allowed',
  },
}
