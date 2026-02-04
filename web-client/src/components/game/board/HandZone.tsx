import { useState } from 'react'
import { useZoneCards, useZone } from '../../../store/selectors'
import type { ZoneId, ClientCard } from '../../../types'
import { calculateFittingCardWidth } from '../../../hooks/useResponsive'
import { useResponsiveContext } from './shared'
import { styles } from './styles'
import { GameCard } from '../card'

/**
 * Row of cards (hand or other horizontal zone).
 * Cards in hand are NOT grouped - each card is shown individually.
 */
export function CardRow({
  zoneId,
  faceDown = false,
  interactive = false,
  small = false,
  inverted = false,
}: {
  zoneId: ZoneId
  faceDown?: boolean
  interactive?: boolean
  small?: boolean
  inverted?: boolean
}) {
  const cards = useZoneCards(zoneId)
  const zone = useZone(zoneId)
  const responsive = useResponsiveContext()

  // For hidden zones (like opponent's hand), use zone size to show face-down placeholders
  const zoneSize = zone?.size ?? 0
  const showPlaceholders = faceDown && cards.length === 0 && zoneSize > 0

  if (cards.length === 0 && !showPlaceholders) {
    return <div style={{ ...styles.emptyZone, fontSize: responsive.fontSize.small }}>No cards</div>
  }

  // Calculate available width for the hand (viewport - padding - zone piles on sides)
  const sideZoneWidth = responsive.pileWidth + 20 // pile + margin
  const availableWidth = responsive.viewportWidth - (responsive.containerPadding * 2) - (sideZoneWidth * 2)

  // Calculate card width that fits all cards
  const cardCount = showPlaceholders ? zoneSize : cards.length
  const baseWidth = small ? responsive.smallCardWidth : responsive.cardWidth
  const minWidth = small ? 30 : 45
  const fittingWidth = calculateFittingCardWidth(
    cardCount,
    availableWidth,
    responsive.cardGap,
    baseWidth,
    minWidth
  )

  // For hands (player or opponent), create a fan effect
  // - Player's own hand: interactive, face-up
  // - Opponent's hand: face-down, inverted (top of screen)
  // - Spectator bottom hand: face-down, not inverted (bottom of screen)
  const isPlayerHand = interactive && !faceDown
  const isOpponentHand = faceDown && inverted
  const isSpectatorBottomHand = faceDown && !inverted && !interactive
  const cardHeight = Math.round(fittingWidth * 1.4)

  if ((isPlayerHand || isOpponentHand || isSpectatorBottomHand) && (cards.length > 0 || showPlaceholders)) {
    return (
      <HandFan
        cards={cards}
        placeholderCount={showPlaceholders ? zoneSize : 0}
        fittingWidth={fittingWidth}
        cardHeight={cardHeight}
        cardGap={responsive.cardGap}
        faceDown={faceDown}
        interactive={interactive}
        small={small}
        inverted={inverted}
      />
    )
  }

  // Render face-down placeholders for hidden zones (non-fan layout)
  if (showPlaceholders) {
    const cardRatio = 1.4
    const height = Math.round(fittingWidth * cardRatio)
    return (
      <div style={{ ...styles.cardRow, gap: responsive.cardGap, padding: responsive.cardGap }}>
        {Array.from({ length: zoneSize }).map((_, index) => (
          <div
            key={`placeholder-${index}`}
            style={{
              ...styles.card,
              width: fittingWidth,
              height,
              borderRadius: responsive.isMobile ? 4 : 8,
              border: '2px solid #333',
              boxShadow: '0 2px 8px rgba(0,0,0,0.5)',
            }}
          >
            <img
              src="https://backs.scryfall.io/large/2/2/222b7a3b-2321-4d4c-af19-19338b134971.jpg?1677416389"
              alt="Card back"
              style={styles.cardImage}
            />
          </div>
        ))}
      </div>
    )
  }

  // Render each card individually (no grouping for hand)
  return (
    <div style={{ ...styles.cardRow, gap: responsive.cardGap, padding: responsive.cardGap }}>
      {cards.map((card) => (
        <GameCard
          key={card.id}
          card={card}
          count={1}
          faceDown={faceDown}
          interactive={interactive}
          small={small}
          overrideWidth={fittingWidth}
          inHand={interactive && !faceDown}
        />
      ))}
    </div>
  )
}

