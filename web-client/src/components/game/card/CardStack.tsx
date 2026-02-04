import type { GroupedCard } from '../../../store/selectors'
import { useResponsiveContext } from '../board/shared'
import { GameCard } from './GameCard'

/**
 * Renders a group of identical cards as an overlapping stack.
 * Each card has its own data-card-id for targeting arrows.
 */
export function CardStack({
  group,
  interactive,
  isOpponentCard,
}: {
  group: GroupedCard
  interactive: boolean
  isOpponentCard: boolean
}) {
  const responsive = useResponsiveContext()

  // For single cards, just render a normal GameCard
  if (group.count === 1) {
    return (
      <GameCard
        card={group.card}
        count={1}
        faceDown={group.card.isFaceDown}
        interactive={interactive}
        battlefield
        isOpponentCard={isOpponentCard}
      />
    )
  }

  // Calculate stack offset (how much each card is offset from the previous)
  const stackOffset = responsive.isMobile ? 12 : 18

  // Calculate total width needed for the stack
  // Use height for tapped cards since they rotate 90 degrees (visually wider)
  const hasAnyTapped = group.cards.some(c => c.isTapped)
  const cardWidth = hasAnyTapped ? responsive.battlefieldCardHeight : responsive.battlefieldCardWidth
  const totalWidth = cardWidth + stackOffset * (group.count - 1)
  const stackHeight = responsive.battlefieldCardHeight  // Always use full height for consistent alignment

  return (
    <div
      style={{
        position: 'relative',
        width: totalWidth,
        height: stackHeight,
        display: 'flex',
        alignItems: 'flex-end',
        transition: 'width 0.15s, height 0.15s',
      }}
    >
      {group.cards.map((card, index) => (
        <div
          key={card.id}
          style={{
            position: 'absolute',
            left: index * stackOffset,
            top: 0,
            bottom: 0,
            display: 'flex',
            alignItems: 'flex-end',
            zIndex: index,
          }}
        >
          <GameCard
            card={card}
            count={1}
            faceDown={card.isFaceDown}
            interactive={interactive}
            battlefield
            isOpponentCard={isOpponentCard}
          />
        </div>
      ))}
    </div>
  )
}
