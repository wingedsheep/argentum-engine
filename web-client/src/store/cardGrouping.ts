import type { EntityId, ClientCard } from '@/types'

/**
 * Pure, store-free card-grouping logic for the battlefield.
 *
 * Identical permanents collapse into one visual stack and split back out the
 * moment one diverges (counter, P/T, tap, damage, combat, attachment, …). This is
 * the display-layer half of "token quantity aggregation"
 * (backlog/number-explosion-safety.md, Option B): the engine still holds one
 * entity per token — Option A caps how many can exist — and grouping plus a
 * bounded render depth make a legitimately huge board cheap to display.
 *
 * Kept in its own module (rather than selectors.ts) so it stays a pure function of
 * its inputs with no store/React dependency — directly unit-testable. Re-exported
 * from selectors.ts for existing call sites.
 */

/**
 * Represents a group of identical cards for display purposes.
 */
export interface GroupedCard {
  /** The representative card to display (first card in group) */
  card: ClientCard
  /** Number of cards in this group */
  count: number
  /** All card IDs in this group (for action handling) */
  cardIds: readonly EntityId[]
  /** All cards in this group (for stacked rendering) */
  cards: readonly ClientCard[]
}

/**
 * Computes a grouping key for a card based on properties that make cards "different".
 * Cards with different keys should NOT be grouped together.
 *
 * The goal is that two cards share a key only when they have *exactly the same projected
 * status* — same printed identity AND same battlefield state (counters, P/T, damage,
 * tapped, combat assignment, summoning sickness, chosen attributes, granted keywords,
 * designations, …). That way a stack of N identical tokens collapses into one visual
 * group, but the moment one of them is buffed, tapped, attacks, etc. it splits back out.
 */
export function computeCardGroupKey(card: ClientCard): string {
  const parts: string[] = [card.name]

  // Cards with counters are different
  const counterEntries = Object.entries(card.counters).filter(([, count]) => count && count > 0)
  if (counterEntries.length > 0) {
    const sortedCounters = counterEntries.sort(([a], [b]) => a.localeCompare(b))
    parts.push(`counters:${JSON.stringify(sortedCounters)}`)
  }

  // Cards with modified P/T are different
  if (card.power !== card.basePower || card.toughness !== card.baseToughness) {
    parts.push(`pt:${card.power}/${card.toughness}`)
  }

  // Cards with attachments (auras/equipment) should never be grouped — use unique ID
  if (card.attachments.length > 0) {
    parts.push(`id:${card.id}`)
  }

  // Cards with linked-exiled cards (e.g. Suspension Field) carry hidden state — never group
  if (card.linkedExile && card.linkedExile.length > 0) {
    parts.push(`id:${card.id}`)
  }

  // Cards with damage are different (for battlefield creatures)
  if (card.damage != null && card.damage > 0) {
    parts.push(`damage:${card.damage}`)
  }

  // Tapped cards are different from untapped
  if (card.isTapped) {
    parts.push('tapped')
  }

  // Combat participants are different from idle permanents, and from each other when they
  // attack/block different things (drives distinct targeting arrows, so they can't share a slot).
  if (card.isAttacking) {
    parts.push(`attacking:${card.attackingTarget ?? ''}`)
  }
  if (card.isBlocking) {
    parts.push(`blocking:${card.blockingTarget ?? ''}`)
  }

  // Summoning-sick creatures behave differently (can't attack/tap) — keep them visually distinct.
  if (card.hasSummoningSickness) {
    parts.push('sick')
  }

  // Phased-out permanents render translucent and are functionally absent — never mix with present ones.
  if (card.isPhasedOut) {
    parts.push('phased')
  }

  // Transformed cards are different
  if (card.isTransformed) {
    parts.push('transformed')
  }

  // Face-down cards are different
  if (card.isFaceDown) {
    parts.push('facedown')
  }

  // "As enters, choose ..." selections (creature type / color / mode) are rendered as badges.
  if (card.chosenCreatureType) parts.push(`ct:${card.chosenCreatureType}`)
  if (card.chosenColor) parts.push(`cc:${card.chosenColor}`)
  if (card.chosenMode) parts.push(`cm:${card.chosenMode}`)

  // Class/Saga progress is a per-permanent badge, not shared identity.
  if (card.classLevel != null) parts.push(`class:${card.classLevel}`)

  // Special designations carry prominent badges.
  if (card.isSuspected) parts.push('suspected')
  if (card.isRingBearer) parts.push('ringbearer')
  if (card.isCommander) parts.push('commander')

  // Copy provenance is badged and shown in details, so a token copy doesn't merge with the original.
  if (card.copyOf) parts.push(`copy:${card.copyOf}`)
  if (card.nonLegendaryCopy) parts.push('nonleg')

  // Granted/projected keywords, ability flags and protections differ when one copy is
  // pumped or granted an ability (without an attachment) but its twin isn't.
  if (card.keywords.length > 0) {
    parts.push(`kw:${[...card.keywords].sort().join(',')}`)
  }
  if (card.abilityFlags && card.abilityFlags.length > 0) {
    parts.push(`af:${[...card.abilityFlags].sort().join(',')}`)
  }
  if (card.protections && card.protections.length > 0) {
    parts.push(`prot:${[...card.protections].sort().join(',')}`)
  }
  if (card.hexproofFromColors && card.hexproofFromColors.length > 0) {
    parts.push(`hpx:${[...card.hexproofFromColors].sort().join(',')}`)
  }

  return parts.join('|')
}

