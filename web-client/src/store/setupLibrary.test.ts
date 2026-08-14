import { describe, it, expect, beforeEach } from 'vitest'
import {
  LAST_SETUP_ID,
  orderedSetups,
  usableSetups,
  useSetupLibrary,
  type SavedSetup,
} from './setupLibrary'
import { RECIPE_VERSION, recipeFromSelection, type LobbyRecipe } from '@/components/lobby/lobbyRecipe'
import type { AvailableSet } from '@/types'

const SETS: readonly AvailableSet[] = [{ code: 'ECL', name: 'Eclipse' }]
const CTX = { aiEnabled: true, availableSets: SETS }

const draftRecipe = (): LobbyRecipe => recipeFromSelection({
  roster: 'GROUP', cards: { kind: 'DRAFT', shape: 'BOOSTER' }, shape: 'BRACKET',
})

const soloRecipe = (): LobbyRecipe => recipeFromSelection({
  roster: 'SOLO', cards: { kind: 'MOMIR' }, shape: 'ONE_GAME',
})

function setup(id: string, name: string, updatedAt: number, recipe = draftRecipe()): SavedSetup {
  return { id, name, recipe, updatedAt, pinned: id !== LAST_SETUP_ID }
}

/**
 * A minimal `window.localStorage`.
 *
 * The suite runs in the plain node environment — every other test here is pure — so rather than pull
 * in jsdom for four methods, this is the four methods. `setupLibrary` reaches for `window`, which is
 * also the guard that keeps it inert during SSR.
 */
function installStorage(): Storage {
  const data = new Map<string, string>()
  const storage = {
    getItem: (k: string) => data.get(k) ?? null,
    setItem: (k: string, v: string) => { data.set(k, String(v)) },
    removeItem: (k: string) => { data.delete(k) },
    clear: () => { data.clear() },
    key: (i: number) => [...data.keys()][i] ?? null,
    get length() { return data.size },
  } as Storage
  ;(globalThis as { window?: unknown }).window = { localStorage: storage }
  ;(globalThis as { localStorage?: unknown }).localStorage = storage
  return storage
}

beforeEach(() => {
  installStorage()
  useSetupLibrary.setState({ setups: [], hydrated: false })
})

describe('capture and save', () => {
  it('keeps exactly one auto-captured slot, overwritten each game', () => {
    const { captureLast } = useSetupLibrary.getState()
    captureLast(draftRecipe())
    captureLast(soloRecipe())
    const setups = useSetupLibrary.getState().setups
    expect(setups.filter((s) => s.id === LAST_SETUP_ID)).toHaveLength(1)
    expect(setups[0]!.recipe.selection.cards.kind).toBe('MOMIR')
  })

  it('does not disturb named setups when capturing', () => {
    const { captureLast, saveSetup } = useSetupLibrary.getState()
    saveSetup({ name: 'Thursday cube', recipe: draftRecipe() })
    captureLast(soloRecipe())
    const setups = useSetupLibrary.getState().setups
    expect(setups.map((s) => s.name)).toContain('Thursday cube')
    expect(setups).toHaveLength(2)
  })

  it('survives a round trip through storage', () => {
    useSetupLibrary.getState().saveSetup({ name: 'Thursday cube', recipe: draftRecipe() })
    useSetupLibrary.setState({ setups: [], hydrated: false })
    useSetupLibrary.getState().hydrate()
    expect(useSetupLibrary.getState().setups.map((s) => s.name)).toEqual(['Thursday cube'])
  })

  it('promotes the auto-captured slot to a kept setup when it is named', () => {
    const { captureLast, renameSetup, captureLast: capture2 } = useSetupLibrary.getState()
    captureLast(draftRecipe())
    renameSetup(LAST_SETUP_ID, 'Thursday cube')
    // The next game must not overwrite it — that is the whole point of naming it.
    capture2(soloRecipe())
    const setups = useSetupLibrary.getState().setups
    expect(setups.find((s) => s.name === 'Thursday cube')).toBeDefined()
    expect(setups.find((s) => s.name === 'Thursday cube')!.id).not.toBe(LAST_SETUP_ID)
    expect(setups.find((s) => s.id === LAST_SETUP_ID)!.recipe.selection.cards.kind).toBe('MOMIR')
  })

  it('deletes by id', () => {
    const saved = useSetupLibrary.getState().saveSetup({ name: 'Gone', recipe: draftRecipe() })
    useSetupLibrary.getState().deleteSetup(saved.id)
    expect(useSetupLibrary.getState().setups).toHaveLength(0)
  })
})

