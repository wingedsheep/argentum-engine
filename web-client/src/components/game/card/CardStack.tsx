import { memo } from 'react'
import type { GroupedCard } from '@/store/selectors.ts'
import { visibleStackDepth } from '@/store/selectors.ts'
import { usePreferences } from '@/store/preferencesStore'
import { useGameStore } from '@/store/gameStore.ts'
import { useResponsiveContext } from '../board/shared'
import { stackOffsetFor } from '../board/battlefieldLayout'
import { isStackExpanded } from '../board/rowStats'
import { GameCard } from './GameCard'

/**
 * Small chip that toggles a stack between its collapsed peek and the fully
 * ungrouped (expanded) layout. Rendered on top of the stack so a click never
 * falls through to a card underneath.
 */
function StackToggle({
  expanded,
  count,
  onToggle,
}: {
  expanded: boolean
  count: number
  onToggle: () => void
}) {
  return (
    <button
      onClick={(e) => {
        e.stopPropagation()
        onToggle()
      }}
      title={expanded ? 'Group these cards back into a stack' : `Ungroup these ${count} cards`}
      style={{
        position: 'absolute',
        top: -8,
        left: -8,
        zIndex: 1000,
        height: 18,
        minWidth: 18,
        padding: '0 5px',
        borderRadius: 9,
        border: '1px solid rgba(255, 255, 255, 0.35)',
        background: 'rgba(30, 41, 59, 0.95)',
        color: 'white',
        fontSize: 11,
        fontWeight: 700,
        lineHeight: 1,
        cursor: 'pointer',
        pointerEvents: 'auto',
        display: 'inline-flex',
        alignItems: 'center',
        justifyContent: 'center',
        boxShadow: '0 1px 3px rgba(0, 0, 0, 0.5)',
        userSelect: 'none',
      }}
    >
      {expanded ? '⤡' : '⤢'}
    </button>
  )
}

/**
 * Renders a group of identical cards as an overlapping stack.
 * Each rendered card has its own data-card-id for targeting arrows.
 *
 * The number of *rendered* layers is capped by the player's preference (default MAX_VISUAL_STACK_DEPTH): a group of
 * N identical tokens paints at most that many peeked cards plus a "×N" count badge
 * on the front card (GameCard renders it when count > 1), instead of one DOM node
 * per token. This keeps a legitimately huge board (a horde of tokens) cheap to
 * display. Members hidden behind the cap are still fully reachable for server-driven
 * actions (legal-action highlights, targeting) because the group carries every id.
 *
 * But some flows need the player to click *individual* hidden members directly —
 * e.g. waterbend, where you tap N identical tokens to pay, and only the front 4 have
 * a clickable card. The ⤢ toggle "ungroups" the stack into a wrapped row of full,
 * individually-clickable cards (and ⤡ collapses it back).
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
  const maxLayers = usePreferences((s) => s.prefs.battlefield.maxVisibleLayers)
  // Store-held (not local state) so the battlefield sizing solver counts an
  // ungrouped stack as the N cards it renders — see `expandedStackCardIds`.
  const expanded = useGameStore((state) => isStackExpanded(group, state.expandedStackCardIds))
  const setStackExpanded = useGameStore((state) => state.setStackExpanded)

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

  // Ungrouped: every member rendered as its own non-overlapping card so each is
  // fully clickable (overlapping peek layers only expose a thin strip of the cards
  // behind the front one). Rendered as siblings in the row itself rather than in a
  // wrapper, so they wrap card by card exactly as the sizing solver counted them —
  // a wrapper box can only move to the next line whole.
  if (expanded) {
    return (
      <>
        {group.cards.map((card, index) => (
          <div key={card.id} style={{ position: 'relative', display: 'flex', alignItems: 'flex-end' }}>
            <GameCard
              card={card}
              count={1}
              faceDown={card.isFaceDown}
              interactive={interactive}
              battlefield
              isOpponentCard={isOpponentCard}
            />
            {index === 0 && (
              <StackToggle expanded count={group.count} onToggle={() => setStackExpanded(group.cardIds, false)} />
            )}
          </div>
        ))}
      </>
    )
  }

  // Calculate stack offset (how much each card is offset from the previous)
  const stackOffset = stackOffsetFor(responsive.isMobile)

  // Render at most the player's "visible stack layers" preference (default
  // MAX_VISUAL_STACK_DEPTH) regardless of how many identical members the group
  // has — the count badge conveys the true size. rowStats reserves the same depth.
  const renderedCards = group.cards.slice(0, visibleStackDepth(group.count, maxLayers))
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
      {/* Ungroup affordance — lets the player split the stack to click individual
          members (e.g. tap 8 of 8 tokens for waterbend, not just the front 4). */}
      <StackToggle expanded={false} count={group.count} onToggle={() => setStackExpanded(group.cardIds, true)} />
    </div>
  )
}

// Memoized: BattlefieldContent re-renders on many store changes, but a stack's
// `group` is a content-stable reference (groupCards/toSinglesStable) so most
// stacks can skip re-rendering when an unrelated card changes.
export const CardStack = memo(CardStackImpl)
