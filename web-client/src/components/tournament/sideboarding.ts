/**
 * Pure helpers behind the between-games sideboarding screen. The server owns legality (it re-checks
 * the deck on submit); these only move cards between the two piles the snapshot handed us and
 * describe the result, so the screen can say what changed before the player commits to it.
 */
import type { SealedCardInfo } from '@/types'

export type CardCounts = Readonly<Record<string, number>>

/** Move one copy of `name` from `from` to `to`; a no-op when `from` has none. */
export function moveOne(from: CardCounts, to: CardCounts, name: string): [CardCounts, CardCounts] | null {
  const have = from[name] ?? 0
  if (have <= 0) return null
  const nextFrom = { ...from }
  if (have === 1) delete nextFrom[name]
  else nextFrom[name] = have - 1
  return [nextFrom, { ...to, [name]: (to[name] ?? 0) + 1 }]
}

/** Add `delta` copies of a free basic land to the deck (never below zero). */
export function adjustBasic(deck: CardCounts, name: string, delta: number): CardCounts {
  const next = Math.max(0, (deck[name] ?? 0) + delta)
  const out = { ...deck }
  if (next === 0) delete out[name]
  else out[name] = next
  return out
}

export function countCards(cards: CardCounts): number {
  return Object.values(cards).reduce((sum, n) => sum + n, 0)
}

/** One line of the change summary: a card and how many copies came in (+) or went out (−). */
export interface DeckChange {
  readonly name: string
  readonly delta: number
}

/**
 * How `deck` differs from `base`, cards brought in first, then cards taken out — each list in name
 * order. Empty when the two decks are the same.
 */
export function deckChanges(base: CardCounts, deck: CardCounts): DeckChange[] {
  const names = new Set([...Object.keys(base), ...Object.keys(deck)])
  const changes: DeckChange[] = []
  for (const name of names) {
    const delta = (deck[name] ?? 0) - (base[name] ?? 0)
    if (delta !== 0) changes.push({ name, delta })
  }
  return changes.sort((a, b) => (a.delta > 0) === (b.delta > 0) ? a.name.localeCompare(b.name) : a.delta > 0 ? -1 : 1)
}

export function sameDeck(a: CardCounts, b: CardCounts): boolean {
  return deckChanges(a, b).length === 0
}

/** The groups the deck list is shown in, in display order. */
export type DeckGroup = 'Creatures' | 'Spells' | 'Lands'

export function deckGroupOf(card: SealedCardInfo | undefined): DeckGroup {
  const type = card?.typeLine ?? ''
  if (/\bLand\b/.test(type) && !/\bCreature\b/.test(type)) return 'Lands'
  if (/\bCreature\b/.test(type)) return 'Creatures'
  return 'Spells'
}

const COLOR_ORDER = ['W', 'U', 'B', 'R', 'G']

/** Colour, then mana value, then name — the order a player scans a pool in. */
export function compareCards(a: SealedCardInfo | undefined, b: SealedCardInfo | undefined, nameA: string, nameB: string): number {
  return colorRank(a) - colorRank(b) || (a?.cmc ?? 0) - (b?.cmc ?? 0) || nameA.localeCompare(nameB)
}

function colorRank(card: SealedCardInfo | undefined): number {
  const colors = card?.colorIdentity ?? []
  if (/\bLand\b/.test(card?.typeLine ?? '')) return 7
  if (colors.length === 0) return 6
  if (colors.length > 1) return 5
  return COLOR_ORDER.indexOf(colors[0] ?? '')
}
