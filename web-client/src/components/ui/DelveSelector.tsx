import { useMemo, useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import { parseManaCost } from '@/utils/manaCost.ts'
import { ManaSymbol } from './ManaSymbols'
import type { EntityId } from '@/types'

/**
 * Delve selector overlay.
 * Shows graveyard cards to exile for delve with a castability indicator
 * showing remaining mana cost and how many more cards to exile to cast.
 * The remaining cost and whether the selection is payable are the server's cost preview for
 * the draft as it stands; the minimum delve needed comes from the enumerator's mana solver.
 * Neither gates Confirm — the server validates the submission and says why if it declines.
 */
export function DelveSelector() {
  const delveSelectionState = useGameStore((state) => state.delveSelectionState)
  const costPreview = useGameStore((state) => state.costPreview)
  const toggleDelveCard = useGameStore((state) => state.toggleDelveCard)
  const cancelDelveSelection = useGameStore((state) => state.cancelDelveSelection)
  const confirmDelveSelection = useGameStore((state) => state.confirmDelveSelection)
  const [viewingBattlefield, setViewingBattlefield] = useState(false)

  const preview = costPreview?.preview ?? null
  const previewPending = costPreview?.pending ?? true
  const remainingSymbols = useMemo(
    () => (preview ? parseManaCost(preview.manaCostString) : null),
    [preview?.manaCostString],
  )

  const castInfo = useMemo(() => {
    if (!delveSelectionState) return null
    const { selectedCards: selected, minDelveNeeded } = delveSelectionState
    const delveCount = selected.length
    // The server's verdict when it has one; the enumerator's minimum until then.
    const isCastable = preview ? preview.affordable : delveCount >= minDelveNeeded
    const moreNeeded = Math.max(0, minDelveNeeded - delveCount)
    return { isCastable, moreNeeded, minDelveNeeded, reason: preview?.error ?? null }
  }, [preview, delveSelectionState?.selectedCards, delveSelectionState?.minDelveNeeded])

  if (!delveSelectionState) return null

  const { cardName, validCards, selectedCards, maxDelve } = delveSelectionState
  const isCardSelected = (eid: EntityId) => selectedCards.includes(eid)

  // When viewing battlefield, show just a floating return button
  if (viewingBattlefield) {
    return (
      <div style={styles.floatingReturn}>
        <button
          onClick={() => setViewingBattlefield(false)}
          style={styles.returnButton}
        >
          Back to Delve
        </button>
      </div>
    )
  }

  return (
    <div style={styles.overlay}>
      <div style={styles.container}>
        <h2 style={styles.title}>Delve</h2>
        <p style={styles.cardName}>{cardName}</p>

        {/* Castability status */}
        {castInfo && (
          <div style={{
            ...styles.castStatus,
            borderColor: castInfo.isCastable ? 'rgba(74, 222, 128, 0.5)' : 'rgba(255, 255, 255, 0.15)',
          }}>
            <div style={styles.castStatusRow}>
              <span style={styles.castStatusLabel}>Remaining cost:</span>
              <div style={{ ...styles.manaSymbols, opacity: previewPending ? 0.55 : 1 }}>
                {remainingSymbols === null ? (
                  <span style={{ color: '#888', fontSize: 14 }}>…</span>
                ) : remainingSymbols.length > 0 ? (
                  remainingSymbols.map((symbol, i) => (
                    <ManaSymbol key={i} symbol={symbol} size={20} />
                  ))
                ) : (
                  <span style={{ color: '#4ade80', fontWeight: 600, fontSize: 14 }}>Free!</span>
                )}
              </div>
            </div>
            <div style={{
              color: castInfo.isCastable ? '#4ade80' : '#fbbf24',
              fontSize: 13,
              fontWeight: 600,
              textAlign: 'center',
              marginTop: 6,
            }} title={castInfo.reason ?? undefined}>
              {castInfo.isCastable
                ? `Castable (minimum ${castInfo.minDelveNeeded} card${castInfo.minDelveNeeded !== 1 ? 's' : ''})`
                : castInfo.moreNeeded > 0
                  ? `Exile ${castInfo.moreNeeded} more card${castInfo.moreNeeded !== 1 ? 's' : ''} to cast (minimum ${castInfo.minDelveNeeded})`
                  : castInfo.reason ?? "Can't pay this yet"
              }
            </div>
          </div>
        )}

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
          {selectedCards.length < maxDelve && (
            <span style={styles.delveRemaining}>
              {' '}&middot; exile {maxDelve - selectedCards.length} more to pay only colored mana
            </span>
          )}
          {selectedCards.length === maxDelve && (
            <span style={styles.delveComplete}>
              {' '}&middot; generic cost fully covered
            </span>
          )}
        </p>

        <div style={styles.buttonRow}>
          <button
            onClick={() => setViewingBattlefield(true)}
            style={styles.viewBattlefieldButton}
          >
            View Battlefield
          </button>
          <button onClick={cancelDelveSelection} style={styles.cancelButton}>
            Cancel
          </button>
          <button
            onClick={confirmDelveSelection}
            disabled={!castInfo?.isCastable}
            style={{
              ...styles.confirmButton,
              ...(!castInfo?.isCastable ? styles.confirmButtonDisabled : {}),
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
    margin: '0 0 16px 0',
    color: '#aaa',
    fontSize: 16,
    textAlign: 'center',
    fontStyle: 'italic',
  },
  castStatus: {
    marginBottom: 16,
    padding: '12px 16px',
    backgroundColor: '#252540',
    borderRadius: 8,
    border: '1px solid rgba(255, 255, 255, 0.15)',
  },
  castStatusRow: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  castStatusLabel: {
    color: '#888',
    fontSize: 14,
  },
  manaSymbols: {
    display: 'flex',
    gap: 3,
    alignItems: 'center',
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
  delveRemaining: {
    color: '#e0a030',
  },
  delveComplete: {
    color: '#4caf50',
  },
  buttonRow: {
    display: 'flex',
    gap: 12,
    justifyContent: 'center',
  },
  viewBattlefieldButton: {
    padding: '10px 24px',
    fontSize: 16,
    fontWeight: 600,
    backgroundColor: '#3b82f6',
    color: '#fff',
    border: 'none',
    borderRadius: 8,
    cursor: 'pointer',
    minWidth: 120,
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
  confirmButtonDisabled: {
    backgroundColor: '#333',
    color: '#666',
    cursor: 'not-allowed',
  },
  // Floating return button (visible when viewing battlefield)
  floatingReturn: {
    position: 'fixed',
    bottom: 70,
    left: '50%',
    transform: 'translateX(-50%)',
    zIndex: 1500,
  },
  returnButton: {
    padding: '10px 24px',
    fontSize: 16,
    fontWeight: 600,
    backgroundColor: '#3b82f6',
    color: '#fff',
    border: 'none',
    borderRadius: 8,
    cursor: 'pointer',
    boxShadow: '0 4px 12px rgba(0, 0, 0, 0.5)',
    whiteSpace: 'nowrap',
  },
}
