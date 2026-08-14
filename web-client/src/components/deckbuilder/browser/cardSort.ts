/**
 * Catalog sort modes shared by the deckbuilder grid and the scenario builder's card browser.
 */
import type { CardSummary } from '../cardFilter'

export type SortMode = 'name' | 'cmc' | 'color' | 'rarity'

export function sortCards(cards: CardSummary[], mode: SortMode): CardSummary[] {
  const arr = [...cards]
  switch (mode) {
    case 'name':
      arr.sort((a, b) => a.name.localeCompare(b.name))
      break
    case 'cmc':
      arr.sort((a, b) => a.cmc - b.cmc || a.name.localeCompare(b.name))
      break
    case 'color':
      arr.sort((a, b) => colorBucket(a) - colorBucket(b) || a.name.localeCompare(b.name))
      break
    case 'rarity':
      arr.sort((a, b) => rarityRank(a.rarity) - rarityRank(b.rarity) || a.name.localeCompare(b.name))
      break
  }
  return arr
}

function colorBucket(c: CardSummary): number {
  if (c.colors.length === 0) return 99
  if (c.colors.length > 1) return 6
  switch (c.colors[0]) {
    case 'WHITE': return 0
    case 'BLUE': return 1
    case 'BLACK': return 2
    case 'RED': return 3
    case 'GREEN': return 4
    default: return 5
  }
}

function rarityRank(r: string): number {
  switch (r) {
    case 'MYTHIC': return 0
    case 'RARE': return 1
    case 'UNCOMMON': return 2
    case 'COMMON': return 3
    default: return 4
  }
}
