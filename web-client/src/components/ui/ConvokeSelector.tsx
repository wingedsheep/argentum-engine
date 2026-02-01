import { useMemo } from 'react'
import { useGameStore, ConvokeCreatureSelection } from '../../store/gameStore'
import type { ConvokeCreatureInfo } from '../../types'

// Color symbol mappings for mana display
const COLOR_SYMBOLS: Record<string, string> = {
  W: 'W',
  U: 'U',
  B: 'B',
  R: 'R',
  G: 'G',
}

const COLOR_NAMES: Record<string, string> = {
  W: 'White',
  U: 'Blue',
  B: 'Black',
  R: 'Red',
  G: 'Green',
}

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
      // Creature is paying for a specific color
      const colorIndex = remaining.indexOf(creature.payingColor)
      if (colorIndex >= 0) {
        remaining.splice(colorIndex, 1)
      }
    } else {
      // Creature is paying generic mana - find first generic symbol
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
 * Determine what color a creature should pay for based on its colors and remaining cost.
 * Returns the color symbol if the creature can pay colored mana, or null for generic.
 */
function determineBestPayment(
  creature: ConvokeCreatureInfo,
  remainingSymbols: string[]
): string | null {
  // Check if any of the creature's colors match remaining colored costs
  for (const color of creature.colors) {
    if (remainingSymbols.includes(color)) {
      return color
    }
  }
  // If no color match, will pay generic (if available)
  const hasGeneric = remainingSymbols.some(s => /^\d+$/.test(s))
  return hasGeneric ? null : null
}

/**
 * MTG Arena-style Convoke selector overlay.
 * Shown when casting spells with Convoke.
 */
export function ConvokeSelector() {
  const convokeSelectionState = useGameStore((state) => state.convokeSelectionState)
  const toggleConvokeCreature = useGameStore((state) => state.toggleConvokeCreature)
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

  const { cardName, validCreatures, selectedCreatures } = convokeSelectionState

  const isCreatureSelected = (entityId: string) =>
    selectedCreatures.some(c => c.entityId === entityId)

  const handleCreatureClick = (creature: ConvokeCreatureInfo) => {
    if (isCreatureSelected(creature.entityId)) {
      // Deselect
      toggleConvokeCreature(creature.entityId, creature.name, null)
    } else {
      // Select - determine best payment
      const payingColor = determineBestPayment(creature, remainingSymbols)
      toggleConvokeCreature(creature.entityId, creature.name, payingColor)
    }
  }

  // Check if the remaining cost can be fully paid with mana (for the confirm button)
  // For now, we assume the player has enough mana for whatever remains
  const canConfirm = true

  return (
    <div style={styles.overlay}>
      <div style={styles.container}>
        <h2 style={styles.title}>Convoke</h2>
        <p style={styles.cardName}>{cardName}</p>

        <div style={styles.costSection}>
          <div style={styles.costRow}>
            <span style={styles.costLabel}>Original Cost:</span>
            <div style={styles.manaSymbols}>
              {originalSymbols.map((symbol, i) => (
                <ManaSymbol key={i} symbol={symbol} />
              ))}
            </div>
          </div>
          <div style={styles.costRow}>
            <span style={styles.costLabel}>Remaining:</span>
            <div style={styles.manaSymbols}>
              {remainingSymbols.length > 0 ? (
                remainingSymbols.map((symbol, i) => (
                  <ManaSymbol key={i} symbol={symbol} />
                ))
              ) : (
                <span style={styles.freeCast}>Free!</span>
              )}
            </div>
          </div>
        </div>

        <div style={styles.creaturesSection}>
          <p style={styles.creaturesLabel}>
            Tap creatures to reduce the cost:
          </p>
          <div style={styles.creatureGrid}>
            {validCreatures.map((creature) => {
              const selected = isCreatureSelected(creature.entityId)
              const selection = selectedCreatures.find(c => c.entityId === creature.entityId)
              return (
                <button
                  key={creature.entityId}
                  onClick={() => handleCreatureClick(creature)}
                  style={{
                    ...styles.creatureButton,
                    ...(selected ? styles.creatureButtonSelected : {}),
                  }}
                >
                  <span style={styles.creatureName}>{creature.name}</span>
                  <div style={styles.creatureColors}>
                    {creature.colors.length > 0 ? (
                      creature.colors.map((color, i) => (
                        <span
                          key={i}
                          style={{
                            ...styles.colorDot,
                            backgroundColor: COLOR_CSS[color] ?? '#888',
                          }}
                          title={COLOR_NAMES[color] ?? color}
                        />
                      ))
                    ) : (
                      <span style={styles.colorless}>Colorless</span>
                    )}
                  </div>
                  {selected && selection?.payingColor && (
                    <span style={styles.payingFor}>
                      Paying {COLOR_SYMBOLS[selection.payingColor]}
                    </span>
                  )}
                  {selected && !selection?.payingColor && (
                    <span style={styles.payingFor}>Paying generic</span>
                  )}
                </button>
              )
            })}
          </div>
        </div>

        <p style={styles.hint}>
          Tap {selectedCreatures.length} creature{selectedCreatures.length !== 1 ? 's' : ''} selected
        </p>

        <div style={styles.buttonRow}>
          <button onClick={cancelConvokeSelection} style={styles.cancelButton}>
            Cancel
          </button>
          <button
            onClick={confirmConvokeSelection}
            disabled={!canConfirm}
            style={{
              ...styles.confirmButton,
              opacity: canConfirm ? 1 : 0.5,
              cursor: canConfirm ? 'pointer' : 'not-allowed',
            }}
          >
            Cast
          </button>
        </div>
      </div>
    </div>
  )
}

/**
 * Renders a single mana symbol.
 */
function ManaSymbol({ symbol }: { symbol: string }) {
  const isGeneric = /^\d+$/.test(symbol)
  const isColor = Object.keys(COLOR_SYMBOLS).includes(symbol)

  return (
    <span
      style={{
        ...styles.manaSymbol,
        backgroundColor: isColor ? COLOR_CSS[symbol] : '#888',
        color: isColor && (symbol === 'W' || symbol === 'G') ? '#000' : '#fff',
      }}
    >
      {isGeneric ? symbol : COLOR_SYMBOLS[symbol] ?? symbol}
    </span>
  )
}

const styles: Record<string, React.CSSProperties> = {
  overlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(0, 0, 0, 0.85)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    zIndex: 1500,
  },
  container: {
    backgroundColor: '#1a1a2e',
    borderRadius: 12,
    padding: 24,
    minWidth: 400,
    maxWidth: 500,
    maxHeight: '80vh',
    overflowY: 'auto',
    border: '2px solid #4a4a6a',
    boxShadow: '0 8px 32px rgba(0, 0, 0, 0.5)',
  },
  title: {
    margin: '0 0 8px 0',
    color: '#fff',
    fontSize: 20,
    textAlign: 'center',
  },
  cardName: {
    margin: '0 0 20px 0',
    color: '#aaa',
    fontSize: 16,
    textAlign: 'center',
    fontStyle: 'italic',
  },
  costSection: {
    marginBottom: 20,
    padding: 16,
    backgroundColor: '#252540',
    borderRadius: 8,
  },
  costRow: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 8,
  },
  costLabel: {
    color: '#888',
    fontSize: 14,
  },
  manaSymbols: {
    display: 'flex',
    gap: 4,
  },
  manaSymbol: {
    width: 24,
    height: 24,
    borderRadius: 12,
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: 12,
    fontWeight: 'bold',
  },
  freeCast: {
    color: '#4caf50',
    fontWeight: 'bold',
    fontSize: 14,
  },
  creaturesSection: {
    marginBottom: 16,
  },
  creaturesLabel: {
    color: '#888',
    fontSize: 14,
    margin: '0 0 12px 0',
  },
  creatureGrid: {
    display: 'flex',
    flexDirection: 'column',
    gap: 8,
  },
  creatureButton: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '10px 14px',
    backgroundColor: '#333',
    border: '2px solid #444',
    borderRadius: 8,
    cursor: 'pointer',
    transition: 'all 0.2s',
  },
  creatureButtonSelected: {
    backgroundColor: '#1a4a1a',
    borderColor: '#4caf50',
  },
  creatureName: {
    color: '#fff',
    fontSize: 14,
    fontWeight: 500,
    flex: 1,
    textAlign: 'left',
  },
  creatureColors: {
    display: 'flex',
    gap: 4,
    marginLeft: 12,
  },
  colorDot: {
    width: 16,
    height: 16,
    borderRadius: 8,
    border: '1px solid #666',
  },
  colorless: {
    color: '#666',
    fontSize: 11,
  },
  payingFor: {
    color: '#4caf50',
    fontSize: 11,
    marginLeft: 8,
    fontStyle: 'italic',
  },
  hint: {
    color: '#666',
    fontSize: 13,
    textAlign: 'center',
    margin: '0 0 20px 0',
  },
  buttonRow: {
    display: 'flex',
    gap: 12,
    justifyContent: 'center',
  },
  cancelButton: {
    padding: '10px 24px',
    fontSize: 16,
    backgroundColor: '#444',
    color: '#fff',
    border: 'none',
    borderRadius: 8,
    cursor: 'pointer',
  },
  confirmButton: {
    padding: '10px 24px',
    fontSize: 16,
    backgroundColor: '#0066cc',
    color: '#fff',
    border: 'none',
    borderRadius: 8,
    cursor: 'pointer',
  },
}
