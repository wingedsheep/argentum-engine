import { useState } from 'react'
import { useGameStore } from '../../store/gameStore'
import type { EntityId } from '../../types'
import { useResponsive, calculateFittingCardWidth } from '../../hooks/useResponsive'
import { getCardImageUrl } from '../../utils/cardImages'

/**
 * Overlay that shows opponent's hand cards when a "look at hand" effect resolves.
 * Uses the same visual style as ZoneSelectionUI for consistency.
 */
export function RevealedHandUI() {
  const revealedHandCardIds = useGameStore((state) => state.revealedHandCardIds)
  const gameState = useGameStore((state) => state.gameState)
  const dismissRevealedHand = useGameStore((state) => state.dismissRevealedHand)
  const hoverCard = useGameStore((state) => state.hoverCard)
  const responsive = useResponsive()
  const [hoveredCardId, setHoveredCardId] = useState<EntityId | null>(null)
  const [minimized, setMinimized] = useState(false)

  if (!revealedHandCardIds || !gameState) return null

  // Get card info for each revealed card
  const cards = revealedHandCardIds
    .map((cardId) => gameState.cards[cardId])
    .filter((card) => card != null)

  // Calculate card size that fits all cards
  const availableWidth = responsive.viewportWidth - responsive.containerPadding * 2 - 64
  const gap = responsive.isMobile ? 8 : 12
  const maxCardWidth = responsive.isMobile ? 100 : 140
  const cardWidth = calculateFittingCardWidth(
    Math.min(cards.length || 1, 8),
    availableWidth,
    gap,
    maxCardWidth,
    60
  )
  const cardHeight = Math.round(cardWidth * 1.4)

  // Handle hover using global store (for the CardPreview component)
  const handleMouseEnter = (cardId: EntityId) => {
    setHoveredCardId(cardId)
    hoverCard(cardId)
  }

  const handleMouseLeave = () => {
    setHoveredCardId(null)
    hoverCard(null)
  }

  // When minimized, show floating button to restore
  if (minimized) {
    return (
      <button
        onClick={() => setMinimized(false)}
        style={{
          position: 'fixed',
          bottom: 70,
          left: '50%',
          transform: 'translateX(-50%)',
          padding: responsive.isMobile ? '10px 16px' : '12px 24px',
          fontSize: responsive.fontSize.normal,
          backgroundColor: '#1e40af',
          color: 'white',
          border: 'none',
          borderRadius: 8,
          cursor: 'pointer',
          fontWeight: 600,
          boxShadow: '0 4px 12px rgba(0,0,0,0.4)',
          zIndex: 100,
          display: 'flex',
          alignItems: 'center',
          gap: 8,
        }}
      >
        ↑ Return to Opponent's Hand
      </button>
    )
  }

  return (
    <div
      style={{
        position: 'fixed',
        top: 0,
        left: 0,
        right: 0,
        bottom: 0,
        backgroundColor: 'rgba(0, 0, 0, 0.92)',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: responsive.isMobile ? 16 : 24,
        padding: responsive.containerPadding,
        pointerEvents: 'auto',
        zIndex: 1000,
      }}
    >
      {/* Header */}
      <div style={{ textAlign: 'center' }}>
        <h2
          style={{
            color: 'white',
            margin: 0,
            fontSize: responsive.isMobile ? 20 : 28,
            fontWeight: 600,
          }}
        >
          Opponent's Hand
        </h2>
        <p
          style={{
            color: '#aaa',
            margin: '8px 0 0',
            fontSize: responsive.fontSize.normal,
          }}
        >
          {cards.length === 0
            ? 'Opponent has no cards in hand'
            : `${cards.length} card${cards.length !== 1 ? 's' : ''}`}
        </p>
      </div>

      {/* Card ribbon */}
      {cards.length > 0 && (
        <div
          style={{
            display: 'flex',
            gap,
            padding: responsive.isMobile ? 12 : 24,
            justifyContent: cards.length <= 6 ? 'center' : 'flex-start',
            overflowX: 'auto',
            maxWidth: '100%',
            scrollBehavior: 'smooth',
          }}
        >
          {cards.map((card) => {
            const isHovered = hoveredCardId === card.id
            const cardImageUrl = getCardImageUrl(card.name, card.imageUri)

            return (
              <div
                key={card.id}
                onMouseEnter={() => handleMouseEnter(card.id)}
                onMouseLeave={handleMouseLeave}
                style={{
                  width: cardWidth,
                  height: cardHeight,
                  backgroundColor: '#1a1a1a',
                  border: isHovered ? '2px solid #666' : '2px solid #333',
                  borderRadius: responsive.isMobile ? 6 : 10,
                  display: 'flex',
                  flexDirection: 'column',
                  overflow: 'hidden',
                  cursor: 'default',
                  transition: 'all 0.2s ease-out',
                  transform: isHovered ? 'translateY(-4px) scale(1.02)' : 'none',
                  boxShadow: isHovered
                    ? '0 8px 20px rgba(255, 255, 255, 0.15)'
                    : '0 4px 12px rgba(0, 0, 0, 0.6)',
                  flexShrink: 0,
                  position: 'relative',
                }}
              >
                <img
                  src={cardImageUrl}
                  alt={card.name}
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
                    padding: responsive.isMobile ? '6px' : '10px',
                    gap: 4,
                  }}
                >
                  <span
                    style={{
                      color: 'white',
                      fontSize: responsive.isMobile ? 10 : 12,
                      fontWeight: 600,
                      textAlign: 'center',
                      lineHeight: 1.2,
                    }}
                  >
                    {card.name}
                  </span>
                  {card.typeLine && (
                    <span
                      style={{
                        color: '#888',
                        fontSize: responsive.isMobile ? 8 : 10,
                        textAlign: 'center',
                      }}
                    >
                      {card.typeLine}
                    </span>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      )}

      {/* Action buttons */}
      <div style={{ display: 'flex', gap: 16 }}>
        <button
          onClick={() => setMinimized(true)}
          style={{
            padding: responsive.isMobile ? '10px 20px' : '12px 28px',
            fontSize: responsive.fontSize.normal,
            backgroundColor: '#1e40af',
            color: 'white',
            border: 'none',
            borderRadius: 8,
            cursor: 'pointer',
            transition: 'all 0.15s',
          }}
        >
          View Battlefield
        </button>
        <button
          onClick={dismissRevealedHand}
          style={{
            padding: responsive.isMobile ? '10px 24px' : '12px 36px',
            fontSize: responsive.fontSize.large,
            backgroundColor: '#16a34a',
            color: 'white',
            border: 'none',
            borderRadius: 8,
            cursor: 'pointer',
            fontWeight: 600,
            transition: 'all 0.15s',
          }}
        >
          OK
        </button>
      </div>
      {/* Card preview is handled by the global CardPreview component in GameBoard */}
    </div>
  )
}
