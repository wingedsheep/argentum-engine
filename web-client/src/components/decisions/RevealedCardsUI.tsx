import { useState } from 'react'
import { useGameStore } from '@/store/gameStore.ts'
import type { EntityId, PendingDecision } from '@/types'
import { useResponsive, calculateFittingCardWidth } from '@/hooks/useResponsive.ts'
import { getCardImageUrl } from '@/utils/cardImages.ts'
import styles from './RevealedCardsUI.module.css'

/**
 * When the caster's reveal is immediately followed by a SelectCards decision whose
 * selectable + non-selectable options cover every revealed card, suppress the
 * reveal overlay for the caster — the selection modal itself is the reveal for
 * them (opponent still sees the reveal normally via their own gameplay update).
 *
 * Used for combined reveal+select flows like Aurora Awakener's Vivid ETB.
 */
function isRevealCoveredByDecision(
  revealedIds: readonly EntityId[] | null | undefined,
  decision: PendingDecision | null
): boolean {
  if (!revealedIds || revealedIds.length === 0 || !decision) return false
  if (decision.type !== 'SelectCardsDecision') return false
  const covered = new Set<EntityId>([
    ...decision.options,
    ...(decision.nonSelectableOptions ?? []),
  ])
  return revealedIds.every((id) => covered.has(id))
}

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
  const pendingDecision = useGameStore((state) => state.pendingDecision)
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

  // Combined reveal+select UX: when the caster is about to pick from the revealed
  // cards (e.g., Aurora Awakener's Vivid ETB), the selection modal already shows
  // every revealed card — so skip the redundant reveal overlay for that player.
  if (
    isCardReveal &&
    revealedCardsInfo!.isYourReveal &&
    isRevealCoveredByDecision(revealedCardsInfo!.cardIds, pendingDecision)
  ) {
    return null
  }

  // Build card list depending on the reveal type
  const cards = isHandReveal
    ? revealedHandCardIds
        .map((cardId) => gameState.cards[cardId])
        .filter((card) => card != null)
    : revealedCardsInfo!.cardIds.map((cardId, index) => {
        const card = gameState.cards[cardId]
        // Prefer the event's imageUri over gameState — the event carries the exact per-entity
        // image (important for basic lands with multiple art variants), while the gameState
        // version may use a different variant from the card registry.
        const eventImageUri = revealedCardsInfo!.imageUris[index] ?? null
        return card
          ? { ...card, imageUri: eventImageUri ?? card.imageUri }
          : {
              id: cardId,
              name: revealedCardsInfo!.cardNames[index] ?? 'Unknown Card',
              imageUri: eventImageUri,
              typeLine: null,
            }
      })

  const onDismiss = isHandReveal ? dismissRevealedHand : dismissRevealedCards

  // Title and subtitle
  const isYourReveal = !isHandReveal && revealedCardsInfo!.isYourReveal
  const title = isHandReveal
    ? "Opponent's Hand"
    : isYourReveal
      ? `Revealed${revealedCardsInfo!.source ? ` — ${revealedCardsInfo!.source}` : ''}`
      : `Opponent Revealed${revealedCardsInfo!.source ? ` — ${revealedCardsInfo!.source}` : ''}`

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
  const handleMouseEnter = (cardId: EntityId, e: React.MouseEvent) => {
    setHoveredCardId(cardId)
    hoverCard(cardId, { x: e.clientX, y: e.clientY })
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

  // Source card image (the card that triggered the reveal)
  const sourceCardName = !isHandReveal ? revealedCardsInfo!.source : null
  const sourceCardImageUrl = sourceCardName ? getCardImageUrl(sourceCardName) : null

  return (
    <div className={styles.overlay}>
      {/* Header */}
      <div className={styles.header}>
        {sourceCardImageUrl && (
          <img
            src={sourceCardImageUrl}
            alt={sourceCardName!}
            className={styles.sourceCard}
          />
        )}
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
                onMouseEnter={(e) => handleMouseEnter(card.id, e)}
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
