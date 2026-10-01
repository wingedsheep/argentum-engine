import { describe, expect, it } from 'vitest'
import { computeCoverage, pipColorOptions, pipGenericAmount } from './manaCoverage'

type Pool = Parameters<typeof computeCoverage>[1]
const pool = (over: Record<string, number> = {}): Pool =>
  ({ white: 0, blue: 0, black: 0, red: 0, green: 0, colorless: 0, restrictedMana: [], ...over }) as unknown as Pool
const emptyPool = pool()

type Source = Parameters<typeof computeCoverage>[3][number]
const source = (entityId: string, producesColors: string[], manaAmount?: number) =>
  ({ entityId, producesColors, ...(manaAmount !== undefined ? { manaAmount } : {}) }) as unknown as Source

describe('pip parsing', () => {
  it('accepts either half of a hybrid', () => {
    expect(pipColorOptions('W/B')).toEqual(['W', 'B'])
  })

  it('keeps the colour half of a monocolour hybrid and exposes its generic half', () => {
    expect(pipColorOptions('2/W')).toEqual(['W'])
    expect(pipGenericAmount('2/W')).toBe(2)
  })

  it('treats a plain colour pip as having no generic half', () => {
    expect(pipGenericAmount('W')).toBeNull()
    expect(pipGenericAmount('3')).toBe(3)
  })
})

describe('computeCoverage', () => {
  // Extort ("you may pay {W/B}") was unpayable: the hybrid pip matched no colour in the floating
  // pass, no source in the selection pass, and parseInt('W/B') was NaN in the generic pass, so the
  // Pay button never enabled.
  it('covers a hybrid pip with a source producing either half', () => {
    const swamp = source('swamp', ['BLACK'])
    const coverage = computeCoverage(['W/B'], emptyPool, ['swamp'] as never, [swamp], 0)
    expect(coverage[0]!.pending).toBe(true)
  })

  it('covers a hybrid pip with floating mana of either half', () => {
    const coverage = computeCoverage(['W/B'], pool({ white: 1 }), [], [], 0)
    expect(coverage[0]!.floating).toBe(true)
  })

  it('leaves a hybrid pip uncovered when neither half is available', () => {
    const forest = source('forest', ['GREEN'])
    const coverage = computeCoverage(['W/B'], emptyPool, ['forest'] as never, [forest], 0)
    expect(coverage[0]!.floating).toBe(false)
    expect(coverage[0]!.pending).toBe(false)
  })

  it('still covers plain coloured and generic pips', () => {
    const plains = source('plains', ['WHITE'])
    const coverage = computeCoverage(['W', '1'], pool({ blue: 1 }), ['plains'] as never, [plains], 0)
    expect(coverage[0]!.pending).toBe(true)
    expect(coverage[1]!.floating).toBe(true)
  })

  // A ward of {3} with a Gilded Lotus selected: the prompt used to count the Lotus as one pip,
  // so the Pay button stayed dead on a payment the server would have accepted.
  it('lets a multi-mana source cover more than one pip', () => {
    const lotus = source('lotus', ['WHITE', 'BLUE', 'BLACK', 'RED', 'GREEN'], 3)
    const coverage = computeCoverage(['3'], emptyPool, ['lotus'] as never, [lotus], 0)
    expect(coverage[0]!.pending).toBe(true)
  })

  it('spends a multi-mana source on a coloured pip first and the rest on generic', () => {
    const lotus = source('lotus', ['WHITE', 'BLUE', 'BLACK', 'RED', 'GREEN'], 3)
    const coverage = computeCoverage(['U', '2'], emptyPool, ['lotus'] as never, [lotus], 0)
    expect(coverage[0]!.pending).toBe(true)
    expect(coverage[1]!.pending).toBe(true)
  })

  it('lets a multi-mana source cover several pips of one colour', () => {
    const lotus = source('lotus', ['WHITE', 'BLUE', 'BLACK', 'RED', 'GREEN'], 3)
    const coverage = computeCoverage(['U', 'U', '1'], emptyPool, ['lotus'] as never, [lotus], 0)
    expect(coverage.map((pip) => pip.pending)).toEqual([true, true, true])
  })

  it('spends a multi-mana source on one colour only', () => {
    // Three mana of any *one* colour: the Lotus can pay {U}{U} but not {U}{W}.
    const lotus = source('lotus', ['WHITE', 'BLUE', 'BLACK', 'RED', 'GREEN'], 3)
    const coverage = computeCoverage(['U', 'W'], emptyPool, ['lotus'] as never, [lotus], 0)
    expect(coverage.filter((pip) => pip.pending)).toHaveLength(1)
  })

  it('reads a source without manaAmount as one mana', () => {
    const plains = source('plains', ['WHITE'])
    const coverage = computeCoverage(['2'], emptyPool, ['plains'] as never, [plains], 0)
    expect(coverage[0]!.pending).toBe(false)
  })
})

describe('server-supplied mana payment colors', () => {
  it('credits white mana for red without duplicating the unit for white', () => {
    const accepted = { R: ['R', 'W'] }
    const coverage = computeCoverage(['R', 'W'], pool({ white: 1 }), [], [], 0, accepted)
    expect(coverage[0]?.floating).toBe(false)
    expect(coverage[1]?.floating).toBe(true)
    expect(computeCoverage(['R'], pool({ white: 1 }), [], [], 0, accepted)[0]?.floating).toBe(true)
  })
  it('uses a selected white source for a red pip', () => {
    const plains = source('white', ['WHITE'], 1)
    const coverage = computeCoverage(['R'], emptyPool, ['white'] as never, [plains], 0, { R: ['R', 'W'] })
    expect(coverage[0]?.pending).toBe(true)
  })
  it('reserves a flexible source for a strict pip before using white as red', () => {
    const dual = source('dual', ['WHITE', 'BLUE'])
    const plains = source('plains', ['WHITE'])
    const coverage = computeCoverage(['U', 'R'], emptyPool, ['dual', 'plains'] as never,
      [dual, plains], 0, { R: ['R', 'W'] })
    expect(coverage.map((pip) => pip.pending)).toEqual([true, true])
  })
  it('backtracks floating mana when a selected source covers only the other pip', () => {
    const plains = source('plains', ['WHITE'])
    const coverage = computeCoverage(['W/U', 'U/B'], pool({ blue: 1 }), ['plains'] as never, [plains])
    expect(coverage.map((pip) => pip.floating || pip.pending)).toEqual([true, true])
  })

  it('covers a large mixed selection with flexible sources and white substitution', () => {
    const duals = Array.from({ length: 12 }, (_, index) => source(`dual-${index}`, ['WHITE', 'BLUE']))
    const plains = Array.from({ length: 12 }, (_, index) => source(`plains-${index}`, ['WHITE']))
    const sources = [...duals, ...plains]
    const coverage = computeCoverage(Array.from({ length: 12 }, () => ['U', 'R']).flat(), emptyPool,
      sources.map((item) => item.entityId), sources, 0, { R: ['R', 'W'] })
    expect(coverage.every((pip) => pip.pending)).toBe(true)
  })

})
