import { describe, it, expect } from 'vitest'
import {
  MODES,
  canRollDeck,
  defaultOptions,
  effectiveOpponents,
  modeFromSlug,
  offersAiAssist,
  opponentRange,
  recipeForOptions,
  rollsDeck,
  selectionFor,
  stagesFor,
  type DraftStyle,
  type HumanTable,
  type PlayOptions,
  type PlayWith,
  type SealedStyle,
  type TableCards,
} from './playModes'
import { validateRecipe } from './lobbyRecipe'
import { lobbyKindFor, seatCap } from './modeMatrix'

const PLAY_WITH: readonly PlayWith[] = ['AI', 'FRIENDS', 'PUBLIC']
const HUMAN_TABLES: readonly HumanTable[] = ['ONE_V_ONE', 'BRACKET']
const DRAFT_STYLES: readonly DraftStyle[] = ['BOOSTER', 'WINSTON', 'GRID', 'COMMANDER']
const SEALED_STYLES: readonly SealedStyle[] = ['STANDARD', 'COMMANDER']
const TABLE_CARDS: readonly TableCards[] = ['DECKS', 'JUMP_IN', 'SEALED', 'DRAFT']

/** Every combination the launch panel can produce, opponent counts included. */
function everyOption(): PlayOptions[] {
  const out: PlayOptions[] = []
  for (const mode of MODES) {
    for (const playWith of PLAY_WITH) {
      for (const humanTable of HUMAN_TABLES) {
        for (const draftStyle of DRAFT_STYLES) {
          for (const sealedStyle of SEALED_STYLES) {
            for (const tableCards of TABLE_CARDS) {
              const base: PlayOptions = {
                ...defaultOptions(mode.id, true),
                playWith, humanTable, draftStyle, sealedStyle, tableCards,
              }
              const range = opponentRange(base)
              const counts = range ? [range.min, range.max] : [1]
              for (const opponents of counts) out.push({ ...base, opponents })
            }
          }
        }
      }
    }
  }
  return out
}

