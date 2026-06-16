/**
 * Keyword ability icon mappings using mana-font CSS classes.
 * Uses the Arena ability icon set from the mana-font package.
 * Class format: "ms ms-ability-{name}" rendered via <i> elements.
 *
 * Keywords without a mana-font glyph fall back to a local SVG via `keywordSvgIcon`.
 */
import persistSvgUrl from './persist.svg'
import bandingSvgUrl from './banding.svg'
import questCounterSvgUrl from '../counters/quest.svg'
import hourglassCounterSvgUrl from '../counters/hourglass.svg'

/** Maps engine keyword names to local SVG URLs (used when mana-font has no glyph). */
export const keywordSvgIcon: Record<string, string> = {
  PERSIST: persistSvgUrl,
  BANDING: bandingSvgUrl,
}

/** Maps engine CounterType names to local SVG URLs (used when mana-font has no glyph or we prefer custom art). */
export const counterSvgIcon: Record<string, string> = {
  QUEST: questCounterSvgUrl,
  HOURGLASS: hourglassCounterSvgUrl,
}

/** Maps engine keyword names to mana-font ability class suffixes */
export const keywordManaClass: Record<string, string> = {
  FLYING: 'ability-flying',
  REACH: 'ability-reach',
  TRAMPLE: 'ability-trample',
  FIRST_STRIKE: 'ability-first-strike',
  DOUBLE_STRIKE: 'ability-double-strike',
  DEATHTOUCH: 'ability-deathtouch',
  LIFELINK: 'ability-lifelink',
  VIGILANCE: 'ability-vigilance',
  HASTE: 'ability-haste',
  HEXPROOF: 'ability-hexproof',
  SHROUD: 'ability-shroud',
  INDESTRUCTIBLE: 'ability-indestructible',
  DEFENDER: 'ability-defender',
  MENACE: 'ability-menace',
  FEAR: 'ability-fear',
  FLASH: 'ability-flash',
  PROWESS: 'ability-prowess',
  WARD: 'ability-ward',
  INTIMIDATE: 'ability-intimidate',
  INFECT: 'ability-infect',
  MORPH: 'ability-morph',
  PROTECTION: 'ability-protection',
  WITHER: 'ability-infect',
  TOXIC: 'ability-toxic',
  CANT_BE_BLOCKED: 'ability-unblockable',
  CHANGELING: 'ability-changeling',
  /** Suspect status (CR 701.60). Rendered via the synthetic SUSPECTED pseudo-keyword from
   *  ProjectedState.isSuspected — the status itself isn't a keyword, but reusing this
   *  icon table keeps the badge rendering uniform. */
  SUSPECTED: 'ability-suspect',
}

export const displayableKeywords = new Set([
  'FLYING', 'REACH', 'TRAMPLE',
  'FIRST_STRIKE', 'DOUBLE_STRIKE', 'DEATHTOUCH',
  'LIFELINK', 'VIGILANCE', 'HASTE', 'HEXPROOF',
  'SHROUD', 'INDESTRUCTIBLE', 'DEFENDER', 'MENACE', 'FEAR',
  'PROWESS', 'WARD', 'INTIMIDATE', 'INFECT',
  'WITHER', 'TOXIC', 'CANT_BE_BLOCKED', 'CHANGELING',
  'PERSIST', 'BANDING',
])

/** Maps engine CounterType to mana-font counter class suffixes */
export const counterManaClass: Record<string, string> = {
  PLUS_ONE_PLUS_ONE: 'counter-plus',
  MINUS_ONE_MINUS_ONE: 'counter-minus',
  LOYALTY: 'counter-loyalty',
  CHARGE: 'counter-charge',
  GEM: 'counter-charge',
  GOLD: 'counter-gold',
  PLAGUE: 'counter-skull',
  TRAP: 'counter-arrow',
  DEPLETION: 'counter-void',
  LORE: 'counter-lore',
  STUN: 'counter-bolt',
  FINALITY: 'counter-finality',
  SUPPLY: 'counter-brick',
  FLYING: 'ability-flying',
  FIRST_STRIKE: 'ability-first-strike',
  LIFELINK: 'ability-lifelink',
  INDESTRUCTIBLE: 'ability-indestructible',
  DEATHTOUCH: 'ability-deathtouch',
  TRAMPLE: 'ability-trample',
  HEXPROOF: 'ability-hexproof',
  STASH: 'counter-charge',
  BLIGHT: 'counter-skull',
  COIN: 'counter-charge',
  FLOOD: 'counter-flood',
  CHORUS: 'counter-charge',
  DREAM: 'counter-charge',
  QUEST: 'counter-lore',
  GROWTH: 'counter-charge',
  TIME: 'counter-time',
  FEATHER: 'counter-charge',
  DECAYED: 'ability-decayed',
  HOPE: 'counter-charge',
  VERSE: 'counter-verse',
  INFLUENCE: 'counter-devotion',
  BURDEN: 'counter-doom',
  LOOT: 'counter-charge',
  WIND: 'counter-vortex',
}
