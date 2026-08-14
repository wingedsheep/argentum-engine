import { describe, it, expect } from 'vitest'
import {
  RECIPE_VERSION,
  recipeFromSelection,
  recipeSummary,
  validateRecipe,
  type LobbyRecipe,
} from './lobbyRecipe'
import {
  ROSTERS,
  SHAPE_IDS,
  cardsChoices,
  shapeChoices,
  shapeAxes,
  shapeFromAxes,
  type Selection,
} from './modeMatrix'
import { CARDS_KINDS, TABLE_VALUES, type CardsAxis } from './axes'
import type { AvailableSet } from '@/types'

const SETS: readonly AvailableSet[] = [
  { code: 'ECL', name: 'Eclipse' },
  { code: 'BLB', name: 'Bloomburrow' },
]

/** Every selection the wizard can actually reach — the same space `modeMatrix.test.ts` walks. */
function everyReachableSelection(): Selection[] {
  const out: Selection[] = []
  for (const roster of ROSTERS) {
    for (const kind of CARDS_KINDS) {
      const cardsChoice = cardsChoices(roster).find((c) => c.value === kind)
      if (!cardsChoice || cardsChoice.disabledReason) continue
      const cards = defaultAxisFor(kind)
      for (const shape of SHAPE_IDS) {
        const shapeChoice = shapeChoices(roster, cards).find((c) => c.value === shape)
        if (!shapeChoice || shapeChoice.disabledReason) continue
        out.push({ roster, cards, shape })
      }
    }
  }
  return out
}

function defaultAxisFor(kind: (typeof CARDS_KINDS)[number]): CardsAxis {
  switch (kind) {
    case 'BRING_A_DECK': return { kind: 'BRING_A_DECK', legality: null }
    case 'RANDOM': return { kind: 'RANDOM' }
    case 'MOMIR': return { kind: 'MOMIR' }
    case 'SEALED': return { kind: 'SEALED', shape: 'STANDARD' }
    case 'DRAFT': return { kind: 'DRAFT', shape: 'BOOSTER' }
  }
}

describe('shapeFromAxes', () => {
  it('inverts shapeAxes for every shape', () => {
    for (const shape of SHAPE_IDS) {
      const { table, event } = shapeAxes(shape)
      expect(shapeFromAxes(table, event)).toBe(shape)
    }
  })

  it('covers every table value', () => {
    // A table with no shape would make a captured recipe fall off the end of the switch.
    for (const table of TABLE_VALUES) {
      expect(SHAPE_IDS).toContain(shapeFromAxes(table, 'SINGLE_GAME'))
    }
  })
})

