import { describe, it, expect } from 'vitest'
import {
  encodeSharedDeck,
  decodeSharedDeck,
  buildShareUrl,
  SHARE_PARAM,
  type SharedDeck,
} from './shareDeck'
import type { PrintingRef } from '@/types'

// A tiny stand-in catalog: card name <-> default printing. The codec is catalog-agnostic and
// takes resolvers, so the tests supply this pair (mirroring what the deckbuilder builds from
// `/api/cards`). Round-tripping through it proves the printing-keyed compact path works.
const CATALOG: Record<string, PrintingRef> = {
  'Lightning Bolt': { setCode: 'M10', collectorNumber: '146' },
  Mountain: { setCode: 'M10', collectorNumber: '244' },
  'Goblin Guide': { setCode: 'ZEN', collectorNumber: '127' },
  'Sol Ring': { setCode: 'C16', collectorNumber: '230' },
  'Arcane Signet': { setCode: 'ELD', collectorNumber: '331' },
  Forest: { setCode: 'M10', collectorNumber: '248' },
  Island: { setCode: 'M10', collectorNumber: '235' },
  'Atraxa, Praetors’ Voice': { setCode: 'C16', collectorNumber: '28' },
  'Unholy Annex // Ritual Chamber': { setCode: 'DSK', collectorNumber: '95' },
  'Lim-Dûl’s Vault': { setCode: 'ALL', collectorNumber: '12a' }, // non-integer collector
  Plains: { setCode: 'BLB', collectorNumber: '270' },
}

const resolvePrinting = (name: string): PrintingRef | null => CATALOG[name] ?? null
const byPrinting = new Map(
  Object.entries(CATALOG).map(([name, p]) => [`${p.setCode}:${p.collectorNumber}`, name]),
)
const resolveName = (p: PrintingRef): string | null =>
  byPrinting.get(`${p.setCode}:${p.collectorNumber}`) ?? null

const roundTrip = async (deck: SharedDeck): Promise<SharedDeck | null> =>
  decodeSharedDeck(await encodeSharedDeck(deck, resolvePrinting), resolveName)

