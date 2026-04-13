import { useMemo } from 'react'
import { useGameStore, ConvokeCreatureSelection } from '@/store/gameStore.ts'

const COLOR_CSS: Record<string, string> = {
  W: '#f8f6d8',
  U: '#0e68ab',
  B: '#150b00',
  R: '#d3202a',
  G: '#00733e',
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
 * Calculate remaining mana cost after applying convoke selections.
 */
function calculateRemainingCost(
  originalSymbols: string[],
  selectedCreatures: ConvokeCreatureSelection[]
): string[] {
  const remaining = [...originalSymbols]

  for (const creature of selectedCreatures) {
    if (creature.payingColor) {
      const colorIndex = remaining.indexOf(creature.payingColor)
      if (colorIndex >= 0) {
        remaining.splice(colorIndex, 1)
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
 * Compact floating HUD bar for convoke selection.
 * Shows mana cost progress and confirm/cancel buttons while
 * creatures are selected directly on the battlefield.
 */
export function ConvokeSelector() {
  const convokeSelectionState = useGameStore((state) => state.convokeSelectionState)
  const cancelConvokeSelection = useGameStore((state) => state.cancelConvokeSelection)
  const confirmConvokeSelection = useGameStore((state) => state.confirmConvokeSelection)

  const originalSymbols = useMemo(() => {
    if (!convokeSelectionState) return []
    return parseManaCost(convokeSelectionState.manaCost)
  }, [convokeSelectionState?.manaCost])

  const remainingSymbols = useMemo(() => {
    if (!convokeSelectionState) return []
    return calculateRemainingCost(originalSymbols, convokeSelectionState.selectedCreatures)
  }, [originalSymbols, convokeSelectionState?.selectedCreatures])

  if (!convokeSelectionState) return null

  const { cardName, selectedCreatures, actionInfo } = convokeSelectionState
  const isAbility = actionInfo.action.type === 'ActivateAbility'

  return (
    <div style={styles.bar}>
      <span style={styles.label}>
        {isAbility ? 'Tap creatures for' : 'Convoke'} <strong>{cardName}</strong>
      </span>
      <span style={styles.divider} />
      <span style={styles.costLabel}>Cost:</span>
      <div style={styles.manaSymbols}>
        {originalSymbols.map((symbol, i) => (
          <ManaSymbol key={i} symbol={symbol} />
        ))}
      </div>
      <span style={styles.arrow}>→</span>
      <div style={styles.manaSymbols}>
        {remainingSymbols.length > 0 ? (
          remainingSymbols.map((symbol, i) => (
            <ManaSymbol key={i} symbol={symbol} />
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
        onClick={confirmConvokeSelection}
        style={styles.confirmButton}
      >
        {isAbility ? 'Activate' : 'Cast'}
      </button>
    </div>
  )
}

function ManaSymbol({ symbol }: { symbol: string }) {
  const isColor = symbol in COLOR_CSS

  return (
    <span
      style={{
        ...styles.manaSymbol,
        backgroundColor: isColor ? COLOR_CSS[symbol] : '#888',
        color: isColor && (symbol === 'W' || symbol === 'G') ? '#000' : '#fff',
      }}
    >
      {symbol}
    </span>
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
    gap: 3,
  },
  manaSymbol: {
    width: 22,
    height: 22,
    borderRadius: 11,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: 11,
    fontWeight: 'bold',
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
}