/**
 * Maximum number of overlapping card layers rendered for a single battlefield
 * stack. A group with more identical members than this collapses to this many
 * peeked layers plus a "×N" count badge (see CardStack) rather than painting one
 * DOM node per token — so a horde of 10,000 identical tokens renders ~4 cards
 * instead of 10,000. This is the display-layer half of "token quantity
 * aggregation" (backlog/number-explosion-safety.md, Option B): the engine still
 * holds one entity per token (Option A caps how many can exist), and grouping +
 * the render cap make a legitimately huge board cheap to display.
 *
 * Action handling is unaffected — GroupedCard.cardIds always lists every member,
 * so targeting/combat still reach the tokens hidden behind the cap.
 */
export const MAX_VISUAL_STACK_DEPTH = 4

/** How many overlapping layers a stack of [count] identical cards actually renders. */
export function visibleStackDepth(count: number): number {
  return Math.min(Math.max(count, 0), MAX_VISUAL_STACK_DEPTH)
}

/**
 * Groups an array of cards by identical projected status (see computeCardGroupKey).
 *
 * Each distinct status becomes exactly ONE group — however many members it has.
 * A horde of identical tokens collapses into a single group; the moment one of
 * them is buffed, tapped, attacks, gains a counter, etc. its key changes and it
 * splits back into its own group automatically. The rendered depth of a group is
 * capped separately (MAX_VISUAL_STACK_DEPTH / CardStack), not here, so callers
 * that need every member (action handling via cardIds, footprint stats) still see
 * the full group.
 *
 * [splitOutIds] forces specific permanents to render on their own (a singleton
 * group keyed by id) even if otherwise identical to their twins. This is how a
 * permanent that is the *target* of a stack object — or the triggering source, or
 * a mid-cast selected target — keeps a `data-card-id` DOM anchor: targeting arrows
 * resolve to that element, and a member hidden behind the render cap would make
 * its arrow silently drop. It mirrors why attackers/blockers already split out
 * (they drive distinct arrows). The set is small (a handful of chosen targets), so
 * it doesn't re-explode a horde.
 */
export function groupCards(
  cards: readonly ClientCard[],
  splitOutIds?: ReadonlySet<EntityId>,
): readonly GroupedCard[] {
  if (cards.length === 0) return []

  const groups = new Map<string, { cards: ClientCard[]; cardIds: EntityId[] }>()

  for (const card of cards) {
    // A split-out permanent must render individually so its arrow can anchor —
    // a unique key guarantees it never merges with a twin.
    const key = splitOutIds?.has(card.id) ? `solo:${card.id}` : computeCardGroupKey(card)
    const existing = groups.get(key)
    if (existing) {
      existing.cards.push(card)
      existing.cardIds.push(card.id)
    } else {
      groups.set(key, { cards: [card], cardIds: [card.id] })
    }
  }

  const result: GroupedCard[] = []
  for (const { cards: groupedCards, cardIds } of groups.values()) {
    const firstCard = groupedCards[0]
    if (!firstCard) continue // Should never happen, but satisfies TypeScript
    result.push({
      card: firstCard,
      count: groupedCards.length,
      cardIds,
      cards: groupedCards,
    })
  }

  return result
}
