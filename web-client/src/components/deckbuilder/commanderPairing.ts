/**
 * Crown-toggle hints for the deckbuilder's commander designation, including partner pairs
 * (CR 702.124: Partner, "Partner—[text]", "Partner with", Choose a Background, Doctor's companion).
 *
 * These only decide which rows offer an active crown and what a crown click does. They don't
 * mirror every pairing rule (matching "Partner with" names, matching "Partner—" texts, colour
 * identity): the server's `/api/decks/validate` is authoritative and reports an illegal pair as
 * `INVALID_COMMANDER_PAIR`, which the issues panel shows.
 */
import type { CardSummary } from './cardFilter'

export type CommanderCard = Pick<CardSummary, 'supertypes' | 'cardTypes' | 'subtypes' | 'canBeCommander' | 'partnerAbilities'>

/** Whether a card is a legendary Background enchantment — the partner a "Choose a Background" takes. */
export function isLegendaryBackground(card: CommanderCard): boolean {
  return (
    card.supertypes.includes('LEGENDARY') &&
    card.cardTypes.includes('ENCHANTMENT') &&
    card.subtypes.includes('Background')
  )
}

/** Whether a card is a legendary Time Lord Doctor creature — the partner a "Doctor's companion" takes. */
export function isLegendaryTimeLordDoctor(card: CommanderCard): boolean {
  const subs = card.subtypes
  const timeLord = subs.includes('Time Lord') || (subs.includes('Time') && subs.includes('Lord'))
  return (
    card.supertypes.includes('LEGENDARY') &&
    card.cardTypes.includes('CREATURE') &&
    timeLord &&
    subs.includes('Doctor')
  )
}

/**
 * Whether a card may be a commander on its own. Prefers the server's `canBeCommander` flag;
 * falls back to "legendary creature" for catalogue payloads that predate it. Never "any
 * planeswalker" — only the few that say so can lead a deck.
 */
export function canCardBeCommander(card: CommanderCard): boolean {
  if (card.canBeCommander !== undefined) return card.canBeCommander
  return card.supertypes.includes('LEGENDARY') && card.cardTypes.includes('CREATURE')
}

const hasAbility = (card: CommanderCard, ability: string): boolean =>
  (card.partnerAbilities ?? []).includes(ability)

const BACKGROUND_CHOICE = 'Choose a Background'
const DOCTORS_COMPANION = "Doctor's companion"

/**
 * Whether [candidate] may join [first] as the second commander of a partner pair: both have a
 * partner ability, or one chooses a Background and the other is a legendary Background, or one is
 * a Doctor's companion and the other a legendary Time Lord Doctor.
 */
export function canPairAsCommanders(first: CommanderCard, candidate: CommanderCard): boolean {
  if (hasAbility(first, BACKGROUND_CHOICE) && isLegendaryBackground(candidate)) return true
  if (hasAbility(candidate, BACKGROUND_CHOICE) && isLegendaryBackground(first)) return true
  if (hasAbility(first, DOCTORS_COMPANION) && isLegendaryTimeLordDoctor(candidate)) return true
  if (hasAbility(candidate, DOCTORS_COMPANION) && isLegendaryTimeLordDoctor(first)) return true
  return (
    (first.partnerAbilities ?? []).length > 0 &&
    (candidate.partnerAbilities ?? []).length > 0 &&
    canCardBeCommander(first) &&
    canCardBeCommander(candidate)
  )
}

/**
 * Whether a row's crown is clickable. A commander's own crown always is (to un-designate it).
 * Otherwise a card that can lead alone may be crowned (it pairs as the second, or replaces the
 * commander); a legendary Background only as the partner of a single "Choose a Background"
 * commander.
 */
export function canCrown(
  card: CommanderCard,
  isCommander: boolean,
  currentCommanders: readonly CommanderCard[],
): boolean {
  if (isCommander) return true
  if (canCardBeCommander(card)) return true
  const only = currentCommanders.length === 1 ? currentCommanders[0] : undefined
  return !!only && isLegendaryBackground(card) && hasAbility(only, BACKGROUND_CHOICE)
}

/**
 * The commander designation after a crown click on [name]. Clicking a commander un-designates it
 * (the second, if any, moves up). Clicking another card designates it: as the first when none is
 * set, as the second when it pairs with the single commander, otherwise it replaces the
 * commander(s). Never more than two.
 */
export function toggleCommanderDesignation(
  current: readonly string[],
  name: string,
  canPair: (first: string, candidate: string) => boolean,
): string[] {
  if (current.includes(name)) return current.filter((c) => c !== name)
  const first = current[0]
  if (first === undefined) return [name]
  if (current.length === 1 && canPair(first, name)) return [first, name]
  return [name]
}

type WithIdentity = { readonly colorIdentity?: readonly string[] | undefined }

/**
 * The colour identity a deck's commander(s) allow (CR 903.4): the union of every commander's
 * identity — a partner pair plays both halves' colours. Empty means colourless.
 */
export function commanderIdentityOf(commanders: readonly WithIdentity[]): ReadonlySet<string> {
  const out = new Set<string>()
  for (const c of commanders) for (const color of c.colorIdentity ?? []) out.add(color)
  return out
}

/**
 * Whether every colour in [card]'s identity lies inside [identity] — the client-side mirror of the
 * server's COLOR_IDENTITY_VIOLATION check. Builders use it only to filter what they *show*; the
 * server still validates the deck.
 */
export function isWithinIdentity(card: WithIdentity, identity: ReadonlySet<string>): boolean {
  return (card.colorIdentity ?? []).every((color) => identity.has(color))
}
