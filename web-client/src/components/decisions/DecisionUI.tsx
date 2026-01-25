import { useState } from 'react'
import { useGameStore } from '../../store/gameStore'
import type { EntityId, SelectCardsDecision } from '../../types'
import { useResponsive, calculateFittingCardWidth, type ResponsiveSizes } from '../../hooks/useResponsive'

/**
 * Decision UI overlay for pending decisions (e.g., discard to hand size).
 */
export function DecisionUI() {
  const pendingDecision = useGameStore((state) => state.pendingDecision)
  const responsive = useResponsive()

  if (!pendingDecision) return null

  // Only handle SelectCardsDecision for now
  if (pendingDecision.type !== 'SelectCardsDecision') return null

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
        gap: responsive.isMobile ? 12 : 24,
        padding: responsive.containerPadding,
        pointerEvents: 'auto',
        zIndex: 1000,
      }}
    >
      <CardSelectionDecision decision={pendingDecision} responsive={responsive} />
    </div>
  )
}

/**
 * Card selection decision - select cards from a list.
 */
function CardSelectionDecision({
  decision,
  responsive,
}: {
  decision: SelectCardsDecision
  responsive: ResponsiveSizes
}) {
  const [selectedCards, setSelectedCards] = useState<EntityId[]>([])
  const submitDecision = useGameStore((s) => s.submitDecision)
  const gameState = useGameStore((s) => s.gameState)

  const canConfirm =
    selectedCards.length >= decision.minSelections &&
    selectedCards.length <= decision.maxSelections

  // Calculate card size that fits all cards
  const availableWidth = responsive.viewportWidth - responsive.containerPadding * 2 - 32
  const gap = responsive.isMobile ? 4 : 8
  const maxCardWidth = responsive.isMobile ? 90 : 130
  const cardWidth = calculateFittingCardWidth(
    decision.options.length,
    availableWidth,
    gap,
    maxCardWidth,
    45
  )

  const toggleCard = (cardId: EntityId) => {
    setSelectedCards((prev) => {
      if (prev.includes(cardId)) {
        return prev.filter((id) => id !== cardId)
      }
      // Don't allow selecting more than max
      if (prev.length >= decision.maxSelections) {
        return prev
      }
      return [...prev, cardId]
    })
  }

  const handleConfirm = () => {
    submitDecision(selectedCards)
    setSelectedCards([])
  }

  return (
    <>
      <h2
        style={{
          color: 'white',
          margin: 0,
          fontSize: responsive.isMobile ? 18 : 24,
          textAlign: 'center',
        }}
      >
        {decision.prompt}
      </h2>

      <p style={{ color: '#888', margin: 0, fontSize: responsive.fontSize.normal }}>
        Selected: {selectedCards.length} / {decision.minSelections}
        {decision.minSelections !== decision.maxSelections &&
          ` - ${decision.maxSelections}`}
      </p>

      {/* Card options */}
      <div
        style={{
          display: 'flex',
          gap,
          padding: responsive.isMobile ? 8 : 16,
          justifyContent: 'center',
          flexWrap: 'wrap',
          maxWidth: '100%',
        }}
      >
        {decision.options.map((cardId) => {
          const card = gameState?.cards[cardId]
          return (
            <DecisionCard
              key={cardId}
              cardId={cardId}
              cardName={card?.name || 'Unknown Card'}
              isSelected={selectedCards.includes(cardId)}
              onClick={() => toggleCard(cardId)}
              cardWidth={cardWidth}
              isMobile={responsive.isMobile}
            />
          )
        })}
      </div>

      {/* Confirm button */}
      <button
        onClick={handleConfirm}
        disabled={!canConfirm}
        style={{
          padding: responsive.isMobile ? '10px 20px' : '12px 32px',
          fontSize: responsive.fontSize.large,
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
 * Card display for decision UI.
 */
function DecisionCard({
  cardId: _cardId,
  cardName,
  isSelected,
  onClick,
  cardWidth = 130,
  isMobile = false,
}: {
  cardId: EntityId
  cardName: string
  isSelected: boolean
  onClick: () => void
  cardWidth?: number
  isMobile?: boolean
}) {
  const cardImageUrl = `https://api.scryfall.com/cards/named?exact=${encodeURIComponent(cardName)}&format=image&version=normal`

  const cardRatio = 1.4
  const cardHeight = Math.round(cardWidth * cardRatio)

  return (
    <div
      onClick={onClick}
      style={{
        width: cardWidth,
        height: cardHeight,
        backgroundColor: isSelected ? '#330000' : '#1a1a1a',
        border: isSelected ? '3px solid #ff4444' : '2px solid #444',
        borderRadius: isMobile ? 6 : 10,
        display: 'flex',
        flexDirection: 'column',
        overflow: 'hidden',
        cursor: 'pointer',
        transition: 'all 0.15s',
        transform: isSelected ? 'translateY(-8px) scale(1.05)' : 'none',
        boxShadow: isSelected
          ? '0 8px 20px rgba(255, 68, 68, 0.3)'
          : '0 4px 8px rgba(0, 0, 0, 0.5)',
        flexShrink: 0,
        position: 'relative',
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
          e.currentTarget.style.display = 'none'
          const fallback = e.currentTarget.nextElementSibling as HTMLElement
          if (fallback) fallback.style.display = 'flex'
        }}
      />

      {/* Fallback when image fails */}
      <div
        style={{
          position: 'absolute',
          inset: 0,
          backgroundColor: '#1a1a1a',
          display: 'none',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          padding: isMobile ? '4px' : '8px',
        }}
      >
        <span
          style={{
            color: 'white',
            fontSize: isMobile ? 9 : 11,
            fontWeight: 500,
            textAlign: 'center',
          }}
        >
          {cardName}
        </span>
      </div>

      {/* Selection indicator */}
      {isSelected && (
        <div
          style={{
            position: 'absolute',
            top: 4,
            right: 4,
            width: 20,
            height: 20,
            backgroundColor: '#ff4444',
            borderRadius: '50%',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: 'white',
            fontWeight: 'bold',
            fontSize: 12,
          }}
        >
          ✓
        </div>
      )}
    </div>
  )
}
