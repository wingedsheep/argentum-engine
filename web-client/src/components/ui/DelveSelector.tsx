import { useMemo } from 'react'
import { useGameStore } from '../../store/gameStore'
import type { EntityId } from '../../types'

/**
 * Parse a mana cost string into individual symbols.
 * e.g., "{4}{U}{B}" -> ["4", "U", "B"]
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
 * Calculate remaining mana cost after applying Delve exile count.
 */
function calculateRemainingCost(
  originalSymbols: string[],
  delveCount: number
): string[] {
  const remaining = [...originalSymbols]

  let reductionsLeft = delveCount
  for (let i = 0; i < remaining.length && reductionsLeft > 0; i++) {
    const symbol = remaining[i]!
    if (/^\d+$/.test(symbol)) {
      const genericValue = parseInt(symbol, 10)
      if (genericValue > reductionsLeft) {
        remaining[i] = String(genericValue - reductionsLeft)
        reductionsLeft = 0
      } else {
        reductionsLeft -= genericValue
        remaining.splice(i, 1)
        i--
      }
    }
  }

  return remaining
}

const COLOR_CSS: Record<string, string> = {
  W: '#f8f6d8',
  U: '#0e68ab',
  B: '#150b00',
  R: '#d3202a',
  G: '#00733e',
}

/**
 * MTG Arena-style Delve selector overlay.
 * Shown when casting spells with Delve to select graveyard cards to exile.
 */
export function DelveSelector() {
  const delveSelectionState = useGameStore((state) => state.delveSelectionState)
  const toggleDelveCard = useGameStore((state) => state.toggleDelveCard)
  const cancelDelveSelection = useGameStore((state) => state.cancelDelveSelection)
  const confirmDelveSelection = useGameStore((state) => state.confirmDelveSelection)

  const originalSymbols = useMemo(() => {
    if (!delveSelectionState) return []
    return parseManaCost(delveSelectionState.manaCost)
  }, [delveSelectionState?.manaCost])

  const remainingSymbols = useMemo(() => {
    if (!delveSelectionState) return []
    return calculateRemainingCost(originalSymbols, delveSelectionState.selectedCards.length)
  }, [originalSymbols, delveSelectionState?.selectedCards])

  if (!delveSelectionState) return null

  const { cardName, validCards, selectedCards, maxDelve } = delveSelectionState

  const isCardSelected = (eid: EntityId) => selectedCards.includes(eid)

  return (
    <div style={styles.overlay}>
      <div style={styles.container}>
        <h2 style={styles.title}>Delve</h2>
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

        <div style={styles.cardsSection}>
          <p style={styles.cardsLabel}>
            Exile cards from your graveyard to reduce the cost (up to {maxDelve}):
          </p>
          <div style={styles.cardGrid}>
            {validCards.map((card) => {
              const selected = isCardSelected(card.entityId)
              return (
                <button
                  key={card.entityId}
                  onClick={() => toggleDelveCard(card.entityId)}
                  style={{
                    ...styles.cardButton,
                    ...(selected ? styles.cardButtonSelected : {}),
                  }}
                >
                  <span style={styles.cardNameText}>{card.name}</span>
                  {selected && (
                    <span style={styles.exileLabel}>Exile</span>
                  )}
                </button>
              )
            })}
          </div>
        </div>

        <p style={styles.hint}>
          {selectedCards.length} card{selectedCards.length !== 1 ? 's' : ''} selected to exile
        </p>

        <div style={styles.buttonRow}>
          <button onClick={cancelDelveSelection} style={styles.cancelButton}>
            Cancel
          </button>
          <button
            onClick={confirmDelveSelection}
            style={styles.confirmButton}
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
  const isColor = Object.keys(COLOR_CSS).includes(symbol)

  return (
    <span
      style={{
        ...styles.manaSymbol,
        backgroundColor: isColor ? COLOR_CSS[symbol] : '#888',
        color: isColor && (symbol === 'W' || symbol === 'G') ? '#000' : '#fff',
      }}
    >
      {isGeneric ? symbol : symbol}
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
  cardsSection: {
    marginBottom: 16,
  },
  cardsLabel: {
    color: '#888',
    fontSize: 14,
    margin: '0 0 12px 0',
  },
  cardGrid: {
    display: 'flex',
    flexDirection: 'column',
    gap: 6,
    maxHeight: 300,
    overflowY: 'auto',
  },
  cardButton: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    padding: '8px 14px',
    backgroundColor: '#333',
    border: '2px solid #444',
    borderRadius: 8,
    cursor: 'pointer',
    transition: 'all 0.2s',
  },
  cardButtonSelected: {
    backgroundColor: '#1a1a4a',
    borderColor: '#6a8aff',
  },
  cardNameText: {
    color: '#fff',
    fontSize: 14,
    fontWeight: 500,
    flex: 1,
    textAlign: 'left',
  },
  exileLabel: {
    color: '#6a8aff',
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
