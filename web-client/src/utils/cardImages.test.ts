import { describe, expect, it } from 'vitest'
import { isBattleTypeLine, landscapeImageRotateDeg } from './cardImages'

describe('isBattleTypeLine', () => {
  it('matches the Battle card type', () => {
    expect(isBattleTypeLine('Battle — Siege')).toBe(true)
  })

  it('ignores everything after the em dash, so a subtype can never match', () => {
    // Contrived, but the point is that only the types half is examined.
    expect(isBattleTypeLine('Creature — Battle Angel')).toBe(false)
  })

  it('does not match a card whose name merely contains the word', () => {
    // Type lines carry types, not names — but the word-boundary check is what makes
    // "Battlefield"-ish types safe too.
    expect(isBattleTypeLine('Sorcery')).toBe(false)
    expect(isBattleTypeLine('Enchantment — Battlefield Forge')).toBe(false)
  })

  it('treats a missing type line as not a battle', () => {
    expect(isBattleTypeLine(null)).toBe(false)
    expect(isBattleTypeLine(undefined)).toBe(false)
    expect(isBattleTypeLine('')).toBe(false)
  })
})

describe('landscapeImageRotateDeg', () => {
  it('prefers the server flag over any local derivation', () => {
    // The flag is computed from CardDefinition.isLandscapePrint, the one place that decides what
    // "printed sideways" means. It wins even when the layout would say otherwise.
    expect(landscapeImageRotateDeg({ isLandscape: true, layout: 'TRANSFORM' })).toBe(90)
    expect(landscapeImageRotateDeg({ isLandscape: false, layout: 'SPLIT' })).toBe(0)
  })

  it('falls back to layout for card shapes that predate the flag', () => {
    expect(landscapeImageRotateDeg({ layout: 'SPLIT', typeLine: 'Enchantment — Room' })).toBe(90)
  })

  it('falls back to the type line for battles, whose layout is TRANSFORM rather than SPLIT', () => {
    // The regression this guards: keying only on `layout === SPLIT` left every battle upright,
    // rendering it sideways in the draft / sealed / deckbuilder / cube hover previews.
    expect(landscapeImageRotateDeg({ layout: 'TRANSFORM', typeLine: 'Battle — Siege' })).toBe(90)
  })

  it('leaves ordinary portrait cards alone, including other transforming DFCs', () => {
    expect(landscapeImageRotateDeg({ layout: 'NORMAL', typeLine: 'Creature — Human Wizard' })).toBe(0)
    expect(landscapeImageRotateDeg({ layout: 'TRANSFORM', typeLine: 'Creature — Human Cleric' })).toBe(0)
    expect(landscapeImageRotateDeg(null)).toBe(0)
    expect(landscapeImageRotateDeg(undefined)).toBe(0)
  })
})