describe('legacy migration', () => {
  it('adopts the wizard’s old one-slot selection and removes the key', () => {
    localStorage.setItem('argentum-last-play-selection', JSON.stringify({
      roster: 'GROUP', cards: { kind: 'DRAFT', shape: 'BOOSTER' }, shape: 'BRACKET',
    }))
    useSetupLibrary.getState().hydrate()
    const setups = useSetupLibrary.getState().setups
    expect(setups).toHaveLength(1)
    expect(setups[0]!.id).toBe(LAST_SETUP_ID)
    expect(setups[0]!.recipe.selection.shape).toBe('BRACKET')
    expect(localStorage.getItem('argentum-last-play-selection')).toBeNull()
  })

  it('ignores a legacy blob that no longer describes anything', () => {
    localStorage.setItem('argentum-last-play-selection', JSON.stringify({ roster: 'GROUP' }))
    useSetupLibrary.getState().hydrate()
    expect(useSetupLibrary.getState().setups).toHaveLength(0)
  })

  it('ignores a stale legacy selection carrying the removed seats field', () => {
    localStorage.setItem('argentum-last-play-selection', JSON.stringify({
      roster: 'SOLO', cards: { kind: 'MOMIR' }, shape: 'ONE_GAME', seats: 2,
    }))
    useSetupLibrary.getState().hydrate()
    expect(useSetupLibrary.getState().setups[0]!.recipe.selection).toEqual({
      roster: 'SOLO', cards: { kind: 'MOMIR' }, shape: 'ONE_GAME',
    })
  })
})

describe('orderedSetups', () => {
  it('leads with the auto-captured slot, then most recent first', () => {
    const list = [
      setup('a', 'Older', 100),
      setup(LAST_SETUP_ID, 'Last played', 50),
      setup('b', 'Newer', 300),
    ]
    expect(orderedSetups(list).map((s) => s.name)).toEqual(['Last played', 'Newer', 'Older'])
  })
})

describe('usableSetups', () => {
  it('drops a setup this server can no longer run', () => {
    const list = [
      setup('a', 'Group draft', 100),
      setup('b', 'Solo Momir', 200, soloRecipe()),
    ]
    // With the AI off, the solo setup describes a game that cannot be created.
    const usable = usableSetups(list, { ...CTX, aiEnabled: false })
    expect(usable.map((s) => s.name)).toEqual(['Group draft'])
  })

  it('carries per-setup notes through so the chip can warn before it is clicked', () => {
    const withMissingSet: SavedSetup = {
      id: 'a',
      name: 'Old sets',
      recipe: { ...draftRecipe(), settings: { setCodes: ['ECL', 'NOPE'] } },
      updatedAt: 1,
      pinned: true,
    }
    const [only] = usableSetups([withMissingSet], CTX)
    expect(only!.notes.join(' ')).toContain('NOPE')
    expect(only!.recipe.settings.setCodes).toEqual(['ECL'])
  })

  it('drops a recipe stored by a future build rather than guessing at it', () => {
    const future: SavedSetup = {
      id: 'a',
      name: 'From the future',
      recipe: { ...draftRecipe(), v: (RECIPE_VERSION + 1) as 1 },
      updatedAt: 1,
      pinned: true,
    }
    expect(usableSetups([future], CTX)).toHaveLength(0)
  })
})
