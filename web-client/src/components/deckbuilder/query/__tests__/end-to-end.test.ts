import { describe, expect, it } from 'vitest'
import { parseQuery } from '../index'
import { CARDS, SPECTACULAR_SPIDER_MAN, names } from './fixtures'

function run(q: string): string[] {
  const { predicate, errors } = parseQuery(q)
  void errors
  return names(CARDS.filter(predicate))
}

function errs(q: string): string[] {
  return parseQuery(q).errors.map((e) => e.message)
}

describe('end-to-end search', () => {
  it('empty query matches every card', () => {
    expect(run('').length).toBe(CARDS.length)
  })

  it('bareword does substring name match', () => {
    expect(run('lightning')).toEqual(['Lightning Bolt'])
  })

  it('!"name" exact match is case-insensitive', () => {
    expect(run('!"lightning bolt"')).toEqual(['Lightning Bolt'])
    expect(run('!Lightning')).toEqual([])
  })

  it('regex name match', () => {
    expect(run('name:/^bolt$/i')).toEqual([])
    expect(run('name:/llano/')).toEqual(['Llanowar Elves'])
  })

  it('type matches union of cardTypes/supertypes/subtypes', () => {
    expect(run('t:elf')).toEqual(['Llanowar Elves'])
  })

  it('type AND-words across the type line (quoted multi-word value)', () => {
    expect(run('t:"legendary creature"')).toEqual(['Niv-Mizzet, Parun'])
  })

  it('cmc comparison', () => {
    expect(run('cmc:1')).toEqual(['Lightning Bolt', 'Llanowar Elves'])
    expect(run('cmc<=2 t:creature')).toEqual(['Llanowar Elves', 'Tarmogoyf'])
    expect(run('cmc>=5')).toEqual(['Niv-Mizzet, Parun', 'Serra Angel'])
  })

  it('color identity contains', () => {
    expect(run('c:r').length).toBeGreaterThan(0)
    expect(run('c:r')).toContain('Lightning Bolt')
    expect(run('c:r')).toContain('Niv-Mizzet, Parun')
  })

  it('color identity exact', () => {
    expect(run('c=ur')).toEqual(['Niv-Mizzet, Parun'])
  })

  it('color identity by guild name', () => {
    // Izzet = U + R
    expect(run('c=izzet')).toEqual(['Niv-Mizzet, Parun'])
  })

  it('color count comparator', () => {
    expect(run('c>=2')).toEqual(['Niv-Mizzet, Parun'])
    expect(run('c:colorless')).toEqual(['Forest'].length === 0 ? [] : []) // Forest has GREEN identity
    // Forest is colorless by printed cost but green by identity — its color
    // identity is non-empty, so c:colorless excludes it.
    expect(run('c:colorless')).toEqual([])
  })

  it('cost: queries printed mana-cost colors', () => {
    // Forest's printed cost is empty — colorless by cost.
    expect(run('cost:colorless')).toContain('Forest')
  })

  it('mana cost multiset compare', () => {
    expect(run('mana:{u}{u}')).toEqual(['Counterspell'])
    expect(run('mana>={u}{u}{u}')).toEqual(['Niv-Mizzet, Parun'])
    expect(run('mana:uu')).toEqual(['Counterspell'])
  })

  it('cross-field numeric (pow vs tou)', () => {
    expect(run('pow>=tou t:creature')).toContain('Niv-Mizzet, Parun')
  })

  it('rarity', () => {
    expect(run('r:mythic')).toEqual(['Tarmogoyf'])
  })

  it('set code', () => {
    expect(run('s:grn')).toEqual(['Niv-Mizzet, Parun'])
  })

  it('s: matches reprints, not just the canonical printing', () => {
    // Banishing Light's canonical setCode is BLB; it's reprinted in EOE. Both
    // should match, and an unrelated set must not.
    expect(run('s:eoe')).toContain('Banishing Light')
    expect(run('s:blb')).toContain('Banishing Light')
    expect(run('s:eoe')).not.toContain('Lightning Bolt')
  })

  it('name matching also checks printed names', () => {
    const withSpidey = [...CARDS, SPECTACULAR_SPIDER_MAN]
    const runAll = (q: string) => names(withSpidey.filter(parseQuery(q).predicate))
    expect(runAll('ademi')).toEqual(['Spectacular Spider-Man'])
    expect(runAll('name:silkchutes')).toEqual(['Spectacular Spider-Man'])
    expect(runAll('!"Ademi of the Silkchutes"')).toEqual(['Spectacular Spider-Man'])
    expect(runAll('name:/^ademi/')).toEqual(['Spectacular Spider-Man'])
  })

  it('printedNameMatcher picks the printing only when the printed name is what matched', async () => {
    const { printedNameMatcher, parseQuery } = await import('../index')
    const spidey = SPECTACULAR_SPIDER_MAN
    expect(printedNameMatcher(parseQuery('ademi').ast)?.(spidey)?.setCode).toBe('OM1')
    expect(printedNameMatcher(parseQuery('ademi t:creature').ast)?.(spidey)?.setCode).toBe('OM1')
    expect(printedNameMatcher(parseQuery('spectacular').ast)?.(spidey)).toBeNull()
    expect(printedNameMatcher(parseQuery('t:creature').ast)).toBeNull()
    expect(printedNameMatcher(parseQuery('ademi or t:instant').ast)).toBeNull()
    // A pasted decklist is an `or` of exact names — each card still picks its printing.
    expect(printedNameMatcher(parseQuery('!"Cut Down" or !"Ademi of the Silkchutes"').ast)?.(spidey)?.setCode).toBe('OM1')
    expect(printedNameMatcher(parseQuery('!"Cut Down" or !"Spectacular Spider-Man"').ast)?.(spidey)).toBeNull()
  })

  it('s:EOE Banishing Light combines reprint set with name match', () => {
    expect(run('s:eoe banishing')).toEqual(['Banishing Light'])
  })

  it('extractSetFilter returns the dominant set code from a top-level AND query', async () => {
    const { extractSetFilter, parseQuery } = await import('../index')
    expect(extractSetFilter(parseQuery('s:eoe banishing').ast)).toBe('EOE')
    expect(extractSetFilter(parseQuery('set:GRN t:creature').ast)).toBe('GRN')
    expect(extractSetFilter(parseQuery('').ast)).toBeNull()
    expect(extractSetFilter(parseQuery('lightning').ast)).toBeNull()
    // OR/NOT branches break the "every result is from this set" invariant — bail out.
    expect(extractSetFilter(parseQuery('s:eoe or t:creature').ast)).toBeNull()
  })

  it('format legality', () => {
    expect(run('f:commander')).toContain('Forest')
    expect(run('f:commander')).toContain('Niv-Mizzet, Parun')
  })

  it('keyword matching', () => {
    expect(run('kw:flying')).toEqual(['Niv-Mizzet, Parun', 'Serra Angel'])
    expect(run('is:flying')).toEqual(['Niv-Mizzet, Parun', 'Serra Angel'])
  })

  it('is:legendary supertype shortcut', () => {
    expect(run('is:legendary')).toEqual(['Niv-Mizzet, Parun'])
  })

  it('is:basic / is:land', () => {
    expect(run('is:basic')).toEqual(['Forest'])
    expect(run('is:land')).toEqual(['Forest'])
  })

  it('is:vanilla excludes cards with oracle text', () => {
    expect(run('is:vanilla')).toEqual([])
  })

  it('layout:transform', () => {
    expect(run('layout:transform')).toEqual([])
  })

  it('negation via leading dash', () => {
    expect(run('t:creature -c:r')).toEqual(['Llanowar Elves', 'Serra Angel', 'Tarmogoyf'])
  })

  it('negation via `not` keyword', () => {
    expect(run('t:creature not c:r')).toEqual(['Llanowar Elves', 'Serra Angel', 'Tarmogoyf'])
  })

  it('OR alternation', () => {
    expect(run('t:instant or is:legendary'))
      .toEqual(['Counterspell', 'Lightning Bolt', 'Niv-Mizzet, Parun'])
  })

  it('parens override default precedence', () => {
    // Without parens: t:creature (AND) c:u OR c:r → matches red creatures and any blue card.
    // With parens: (c:u OR c:r) AND t:creature → only blue/red creatures.
    expect(run('(c:u or c:r) t:creature')).toEqual(['Niv-Mizzet, Parun'])
  })

  it('unknown filter produces an error with a suggestion', () => {
    const messages = errs('artist:foo')
    expect(messages.some((m) => m.includes('Unknown filter'))).toBe(true)
  })

  it('typo on a known key gets a "did you mean" suggestion', () => {
    const { errors } = parseQuery('clor:r')
    expect(errors[0]?.suggestion).toBe('Did you mean "color:"?')
  })

  it('invalid color value reports error and matches nothing', () => {
    expect(run('c:k')).toEqual([])
    const messages = errs('c:k')
    expect(messages.some((m) => m.includes('Unknown color'))).toBe(true)
  })

  it('non-numeric for cmc reports error', () => {
    const messages = errs('cmc:abc')
    expect(messages.length).toBeGreaterThan(0)
  })

  it('unsupported operator on a key', () => {
    const messages = errs('name<=foo')
    expect(messages.some((m) => m.includes('Operator'))).toBe(true)
  })
})
