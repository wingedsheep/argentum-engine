import { describe, expect, it } from 'vitest'
import type { EntityId } from '@/types'
import { Keyword } from '@/types/enums'
import { bandIsLegal, mergedBand, type BandingCard } from './combatBands'

const id = (s: string) => s as EntityId
const LEGENDARY = 'BANDS_WITH_OTHER_SUPERTYPE_LEGENDARY'
const WOLVES = 'BANDS_WITH_OTHER_NAMED_Wolves of the Hunt'
const cards: Record<string, BandingCard> = {
  knight: { keywords: [Keyword.BANDING] },
  squire: { keywords: [Keyword.BANDING] },
  bear: { keywords: [] },
  wolf: { keywords: [] },
  // Legendary creatures; `serra` has "bands with other legendary creatures" (Cathedral of Serra).
  serra: { keywords: [], bandsWithOther: [LEGENDARY], bandQualities: [LEGENDARY] },
  lord: { keywords: [], bandQualities: [LEGENDARY] },
  hero: { keywords: [], bandQualities: [LEGENDARY] },
  // A creature with the ability whose quality it lacks can't anchor the band.
  outsider: { keywords: [], bandsWithOther: [WOLVES], bandQualities: [LEGENDARY] },
}
const cardOf = (x: EntityId) => cards[x]

describe('mergedBand', () => {
  it('forms a fresh pair when neither creature is banded', () => {
    expect(mergedBand([], id('knight'), id('bear'))).toEqual(['knight', 'bear'])
  })

  it('folds both existing bands into one', () => {
    const bands = [[id('knight'), id('bear')], [id('squire'), id('wolf')]]
    expect(mergedBand(bands, id('knight'), id('squire'))).toEqual(['knight', 'squire', 'bear', 'wolf'])
  })
})

describe('bandIsLegal (CR 702.22c)', () => {
  it('allows one non-banding creature alongside banders', () => {
    expect(bandIsLegal([id('knight'), id('squire'), id('bear')], cardOf)).toBe(true)
  })

  it('refuses two creatures without banding', () => {
    expect(bandIsLegal([id('bear'), id('wolf')], cardOf)).toBe(false)
    expect(bandIsLegal([id('knight'), id('bear'), id('wolf')], cardOf)).toBe(false)
  })

  it('allows any number of creatures sharing a quality one of them bands with other (CR 702.22b)', () => {
    expect(bandIsLegal([id('serra'), id('lord'), id('hero')], cardOf)).toBe(true)
  })

  it('refuses a bands-with-other band when a member lacks the quality', () => {
    expect(bandIsLegal([id('serra'), id('lord'), id('bear')], cardOf)).toBe(false)
  })

  it('refuses a band whose only bands-with-other ability names a quality the members lack', () => {
    expect(bandIsLegal([id('outsider'), id('lord'), id('hero')], cardOf)).toBe(false)
  })

  it('needs a member with the ability — sharing the quality alone is not enough', () => {
    expect(bandIsLegal([id('lord'), id('hero')], cardOf)).toBe(false)
  })
})
