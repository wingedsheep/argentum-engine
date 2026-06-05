import { describe, it, expect } from 'vitest'
import {
  encodeSharedDeck,
  decodeSharedDeck,
  buildShareUrl,
  SHARE_PARAM,
  type SharedDeck,
} from './shareDeck'

describe('shareDeck codec', () => {
  it('round-trips a plain name-only deck', async () => {
    const deck: SharedDeck = {
      name: 'Mono-Red Aggro',
      cards: { 'Lightning Bolt': 4, 'Mountain': 20, 'Goblin Guide': 4 },
    }
    expect(await decodeSharedDeck(await encodeSharedDeck(deck))).toEqual(deck)
  })

  it('round-trips format, commander, and printing pins', async () => {
    const deck: SharedDeck = {
      name: 'Atraxa Superfriends',
      cards: { 'Sol Ring': 1, 'Arcane Signet': 1, 'Forest': 10 },
      format: 'COMMANDER',
      commander: 'Atraxa, Praetors’ Voice',
      commanderPrinting: { setCode: 'C16', collectorNumber: '28' },
      printings: { 'Sol Ring': { setCode: 'C21', collectorNumber: '263' } },
    }
    expect(await decodeSharedDeck(await encodeSharedDeck(deck))).toEqual(deck)
  })

  it('preserves Unicode and split-card (DFC) names', async () => {
    const deck: SharedDeck = {
      name: 'Æther — déjà vu',
      cards: { 'Unholy Annex // Ritual Chamber': 2, 'Lim-Dûl’s Vault': 1 },
    }
    expect(await decodeSharedDeck(await encodeSharedDeck(deck))).toEqual(deck)
  })

  it('produces a URL-safe code (no +, /, = or whitespace)', async () => {
    const code = await encodeSharedDeck({
      name: 'Test',
      cards: { 'Some Very Long Card Name That Compresses Poorly': 4 },
    })
    expect(code).toMatch(/^[A-Za-z0-9_-]+$/)
  })

  it('compresses large decks well under chat limits', async () => {
    // 99 distinct singleton names — the commander-deck worst case.
    const cards: Record<string, number> = {}
    for (let i = 0; i < 99; i++) cards[`Wandering Verdant Sentinel of the Wilds #${i}`] = 1
    const code = await encodeSharedDeck({ name: 'Big Pile', format: 'COMMANDER', cards })
    expect(code.length).toBeLessThan(2000)
  })

  it('builds the share URL with the deckbuilder route and param', () => {
    expect(buildShareUrl('https://play.example.com', 'ABC123')).toBe(
      `https://play.example.com/deckbuilder?${SHARE_PARAM}=ABC123`,
    )
  })

  it('decodes a legacy uncompressed (plain-JSON) share code', async () => {
    const code = await encodeAsCode({ v: 1, n: 'Legacy', d: [['Plains', 7]] })
    expect(await decodeSharedDeck(code)).toEqual({ name: 'Legacy', cards: { Plains: 7 } })
  })

  it('returns null for malformed / non-share input', async () => {
    expect(await decodeSharedDeck('')).toBeNull()
    expect(await decodeSharedDeck('not-base64-!!!')).toBeNull()
    // Valid base64url of JSON that isn't a v1 share payload.
    expect(await decodeSharedDeck(await encodeAsCode({ hello: 'world' }))).toBeNull()
    // Right version but no usable cards.
    expect(await decodeSharedDeck(await encodeAsCode({ v: 1, n: 'x', d: [] }))).toBeNull()
  })

  it('drops malformed rows but keeps the valid ones', async () => {
    const code = await encodeAsCode({
      v: 1,
      n: 'Mixed',
      d: [
        ['Good Card', 2],
        ['Zero', 0], // dropped: non-positive count
        ['Bad Count', 'three'], // dropped: count not a number
        [42, 1], // dropped: name not a string
        'nonsense', // dropped: not a tuple
      ],
    })
    expect(await decodeSharedDeck(code)).toEqual({ name: 'Mixed', cards: { 'Good Card': 2 } })
  })

  it('sums duplicate rows for the same card name', async () => {
    const code = await encodeAsCode({
      v: 1,
      n: 'Dupes',
      d: [
        ['Island', 2],
        ['Island', 3],
      ],
    })
    expect((await decodeSharedDeck(code))?.cards).toEqual({ Island: 5 })
  })
})

// Encode an arbitrary object as an *uncompressed* (legacy-format) share code: base64url of the
// raw JSON. Lets tests craft payloads the current encoder would never emit (wrong version,
// malformed rows) and also exercises the decoder's legacy fallback path.
async function encodeAsCode(obj: unknown): Promise<string> {
  const bytes = new TextEncoder().encode(JSON.stringify(obj))
  let binary = ''
  for (const b of bytes) binary += String.fromCharCode(b)
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}