describe('recipeFromSelection', () => {
  it('round-trips every reachable selection through validation', () => {
    for (const selection of everyReachableSelection()) {
      const recipe = recipeFromSelection(selection)
      const revived = validateRecipe(JSON.parse(JSON.stringify(recipe)), {
        aiEnabled: true,
        availableSets: SETS,
      })
      expect(revived, JSON.stringify(selection)).not.toBeNull()
      expect(revived!.recipe.selection).toEqual(selection)
      expect(revived!.notes).toEqual([])
    }
  })

  it('carries no settings — a fresh selection has not chosen any', () => {
    // The whole point of Phase 0: the launch path used to invent `['ECL'], 6, 45`.
    for (const selection of everyReachableSelection()) {
      expect(recipeFromSelection(selection).settings).toEqual({})
    }
  })

  it('never auto-starts, because no deck has been picked yet', () => {
    for (const selection of everyReachableSelection()) {
      expect(recipeFromSelection(selection).autoStart).toBe(false)
    }
  })

  it('seeds a single AI opponent for a solo tournament lobby, leaving the rest to Add AI', () => {
    const soloDraft = recipeFromSelection({
      roster: 'SOLO', cards: { kind: 'DRAFT', shape: 'BOOSTER' }, shape: 'BRACKET',
    })
    expect(soloDraft.aiSeats).toBe(1)

    const soloFreeForAll = recipeFromSelection({
      roster: 'SOLO', cards: { kind: 'BRING_A_DECK', legality: null }, shape: 'FREE_FOR_ALL',
    })
    expect(soloFreeForAll.aiSeats).toBe(1)

    // Two-Headed Giant is the exception: the server seats exactly four, so one opponent would open a
    // lobby that cannot start.
    const soloTwoHeaded = recipeFromSelection({
      roster: 'SOLO', cards: { kind: 'BRING_A_DECK', legality: null }, shape: 'TWO_HEADED_GIANT',
    })
    expect(soloTwoHeaded.aiSeats).toBe(3)

    const soloOneGame = recipeFromSelection({
      roster: 'SOLO', cards: { kind: 'BRING_A_DECK', legality: null }, shape: 'ONE_GAME',
    })
    expect(soloOneGame.aiSeats).toBe(0)

    const soloMomir = recipeFromSelection({
      roster: 'SOLO', cards: { kind: 'MOMIR' }, shape: 'ONE_GAME',
    })
    expect(soloMomir.aiSeats).toBe(0)

    const groupDraft = recipeFromSelection({
      roster: 'GROUP', cards: { kind: 'DRAFT', shape: 'BOOSTER' }, shape: 'BRACKET',
    })
    expect(groupDraft.aiSeats).toBe(0)
  })

  it('opens the picker on Random only for a rolled pool', () => {
    expect(recipeFromSelection({
      roster: 'SOLO', cards: { kind: 'RANDOM' }, shape: 'ONE_GAME',
    }).deck).toEqual({ kind: 'RANDOM' })
    expect(recipeFromSelection({
      roster: 'SOLO', cards: { kind: 'BRING_A_DECK', legality: null }, shape: 'ONE_GAME',
    }).deck).toEqual({ kind: 'NONE' })
  })
})

