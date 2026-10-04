/**
 * Adapter from the sealed/draft pool's `SealedCardInfo` to the deckbuilder's `CardSummary`, so the
 * pool filter can run the same Scryfall-style query language (`parseQuery`) as the constructed
 * deckbuilder's catalog search.
 *
 * `SealedCardInfo` carries a printed type line rather than structured type lists, so this splits
 * it the way it is printed: words before the dash are supertypes + card types, words after are
 * subtypes. A double-faced card contributes both faces' types and oracle text, matching Scryfall,
 * which searches every face.
 */
import type { SealedCardInfo } from '@/types'
import type { CardSummary } from '../deckbuilder/cardFilter'
import { parseManaCost } from '@/utils/manaCost'

/** Card types (CR 205.2a) in the engine's uppercase spelling. `Tribal` is Kindred's old name. */
const CARD_TYPES: Record<string, string> = {
  artifact: 'ARTIFACT',
  battle: 'BATTLE',
  creature: 'CREATURE',
  enchantment: 'ENCHANTMENT',
  instant: 'INSTANT',
  kindred: 'KINDRED',
  tribal: 'KINDRED',
  land: 'LAND',
  planeswalker: 'PLANESWALKER',
  sorcery: 'SORCERY',
}

const COLOR_OF_PIP: Record<string, string> = {
  W: 'WHITE', U: 'BLUE', B: 'BLACK', R: 'RED', G: 'GREEN',
}

const COLOR_ORDER = ['WHITE', 'BLUE', 'BLACK', 'RED', 'GREEN']

interface ParsedTypeLine {
  cardTypes: string[]
  supertypes: string[]
  subtypes: string[]
}

/** Split a printed type line ("Legendary Creature — Elf Warrior") into its three lists. */
export function parseTypeLine(typeLine: string | null | undefined): ParsedTypeLine {
  const out: ParsedTypeLine = { cardTypes: [], supertypes: [], subtypes: [] }
  if (!typeLine) return out
  // Split cards print "Instant // Sorcery"; each half is its own type line.
  for (const half of typeLine.split('//')) {
    const [front, back] = half.split(/\s+[—-]\s+/, 2)
    for (const word of (front ?? '').trim().split(/\s+/)) {
      if (!word) continue
      const cardType = CARD_TYPES[word.toLowerCase()]
      if (cardType) pushUnique(out.cardTypes, cardType)
      else pushUnique(out.supertypes, word.toUpperCase())
    }
    for (const word of (back ?? '').trim().split(/\s+/)) {
      if (word) pushUnique(out.subtypes, word)
    }
  }
  return out
}

function pushUnique(list: string[], value: string) {
  if (!list.includes(value)) list.push(value)
}

/** Mana value from the printed cost: numbers count as themselves, X as 0, `{2/W}` as 2, any other pip as 1. */
function manaValue(manaCost: string): number {
  let total = 0
  for (const symbol of parseManaCost(manaCost)) {
    const head = symbol.split('/')[0]!
    const n = parseInt(head, 10)
    if (!isNaN(n)) total += n
    else if (!/^[XYZ]$/i.test(symbol)) total += 1
  }
  return total
}

/** Printed mana-cost colours, in WUBRG order — what `cost:` filters on. */
function costColors(manaCost: string): string[] {
  const seen = new Set<string>()
  for (const symbol of parseManaCost(manaCost)) {
    for (const part of symbol.toUpperCase().split('/')) {
      const color = COLOR_OF_PIP[part]
      if (color) seen.add(color)
    }
  }
  return COLOR_ORDER.filter((c) => seen.has(c))
}

export function poolCardToSummary(card: SealedCardInfo): CardSummary {
  const manaCost = card.manaCost ?? ''
  const front = parseTypeLine(card.typeLine)
  const back = parseTypeLine(card.backFaceTypeLine)
  const oracleText = [card.oracleText, card.backFaceOracleText]
    .filter((t): t is string => !!t && t.trim().length > 0)
    .join('\n')
  return {
    name: card.name,
    manaCost,
    cmc: card.cmc ?? manaValue(manaCost),
    colors: costColors(manaCost),
    colorIdentity: [...(card.colorIdentity ?? [])],
    cardTypes: [...new Set([...front.cardTypes, ...back.cardTypes])],
    supertypes: [...new Set([...front.supertypes, ...back.supertypes])],
    subtypes: [...new Set([...front.subtypes, ...back.subtypes])],
    basicLand: front.supertypes.includes('BASIC') && front.cardTypes.includes('LAND'),
    rarity: card.rarity,
    setCode: card.setCode ?? null,
    collectorNumber: card.collectorNumber ?? null,
    oracleText: oracleText || null,
    power: card.power != null ? String(card.power) : null,
    toughness: card.toughness != null ? String(card.toughness) : null,
    imageUri: card.imageUri,
    keywords: [...(card.keywords ?? [])],
    isDoubleFaced: !!card.isDoubleFaced,
    backFaceName: card.backFaceName ?? null,
    backFaceImageUri: card.backFaceImageUri ?? null,
    ...(card.layout !== undefined ? { layout: card.layout } : {}),
    ...(card.isLandscape !== undefined ? { isLandscape: card.isLandscape } : {}),
  }
}
