import { useState } from 'react'
import { useGameStore } from '../../store/gameStore'
import type { EntityId } from '../../types'
import { useResponsive, calculateFittingCardWidth } from '../../hooks/useResponsive'
import { getCardImageUrl } from '../../utils/cardImages'
import styles from './RevealedCardsUI.module.css'

/**
 * Overlay that shows revealed cards - either from a "look at hand" / "reveal hand"
 * effect or from a library reveal effect (e.g., Animal Magnetism, Sylvan Tutor).
 *
 * Can be minimized to "View Battlefield" so the player can see the board state
 * while the reveal is active.
 */
export function RevealedCardsUI() {
  const revealedCardsInfo = useGameStore((state) => state.revealedCardsInfo)
  const revealedHandCardIds = useGameStore((state) => state.revealedHandCardIds)
  const gameState = useGameStore((state) => state.gameState)
  const dismissRevealedCards = useGameStore((state) => state.dismissRevealedCards)
  const dismissRevealedHand = useGameStore((state) => state.dismissRevealedHand)
  const hoverCard = useGameStore((state) => state.hoverCard)
  const responsive = useResponsive()
  const [hoveredCardId, setHoveredCardId] = useState<EntityId | null>(null)
  const [minimized, setMinimized] = useState(false)

  // Determine which reveal mode is active
  const isHandReveal = !!revealedHandCardIds
  const isCardReveal = !!revealedCardsInfo

  if ((!isHandReveal && !isCardReveal) || !gameState) return null

  // Build card list depending on the reveal type
  const cards = isHandReveal
    ? revealedHandCardIds
        .map((cardId) => gameState.cards[cardId])
        .filter((card) => card != null)
    : revealedCardsInfo!.cardIds.map((cardId, index) => {
        const card = gameState.cards[cardId]
        return card ?? {
          id: cardId,
          name: revealedCardsInfo!.cardNames[index] ?? 'Unknown Card',
          imageUri: revealedCardsInfo!.imageUris[index] ?? null,
          typeLine: null,
        }
      })

  const onDismiss = isHandReveal ? dismissRevealedHand : dismissRevealedCards

  // Title and subtitle
  const title = isHandReveal
    ? "Opponent's Hand"
    : `Opponent Revealed${revealedCardsInfo!.source ? ` (${revealedCardsInfo!.source})` : ''}`

  const subtitle = isHandReveal
    ? cards.length === 0
      ? 'Opponent has no cards in hand'
      : `${cards.length} card${cards.length !== 1 ? 's' : ''}`
    : revealedCardsInfo!.cardNames.join(', ')

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

  // When minimized, show floating button to restore
  if (minimized) {
    return (
      <button
        onClick={() => setMinimized(false)}
        className={styles.restoreButton}
      >
        {isHandReveal ? "↑ Return to Opponent's Hand" : '↑ Return to Revealed Cards'}
      </button>
    )
  }

  return (
    <div className={styles.overlay}>
      {/* Header */}
      <div className={styles.header}>
        <h2 className={styles.title}>{title}</h2>
        <p className={styles.subtitle}>{subtitle}</p>
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

      {/* Action buttons */}
      <div className={styles.buttonRow}>
        <button onClick={() => setMinimized(true)} className={styles.viewBattlefieldButton}>
          View Battlefield
        </button>
        <button onClick={onDismiss} className={styles.okButton}>
          OK
        </button>
      </div>
      {/* Card preview is handled by the global CardPreview component in GameBoard */}
    </div>
  )
}
