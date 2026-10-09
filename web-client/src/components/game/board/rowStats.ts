import { visibleStackDepth, type GroupedCard } from '@/store/cardGrouping'
import type { EntityId } from '@/types'
import type { RowStats } from './battlefieldLayout'

/**
 * Per-row footprint stats for the fit constraints in `battlefieldLayout.ts`.
 *
 * Tapped stacks are rotated 90° on the battlefield — their horizontal footprint
 * is cardHeight (≈1.4 × cardWidth) rather than cardWidth — and every card
 * stacked behind a group's first adds a fixed peek offset. Counted per row so
 * the horizontal-fit constraint reserves the true width; otherwise a crowded
 * row on a narrow viewport overflows into an unbudgeted wrap line, which pushes
 * the row up into the center HUD.
 *
 * Counts are *rendered stacks* (after `groupCards`), not raw cards: a collapsed
 * horde paints at most MAX_VISUAL_STACK_DEPTH cards, so the footprint uses the
 * capped depth (`visibleStackDepth`), not the raw count.
 *
 * A stack the player ungrouped with the ⤢ toggle (`expanded`) renders every
 * member as its own full card, so it counts as that many items and no peeks.
 *
 * [maxLayers] is the player's "visible stack layers" preference (CardStack draws the same cap).
 */
export function rowStats(
  expanded: ReadonlySet<EntityId>,
  maxLayers: number,
  ...groupLists: (readonly GroupedCard[])[]
): RowStats {
  let count = 0
  let tapped = 0
  let stackedExtra = 0
  for (const groups of groupLists) {
    for (const group of groups) {
      // Every member of a group shares its tapped state (it's part of the
      // group key), so the representative answers for the whole stack.
      if (isStackExpanded(group, expanded)) {
        count += group.count
        if (group.card.isTapped) tapped += group.count
        continue
      }
      count++
      if (group.card.isTapped) tapped++
      stackedExtra += visibleStackDepth(group.count, maxLayers) - 1
    }
  }
  return { count, tapped, stackedExtra }
}

/** True when a multi-card stack has been ungrouped (see `expandedStackCardIds`). */
export function isStackExpanded(group: GroupedCard, expanded: ReadonlySet<EntityId>): boolean {
  if (group.count <= 1 || expanded.size === 0) return false
  return group.cardIds.some((id) => expanded.has(id))
}
