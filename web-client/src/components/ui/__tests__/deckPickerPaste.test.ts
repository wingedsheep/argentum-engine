/**
 * Pins the lobby deck picker's paste box to the shared decklist parser.
 *
 * The picker used to keep only `parsed.entries`, so a pasted Arena export arrived with an empty
 * sideboard however faithfully the parser had read it. These exercise the two halves of the paste
 * tab that decide what reaches the server: the text → {main, sideboard} split, and the render back
 * into the textarea when a saved deck is opened for editing.
 */
import { describe, expect, it } from 'vitest'
import { formatDeckText, parseDeckText } from '../deckPasteText'

const ARENA_EXPORT = `Deck
4 Monastery Swiftspear
2 Mountain

Sideboard
2 Redcap Melee
1 Ghost Vacuum`

describe('DeckPicker paste parsing', () => {
  it('keeps the Sideboard section out of the deck and in the sideboard', () => {
    const parsed = parseDeckText(ARENA_EXPORT)
    expect(parsed.cards).toEqual({ 'Monastery Swiftspear': 4, Mountain: 2 })
    expect(parsed.sideboard).toEqual({ 'Redcap Melee': 2, 'Ghost Vacuum': 1 })
  })

  it('reads the per-line SB: prefix as sideboard too', () => {
    const parsed = parseDeckText('4 Lightning Bolt\nSB: 2 Counterspell')
    expect(parsed.cards).toEqual({ 'Lightning Bolt': 4 })
    expect(parsed.sideboard).toEqual({ Counterspell: 2 })
  })

  it('rescues the bare-name shorthand onto the board it was written under', () => {
    const parsed = parseDeckText('Deck\nLightning Bolt\nSideboard\nCounterspell')
    expect(parsed.cards).toEqual({ 'Lightning Bolt': 1 })
    expect(parsed.sideboard).toEqual({ Counterspell: 1 })
  })

  // The rescue reads the *preprocessed* line. Reading the raw one instead produced a card named
  // "SB: Counterspell", which then silently vanished: the server drops it as unknown, after the
  // paste box has already counted it in the sideboard total.
  it('strips the SB: prefix when rescuing a bare name, rather than baking it into the name', () => {
    const parsed = parseDeckText('4 Lightning Bolt\nSB: Counterspell')
    expect(parsed.cards).toEqual({ 'Lightning Bolt': 4 })
    expect(parsed.sideboard).toEqual({ Counterspell: 1 })
  })

  it('strips Moxfield decorations when rescuing a bare name', () => {
    const parsed = parseDeckText('Lightning Bolt *F*\nSideboard\nCounterspell #tag')
    expect(parsed.cards).toEqual({ 'Lightning Bolt': 1 })
    expect(parsed.sideboard).toEqual({ Counterspell: 1 })
  })

  it('keeps the commander in the deck, a companion on the sideboard, and drops the maybeboard', () => {
    const parsed = parseDeckText([
      'Commander',
      "1 Atraxa, Praetors' Voice",
      'Companion',
      '1 Lurrus of the Dream-Den',
      'Deck',
      '1 Sol Ring',
      'Maybeboard',
      '1 Doubling Season',
    ].join('\n'))
    expect(parsed.cards).toEqual({ "Atraxa, Praetors' Voice": 1, 'Sol Ring': 1 })
    expect(parsed.sideboard).toEqual({ 'Lurrus of the Dream-Den': 1 })
    // A Companion section names the deck's companion (CR 702.139).
    expect(parsed.companion).toBe('Lurrus of the Dream-Den')
  })

  it('writes a companion back under its own header so the paste reads it again', () => {
    const text = formatDeckText({ 'Sol Ring': 1 }, { 'Lurrus of the Dream-Den': 1, Counterspell: 2 }, 'Lurrus of the Dream-Den')
    expect(text).toContain('Companion\n1 Lurrus of the Dream-Den')
    const again = parseDeckText(text)
    expect(again.companion).toBe('Lurrus of the Dream-Den')
    expect(again.sideboard).toEqual({ 'Lurrus of the Dream-Den': 1, Counterspell: 2 })
  })

  it('leaves the sideboard empty when the list has no sideboard section', () => {
    expect(parseDeckText('4 Lightning Bolt').sideboard).toEqual({})
  })

  it('round-trips through the textarea rendering', () => {
    const parsed = parseDeckText(ARENA_EXPORT)
    const text = formatDeckText(parsed.cards, parsed.sideboard)
    expect(text).toContain('Sideboard')
    const again = parseDeckText(text)
    expect(again.cards).toEqual(parsed.cards)
    expect(again.sideboard).toEqual(parsed.sideboard)
  })

  it('omits the Sideboard header when there is nothing to put under it', () => {
    expect(formatDeckText({ 'Lightning Bolt': 4 }, {})).toBe('4 Lightning Bolt')
    expect(formatDeckText({ 'Lightning Bolt': 4 })).toBe('4 Lightning Bolt')
  })
})
