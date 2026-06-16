import { memo } from 'react'
import type { GroupedCard } from '@/store/selectors.ts'
import { MAX_VISUAL_STACK_DEPTH } from '@/store/selectors.ts'
import { useResponsiveContext } from '../board/shared'
import { GameCard } from './GameCard'

/**
 * Renders a group of identical cards as an overlapping stack.
 * Each rendered card has its own data-card-id for targeting arrows.
 *
 * The number of *rendered* layers is capped at MAX_VISUAL_STACK_DEPTH: a group of
 * N identical tokens paints at most that many peeked cards plus a "×N" count badge
 * on the front card (GameCard renders it when count > 1), instead of one DOM node
 * per token. This keeps a legitimately huge board (a horde of tokens) cheap to
 * display. Members hidden behind the cap are still fully reachable for actions —
 * the server sends per-entity legal actions and the group carries every id.
 */
function CardStackImpl({
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

  // Render at most MAX_VISUAL_STACK_DEPTH overlapping layers regardless of how
  // many identical members the group has — the count badge conveys the true size.
  const renderedCards = group.cards.slice(0, MAX_VISUAL_STACK_DEPTH)
  // The group key guarantees every member shares the same tapped state, so the
  // representative answers for the whole stack (O(1), avoids scanning a horde).
  const hasAnyTapped = group.card.isTapped
  const cardWidth = hasAnyTapped ? responsive.battlefieldCardHeight : responsive.battlefieldCardWidth
  const totalWidth = cardWidth + stackOffset * (renderedCards.length - 1)
  const stackHeight = responsive.battlefieldCardHeight  // Always use full height for consistent alignment
  // Only badge the count when members are hidden behind the cap — small stacks
  // that are fully visible look exactly as before.
  const hasHiddenMembers = group.count > renderedCards.length
  const frontIndex = renderedCards.length - 1

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
      {renderedCards.map((card, index) => (
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
            count={hasHiddenMembers && index === frontIndex ? group.count : 1}
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

// Memoized: BattlefieldContent re-renders on many store changes, but a stack's
// `group` is a content-stable reference (groupCards/toSinglesStable) so most
// stacks can skip re-rendering when an unrelated card changes.
export const CardStack = memo(CardStackImpl)
