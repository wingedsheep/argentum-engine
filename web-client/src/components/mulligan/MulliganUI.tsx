import { useGameStore, type MulliganState } from '../../store/gameStore'
import type { EntityId } from '../../types'

/**
 * Mulligan UI overlay.
 */
export function MulliganUI() {
  const mulliganState = useGameStore((state) => state.mulliganState)

  if (!mulliganState) return null

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.85)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 24,
        pointerEvents: 'auto',
        zIndex: 1000,
      }}
    >
      {mulliganState.phase === 'deciding' ? (
        <MulliganDecision state={mulliganState} />
      ) : (
        <ChooseBottomCards state={mulliganState} />
      )}
    </div>
  )
}

/**
 * Mulligan decision phase - keep or mulligan.
 */
function MulliganDecision({ state }: { state: MulliganState }) {
  const keepHand = useGameStore((s) => s.keepHand)
  const mulligan = useGameStore((s) => s.mulligan)

  return (
    <>
      <h2 style={{ color: 'white', margin: 0 }}>
        {state.mulliganCount === 0
          ? 'Opening Hand'
          : `Mulligan ${state.mulliganCount}`}
      </h2>

      <p style={{ color: '#888', margin: 0 }}>
        {state.mulliganCount > 0 &&
          `If you keep, you'll put ${state.cardsToPutOnBottom} card(s) on the bottom.`}
      </p>

      {/* Hand preview */}
      <div
        style={{
          display: 'flex',
          gap: 8,
          padding: 16,
        }}
      >
        {state.hand.map((cardId) => {
          const cardInfo = state.cards[cardId]
          return (
            <MulliganCard
              key={cardId}
              cardId={cardId}
              cardName={cardInfo?.name || 'Unknown'}
              imageUri={cardInfo?.imageUri}
              selectable={false}
            />
          )
        })}
      </div>

      {/* Action buttons */}
      <div style={{ display: 'flex', gap: 16 }}>
        <button
          onClick={keepHand}
          style={{
            padding: '12px 32px',
            fontSize: 18,
            backgroundColor: '#00aa00',
            color: 'white',
            border: 'none',
            borderRadius: 8,
            cursor: 'pointer',
          }}
        >
          Keep Hand
        </button>

        <button
          onClick={mulligan}
          style={{
            padding: '12px 32px',
            fontSize: 18,
            backgroundColor: '#cc6600',
            color: 'white',
            border: 'none',
            borderRadius: 8,
            cursor: 'pointer',
          }}
        >
          Mulligan
        </button>
      </div>
    </>
  )
}

/**
 * Choose cards to put on bottom after keeping.
 */
function ChooseBottomCards({ state }: { state: MulliganState }) {
  const chooseBottomCards = useGameStore((s) => s.chooseBottomCards)
  const toggleMulliganCard = useGameStore((s) => s.toggleMulliganCard)

  const canConfirm = state.selectedCards.length === state.cardsToPutOnBottom

  return (
    <>
      <h2 style={{ color: 'white', margin: 0 }}>
        Choose {state.cardsToPutOnBottom} Card
        {state.cardsToPutOnBottom > 1 ? 's' : ''} for Bottom
      </h2>

      <p style={{ color: '#888', margin: 0 }}>
        Selected: {state.selectedCards.length} / {state.cardsToPutOnBottom}
      </p>

      {/* Hand with selectable cards */}
      <div
        style={{
          display: 'flex',
          gap: 8,
          padding: 16,
        }}
      >
        {state.hand.map((cardId) => {
          const cardInfo = state.cards[cardId]
          return (
            <MulliganCard
              key={cardId}
              cardId={cardId}
              cardName={cardInfo?.name || 'Unknown'}
              imageUri={cardInfo?.imageUri}
              selectable
              isSelected={state.selectedCards.includes(cardId)}
              onClick={() => toggleMulliganCard(cardId)}
            />
          )
        })}
      </div>

      {/* Confirm button */}
      <button
        onClick={() => chooseBottomCards(state.selectedCards)}
        disabled={!canConfirm}
        style={{
          padding: '12px 32px',
          fontSize: 18,
          backgroundColor: canConfirm ? '#00aa00' : '#444',
          color: canConfirm ? 'white' : '#888',
          border: 'none',
          borderRadius: 8,
          cursor: canConfirm ? 'pointer' : 'not-allowed',
        }}
      >
        Confirm
      </button>
    </>
  )
}

/**
 * Card display for mulligan UI.
 * Uses imageUri from server if available, otherwise falls back to Scryfall API.
 */
function MulliganCard({
  cardId: _cardId,
  cardName,
  imageUri,
  selectable,
  isSelected = false,
  onClick,
}: {
  cardId: EntityId
  cardName: string
  imageUri?: string | null | undefined
  selectable: boolean
  isSelected?: boolean
  onClick?: () => void
}) {
  // Use provided imageUri or fall back to Scryfall API
  const cardImageUrl = imageUri || `https://api.scryfall.com/cards/named?exact=${encodeURIComponent(cardName)}&format=image&version=normal`

  return (
    <div
      onClick={selectable ? onClick : undefined}
      style={{
        width: 130,
        height: 182,
        backgroundColor: isSelected ? '#003300' : '#1a1a1a',
        border: isSelected ? '3px solid #00ff00' : '2px solid #444',
        borderRadius: 10,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
        cursor: selectable ? 'pointer' : 'default',
        transition: 'all 0.15s',
        transform: isSelected ? 'translateY(-8px) scale(1.05)' : 'none',
        boxShadow: isSelected ? '0 8px 20px rgba(0, 255, 0, 0.3)' : '0 4px 8px rgba(0, 0, 0, 0.5)',
      }}
    >
      {/* Card image */}
      <img
        src={cardImageUrl}
        alt={cardName}
        style={{
          width: '100%',
          height: '100%',
          objectFit: 'cover',
        }}
        onError={(e) => {
          // Hide image on error and show placeholder
          e.currentTarget.style.display = 'none'
        }}
      />

      {/* Fallback text (shown if image fails) */}
      <div
        style={{
          position: 'absolute',
          bottom: 0,
          left: 0,
          right: 0,
          backgroundColor: 'rgba(0, 0, 0, 0.8)',
          padding: '8px',
          display: 'none', // Will be shown via CSS when image fails
        }}
      >
        <span
          style={{
            color: 'white',
            fontSize: 11,
            fontWeight: 500,
          }}
        >
          {cardName}
        </span>
      </div>
    </div>
  )
}
