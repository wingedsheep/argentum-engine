import { describe, expect, it } from 'vitest'
import {
  canonicalNameIn,
  designatedCommanders,
  inferCommanders,
  parseArenaDeckList,
  type ParseResult,
} from './parseArenaDeck'
import type { CommanderCard } from './commanderPairing'

const names = (list: readonly { name: string }[]) => list.map((e) => e.name)
const total = (list: readonly { count: number }[]) => list.reduce((a, e) => a + e.count, 0)

// A small commander-pairing catalogue for the inference tests.
const legend = (extra: Partial<CommanderCard> = {}): CommanderCard => ({
  supertypes: ['LEGENDARY'], cardTypes: ['CREATURE'], subtypes: [], canBeCommander: true, ...extra,
})
const CATALOG: Record<string, CommanderCard> = {
  'kodama of the east tree': legend({ partnerAbilities: ['Partner'] }),
  'ishai, ojutai dragonspeaker': legend({ partnerAbilities: ['Partner'] }),
  "atraxa, praetors' voice": legend(),
  'lurrus of the dream-den': legend(),
  'wilson, refined grizzly': legend({ partnerAbilities: ['Choose a Background'] }),
  'raised by giants': { supertypes: ['LEGENDARY'], cardTypes: ['ENCHANTMENT'], subtypes: ['Background'], canBeCommander: false },
  'thalia, guardian of thraben': legend(),
  'sol ring': { supertypes: [], cardTypes: ['ARTIFACT'], subtypes: [], canBeCommander: false },
  counterspell: { supertypes: [], cardTypes: ['INSTANT'], subtypes: [], canBeCommander: false },
}
const lookup = (name: string) => CATALOG[name.toLowerCase()]
const infer = (parsed: ParseResult, commanderFormat = false) => inferCommanders(parsed, { commanderFormat, lookup })

/** `n` filler lines of distinct one-ofs, so a sample reaches a real deck size. */
const filler = (n: number, prefix = 'Filler') => Array.from({ length: n }, (_, i) => `1 ${prefix} ${i + 1}`)

describe('Archidekt exports', () => {
  // Archidekt's text export: `1x Name (set) number`, foil flag, `[Category{flags},…]` tags and
  // `^Label,#colour^` labels.
  const archidekt = [
    "1x Atraxa, Praetors' Voice (2x2) 190 [Commander{top}]",
    '1x Sol Ring (cmm) 410 *F* [Ramp]',
    '1x Arcane Signet (cmm) 367 [Ramp,Mana Rock]',
    '1x Doubling Season (2x2) 145 [Maybeboard{noDeck}{noPrice},Proliferate]',
    '1x Swords to Plowshares (cmm) 50 [Sideboard{noDeck}{noPrice}]',
    '1x Command Tower (cmm) 1018 [Land] ^Have,#37d67a^',
    '36x Forest (cmm) 1033 [Land]',
  ].join('\n')

  it('strips category tags and labels, keeping set and collector number', () => {
    const parsed = parseArenaDeckList(archidekt)
    expect(parsed.errors).toEqual([])
    expect(parsed.entries).toEqual([
      expect.objectContaining({ count: 1, name: 'Sol Ring', setCode: 'CMM', collectorNumber: '410' }),
      expect.objectContaining({ count: 1, name: 'Arcane Signet', setCode: 'CMM', collectorNumber: '367' }),
      expect.objectContaining({ count: 1, name: 'Command Tower', setCode: 'CMM', collectorNumber: '1018' }),
      expect.objectContaining({ count: 36, name: 'Forest' }),
    ])
  })

  it('designates the Commander category and keeps {noDeck} categories out of the deck', () => {
    const parsed = parseArenaDeckList(archidekt)
    expect(designatedCommanders(parsed.commander)).toEqual(["Atraxa, Praetors' Voice"])
    expect(names(parsed.excluded)).toEqual(['Doubling Season'])
    expect(names(parsed.sideboard)).toEqual(['Swords to Plowshares'])
  })

  it('reads a partner pair as two Commander-category cards', () => {
    const parsed = parseArenaDeckList([
      '1x Kodama of the East Tree (c16) 40 [Commander{top}]',
      '1x Ishai, Ojutai Dragonspeaker (c16) 41 [Commander{top}]',
      '1x Sol Ring [Ramp]',
    ].join('\n'))
    expect(designatedCommanders(parsed.commander)).toEqual(['Kodama of the East Tree', 'Ishai, Ojutai Dragonspeaker'])
    expect(names(parsed.entries)).toEqual(['Sol Ring'])
  })
})