describe('validateRecipe', () => {
  const base: LobbyRecipe = {
    v: RECIPE_VERSION,
    selection: { roster: 'GROUP', cards: { kind: 'DRAFT', shape: 'BOOSTER' }, shape: 'BRACKET' },
    settings: { setCodes: ['ECL'], boosterCount: 4 },
    deck: { kind: 'NONE' },
    aiSeats: 0,
    autoStart: false,
  }
  const ctx = { aiEnabled: true, availableSets: SETS }

  it('accepts a well-formed recipe unchanged', () => {
    const out = validateRecipe(base, ctx)
    expect(out?.recipe.settings.setCodes).toEqual(['ECL'])
    expect(out?.notes).toEqual([])
  })

  it.each([
    ['not an object', 42],
    ['null', null],
    ['a future version', { ...base, v: 99 }],
    ['no selection', { ...base, selection: undefined }],
    ['an unknown roster', { ...base, selection: { ...base.selection, roster: 'CROWD' } }],
    ['an unknown shape', { ...base, selection: { ...base.selection, shape: 'PYRAMID' } }],
    ['an unknown cards kind', { ...base, selection: { ...base.selection, cards: { kind: 'VINTAGE' } } }],
    ['an unknown draft sub-shape', {
      ...base,
      selection: { ...base.selection, cards: { kind: 'DRAFT', shape: 'ROCHESTER' } },
    }],
  ])('rejects %s', (_label, raw) => {
    expect(validateRecipe(raw, ctx)).toBeNull()
  })

  it('rejects a solo recipe when the server has no AI', () => {
    const solo = { ...base, selection: { ...base.selection, roster: 'SOLO' as const } }
    expect(validateRecipe(solo, { ...ctx, aiEnabled: false })).toBeNull()
    expect(validateRecipe(solo, ctx)).not.toBeNull()
  })

  it('rejects a combination this build no longer reaches', () => {
    // Momir has no bracket implementation — the same verdict the wizard's step 3 gives.
    const stale = {
      ...base,
      selection: { roster: 'SOLO', cards: { kind: 'MOMIR' }, shape: 'BRACKET' },
    }
    expect(validateRecipe(stale, ctx)).toBeNull()
  })

  it('drops set codes this server does not have, and says which', () => {
    const out = validateRecipe({ ...base, settings: { setCodes: ['ECL', 'XYZ'] } }, ctx)
    expect(out?.recipe.settings.setCodes).toEqual(['ECL'])
    expect(out?.notes.join(' ')).toContain('XYZ')
  })

  it('keeps a deferred random-set slot, which is an instruction rather than a set code', () => {
    const out = validateRecipe({ ...base, settings: { setCodes: ['RANDOM', 'RANDOM-2'] } }, ctx)
    expect(out?.recipe.settings.setCodes).toEqual(['RANDOM', 'RANDOM-2'])
    expect(out?.notes).toEqual([])
  })

  it('trusts the stored sets when the catalogue has not arrived yet', () => {
    // Trimming against an empty catalogue would delete every code the moment a setup is read
    // before the socket handshake lands.
    const out = validateRecipe(base, { aiEnabled: true, availableSets: [] })
    expect(out?.recipe.settings.setCodes).toEqual(['ECL'])
    expect(out?.notes).toEqual([])
  })

  it('clamps out-of-range numbers instead of sending them', () => {
    const out = validateRecipe({
      ...base,
      settings: { boosterCount: 900, pickTimeSeconds: -5, picksPerRound: 7, gamesPerMatch: 99 },
    }, ctx)
    expect(out?.recipe.settings.boosterCount).toBe(16)
    expect(out?.recipe.settings.pickTimeSeconds).toBe(10)
    expect(out?.recipe.settings.picksPerRound).toBe(2)
    expect(out?.recipe.settings.gamesPerMatch).toBe(5)
  })

  it('discards settings of the wrong type rather than forwarding them', () => {
    const out = validateRecipe({
      ...base,
      settings: { chaosBoosters: 'yes', rules: 'PAUPER', attackMode: 'BACKWARDS', boosterCount: 'six' },
    }, ctx)
    expect(out?.recipe.settings.chaosBoosters).toBeUndefined()
    expect(out?.recipe.settings.rules).toBeUndefined()
    expect(out?.recipe.settings.attackMode).toBeUndefined()
    expect(out?.recipe.settings.boosterCount).toBe(6)
  })

  it('falls back to no deck when the stored reference is unusable', () => {
    expect(validateRecipe({ ...base, deck: { kind: 'SAVED', name: '' } }, ctx)?.recipe.deck)
      .toEqual({ kind: 'NONE' })
    expect(validateRecipe({ ...base, deck: 'Goblins' }, ctx)?.recipe.deck)
      .toEqual({ kind: 'NONE' })
    expect(validateRecipe({ ...base, deck: { kind: 'SAVED', name: 'Goblins' } }, ctx)?.recipe.deck)
      .toEqual({ kind: 'SAVED', name: 'Goblins' })
  })

  it('keeps a cube only when it still has cards to build from', () => {
    const cube = { name: 'Vintage', cards: ['Black Lotus'], basicLandSetCode: 'ECL', packSize: 15, poolPlay: false }
    expect(validateRecipe({ ...base, settings: { cube } }, ctx)?.recipe.settings.cube).toEqual(cube)
    expect(validateRecipe({ ...base, settings: { cube: { ...cube, cards: [] } } }, ctx)
      ?.recipe.settings.cube).toBeUndefined()
  })
})

describe('recipeSummary', () => {
  it('leads with the three answers, then what the recipe adds over them', () => {
    const recipe: LobbyRecipe = {
      v: RECIPE_VERSION,
      selection: { roster: 'GROUP', cards: { kind: 'DRAFT', shape: 'BOOSTER' }, shape: 'BRACKET' },
      settings: { setCodes: ['ECL', 'BLB'], boosterCount: 3 },
      deck: { kind: 'NONE' },
      aiSeats: 0,
      autoStart: false,
    }
    expect(recipeSummary(recipe, SETS))
      .toBe('A group · Booster Draft · Round-robin bracket · Eclipse + Bloomburrow · 3 packs')
  })

  it('says nothing about packs for a format that opens none', () => {
    const recipe: LobbyRecipe = {
      v: RECIPE_VERSION,
      selection: { roster: 'SOLO', cards: { kind: 'BRING_A_DECK', legality: null }, shape: 'ONE_GAME' },
      settings: { boosterCount: 6 },
      deck: { kind: 'SAVED', name: 'Goblins' },
      aiSeats: 0,
      autoStart: true,
    }
    expect(recipeSummary(recipe, SETS)).toBe('Just me · Bring a deck · One game · Goblins')
  })
})
