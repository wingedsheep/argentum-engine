import { describe, it, expect } from 'vitest'
import { displayableKeywords, keywordManaClass, keywordSvgIcon } from './index'

/**
 * Landwalk decides whether an attacker can be blocked at all, and cards both grant it and take it
 * away (The Dark's Scarwood Hag does both halves), so it has to render as an on-card badge —
 * without one, "gains forestwalk" and "loses forestwalk" look identical to the player.
 */
const LANDWALK = [
  'SWAMPWALK',
  'FORESTWALK',
  'ISLANDWALK',
  'MOUNTAINWALK',
  'PLAINSWALK',
  'DESERTWALK',
  'NONBASIC_LANDWALK',
]

describe('keyword icons', () => {
  it('renders a badge for every landwalk keyword', () => {
    for (const keyword of LANDWALK) {
      expect(displayableKeywords.has(keyword), `${keyword} must be displayable`).toBe(true)
    }
  })

  it('gives every displayable keyword a glyph rather than the generic fallback', () => {
    for (const keyword of displayableKeywords) {
      const hasGlyph = keyword in keywordManaClass || keyword in keywordSvgIcon
      expect(hasGlyph, `${keyword} has no icon mapping`).toBe(true)
    }
  })
})
