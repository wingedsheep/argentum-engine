/**
 * Keyword ability icon mappings using mana-font CSS classes.
 * Uses the Arena ability icon set from the mana-font package.
 * Class format: "ms ms-ability-{name}" rendered via <i> elements.
 */

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
  CANT_BE_BLOCKED: 'ability-unblockable',
}

export const displayableKeywords = new Set([
  'FLYING', 'REACH', 'TRAMPLE',
  'FIRST_STRIKE', 'DOUBLE_STRIKE', 'DEATHTOUCH',
  'LIFELINK', 'VIGILANCE', 'HASTE', 'HEXPROOF',
  'SHROUD', 'INDESTRUCTIBLE', 'DEFENDER', 'MENACE', 'FEAR',
  'FLASH', 'PROWESS', 'WARD', 'INTIMIDATE', 'INFECT',
  'CANT_BE_BLOCKED',
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
  INDESTRUCTIBLE: 'ability-indestructible',
  STASH: 'counter-charge',
  BLIGHT: 'counter-skull',
}