describe('playModes', () => {
  it('only ever produces a recipe the matrix accepts', () => {
    for (const options of everyOption()) {
      const recipe = recipeForOptions(options)
      const validated = validateRecipe(recipe, { aiEnabled: true, availableSets: [] })
      expect(validated, JSON.stringify(options)).not.toBeNull()
      expect(validated!.recipe.selection).toEqual(recipe.selection)
    }
  })

  it('never seats more AI than the lobby has room for', () => {
    for (const options of everyOption().filter((o) => o.playWith === 'AI')) {
      const recipe = recipeForOptions(options)
      const { roster, cards, shape } = recipe.selection
      expect(1 + recipe.aiSeats, JSON.stringify(options)).toBeLessThanOrEqual(seatCap(roster, cards, shape))
    }
  })

  it('auto-starts only games against the AI', () => {
    for (const options of everyOption()) {
      expect(recipeForOptions(options).autoStart).toBe(options.playWith === 'AI')
    }
  })

  it('lists a public lobby and keeps a friends lobby private', () => {
    const draft = defaultOptions('DRAFT', true)
    expect(recipeForOptions({ ...draft, playWith: 'PUBLIC' }).settings.isPublic).toBe(true)
    expect(recipeForOptions({ ...draft, playWith: 'FRIENDS' }).settings.isPublic).toBeUndefined()
  })

  it('plays a 1v1 constructed game against one AI in the quick lobby', () => {
    const options = { ...defaultOptions('CONSTRUCTED', true), deck: { kind: 'SAVED', name: 'Goblins' } as const }
    const recipe = recipeForOptions(options)
    expect(lobbyKindFor(recipe.selection)).toBe('QUICK')
    expect(recipe.deck).toEqual({ kind: 'SAVED', name: 'Goblins' })
    expect(recipe.aiSeats).toBe(0)
  })

  it('carries a starter deck or a rolled deck into the recipe', () => {
    const constructed = defaultOptions('CONSTRUCTED', true)
    expect(recipeForOptions({ ...constructed, deck: { kind: 'EXAMPLE', name: 'Boros Mice' } }).deck)
      .toEqual({ kind: 'EXAMPLE', name: 'Boros Mice' })
    expect(recipeForOptions({ ...constructed, deck: { kind: 'RANDOM' } }).deck).toEqual({ kind: 'RANDOM' })
    expect(stagesFor({ ...constructed, deck: { kind: 'EXAMPLE', name: 'Boros Mice' } })).toEqual(['Play'])
  })

  it('offers a rolled deck only where the quick lobby runs the game', () => {
    const constructed = defaultOptions('CONSTRUCTED', true)
    expect(canRollDeck(constructed)).toBe(true)
    // Three AI is a premade-decks bracket, which takes a submitted list only.
    expect(canRollDeck({ ...constructed, opponents: 3 })).toBe(false)
    expect(recipeForOptions({ ...constructed, opponents: 3, deck: { kind: 'RANDOM' } }).deck.kind).not.toBe('RANDOM')
    expect(canRollDeck(defaultOptions('DRAFT', true))).toBe(false)
  })

  it('turns more AI opponents into a bracket with that many AI seats', () => {
    const recipe = recipeForOptions({ ...defaultOptions('CONSTRUCTED', true), opponents: 3 })
    expect(recipe.selection.shape).toBe('BRACKET')
    expect(recipe.aiSeats).toBe(3)
  })

  it('drafts against three AI by default, on the chosen set', () => {
    const recipe = recipeForOptions({ ...defaultOptions('DRAFT', true), setCode: 'BLB' })
    expect(recipe.selection).toMatchObject({ roster: 'SOLO', cards: { kind: 'DRAFT', shape: 'BOOSTER' }, shape: 'BRACKET' })
    expect(recipe.aiSeats).toBe(3)
    expect(recipe.settings.setCodes).toEqual(['BLB'])
  })

  it('passes a deferred Random Set through for the server to roll at start', () => {
    expect(recipeForOptions({ ...defaultOptions('SEALED', true), setCode: 'RANDOM' }).settings.setCodes).toEqual(['RANDOM'])
    expect(recipeForOptions({ ...defaultOptions('DRAFT', true), setCode: 'RANDOM' }).settings.setCodes).toEqual(['RANDOM'])
  })

  it('asks about AI assistance only for AI games built from packs, and carries the answer', () => {
    expect(offersAiAssist(defaultOptions('SEALED', true))).toBe(true)
    expect(offersAiAssist(defaultOptions('DRAFT', true))).toBe(true)
    expect(offersAiAssist({ ...defaultOptions('FREE_FOR_ALL', true), tableCards: 'SEALED' })).toBe(true)
    expect(offersAiAssist(defaultOptions('CONSTRUCTED', true))).toBe(false)
    // People in a lobby: the host switches it on in the lobby's settings instead.
    expect(offersAiAssist({ ...defaultOptions('SEALED', true), playWith: 'FRIENDS' })).toBe(false)

    const sealed = defaultOptions('SEALED', true)
    expect(recipeForOptions(sealed).settings.aiAssistEnabled).toBeUndefined()
    expect(recipeForOptions({ ...sealed, aiAssist: true }).settings.aiAssistEnabled).toBe(true)
    expect(recipeForOptions({ ...sealed, aiAssist: true, playWith: 'FRIENDS' }).settings.aiAssistEnabled).toBeUndefined()
  })

  it('pins Winston to one opponent and Team vs. Team to even tables', () => {
    expect(effectiveOpponents({ ...defaultOptions('DRAFT', true), draftStyle: 'WINSTON', opponents: 5 })).toBe(1)
    expect(effectiveOpponents({ ...defaultOptions('TEAM_VS_TEAM', true), opponents: 4 }) % 2).toBe(1)
  })

  it('runs Commander under Commander rules and deck legality', () => {
    const duel = recipeForOptions({ ...defaultOptions('COMMANDER', true), opponents: 1 })
    expect(duel.selection.shape).toBe('BRACKET')
    expect(duel.settings).toMatchObject({ rules: 'COMMANDER', deckFormat: 'COMMANDER' })
    const pod = recipeForOptions({ ...defaultOptions('COMMANDER', true), opponents: 3 })
    expect(pod.selection.shape).toBe('FREE_FOR_ALL')
  })

  it('opens a friends lobby when the AI is off on this server', () => {
    expect(defaultOptions('DRAFT', false).playWith).toBe('FRIENDS')
  })

  it('says how many steps there are before committing', () => {
    expect(stagesFor({ ...defaultOptions('DRAFT', true) })).toEqual(['Draft', 'Build 40', 'Everyone plays everyone', 'Standings'])
    expect(stagesFor({ ...defaultOptions('MOMIR', true) })).toEqual(['Play'])
    expect(stagesFor({ ...defaultOptions('RANDOM', true), playWith: 'FRIENDS' })).toEqual(['Players join', 'Get a deck', 'Play'])
  })

  it('rolls a Random deck from the chosen set, or any set when none is chosen', () => {
    const random = defaultOptions('RANDOM', true)
    expect(rollsDeck(random)).toBe(true)
    expect(recipeForOptions(random).deck).toEqual({ kind: 'RANDOM' })
    expect(recipeForOptions({ ...random, randomSetCode: 'BLB' }).deck).toEqual({ kind: 'RANDOM', setCodes: ['BLB'] })

    // "Random deck" as your Constructed deck offers the same choice; a starter deck does not.
    const constructed = { ...defaultOptions('CONSTRUCTED', true), randomSetCode: 'BLB' }
    expect(rollsDeck({ ...constructed, deck: { kind: 'RANDOM' } })).toBe(true)
    expect(recipeForOptions({ ...constructed, deck: { kind: 'RANDOM' } }).deck).toEqual({ kind: 'RANDOM', setCodes: ['BLB'] })
    expect(rollsDeck({ ...constructed, deck: { kind: 'EXAMPLE', name: 'Goblins' } })).toBe(false)
    expect(recipeForOptions({ ...constructed, deck: { kind: 'EXAMPLE', name: 'Goblins' } }).deck)
      .toEqual({ kind: 'EXAMPLE', name: 'Goblins' })
    // A bracket can't roll a deck, so a remembered set doesn't sneak one in.
    expect(rollsDeck({ ...constructed, deck: { kind: 'RANDOM' }, opponents: 3 })).toBe(false)
  })

  it('round-trips every mode through its slug', () => {
    for (const mode of MODES) expect(modeFromSlug(mode.slug)).toBe(mode.id)
    expect(modeFromSlug('solo')).toBeNull()
  })

  it('keeps every selection inside the space the old wizard reached', () => {
    // Spot-check the translation for the modes whose roster depends on a panel choice.
    expect(selectionFor({ ...defaultOptions('CONSTRUCTED', true), playWith: 'FRIENDS', humanTable: 'ONE_V_ONE' }).roster).toBe('FRIEND')
    expect(selectionFor({ ...defaultOptions('CONSTRUCTED', true), playWith: 'FRIENDS', humanTable: 'BRACKET' }).roster).toBe('GROUP')
    expect(selectionFor({ ...defaultOptions('MOMIR', true), playWith: 'PUBLIC' }).roster).toBe('FRIEND')
  })
})