describe('Moxfield / MTGO exports with an implicit commander', () => {
  // MTGO's text export (and Moxfield's "MTGO" export): the 98 + the commanders under SIDEBOARD:.
  const mtgo = [
    '1 Sol Ring',
    '1 Counterspell',
    ...filler(96),
    '',
    'SIDEBOARD:',
    '1 Kodama of the East Tree',
    '1 Ishai, Ojutai Dragonspeaker',
  ].join('\n')

  it('takes a legal partner pair on the sideboard of a 100-card list as the commanders', () => {
    const { parsed, inferredFrom } = infer(parseArenaDeckList(mtgo))
    expect(inferredFrom).toBe('sideboard')
    expect(designatedCommanders(parsed.commander)).toEqual(['Kodama of the East Tree', 'Ishai, Ojutai Dragonspeaker'])
    expect(parsed.sideboard).toEqual([])
    expect(total(parsed.entries) + total(parsed.commander)).toBe(100)
  })

  it('takes a single commander from the sideboard when the format is Commander, whatever the count', () => {
    const text = ['1 Sol Ring', ...filler(50), '', 'SIDEBOARD:', "1 Atraxa, Praetors' Voice"].join('\n')
    expect(infer(parseArenaDeckList(text)).inferredFrom).toBeNull()
    const { parsed, inferredFrom } = infer(parseArenaDeckList(text), true)
    expect(inferredFrom).toBe('sideboard')
    expect(names(parsed.commander)).toEqual(["Atraxa, Praetors' Voice"])
  })

  it('puts the lead commander first for a Background pair', () => {
    const text = [...filler(98), '', 'SIDEBOARD:', '1 Raised by Giants', '1 Wilson, Refined Grizzly'].join('\n')
    const { parsed } = infer(parseArenaDeckList(text))
    expect(names(parsed.commander)).toEqual(['Wilson, Refined Grizzly', 'Raised by Giants'])
  })

  it("doesn't pair two legends that can't partner", () => {
    const text = [...filler(98), '', 'SIDEBOARD:', "1 Atraxa, Praetors' Voice", '1 Thalia, Guardian of Thraben'].join('\n')
    const { parsed, inferredFrom } = infer(parseArenaDeckList(text))
    expect(inferredFrom).toBeNull()
    expect(parsed.commander).toEqual([])
    expect(parsed.sideboard).toHaveLength(2)
  })

  it('takes the trailing block of a plain-text export as the commander', () => {
    // Moxfield's plain-text export: the 99, a blank line, then the commander.
    const text = ['1 Sol Ring', ...filler(98), '', "1 Atraxa, Praetors' Voice"].join('\n')
    const { parsed, inferredFrom } = infer(parseArenaDeckList(text))
    expect(inferredFrom).toBe('trailing')
    expect(names(parsed.commander)).toEqual(["Atraxa, Praetors' Voice"])
    expect(names(parsed.entries)).not.toContain("Atraxa, Praetors' Voice")
    expect(total(parsed.entries)).toBe(99)
  })

  it('leaves a 60-card deck with a real 15-card sideboard alone', () => {
    const text = [
      '4 Thalia, Guardian of Thraben',
      '4 Counterspell',
      ...filler(52),
      '',
      'SIDEBOARD:',
      ...filler(15, 'Side'),
    ].join('\n')
    const parsed = parseArenaDeckList(text)
    const result = infer(parsed)
    expect(result.inferredFrom).toBeNull()
    expect(result.parsed).toBe(parsed)
    expect(total(parsed.sideboard)).toBe(15)
  })

  it('leaves a 60-card deck whose small sideboard is a lone legend alone', () => {
    const text = [...filler(60), '', 'SIDEBOARD:', '1 Thalia, Guardian of Thraben'].join('\n')
    expect(infer(parseArenaDeckList(text)).inferredFrom).toBeNull()
  })

  it('never overrides an explicit Commander section', () => {
    const text = ['Commander', "1 Atraxa, Praetors' Voice", '', 'Deck', ...filler(97), '', 'Sideboard', '1 Thalia, Guardian of Thraben', '1 Sol Ring'].join('\n')
    const { parsed, inferredFrom } = infer(parseArenaDeckList(text), true)
    expect(inferredFrom).toBeNull()
    expect(names(parsed.commander)).toEqual(["Atraxa, Praetors' Voice"])
  })

  it('reads a companion on the sideboard as nothing more than that', () => {
    const text = ['Companion', '1 Lurrus of the Dream-Den', '', 'Deck', ...filler(99)].join('\n')
    const { parsed, inferredFrom } = infer(parseArenaDeckList(text), true)
    expect(inferredFrom).toBeNull()
    expect(names(parsed.companion)).toEqual(['Lurrus of the Dream-Den'])
  })
})

