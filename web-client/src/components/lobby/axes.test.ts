/**
 * Deck legality is filtered by table — but as a *consequence* of the Rules × Table rule, never as a
 * second copy of it.
 *
 * The distinction is the whole point of the Rules axis, and it is easy to get backwards. Commander
 * deck legality is not an independent property that merely pairs badly with Two-Headed Giant: it is
 * *defined relative to a commander*. CR 903.4 makes every card's colour identity a subset of the
 * commander's, so with no commander there is no anchor — and `DeckValidator` proves it, passing
 * `commanderAware = false` on the legacy entry point and silently dropping both MISSING_COMMANDER
 * and the identity check. "Commander legality, Standard rules" is therefore not Commander deck
 * construction under house rules; it is 100 singleton cards with the format's defining rule switched
 * off and a commander the server discards.
 *
 * So commander legality implies Commander rules, a 2HG table can't have those, and it can't offer
 * that legality either. What this file pins is that the implication is *derived* from
 * `rulesTableBlock` rather than restated — the failure mode being two rules that agree today and
 * drift tomorrow, which is exactly how the bug in #1552 came about.
 */
import { describe, expect, it } from 'vitest'
import {
  LEGALITY_OPTIONS,
  TABLE_VALUES,
  isCommanderDeckLegality,
  legalityOptionsForTable,
  rulesForLegality,
  rulesTableBlock,
} from './axes'

describe('deck legality options', () => {
  it('name the commander-shaped formats, which imply Commander rules', () => {
    expect(isCommanderDeckLegality('COMMANDER')).toBe(true)
    expect(isCommanderDeckLegality('BRAWL')).toBe(true)
    expect(isCommanderDeckLegality('STANDARD_BRAWL')).toBe(true)
    expect(isCommanderDeckLegality('STANDARD')).toBe(false)
    expect(isCommanderDeckLegality(null)).toBe(false)

    expect(rulesForLegality('COMMANDER')).toBe('COMMANDER')
    expect(rulesForLegality('BRAWL')).toBe('COMMANDER')
    expect(rulesForLegality('STANDARD')).toBe('STANDARD')
    expect(rulesForLegality(null)).toBe('STANDARD')
  })

  it('are offered in full at every table that can host the rules they imply', () => {
    for (const table of TABLE_VALUES) {
      if (table === 'TWO_HEADED_GIANT') continue
      expect(legalityOptionsForTable(table), table).toEqual(LEGALITY_OPTIONS)
    }
  })

  it('drop exactly the commander formats at a Two-Headed Giant table', () => {
    const values = legalityOptionsForTable('TWO_HEADED_GIANT').map((o) => o.value)

    expect(values).not.toContain('COMMANDER')
    expect(values).not.toContain('BRAWL')
    expect(values).not.toContain('STANDARD_BRAWL')
    expect(values).toContain('STANDARD')
    expect(values).toContain('MODERN')
  })

  it('agree with the Rules × Table rule at every table, because they are derived from it', () => {
    // The assertion that matters: no option survives whose implied rules that table would refuse,
    // and none is dropped whose rules it would accept. Restating the rule instead of deriving it is
    // what this catches — a hand-written filter passes the two cases above and fails this one as
    // soon as either side changes.
    for (const table of TABLE_VALUES) {
      const offered = legalityOptionsForTable(table)
      for (const option of LEGALITY_OPTIONS) {
        const blocked = rulesTableBlock(rulesForLegality(option.value), table) !== null
        expect(offered.includes(option), `${option.value} @ ${table}`).toBe(!blocked)
      }
    }
  })
})
