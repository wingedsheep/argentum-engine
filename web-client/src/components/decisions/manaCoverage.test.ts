import { describe, expect, it } from 'vitest'
import { computeCoverage, pipColorOptions, pipGenericAmount } from './manaCoverage'

type Pool = Parameters<typeof computeCoverage>[1]
const pool = (over: Record<string, number> = {}): Pool =>
  ({ white: 0, blue: 0, black: 0, red: 0, green: 0, colorless: 0, restrictedMana: [], ...over }) as unknown as Pool
const emptyPool = pool()

type Source = Parameters<typeof computeCoverage>[3][number]
const source = (entityId: string, producesColors: string[]) =>
  ({ entityId, producesColors }) as unknown as Source

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
})
