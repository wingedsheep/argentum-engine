import { describe, expect, it } from 'vitest'
import {
  cheapestCost,
  computeAutoTapPreview,
  manaCostCastableWith,
  manaCostColors,
  playableWithinColors,
  reduceCostByHarmonizeTap,
  type TrimmableManaSource,
} from './manaCost'

const set = (...cs: string[]) => new Set(cs)

describe('manaCostCastableWith', () => {
  it('mono colored pip requires that colour', () => {
    expect(manaCostCastableWith('{R}', set('R'))).toBe(true)
    expect(manaCostCastableWith('{R}', set('W'))).toBe(false)
  })

  it('hybrid pip is payable with either half', () => {
    expect(manaCostCastableWith('{R/W}', set('W'))).toBe(true)
    expect(manaCostCastableWith('{R/W}', set('R'))).toBe(true)
    expect(manaCostCastableWith('{R/W}', set('U'))).toBe(false)
    // Two hybrids both satisfiable by the same single colour.
    expect(manaCostCastableWith('{G/W}{G/W}', set('W'))).toBe(true)
  })

  it('phyrexian and twobrid pips never force a colour', () => {
    expect(manaCostCastableWith('{R/P}', set())).toBe(true)
    expect(manaCostCastableWith('{2/W}', set())).toBe(true)
  })

  it('generic / X / colorless symbols never force a colour', () => {
    expect(manaCostCastableWith('{3}{X}{C}', set())).toBe(true)
  })

  it('a hard pip alongside a hybrid gates on both the hard colour and a hybrid half', () => {
    expect(manaCostCastableWith('{R}{G/W}', set('W'))).toBe(false) // {R} unpayable
    expect(manaCostCastableWith('{R}{G/W}', set('R'))).toBe(false) // {G/W} unpayable
    expect(manaCostCastableWith('{R}{G/W}', set('R', 'W'))).toBe(true)
  })
})

describe('manaCostColors', () => {
  it('collects every colour letter including both halves of a hybrid', () => {
    expect([...manaCostColors('{1}{R/W}')].sort()).toEqual(['R', 'W'])
    expect([...manaCostColors('{2}{U}')].sort()).toEqual(['U'])
    expect([...manaCostColors('{2/W}')].sort()).toEqual(['W'])
  })
})

describe('playableWithinColors', () => {
  it('hybrid card survives a single-colour "at most"', () => {
    // Figure of Destiny: {R/W}, identity {R, W}.
    expect(playableWithinColors('{R/W}', set('R', 'W'), set('W'))).toBe(true)
    expect(playableWithinColors('{R/W}', set('R', 'W'), set('R'))).toBe(true)
    expect(playableWithinColors('{R/W}', set('R', 'W'), set('U'))).toBe(false)
  })

  it('non-hybrid multicolour still needs every colour', () => {
    expect(playableWithinColors('{U}{R}', set('U', 'R'), set('U'))).toBe(false)
    expect(playableWithinColors('{U}{R}', set('U', 'R'), set('U', 'R'))).toBe(true)
  })

  it('identity colour from text/land (not in the cost) must be allowed', () => {
    // A {R/W} body with an off-colour {G} activation cost: green can't be paid in white.
    expect(playableWithinColors('{R/W}', set('R', 'W', 'G'), set('W'))).toBe(false)
    expect(playableWithinColors('{R/W}', set('R', 'W', 'G'), set('W', 'G'))).toBe(true)
    // Dual land: no cost, identity {W, U}.
    expect(playableWithinColors('', set('W', 'U'), set('W'))).toBe(false)
    expect(playableWithinColors('', set('W', 'U'), set('W', 'U'))).toBe(true)
  })

  it('colorless card passes any allowed set', () => {
    expect(playableWithinColors('{2}', set(), set())).toBe(true)
    expect(playableWithinColors('{2}', set(), set('W'))).toBe(true)
  })
})

