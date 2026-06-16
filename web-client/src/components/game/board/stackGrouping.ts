import type { ClientCard } from '@/types/gameState'
import type { EntityId } from '@/types'

/**
 * Visual grouping for the stack zone.
 *
 * When many structurally-identical objects pile onto the stack at once (storm/copy effects, a
 * board of identical ETB or upkeep triggers, a token swarm sharing one trigger), rendering each
 * as its own fanned card is unreadable. We fold *contiguous runs* of identical items into a
 * single collapsible pile with a `×N` count.
 *
 * Contiguous-only is deliberate: the stack is LIFO and its order is meaningful, so we must never
 * collapse across an intervening different (or differently-controlled) item and imply an order
 * that isn't real. In practice the high-volume cases are contiguous because they're pushed
 * back-to-back. This is purely a display transform — it changes nothing about resolution,
 * priority, or targeting.
 */

/**
 * Identity key deciding whether two adjacent stack items are "the same thing" for collapsing.
 *
 * Targets are intentionally excluded: two copies of a spell pointed at different creatures still
 * collapse into one "×2" pile, expandable to inspect each. The discriminators below are the
 * fields that actually change *what the object is/does*, not *what it points at*.
 */
export function stackGroupKey(card: ClientCard): string {
  return [
    card.controllerId,
    card.name,
    card.typeLine,
    // Effect signature: spells carry `stackText`, abilities carry their text in `oracleText`.
    card.stackText ?? card.oracleText ?? '',
    card.chosenX ?? '',
    card.sourceZone ?? '',
    (card.chosenModeDescriptions ?? []).join('|'),
    card.chosenCreatureType ?? '',
  ].join('§')
}

export interface StackGroup {
  /**
   * Stable, unique id for React keys and expand/collapse state — the first member's entity id.
   * (The identity `key` can repeat across two non-contiguous runs, so it can't serve as the id.)
   */
  readonly groupId: EntityId
  /** The identity key shared by every member (see {@link stackGroupKey}). */
  readonly key: string
  /** Members in stack order (same order as the input slice). */
  readonly items: readonly ClientCard[]
}

/**
 * Fold a stack-ordered card list into contiguous identity groups, preserving order.
 * A run of length 1 is just a single-member group.
 */
export function groupStackCards(cards: readonly ClientCard[]): StackGroup[] {
  const groups: { groupId: EntityId; key: string; items: ClientCard[] }[] = []
  for (const card of cards) {
    const key = stackGroupKey(card)
    const last = groups[groups.length - 1]
    if (last && last.key === key) {
      last.items.push(card)
    } else {
      groups.push({ groupId: card.id, key, items: [card] })
    }
  }
  return groups
}
