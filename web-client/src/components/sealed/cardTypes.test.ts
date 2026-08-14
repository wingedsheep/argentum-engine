import { describe, it, expect } from 'vitest'
import {
  classifyCardType,
  formatTypeLine,
  rarityInitial,
  rarityLabel,
  summarizeDeckTypes,
} from './cardTypes'

describe('classifyCardType', () => {
  it('buckets the plain card types', () => {
    expect(classifyCardType('Instant')).toBe('Instant')
    expect(classifyCardType('Sorcery')).toBe('Sorcery')
    expect(classifyCardType('Artifact — Equipment')).toBe('Artifact')
    expect(classifyCardType('Enchantment — Aura')).toBe('Enchantment')
    expect(classifyCardType('Legendary Planeswalker — Jace')).toBe('Planeswalker')
    expect(classifyCardType('Battle — Siege')).toBe('Battle')
  })

  it('treats multi-type cards as creatures', () => {
    expect(classifyCardType('Artifact Creature — Golem')).toBe('Creature')
    expect(classifyCardType('Enchantment Creature — Nymph')).toBe('Creature')
    expect(classifyCardType('Legendary Creature — Human Wizard')).toBe('Creature')
  })

  it('treats land creatures as lands — they eat the land drop, not a spell slot', () => {
    expect(classifyCardType('Land Creature — Forest Dryad')).toBe('Land')
    expect(classifyCardType('Artifact Land')).toBe('Land')
    expect(classifyCardType('Basic Land — Island')).toBe('Land')
  })

  it('ignores subtypes when matching card types', () => {
    // "Artificer" and "Islandwalk"-ish subtypes must not leak into the card-type check.
    expect(classifyCardType('Creature — Human Artificer')).toBe('Creature')
    expect(classifyCardType('Enchantment — Saga')).toBe('Enchantment')
  })

  it('handles the ASCII dash some definitions carry', () => {
    expect(classifyCardType('Creature - Goblin Warrior')).toBe('Creature')
  })

  it('falls back to Other for unrecognised type lines', () => {
    expect(classifyCardType('Conspiracy')).toBe('Other')
  })
})

describe('summarizeDeckTypes', () => {
  it('counts each card once, in display order, dropping empty buckets', () => {
    expect(
      summarizeDeckTypes([
        'Creature — Goblin',
        'Artifact Creature — Golem',
        'Instant',
        'Instant',
        'Enchantment — Aura',
      ]),
    ).toEqual([
      { type: 'Creature', count: 2 },
      { type: 'Instant', count: 2 },
      { type: 'Enchantment', count: 1 },
    ])
  })

  it('folds basic lands into the Land bucket', () => {
    expect(summarizeDeckTypes(['Land — Desert', 'Sorcery'], 16)).toEqual([
      { type: 'Sorcery', count: 1 },
      { type: 'Land', count: 17 },
    ])
  })

  it('returns nothing for an empty deck', () => {
    expect(summarizeDeckTypes([])).toEqual([])
    expect(summarizeDeckTypes([], 0)).toEqual([])
  })
})

describe('rarity helpers', () => {
  it('uses M for mythic and the initial otherwise', () => {
    expect(rarityInitial('MYTHIC')).toBe('M')
    expect(rarityInitial('rare')).toBe('R')
    expect(rarityInitial('UNCOMMON')).toBe('U')
    expect(rarityInitial('COMMON')).toBe('C')
  })

  it('labels known rarities and passes unknown ones through uppercased', () => {
    expect(rarityLabel('mythic')).toBe('Mythic Rare')
    expect(rarityLabel('WEIRD')).toBe('WEIRD')
  })
})

describe('formatTypeLine', () => {
  it('normalizes the ASCII dash to an em-dash', () => {
    expect(formatTypeLine('Creature - Goblin Warrior')).toBe('Creature — Goblin Warrior')
    expect(formatTypeLine('Creature — Goblin Warrior')).toBe('Creature — Goblin Warrior')
    expect(formatTypeLine('Instant')).toBe('Instant')
  })
})