describe('other deck-site shapes', () => {
  it('keeps an Arena Companion section instead of dropping it', () => {
    const parsed = parseArenaDeckList([
      'Companion',
      '1 Lurrus of the Dream-Den (IKO) 226',
      '',
      'Deck',
      '4 Thalia, Guardian of Thraben (DKA) 24',
      '',
      'Sideboard',
      '2 Counterspell',
    ].join('\n'))
    expect(parsed.errors).toEqual([])
    expect(parsed.companion).toEqual([expect.objectContaining({ name: 'Lurrus of the Dream-Den', setCode: 'IKO' })])
    expect(names(parsed.entries)).toEqual(['Thalia, Guardian of Thraben'])
    expect(names(parsed.sideboard)).toEqual(['Counterspell'])
  })

  it('reads TappedOut *CMDR* markers', () => {
    const parsed = parseArenaDeckList([
      "1x Atraxa, Praetors' Voice *CMDR*",
      '1x Sol Ring',
      '1x Counterspell *F*',
    ].join('\n'))
    expect(parsed.errors).toEqual([])
    expect(names(parsed.commander)).toEqual(["Atraxa, Praetors' Voice"])
    expect(names(parsed.entries)).toEqual(['Sol Ring', 'Counterspell'])
  })

  it('reads deckstats // headers, set prefixes and # !Commander markers', () => {
    const parsed = parseArenaDeckList([
      '//Main',
      '1 [CMM#410] Sol Ring',
      '1 [2X2] Counterspell',
      '//Lands',
      '36 Forest',
      '',
      '//Maybeboard',
      '1 [2X2#145] Doubling Season',
      '',
      '//Commander',
      "1 [2X2#190] Atraxa, Praetors' Voice # !Commander",
    ].join('\n'))
    expect(parsed.errors).toEqual([])
    expect(names(parsed.entries)).toEqual(['Sol Ring', 'Counterspell', 'Forest'])
    expect(names(parsed.excluded)).toEqual(['Doubling Season'])
    expect(names(parsed.commander)).toEqual(["Atraxa, Praetors' Voice"])
  })

  it('reads a deckstats commander marker outside the Commander section', () => {
    const parsed = parseArenaDeckList(["1 Atraxa, Praetors' Voice # !Commander", '1 Sol Ring'].join('\n'))
    expect(names(parsed.commander)).toEqual(["Atraxa, Praetors' Voice"])
  })

  it('accepts headers with a colon or a card count', () => {
    const parsed = parseArenaDeckList([
      'Commander (1)',
      "1 Atraxa, Praetors' Voice",
      'Mainboard (2):',
      '1 Sol Ring',
      '1 Counterspell',
      'SIDEBOARD:',
      '1 Thalia, Guardian of Thraben',
    ].join('\n'))
    expect(parsed.errors).toEqual([])
    expect(names(parsed.commander)).toEqual(["Atraxa, Praetors' Voice"])
    expect(names(parsed.entries)).toEqual(['Sol Ring', 'Counterspell'])
    expect(names(parsed.sideboard)).toEqual(['Thalia, Guardian of Thraben'])
  })

  it('reports lines it cannot read instead of dropping them', () => {
    const parsed = parseArenaDeckList(['Creatures (2)', '2 Thalia, Guardian of Thraben', 'Sol Ring', '0 Counterspell'].join('\n'))
    expect(parsed.errors.map((e) => [e.line, e.reason])).toEqual([
      [1, 'unrecognised line format'],
      [3, 'unrecognised line format'],
      [4, 'invalid card count'],
    ])
    expect(names(parsed.entries)).toEqual(['Thalia, Guardian of Thraben'])
  })

  it("doesn't call a grouped list's last block a trailing block when it has a header", () => {
    const parsed = parseArenaDeckList(['4 Sol Ring', '', 'Deck', "1 Atraxa, Praetors' Voice"].join('\n'))
    expect(parsed.trailingBlock).toEqual([])
  })
})

describe('importing a Commander section', () => {
  it('designates both commanders of a partner pair', () => {
    const parsed = parseArenaDeckList([
      'Commander',
      '1 Kodama of the East Tree (C16) 40',
      '1 Ishai, Ojutai Dragonspeaker',
      '',
      'Deck',
      '1 Sol Ring',
    ].join('\n'))
    expect(parsed.errors).toEqual([])
    expect(parsed.entries.map((e) => e.name)).toEqual(['Sol Ring'])
    expect(designatedCommanders(parsed.commander)).toEqual([
      'Kodama of the East Tree',
      'Ishai, Ojutai Dragonspeaker',
    ])
  })

  it('designates at most two distinct commanders; the rest stay deck cards', () => {
    const parsed = parseArenaDeckList(['Commander', '1 A', '1 A', '1 B', '1 C'].join('\n'))
    expect(designatedCommanders(parsed.commander)).toEqual(['A', 'B'])
  })

  it('uses the catalogue casing for a name it knows', () => {
    const parsed = parseArenaDeckList(['Commander', '1 kodama of the east tree', '1 Unknown Legend'].join('\n'))
    const canonical = canonicalNameIn([{ name: 'Kodama of the East Tree' }])
    expect(designatedCommanders(parsed.commander, canonical)).toEqual(['Kodama of the East Tree', 'Unknown Legend'])
  })

  it('designates nothing without a Commander section', () => {
    expect(designatedCommanders(parseArenaDeckList('4 Lightning Bolt').commander)).toEqual([])
  })
})
