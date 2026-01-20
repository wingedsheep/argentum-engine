import { useGameStore, type MulliganState } from '../../store/gameStore'
import { useCard } from '../../store/selectors'
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
        position: 'absolute',
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
        {state.hand.map((cardId) => (
          <MulliganCard key={cardId} cardId={cardId} selectable={false} />
        ))}
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
        {state.hand.map((cardId) => (
          <MulliganCard
            key={cardId}
            cardId={cardId}
            selectable
            isSelected={state.selectedCards.includes(cardId)}
            onClick={() => toggleMulliganCard(cardId)}
          />
        ))}
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
 */
function MulliganCard({
  cardId,
  selectable,
  isSelected = false,
  onClick,
}: {
  cardId: EntityId
  selectable: boolean
  isSelected?: boolean
  onClick?: () => void
}) {
  const card = useCard(cardId)

  if (!card) return null

  return (
    <div
      onClick={selectable ? onClick : undefined}
      style={{
        width: 120,
        height: 168,
        backgroundColor: isSelected ? '#003300' : '#222',
        border: isSelected ? '3px solid #00ff00' : '1px solid #444',
        borderRadius: 8,
        display: 'flex',
        flexDirection: 'column',
        padding: 8,
        cursor: selectable ? 'pointer' : 'default',
        transition: 'all 0.15s',
        transform: isSelected ? 'translateY(-8px)' : 'none',
      }}
    >
      {/* Card name */}
      <span
        style={{
          color: 'white',
          fontSize: 12,
          fontWeight: 500,
          marginBottom: 4,
        }}
      >
        {card.name}
      </span>

      {/* Mana cost */}
      <span style={{ color: '#888', fontSize: 11 }}>{card.manaCost}</span>

      {/* Type line */}
      <span
        style={{
          color: '#666',
          fontSize: 10,
          marginTop: 'auto',
        }}
      >
        {card.typeLine}
      </span>

      {/* P/T for creatures */}
      {card.power !== null && card.toughness !== null && (
        <span
          style={{
            color: 'white',
            fontSize: 12,
            fontWeight: 'bold',
            alignSelf: 'flex-end',
          }}
        >
          {card.power}/{card.toughness}
        </span>
      )}
    </div>
  )
}