describe('reduceCostByHarmonizeTap', () => {
  // Nature's Rhythm — Harmonize {X}{G}{G}{G}{G}. Cast with X=5, then tap a power-4
  // creature (Krotiq Nestguard). The tap reduces GENERIC mana — and {X} is generic —
  // so the {5} drops to {1}: the player owes {1}{G}{G}{G}{G}, not {5}{G}{G}{G}{G}.
  it('reduces the generic {X} of a Harmonize cost by the tapped power', () => {
    expect(reduceCostByHarmonizeTap('{X}{G}{G}{G}{G}', 5, 4)).toEqual(['1', 'G', 'G', 'G', 'G'])
  })

  it('floors the generic at zero and never touches colored pips', () => {
    // Power 6 tap against X=5: the extra reduction is wasted, {G}{G}{G}{G} stays.
    expect(reduceCostByHarmonizeTap('{X}{G}{G}{G}{G}', 5, 6)).toEqual(['G', 'G', 'G', 'G'])
  })

  it('reduces printed generic before the X (Channeled Dragonfire-style)', () => {
    // {2}{R}{R} with no X, tap power 2 -> {R}{R}.
    expect(reduceCostByHarmonizeTap('{2}{R}{R}', 0, 2)).toEqual(['R', 'R'])
    // Mixed printed + X: {1}{X}{G}, X=3, tap 2 -> 4 generic total, 2 removed -> {2}{G}.
    expect(reduceCostByHarmonizeTap('{1}{X}{G}', 3, 2)).toEqual(['2', 'G'])
  })

  it('no tap (reduction 0) just expands {X}', () => {
    expect(reduceCostByHarmonizeTap('{X}{G}{G}{G}{G}', 5, 0)).toEqual(['5', 'G', 'G', 'G', 'G'])
  })
})

describe('Harmonize-X mana pre-selection', () => {
  // Six Forests; the harmonize cost after the tap is {1}{G}{G}{G}{G} = 5 mana.
  // The pre-selection must tap exactly 5 Forests, not all 6.
  const forests: TrimmableManaSource<string>[] = Array.from({ length: 6 }, (_, i) => ({
    entityId: `forest-${i}`,
    producesColors: ['G'],
    manaAmount: 1,
  }))

  it('pre-taps exactly the lands the reduced cost needs (regression: tapped all 6)', () => {
    const reduced = reduceCostByHarmonizeTap('{X}{G}{G}{G}{G}', 5, 4) // power-4 tap
    const preselect = computeAutoTapPreview(forests, reduced)
    expect(preselect).toHaveLength(5)
  })

  it('without the tap it would need all 6 Forests for X=5', () => {
    const reduced = reduceCostByHarmonizeTap('{X}{G}{G}{G}{G}', 5, 0)
    expect(computeAutoTapPreview(forests, reduced)).toHaveLength(6)
  })
})

describe('cheapestCost', () => {
  // Emerge {5}{U} (CR 702.119a) after the server priced each sacrificeable creature: a 2-drop
  // leaves {3}{U} (4 mana), a 3-drop leaves {2}{U} (3 mana). The cast button shows the best case,
  // so it can't claim {5}{U} while sitting enabled on four lands.
  it('picks the option with the lowest total mana, not the fewest symbols', () => {
    expect(cheapestCost(['{3}{U}', '{2}{U}'])).toBe('{2}{U}')
    expect(cheapestCost(['{U}{U}{U}', '{4}'])).toBe('{U}{U}{U}')
  })

  it('a lone candidate is its own cheapest', () => {
    expect(cheapestCost(['{3}{U}'])).toBe('{3}{U}')
  })

  it('a cost the reduction wiped out entirely is cheaper than any mana cost', () => {
    expect(cheapestCost(['{U}', ''])).toBe('')
  })

  it('no candidates means no cost to show', () => {
    expect(cheapestCost([])).toBeUndefined()
  })
})
