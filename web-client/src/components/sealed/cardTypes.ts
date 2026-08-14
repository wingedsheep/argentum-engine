/**
 * Card-type classification helpers shared by the sealed/draft deck builder's deck panel.
 *
 * The deck breakdown assigns every card exactly one bucket so the counts sum to the deck size —
 * a "distribution", not a set of overlapping tallies.
 */

/** Buckets tracked by the deck breakdown, in display order. */
export const DECK_CARD_TYPES = [
  'Creature',
  'Instant',
  'Sorcery',
  'Artifact',
  'Enchantment',
  'Planeswalker',
  'Battle',
  'Land',
  'Other',
] as const

export type DeckCardType = (typeof DECK_CARD_TYPES)[number]

/** Bar/chip colour per bucket. Creature/Land reuse the shades the old Creatures/Lands stats used. */
export const CARD_TYPE_COLORS: Record<DeckCardType, string> = {
  Creature: '#8bc34a',
  Instant: '#4fc3f7',
  Sorcery: '#ba68c8',
  Artifact: '#b0bec5',
  Enchantment: '#ffd54f',
  Planeswalker: '#ff8a65',
  Battle: '#ef5350',
  Land: '#a1887f',
  Other: '#888888',
}

/** Plural chip label per bucket ("Other" is already a mass noun). */
const CARD_TYPE_PLURALS: Record<DeckCardType, string> = {
  Creature: 'Creatures',
  Instant: 'Instants',
  Sorcery: 'Sorceries',
  Artifact: 'Artifacts',
  Enchantment: 'Enchantments',
  Planeswalker: 'Planeswalkers',
  Battle: 'Battles',
  Land: 'Lands',
  Other: 'Other',
}

/** Chip label for a bucket, singular at a count of one ("1 Artifact", not "1 Artifacts"). */
export function cardTypeLabel(type: DeckCardType, count: number): string {
  return count === 1 ? (type === 'Other' ? 'Other' : type) : CARD_TYPE_PLURALS[type]
}

/** Rarity swatch colours, shared by the pool's section headers and the deck rows' rarity badge. */
export const RARITY_COLORS: Record<string, string> = {
  MYTHIC: '#ff8b00',
  RARE: '#ffd700',
  UNCOMMON: '#c0c0c0',
  COMMON: '#888888',
  SPECIAL: '#b565d8',
  BONUS: '#b565d8',
}

export const RARITY_LABELS: Record<string, string> = {
  MYTHIC: 'Mythic Rare',
  RARE: 'Rare',
  UNCOMMON: 'Uncommon',
  COMMON: 'Common',
  SPECIAL: 'Special',
  BONUS: 'Bonus',
}

/** Single-letter badge per rarity — 'M', 'R', 'U', 'C'. Falls back to the first letter. */
export function rarityInitial(rarity: string): string {
  const key = rarity.toUpperCase()
  return key === 'MYTHIC' ? 'M' : (key.charAt(0) || '?')
}

export function rarityColor(rarity: string): string {
  return RARITY_COLORS[rarity.toUpperCase()] ?? '#888888'
}

export function rarityLabel(rarity: string): string {
  const key = rarity.toUpperCase()
  return RARITY_LABELS[key] ?? key
}

/**
 * Everything before the type line's dash — the card types and supertypes. Handles both the
 * em-dash Scryfall prints and the ASCII " - " some of our own definitions carry.
 *
 * Splitting first matters: subtypes would otherwise poison substring checks (an
 * "Enchantment — Saga" is not an artifact, but "Artificer" would match a naive `includes`).
 */
function cardTypesPortion(typeLine: string): string {
  const dashIndex = typeLine.indexOf('—')
  const hyphenIndex = typeLine.indexOf(' - ')
  const splitIndex = dashIndex !== -1 ? dashIndex : hyphenIndex
  return (splitIndex === -1 ? typeLine : typeLine.slice(0, splitIndex)).toLowerCase()
}

/**
 * Put a card in exactly one bucket. Precedence resolves multi-type cards the way a deckbuilder
 * thinks about them: an Artifact Creature is a creature, and Dryad Arbor is a land (it costs no
 * mana and eats the land drop).
 */
export function classifyCardType(typeLine: string): DeckCardType {
  const types = cardTypesPortion(typeLine)
  if (types.includes('land')) return 'Land'
  if (types.includes('creature')) return 'Creature'
  if (types.includes('planeswalker')) return 'Planeswalker'
  if (types.includes('battle')) return 'Battle'
  if (types.includes('instant')) return 'Instant'
  if (types.includes('sorcery')) return 'Sorcery'
  if (types.includes('artifact')) return 'Artifact'
  if (types.includes('enchantment')) return 'Enchantment'
  return 'Other'
}

/**
 * Count deck cards per bucket. `extraLands` folds in the basic lands, which live outside the
 * card list in `landCounts`.
 */
export function summarizeDeckTypes(
  typeLines: readonly string[],
  extraLands = 0,
): Array<{ type: DeckCardType; count: number }> {
  const counts = new Map<DeckCardType, number>()
  for (const typeLine of typeLines) {
    const type = classifyCardType(typeLine)
    counts.set(type, (counts.get(type) ?? 0) + 1)
  }
  if (extraLands > 0) counts.set('Land', (counts.get('Land') ?? 0) + extraLands)
  return DECK_CARD_TYPES.filter((type) => (counts.get(type) ?? 0) > 0).map((type) => ({
    type,
    count: counts.get(type) ?? 0,
  }))
}

/**
 * The subtitle shown under a deck row's card name: the printed type line with its dash
 * normalized to an em-dash, so "Creature - Goblin Warrior" and "Creature — Goblin Warrior"
 * render identically.
 */
export function formatTypeLine(typeLine: string): string {
  return typeLine.replace(' - ', ' — ').trim()
}
