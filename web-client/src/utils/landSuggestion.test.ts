import { describe, expect, it } from 'vitest'
import {
  suggestBasicLands,
  type BasicLand,
  type DeckEntry,
} from './landSuggestion'

const basics: BasicLand[] = [
  { name: 'Plains', color: 'W' },
  { name: 'Island', color: 'U' },
  { name: 'Swamp', color: 'B' },
  { name: 'Mountain', color: 'R' },
  { name: 'Forest', color: 'G' },
]

function spell(name: string, manaCost: string, cmc: number, count: number): DeckEntry {
  return {
    name,
    manaCost,
    cmc,
    count,
    isLand: false,
    isBasicLand: false,
    producedColors: [],
  }
}

describe('suggestBasicLands', () => {
  it('keeps the Limited default at 17 lands', () => {
    const result = suggestBasicLands({
      entries: [
        spell('White spell', '{1}{W}', 2, 12),
        spell('Blue spell', '{2}{U}', 3, 11),
      ],
      availableBasics: basics,
      minDeckSize: 40,
    })

    expect(Object.values(result).reduce((sum, count) => sum + count, 0)).toBe(17)
  })

  it('reserves enough sources for an early double-pipped color', () => {
    const result = suggestBasicLands({
      entries: [
        spell('Red two-drop', '{1}{R}', 2, 12),
        spell('Blue double-pip', '{1}{U}{U}', 3, 2),
        spell('Colorless filler', '{3}', 3, 9),
      ],
      availableBasics: basics,
      minDeckSize: 40,
    })

    expect(result.Island ?? 0).toBeGreaterThanOrEqual(7)
    expect((result.Mountain ?? 0) + (result.Island ?? 0)).toBe(17)
  })

  it('credits dual lands before allocating basics while scaling to the picked cards', () => {
    const dual: DeckEntry = {
      name: 'Azorius dual',
      manaCost: '',
      cmc: 0,
      count: 4,
      isLand: true,
      isBasicLand: false,
      producedColors: ['W', 'U'],
    }
    const result = suggestBasicLands({
      entries: [
        spell('White spell', '{1}{W}', 2, 10),
        spell('Blue spell', '{1}{U}', 2, 9),
        dual,
      ],
      availableBasics: basics,
      minDeckSize: 40,
    })

    expect((result.Plains ?? 0) + (result.Island ?? 0)).toBe(9)
    expect(result.Plains ?? 0).toBeGreaterThan(0)
    expect(result.Island ?? 0).toBeGreaterThan(0)
  })

  it('splits hybrid requirements between either payable color', () => {
    const result = suggestBasicLands({
      entries: [spell('Hybrid spell', '{1}{W/U}', 2, 23)],
      availableBasics: basics,
      minDeckSize: 40,
    })

    expect(Math.abs((result.Plains ?? 0) - (result.Island ?? 0))).toBeLessThanOrEqual(1)
    expect((result.Plains ?? 0) + (result.Island ?? 0)).toBe(15)
  })

  it('suggests no off-color basic for a hybrid pip the deck can already pay', () => {
    const result = suggestBasicLands({
      entries: [
        spell('Green one-drop', '{G}', 1, 8),
        spell('Black one-drop', '{B}', 1, 7),
        spell('Green three-drop', '{2}{G}', 3, 7),
        spell('Wary Farmer', '{1}{G/W}{G/W}', 3, 1),
      ],
      availableBasics: basics,
      minDeckSize: 40,
    })

    expect(result.Plains).toBe(0)
    expect((result.Forest ?? 0) + (result.Swamp ?? 0)).toBe(15)
  })

  it('charges a two-color hybrid to whichever of its colors the deck needs most', () => {
    const result = suggestBasicLands({
      entries: [
        spell('Green one-drop', '{G}', 1, 14),
        spell('Black splash', '{3}{B}', 4, 1),
        spell('Golgari hybrid', '{4}{B/G}', 5, 8),
      ],
      availableBasics: basics,
      minDeckSize: 40,
    })

    expect(result.Swamp).toBeLessThanOrEqual(3)
    expect((result.Forest ?? 0) + (result.Swamp ?? 0)).toBe(17)
  })

  it('puts every basic into the sole color of a mono-color Limited deck', () => {
    const result = suggestBasicLands({
      entries: [spell('Green spell', '{2}{G}', 3, 23)],
      availableBasics: basics,
      minDeckSize: 40,
    })

    expect(result.Forest).toBe(17)
    expect(result.Plains).toBe(0)
    expect(result.Island).toBe(0)
    expect(result.Swamp).toBe(0)
    expect(result.Mountain).toBe(0)
  })

  it('keeps a late single-card splash smaller than the early main color', () => {
    const result = suggestBasicLands({
      entries: [
        spell('Red two-drop', '{1}{R}', 2, 18),
        spell('Black finisher', '{5}{B}', 6, 1),
        spell('Colorless filler', '{3}', 3, 4),
      ],
      availableBasics: basics,
      minDeckSize: 40,
    })

    expect(result.Swamp).toBeGreaterThanOrEqual(3)
    expect(result.Mountain ?? 0).toBeGreaterThan(result.Swamp ?? 0)
    expect((result.Mountain ?? 0) + (result.Swamp ?? 0)).toBe(17)
  })

  it('scales a partial constructed deck instead of filling it to 60 cards', () => {
    const result = suggestBasicLands({
      entries: [
        spell('White two-drop', '{1}{W}', 2, 18),
        spell('Blue three-drop', '{2}{U}', 3, 18),
      ],
      availableBasics: basics,
      minDeckSize: 60,
    })

    expect(Object.values(result).reduce((sum, count) => sum + count, 0)).toBe(27)
    expect(Math.abs((result.Plains ?? 0) - (result.Island ?? 0))).toBeLessThanOrEqual(1)
  })

  it('lets two mana-producing nonlands replace one land when the deck has enough spells', () => {
    const manaRock: DeckEntry = {
      name: 'Mana rock',
      manaCost: '{2}',
      cmc: 2,
      count: 2,
      isLand: false,
      isBasicLand: false,
      producedColors: ['W', 'U'],
    }
    const result = suggestBasicLands({
      entries: [
        spell('White spell', '{1}{W}', 2, 11),
        spell('Blue spell', '{1}{U}', 2, 11),
        manaRock,
      ],
      availableBasics: basics,
      minDeckSize: 40,
    })

    expect(Object.values(result).reduce((sum, count) => sum + count, 0)).toBe(15)
  })

  it('uses an available fallback basic when the matching basic is unavailable', () => {
    const result = suggestBasicLands({
      entries: [spell('Blue spell', '{1}{U}', 2, 23)],
      availableBasics: [{ name: 'Wastes-like fallback', color: 'W' }],
      minDeckSize: 40,
    })

    expect(result['Wastes-like fallback']).toBe(15)
  })

  it('returns zeroes for an empty deck instead of inventing a mana base', () => {
    const result = suggestBasicLands({ entries: [], availableBasics: basics, minDeckSize: 40 })

    expect(result).toEqual({ Plains: 0, Island: 0, Swamp: 0, Mountain: 0, Forest: 0 })
  })

  it('treats two hybrid symbols as needing two combined white-or-blue sources', () => {
    const result = suggestBasicLands({
      entries: [
        spell('Red two-drop', '{1}{R}', 2, 12),
        spell('Double hybrid spell', '{W/U}{W/U}', 2, 2),
        spell('Colorless filler', '{3}', 3, 9),
      ],
      availableBasics: basics,
      minDeckSize: 40,
    })

    expect((result.Plains ?? 0) + (result.Island ?? 0)).toBeGreaterThanOrEqual(7)
    expect((result.Plains ?? 0) + (result.Island ?? 0) + (result.Mountain ?? 0)).toBe(17)
  })

  it('does not require a colored source for a purely Phyrexian pip', () => {
    const result = suggestBasicLands({
      entries: [
        spell('Red spell', '{1}{R}', 2, 22),
        spell('Phyrexian spell', '{W/P}', 1, 1),
      ],
      availableBasics: basics,
      minDeckSize: 40,
    })

    expect(result.Plains).toBe(0)
    expect(result.Mountain).toBe(15)
  })

  it('does not merge the colored requirements of mutually exclusive card faces', () => {
    const result = suggestBasicLands({
      entries: [
        spell('White spell', '{1}{W}', 2, 22),
        spell('Two-faced spell', '{1}{W} // {3}{U}{U}', 2, 1),
      ],
      availableBasics: basics,
      minDeckSize: 40,
    })

    expect(result.Island).toBe(0)
    expect(result.Plains).toBe(15)
  })

  it('keeps every represented color alive in a five-color Limited deck', () => {
    const result = suggestBasicLands({
      entries: [
        spell('White spell', '{2}{W}', 3, 5),
        spell('Blue spell', '{2}{U}', 3, 5),
        spell('Black spell', '{2}{B}', 3, 5),
        spell('Red spell', '{2}{R}', 3, 4),
        spell('Green spell', '{2}{G}', 3, 4),
      ],
      availableBasics: basics,
      minDeckSize: 40,
    })

    for (const land of basics) expect(result[land.name] ?? 0).toBeGreaterThanOrEqual(3)
    expect(Object.values(result).reduce((sum, count) => sum + count, 0)).toBe(17)
  })

  it('adds no basics when existing lands already meet the target', () => {
    const fiveColorLand: DeckEntry = {
      name: 'Five-color land',
      manaCost: '',
      cmc: 0,
      count: 20,
      isLand: true,
      isBasicLand: false,
      producedColors: ['W', 'U', 'B', 'R', 'G'],
    }
    const result = suggestBasicLands({
      entries: [spell('Gold spell', '{W}{U}{B}{R}{G}', 5, 23), fiveColorLand],
      availableBasics: basics,
      minDeckSize: 40,
    })

    expect(Object.values(result).reduce((sum, count) => sum + count, 0)).toBe(0)
  })

  it('scales with the cards picked instead of filling a partial sealed deck with basics', () => {
    const result = suggestBasicLands({
      entries: [
        spell('Green two-drop', '{1}{G}', 2, 6),
        spell('Green three-drop', '{2}{G}', 3, 4),
      ],
      availableBasics: basics,
      minDeckSize: 40,
    })

    // 10 spells at a ~0.425 ratio wants ~7 lands, not the 30 that filling out
    // a 40-card deck would demand.
    expect(result.Forest).toBe(7)
    expect(Object.values(result).reduce((sum, count) => sum + count, 0)).toBe(7)
  })

  it('scales with the cards picked when no deck size is known', () => {
    const result = suggestBasicLands({
      entries: [
        spell('White two-drop', '{1}{W}', 2, 6),
        spell('White three-drop', '{2}{W}', 3, 6),
      ],
      availableBasics: basics,
    })

    expect(result.Plains).toBe(9)
  })

  it('grows the suggestion monotonically as more cards are picked', () => {
    const totals = [6, 12, 18, 23].map((count) => {
      const result = suggestBasicLands({
        entries: [spell('Blue three-drop', '{2}{U}', 3, count)],
        availableBasics: basics,
        minDeckSize: 40,
      })
      return Object.values(result).reduce((sum, n) => sum + n, 0)
    })

    expect(totals).toEqual([...totals].sort((a, b) => a - b))
    expect(totals[0]).toBeLessThan(10)
    expect(totals[3]).toBe(17)
  })

  it('continues scaling after the old 22-card fill-to-40 threshold', () => {
    const totals = [22, 24, 30].map((count) => {
      const result = suggestBasicLands({
        entries: [spell('Green three-drop', '{2}{G}', 3, count)],
        availableBasics: basics,
        minDeckSize: 40,
      })
      return Object.values(result).reduce((sum, n) => sum + n, 0)
    })

    expect(totals).toEqual([16, 18, 22])
    expect(totals.map((lands, index) => lands + [22, 24, 30][index]!)).toEqual([38, 42, 52])
  })

  it('shares a partial deck of too-few lands across all its colors', () => {
    const result = suggestBasicLands({
      entries: [
        spell('White one-drop', '{W}', 1, 2),
        spell('Blue one-drop', '{U}', 1, 2),
        spell('Black one-drop', '{B}', 1, 2),
      ],
      availableBasics: basics,
      minDeckSize: 40,
    })

    for (const color of ['Plains', 'Island', 'Swamp']) {
      expect(result[color] ?? 0).toBeGreaterThan(0)
    }
  })

  it('uses only the first available printing for a basic-land color', () => {
    const result = suggestBasicLands({
      entries: [spell('White spell', '{1}{W}', 2, 23)],
      availableBasics: [
        { name: 'Plains A', color: 'W' },
        { name: 'Plains B', color: 'W' },
      ],
      minDeckSize: 40,
    })

    expect(result['Plains A']).toBe(15)
    expect(result['Plains B']).toBe(0)
  })
})
