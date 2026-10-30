import { describe, expect, it } from 'vitest'
import { filterGuideSets, sortGuideSets, toGuideSets, type GuideFilter } from './setGuideModel'
import { SET_PROFILES, SET_THEMES } from './setProfiles'

const sets = toGuideSets([
  { setCode: 'POR', setName: 'Portal', implementedCount: 222, releaseDate: '1997-05-01' },
  { setCode: 'BLB', setName: 'Bloomburrow', implementedCount: 261, releaseDate: '2024-08-02' },
  { setCode: 'MH3', setName: 'Modern Horizons 3', implementedCount: 303, releaseDate: '2024-06-14' },
  { setCode: 'ZZZ', setName: 'Unprofiled', implementedCount: 10, releaseDate: '2030-01-01' },
])

const none: GuideFilter = { search: '', quick: new Set(), themes: new Set() }
const codes = (xs: { code: string }[]) => xs.map((s) => s.code)

describe('setGuideModel', () => {
  it('joins each set with its profile and year', () => {
    expect(sets[0]?.profile?.tagline).toBeTruthy()
    expect(sets[0]?.year).toBe('1997')
    expect(sets[3]?.profile).toBeUndefined()
  })

  it('combines quick filters, themes and search', () => {
    expect(codes(filterGuideSets(sets, { ...none, quick: new Set(['beginner']) }))).toEqual(['POR'])
    expect(codes(filterGuideSets(sets, { ...none, quick: new Set(['fast', 'highPower']) }))).toEqual(['BLB', 'MH3'])
    expect(codes(filterGuideSets(sets, { ...none, themes: new Set(['tribal']) }))).toEqual(['BLB'])
    expect(codes(filterGuideSets(sets, { ...none, search: 'offspring' }))).toEqual(['BLB'])
    expect(codes(filterGuideSets(sets, { ...none, search: 'mh3' }))).toEqual(['MH3'])
  })

  it('sorts by rating, highest first, with unprofiled sets last', () => {
    expect(codes(sortGuideSets(sets, 'power'))).toEqual(['MH3', 'BLB', 'POR', 'ZZZ'])
    expect(codes(sortGuideSets(sets, 'newest'))).toEqual(['ZZZ', 'BLB', 'MH3', 'POR'])
  })

  it('profiles use only known themes and in-range ratings', () => {
    const known = new Set(SET_THEMES.map((t) => t.id))
    for (const [code, p] of Object.entries(SET_PROFILES)) {
      expect(p.themes.every((t) => known.has(t)), code).toBe(true)
      expect(p.mechanics.length, code).toBeGreaterThan(0)
      for (const r of [p.power, p.speed, p.complexity]) expect(r >= 1 && r <= 5, code).toBe(true)
    }
  })
})