describe('shareDeck codec', () => {
  it('round-trips a plain deck via printing lookup', async () => {
    const deck: SharedDeck = {
      name: 'Mono-Red Aggro',
      cards: { 'Lightning Bolt': 4, Mountain: 20, 'Goblin Guide': 4 },
    }
    expect(await roundTrip(deck)).toEqual(deck)
  })

  it('round-trips format, commander, and reprint pins', async () => {
    const deck: SharedDeck = {
      name: 'Atraxa Superfriends',
      cards: { 'Sol Ring': 1, 'Arcane Signet': 1, Forest: 10 },
      format: 'COMMANDER',
      commander: 'Atraxa, Praetors’ Voice',
      commanderPrinting: { setCode: 'C16', collectorNumber: '28' },
      // A pin to a *different* printing than the catalog default → carried by name in overflow.
      printings: { 'Sol Ring': { setCode: 'C21', collectorNumber: '263' } },
    }
    expect(await roundTrip(deck)).toEqual(deck)
  })

  it('carries the companion, which comes back on the sideboard (CR 702.139)', async () => {
    const decoded = await roundTrip({
      name: 'Odd Burn',
      cards: { 'Lightning Bolt': 4, Mountain: 20 },
      sideboard: { 'Obosh, the Preypiercer': 1, 'Goblin Guide': 2 },
      companion: 'Obosh, the Preypiercer',
    })
    expect(decoded?.companion).toBe('Obosh, the Preypiercer')
    // The rest of the sideboard isn't in the share code; the companion is.
    expect(decoded?.sideboard).toEqual({ 'Obosh, the Preypiercer': 1 })
  })

  it('preserves Unicode, split-card (DFC), and non-integer-collector names', async () => {
    const deck: SharedDeck = {
      name: 'Æther — déjà vu',
      cards: { 'Unholy Annex // Ritual Chamber': 2, 'Lim-Dûl’s Vault': 1 },
    }
    expect(await roundTrip(deck)).toEqual(deck)
  })

  it('carries the name when the catalog cannot resolve the card', async () => {
    // Not in CATALOG → no printing to key on, so the name rides along and still round-trips.
    const deck: SharedDeck = { name: 'Unknowns', cards: { 'Totally Made-Up Card': 3 } }
    expect(await roundTrip(deck)).toEqual(deck)
  })

  it('drops a card with zero copies', async () => {
    const code = await encodeSharedDeck(
      { name: 'x', cards: { 'Lightning Bolt': 4, Mountain: 0 } },
      resolvePrinting,
    )
    expect((await decodeSharedDeck(code, resolveName))?.cards).toEqual({ 'Lightning Bolt': 4 })
  })

  it('produces a URL-safe code (no +, /, = or whitespace)', async () => {
    const code = await encodeSharedDeck(
      { name: 'Test', cards: { 'Lightning Bolt': 4, Mountain: 20 } },
      resolvePrinting,
    )
    expect(code).toMatch(/^[A-Za-z0-9_-]+$/)
  })

  it('is far more compact than carrying names (large singleton deck)', async () => {
    // 60 distinct singletons, all at their catalog-default printing across two sets.
    const cards: Record<string, number> = {}
    const cat: Record<string, PrintingRef> = {}
    for (let i = 0; i < 60; i++) {
      const name = `Wandering Verdant Sentinel of the Wilds #${i}`
      cards[name] = 1
      cat[name] = { setCode: i % 2 ? 'BRO' : 'DMU', collectorNumber: String(i + 1) }
    }
    const resolve = (n: string) => cat[n] ?? null
    const code = await encodeSharedDeck({ name: 'Big Pile', format: 'COMMANDER', cards }, resolve)
    expect(code.length).toBeLessThan(2000)
    // Same deck carrying names (v1-style JSON) would be far larger; sanity-check we beat it.
    const namesPayload = JSON.stringify({ v: 1, n: 'Big Pile', d: Object.entries(cards) })
    expect(code.length).toBeLessThan(namesPayload.length / 2)
  })

  it('builds the share URL with the deckbuilder route and param', () => {
    expect(buildShareUrl('https://play.example.com', 'ABC123')).toBe(
      `https://play.example.com/deckbuilder?${SHARE_PARAM}=ABC123`,
    )
  })

  it('decodes a legacy v1 (JSON, name-keyed) share code', async () => {
    const code = await encodeAsCode({ v: 1, n: 'Legacy', d: [['Plains', 7]] })
    expect(await decodeSharedDeck(code, resolveName)).toEqual({ name: 'Legacy', cards: { Plains: 7 } })
  })

  it('decodes a legacy v1 code with printing pins', async () => {
    const code = await encodeAsCode({
      v: 1,
      n: 'Legacy Pins',
      f: 'COMMANDER',
      c: 'Atraxa, Praetors’ Voice',
      cp: ['C16', '28'],
      d: [['Sol Ring', 1, 'C21', '263']],
    })
    expect(await decodeSharedDeck(code, resolveName)).toEqual({
      name: 'Legacy Pins',
      cards: { 'Sol Ring': 1 },
      format: 'COMMANDER',
      commander: 'Atraxa, Praetors’ Voice',
      commanderPrinting: { setCode: 'C16', collectorNumber: '28' },
      printings: { 'Sol Ring': { setCode: 'C21', collectorNumber: '263' } },
    })
  })

  it('returns null for malformed / non-share input', async () => {
    expect(await decodeSharedDeck('', resolveName)).toBeNull()
    expect(await decodeSharedDeck('not-base64-!!!', resolveName)).toBeNull()
    // Valid base64url of JSON that isn't a v1 share payload.
    expect(await decodeSharedDeck(await encodeAsCode({ hello: 'world' }), resolveName)).toBeNull()
    // Right v1 version but no usable cards.
    expect(await decodeSharedDeck(await encodeAsCode({ v: 1, n: 'x', d: [] }), resolveName)).toBeNull()
  })

  it('drops compact entries that no longer resolve in the catalog', async () => {
    // Encode against a catalog that knows the card, decode against one that doesn't.
    const code = await encodeSharedDeck(
      { name: 'Stale', cards: { 'Goblin Guide': 4, 'Lightning Bolt': 2 } },
      resolvePrinting,
    )
    const partial = (p: PrintingRef): string | null =>
      p.setCode === 'M10' && p.collectorNumber === '146' ? 'Lightning Bolt' : null
    expect((await decodeSharedDeck(code, partial))?.cards).toEqual({ 'Lightning Bolt': 2 })
  })

  it('round-trips a partner pair, with and without printing pins', async () => {
    const pinned: SharedDeck = {
      name: 'Partners',
      cards: { 'Sol Ring': 1, Forest: 10 },
      format: 'COMMANDER',
      commander: 'Kodama of the East Tree',
      commanderPrinting: { setCode: 'C16', collectorNumber: '40' },
      secondCommander: 'Ishai, Ojutai Dragonspeaker',
      secondCommanderPrinting: { setCode: 'C16', collectorNumber: '41' },
    }
    expect(await roundTrip(pinned)).toEqual(pinned)
    const bare: SharedDeck = {
      name: 'Partners',
      cards: { Forest: 10 },
      commander: 'Kodama of the East Tree',
      secondCommander: 'Ishai, Ojutai Dragonspeaker',
    }
    expect(await roundTrip(bare)).toEqual(bare)
  })

  it('decodes a pre-partner v2 commander record unchanged', async () => {
    // The exact rec-1 shapes the single-commander encoder emitted: name, and name+printing.
    const pinned = await encodeAsV2(['2\x1fOld\x1fCOMMANDER', 'Atraxa, Praetors’ Voice\x1fC16\x1f28', '', 'Island\x1f2'])
    expect(await decodeSharedDeck(pinned, resolveName)).toEqual({
      name: 'Old',
      cards: { Island: 2 },
      format: 'COMMANDER',
      commander: 'Atraxa, Praetors’ Voice',
      commanderPrinting: { setCode: 'C16', collectorNumber: '28' },
    })
    const bare = await encodeAsV2(['2\x1fOld\x1f', 'Atraxa, Praetors’ Voice', '', 'Island\x1f2'])
    expect(await decodeSharedDeck(bare, resolveName)).toEqual({
      name: 'Old',
      cards: { Island: 2 },
      commander: 'Atraxa, Praetors’ Voice',
    })
  })

  it('decodes a legacy v1 code with a second commander', async () => {
    const code = await encodeAsCode({
      v: 1,
      n: 'Legacy Partners',
      c: 'Kodama of the East Tree',
      c2: 'Ishai, Ojutai Dragonspeaker',
      cp2: ['C16', '41'],
      d: [['Forest', 10]],
    })
    expect(await decodeSharedDeck(code, resolveName)).toEqual({
      name: 'Legacy Partners',
      cards: { Forest: 10 },
      commander: 'Kodama of the East Tree',
      secondCommander: 'Ishai, Ojutai Dragonspeaker',
      secondCommanderPrinting: { setCode: 'C16', collectorNumber: '41' },
    })
  })

  it('sums duplicate names that resolve to the same card', async () => {
    // Two pins to the same non-default printing both fall to overflow and merge by name.
    const code = await encodeAsV2(['2\x1fDupes\x1f', '', '', 'Island\x1f2\x1fM10\x1f235', 'Island\x1f3\x1fM10\x1f235'])
    expect((await decodeSharedDeck(code, resolveName))?.cards).toEqual({ Island: 5 })
  })
})

// Encode an arbitrary object as an *uncompressed* (legacy-format) share code: base64url of the
// raw JSON. Lets tests craft v1 payloads the current encoder would never emit, and exercises the
// decoder's uncompressed-fallback path.
async function encodeAsCode(obj: unknown): Promise<string> {
  return base64urlOf(new TextEncoder().encode(JSON.stringify(obj)))
}

// Encode a hand-built v2 record list (RS-joined) uncompressed, for crafting edge payloads.
async function encodeAsV2(records: string[]): Promise<string> {
  return base64urlOf(new TextEncoder().encode(records.join('\x1e')))
}

function base64urlOf(bytes: Uint8Array): string {
  let binary = ''
  for (const b of bytes) binary += String.fromCharCode(b)
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}
