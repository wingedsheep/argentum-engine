import { useMemo, useState } from 'react'
import { useGameStore } from '../../store/gameStore'
import { getCardImageUrl } from '../../utils/cardImages'
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
                <DelveCard
                  key={card.entityId}
                  name={card.name}
                  imageUri={card.imageUri}
                  selected={selected}
                  onClick={() => toggleDelveCard(card.entityId)}
                />
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
 * A single card in the Delve selection grid, showing the card image with a selection overlay.
 */
function DelveCard({
  name,
  imageUri,
  selected,
  onClick,
}: {
  name: string
  imageUri?: string | null | undefined
  selected: boolean
  onClick: () => void
}) {
  const [imgError, setImgError] = useState(false)
  const imageUrl = getCardImageUrl(name, imageUri, 'normal')

  return (
    <button
      onClick={onClick}
      style={{
        ...styles.cardButton,
        ...(selected ? styles.cardButtonSelected : {}),
      }}
      aria-label={name}
    >
      {!imgError ? (
        <img
          src={imageUrl}
          alt={name}
          style={styles.cardImage}
          onError={() => setImgError(true)}
        />
      ) : (
        <div style={styles.cardFallback}>
          <span style={styles.cardFallbackName}>{name}</span>
        </div>
      )}
      {selected && (
        <div style={styles.selectedOverlay}>
          <span style={styles.exileLabel}>EXILE</span>
        </div>
      )}
    </button>
  )
}

/**
 * Renders a single mana symbol.
 */
function ManaSymbol({ symbol }: { symbol: string }) {
  const isColor = Object.keys(COLOR_CSS).includes(symbol)

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
    maxWidth: '90vw',
    maxHeight: '90vh',
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
    flexWrap: 'wrap',
    gap: 10,
    justifyContent: 'center',
    maxHeight: '50vh',
    overflowY: 'auto',
    padding: '4px 0',
  },
  cardButton: {
    position: 'relative',
    width: 130,
    height: 182,
    padding: 0,
    backgroundColor: 'transparent',
    border: '3px solid transparent',
    borderRadius: 8,
    cursor: 'pointer',
    overflow: 'hidden',
    transition: 'border-color 0.2s, transform 0.15s',
    flexShrink: 0,
  },
  cardButtonSelected: {
    borderColor: '#6a8aff',
    transform: 'translateY(-4px)',
  },
  cardImage: {
    width: '100%',
    height: '100%',
    objectFit: 'cover',
    borderRadius: 5,
  },
  cardFallback: {
    width: '100%',
    height: '100%',
    backgroundColor: '#333',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 8,
    borderRadius: 5,
  },
  cardFallbackName: {
    color: '#fff',
    fontSize: 12,
    textAlign: 'center',
    wordBreak: 'break-word',
  },
  selectedOverlay: {
    position: 'absolute',
    top: 0,
    left: 0,
    right: 0,
    bottom: 0,
    backgroundColor: 'rgba(106, 138, 255, 0.35)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    borderRadius: 5,
  },
  exileLabel: {
    color: '#fff',
    fontSize: 14,
    fontWeight: 'bold',
    textShadow: '0 1px 4px rgba(0,0,0,0.8)',
    letterSpacing: 2,
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
