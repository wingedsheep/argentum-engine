import { useState } from 'react'
import { useGameStore } from '../../store/gameStore'
import type { EntityId } from '../../types'
import { useResponsive, calculateFittingCardWidth } from '../../hooks/useResponsive'
import { getCardImageUrl } from '../../utils/cardImages'
import styles from './RevealedCardsUI.module.css'

/**
 * Overlay that shows cards revealed by the opponent (e.g., from Sylvan Tutor).
 * Only shown to opponents - the revealing player already knows what they searched for.
 * Uses the same visual style as RevealedHandUI for consistency.
 */
export function RevealedCardsUI() {
  const revealedCardsInfo = useGameStore((state) => state.revealedCardsInfo)
  const gameState = useGameStore((state) => state.gameState)
  const dismissRevealedCards = useGameStore((state) => state.dismissRevealedCards)
  const hoverCard = useGameStore((state) => state.hoverCard)
  const responsive = useResponsive()
  const [hoveredCardId, setHoveredCardId] = useState<EntityId | null>(null)

  if (!revealedCardsInfo || !gameState) return null

  // Get card info for each revealed card
  const cards = revealedCardsInfo.cardIds
    .map((cardId, index) => {
      const card = gameState.cards[cardId]
      // Fall back to the card names and imageUris from the event if card is not in state
      // (e.g., it went to top of library and isn't visible)
      return card ?? {
        id: cardId,
        name: revealedCardsInfo.cardNames[index] ?? 'Unknown Card',
        imageUri: revealedCardsInfo.imageUris[index] ?? null,
        typeLine: null,
      }
    })

  // Calculate card size that fits all cards
  const availableWidth = responsive.viewportWidth - responsive.containerPadding * 2 - 64
  const gap = responsive.isMobile ? 8 : 12
  const maxCardWidth = responsive.isMobile ? 120 : 180
  const cardWidth = calculateFittingCardWidth(
    Math.min(cards.length || 1, 6),
    availableWidth,
    gap,
    maxCardWidth,
    80
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

  const sourceText = revealedCardsInfo.source ? ` (${revealedCardsInfo.source})` : ''

  return (
    <div className={styles.overlay}>
      {/* Header */}
      <div className={styles.header}>
        <h2 className={styles.title}>
          Opponent Revealed{sourceText}
        </h2>
        <p className={styles.subtitle}>
          {revealedCardsInfo.cardNames.join(', ')}
        </p>
      </div>

      {/* Card ribbon */}
      {cards.length > 0 && (
        <div className={styles.cardRibbon} style={{ gap }}>
          {cards.map((card) => {
            const isHovered = hoveredCardId === card.id
            const cardImageUrl = getCardImageUrl(card.name, card.imageUri)

            return (
              <div
                key={card.id}
                onMouseEnter={() => handleMouseEnter(card.id)}
                onMouseLeave={handleMouseLeave}
                className={styles.card}
                style={{
                  width: cardWidth,
                  height: cardHeight,
                  borderColor: isHovered ? 'var(--color-highlight)' : undefined,
                  transform: isHovered ? 'translateY(-4px) scale(1.02)' : undefined,
                  boxShadow: isHovered ? '0 8px 20px var(--color-highlight-shadow)' : undefined,
                }}
              >
                <img
                  src={cardImageUrl}
                  alt={card.name}
                  className={styles.cardImage}
                  onError={(e) => {
                    e.currentTarget.style.display = 'none'
                    const fallback = e.currentTarget.nextElementSibling as HTMLElement
                    if (fallback) fallback.style.display = 'flex'
                  }}
                />
                {/* Fallback when image fails */}
                <div className={styles.cardFallback}>
                  <span className={styles.cardFallbackName}>
                    {card.name}
                  </span>
                  {card.typeLine && (
                    <span className={styles.cardFallbackType}>
                      {card.typeLine}
                    </span>
                  )}
                </div>
              </div>
            )
          })}
        </div>
      )}

      {/* OK button */}
      <button onClick={dismissRevealedCards} className={styles.okButton}>
        OK
      </button>
      {/* Card preview is handled by the global CardPreview component in GameBoard */}
    </div>
  )
}