/**
 * Hand display with fan/arc effect - cards slightly overlap and rotate like held cards.
 */
export function HandFan({
  cards,
  placeholderCount = 0,
  fittingWidth,
  cardHeight,
  faceDown,
  interactive,
  small,
  inverted = false,
}: {
  cards: readonly ClientCard[]
  placeholderCount?: number
  fittingWidth: number
  cardHeight: number
  cardGap: number
  faceDown: boolean
  interactive: boolean
  small: boolean
  inverted?: boolean
}) {
  const [, setHoveredIndex] = useState<number | null>(null)

  const cardCount = placeholderCount > 0 ? placeholderCount : cards.length

  // Scale fan parameters based on card count
  // Fewer cards = more spread, more cards = tighter fan
  const maxRotation = Math.min(12, 40 / Math.max(cardCount, 1)) // Max rotation at edges (degrees)
  const maxVerticalOffset = Math.min(15, 45 / Math.max(cardCount, 1)) // Max rise at center (pixels)

  // Calculate overlap - more overlap with more cards, but keep it readable
  const overlapFactor = Math.max(0.5, 0.85 - (cardCount * 0.025))
  const cardSpacing = fittingWidth * overlapFactor

  // Total width of the hand fan
  const totalWidth = cardSpacing * (cardCount - 1) + fittingWidth

  // Allow cards to extend slightly beyond the visible area to save vertical space
  const edgeMargin = -15

  // For inverted fan, flip the arc and rotation direction
  const rotationMultiplier = inverted ? -1 : 1

  // Create array of items to render (either cards or placeholder indices)
  const items = placeholderCount > 0
    ? Array.from({ length: placeholderCount }, (_, i) => ({ type: 'placeholder' as const, index: i }))
    : cards.map((card, index) => ({ type: 'card' as const, card, index }))

  return (
    <div
      style={{
        position: 'relative',
        width: totalWidth,
        height: cardHeight + maxVerticalOffset + 40, // Extra space for hover lift
        marginBottom: inverted ? 0 : edgeMargin,
        marginTop: inverted ? edgeMargin : 0,
      }}
    >
      {items.map((item, index) => {
        // Calculate position from center (-1 to 1)
        const centerOffset = cardCount > 1
          ? (index - (cardCount - 1) / 2) / ((cardCount - 1) / 2)
          : 0

        // Calculate rotation (edges rotate away from center)
        const rotation = centerOffset * maxRotation * rotationMultiplier

        // Calculate vertical offset (arc shape - center cards are higher/lower)
        const verticalOffset = (1 - Math.abs(centerOffset) ** 1.5) * maxVerticalOffset

        // Calculate horizontal position
        const left = index * cardSpacing

        // Z-index: center cards on top
        const zIndex = 50 - Math.abs(index - Math.floor(cardCount / 2))

        const key = item.type === 'card' ? item.card.id : `placeholder-${item.index}`

        return (
          <div
            key={key}
            style={{
              position: 'absolute',
              left,
              ...(inverted
                ? { top: edgeMargin, transform: `translateY(${verticalOffset}px) rotate(${rotation}deg)` }
                : { bottom: edgeMargin, transform: `translateY(${-verticalOffset}px) rotate(${rotation}deg)` }
              ),
              transformOrigin: inverted ? 'top center' : 'bottom center',
              zIndex,
              transition: 'transform 0.12s ease-out, left 0.12s ease-out',
              cursor: interactive ? 'pointer' : 'default',
            }}
            onMouseEnter={() => !inverted && setHoveredIndex(index)}
            onMouseLeave={() => !inverted && setHoveredIndex(null)}
          >
            {item.type === 'card' ? (
              <GameCard
                card={item.card}
                count={1}
                faceDown={faceDown}
                interactive={interactive}
                small={small}
                overrideWidth={fittingWidth}
                inHand={interactive && !faceDown}
              />
            ) : (
              <div
                style={{
                  width: fittingWidth,
                  height: cardHeight,
                  borderRadius: 6,
                  border: '2px solid #333',
                  boxShadow: '0 2px 8px rgba(0,0,0,0.5)',
                  overflow: 'hidden',
                }}
              >
                <img
                  src="https://backs.scryfall.io/large/2/2/222b7a3b-2321-4d4c-af19-19338b134971.jpg?1677416389"
                  alt="Card back"
                  style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                />
              </div>
            )}
          </div>
        )
      })}
    </div>
  )
}
