/**
 * Pins the `deck-import-export` help topic to the parser.
 *
 * The topic spells out every accepted line shape and section header, because there is no vendor spec
 * to link at — this parser *is* the spec. That only stays true if the two are checked against each
 * other, so every shape the topic promises is exercised here.
 */
import { describe, expect, it } from 'vitest'
import { parseArenaDeckList } from '../parseArenaDeck'

describe('help topic deck-import-export claims', () => {
  it('plain, "x", Arena and Moxfield bulk-edit line shapes', () => {
    const r = parseArenaDeckList([
      '4 Lightning Bolt',
      '4x Counterspell',
      '1 Sol Ring (LEA) 161',
      '1 Lightning Bolt (LEA) *F* *A* 42 #tag',
    ].join('\n'))
    expect(r.errors).toEqual([])
    expect(r.entries.map((e) => [e.count, e.name])).toEqual([
      [4, 'Lightning Bolt'], [4, 'Counterspell'], [1, 'Sol Ring'], [1, 'Lightning Bolt'],
    ])
    expect(r.entries[2]).toMatchObject({ setCode: 'LEA', collectorNumber: '161' })
  })

  it('SB: prefix and the section headers the topic names', () => {
    const r = parseArenaDeckList([
      'Deck', '4 Lightning Bolt',
      'SB: 2 Counterspell',
      'Commander', '1 Sol Ring',
      'Sideboard', '1 Counterspell',
    ].join('\n'))
    expect(r.errors).toEqual([])
    expect(r.entries.map((e) => e.name)).toEqual(['Lightning Bolt'])
    expect(r.sideboard.map((e) => [e.count, e.name])).toEqual([[2, 'Counterspell'], [1, 'Counterspell']])
    expect(r.commander.map((e) => e.name)).toEqual(['Sol Ring'])
  })

  it('the other header spellings the topic names are all recognised', () => {
    for (const header of ['Mainboard', 'Main Deck', 'Maindeck', 'MAINBOARD']) {
      const r = parseArenaDeckList(`${header}\n4 Lightning Bolt`)
      expect({ header, errors: r.errors, names: r.entries.map((e) => e.name) })
        .toEqual({ header, errors: [], names: ['Lightning Bolt'] })
    }
    for (const header of ['Side', 'SB']) {
      const r = parseArenaDeckList(`4 Lightning Bolt\n${header}\n1 Counterspell`)
      expect({ header, side: r.sideboard.map((e) => e.name) }).toEqual({ header, side: ['Counterspell'] })
    }
    // Companion and About are skipped, not imported.
    const skipped = parseArenaDeckList('4 Lightning Bolt\nCompanion\n1 Sol Ring\nAbout\nName Thing')
    expect(skipped.entries.map((e) => e.name)).toEqual(['Lightning Bolt'])
  })

  it('ignores blank lines and // or # comments', () => {
    const r = parseArenaDeckList(['// a comment', '', '# another', '4 Lightning Bolt'].join('\n'))
    expect(r.errors).toEqual([])
    expect(r.entries.map((e) => e.name)).toEqual(['Lightning Bolt'])
  })

  it('reports a malformed card line instead of dropping it', () => {
    const r = parseArenaDeckList('this is not a card line at all!!')
    expect(r.errors.length).toBeGreaterThan(0)
    expect(r.errors[0]).toMatchObject({ line: 1 })
  })
})
