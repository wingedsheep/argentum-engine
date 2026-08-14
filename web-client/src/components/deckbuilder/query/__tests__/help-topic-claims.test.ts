/**
 * Pins the `search-syntax` help topic to the parser.
 *
 * The topic lists operators by example, and a help page that documents a filter the parser rejects
 * is worse than one that says nothing. Two of these examples were wrong when the topic was written
 * (`banned:` isn't a filter, `is:split` reports a modelling gap) and only this caught it. Change the
 * topic's examples, change this list.
 */
import { describe, expect, it } from 'vitest'
import { parseQuery } from '../index'

const CLAIMED = [
  't:creature', 'c<=rw', 'cmc>=4', 'o:flying', 'f:standard', 'is:legendary',
  'c:rg', 'c:azorius', 'c:colorless', 'id:r',
  't:goblin', 't:legendary', 'kw:trample',
  'mv>=4', 'm:{2/G}',
  'pow>=4', 'tou<2', 'loy:3',
  's:fdn', 'r:mythic',
  'f:commander', 'f:pauper',
  'is:permanent', 'is:multicolor', 'is:vanilla', 'is:dfc',
  'lightning bolt', '-t:creature', 't:goblin or t:elf', '(t:goblin or t:elf) t:creature',
  '"lightning bolt"',
]

describe('help topic search-syntax claims', () => {
  for (const q of CLAIMED) {
    it(`parses ${q}`, () => {
      expect({ q, errors: parseQuery(q).errors.map((e) => e.message) }).toEqual({ q, errors: [] })
    })
  }
  it('rejects an unimplemented Scryfall key', () => {
    expect(parseQuery('artist:rk-post').errors.map((e) => e.message).join(' ')).toMatch(/Unknown filter/)
  })
  it('is:split reports the modelling gap the topic quotes', () => {
    expect(parseQuery('is:split').errors.map((e) => e.message).join(' '))
      .toContain('split-card layout not modelled')
  })
})
