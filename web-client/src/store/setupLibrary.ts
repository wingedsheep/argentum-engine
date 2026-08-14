/**
 * Setup library — the games you play often, saved so you don't answer the wizard again.
 *
 * A **setup** is a named {@link LobbyRecipe}: the three wizard answers plus every host setting worth
 * reproducing. One click on the home screen rebuilds the whole lobby — a saved 8-player cube draft
 * comes back with its cube, its pack counts, its timer and its ban list, invite code and all.
 *
 * Shaped like {@link ../store/cubeLibrary cubeLibrary} and {@link ../store/banListLibrary
 * banListLibrary}: a small standalone Zustand store over localStorage, no WebSocket involvement.
 * This is the guest/offline path; the account-backed half (`/api/account/setups`) is the natural
 * next step and would merge over this exactly as `useUnifiedCubes` merges over `cubeLibrary`.
 *
 * ## Two ways in, on purpose
 *
 * - **The last one, captured automatically** when you press Start or Ready. Named
 *   {@link LAST_SETUP_ID} and always first on the rail.
 * - **A named one, saved deliberately** with the lobby's ★ button.
 *
 * Building only the button would be the obvious mistake. The `Play again` chip this replaces existed
 * because nobody saves something before they know they'll want it twice — but that chip could only
 * repeat four of the twenty-odd answers, because a bare `Selection` was all there was to store.
 *
 * ## Storage versions
 * - **v1** — `{ id, name, recipe, updatedAt, pinned }`, recipes at `RECIPE_VERSION` 1.
 */
import { create } from 'zustand'
import type { AvailableSet } from '@/types'
import {
  recipeFromSelection,
  validateRecipe,
  type LobbyRecipe,
} from '@/components/lobby/lobbyRecipe'

export interface SavedSetup {
  id: string
  name: string
  recipe: LobbyRecipe
  updatedAt: number
  /** Kept at the front of the rail. The auto-captured entry never is; you didn't choose it. */
  pinned: boolean
}

interface SetupStorageV1 {
  version: 1
  setups: SavedSetup[]
}

const STORAGE_KEY = 'argentum.setups'
const STORAGE_VERSION = 1

/**
 * The wizard's old one-slot memory. Read once and deleted, so an existing player's last game shows
 * up on the rail on their first visit after this ships rather than starting from nothing.
 */
const LEGACY_SELECTION_KEY = 'argentum-last-play-selection'

/** The auto-captured entry's fixed id — one slot, overwritten every time you start a game. */
export const LAST_SETUP_ID = 'last'

interface SetupLibraryState {
  setups: SavedSetup[]
  hydrated: boolean

  hydrate: () => void
  /** Overwrite the single auto-captured slot. Called when a game actually starts. */
  captureLast: (recipe: LobbyRecipe) => void
  /** Save (or update) a named setup. */
  saveSetup: (input: { id?: string; name: string; recipe: LobbyRecipe }) => SavedSetup
  deleteSetup: (id: string) => void
  renameSetup: (id: string, newName: string) => void
}

function loadFromStorage(): SavedSetup[] {
  if (typeof window === 'undefined') return []
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) return migrateLegacySelection()
    const parsed = JSON.parse(raw) as SetupStorageV1
    if (!parsed || !Array.isArray(parsed.setups)) return []
    if (parsed.version !== STORAGE_VERSION) return []
    return parsed.setups
  } catch {
    return []
  }
}

/**
 * Turn the wizard's stored `Selection` into a v1 setup, once, then drop the old key.
 *
 * The selection is all it ever held, so the result carries no settings — which is honest: those
 * answers were never recorded. It re-validates through {@link recipeFromSelection}'s own path rather
 * than being trusted, since it may predate a build where the combination changed shape.
 */
function migrateLegacySelection(): SavedSetup[] {
  try {
    const raw = window.localStorage.getItem(LEGACY_SELECTION_KEY)
    if (!raw) return []
    window.localStorage.removeItem(LEGACY_SELECTION_KEY)
    const parsed: unknown = JSON.parse(raw)
    if (typeof parsed !== 'object' || parsed === null) return []
    const { roster, cards, shape } = parsed as Record<string, unknown>
    if (!roster || !cards || !shape) return []
    // `aiEnabled: true` and an empty catalogue: this is a *shape* check, and the real one happens on
    // read against the live server. Being generous here only risks one dead chip, which validates
    // away; being strict would silently drop a setup on a slow handshake.
    const checked = validateRecipe(
      recipeFromSelection({ roster, cards, shape } as never),
      { aiEnabled: true, availableSets: [] },
    )
    if (!checked) return []
    return [{
      id: LAST_SETUP_ID,
      name: 'Last played',
      recipe: checked.recipe,
      updatedAt: Date.now(),
      pinned: false,
    }]
  } catch {
    return []
  }
}

function persist(setups: SavedSetup[]) {
  if (typeof window === 'undefined') return
  const envelope: SetupStorageV1 = { version: STORAGE_VERSION, setups }
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(envelope))
  } catch {
    // Private browsing / full quota. A setup is a convenience, so failing to store is not an error.
  }
}

function generateId(): string {
  return `setup-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

export const useSetupLibrary = create<SetupLibraryState>((set, get) => ({
  setups: [],
  hydrated: false,

  hydrate: () => {
    if (get().hydrated) return
    set({ setups: loadFromStorage(), hydrated: true })
  },

  captureLast: (recipe) => {
    const entry: SavedSetup = {
      id: LAST_SETUP_ID,
      name: 'Last played',
      recipe,
      updatedAt: Date.now(),
      pinned: false,
    }
    const setups = [entry, ...get().setups.filter((s) => s.id !== LAST_SETUP_ID)]
    persist(setups)
    set({ setups })
  },

  saveSetup: ({ id, name, recipe }) => {
    const setupId = id ?? generateId()
    const saved: SavedSetup = { id: setupId, name, recipe, updatedAt: Date.now(), pinned: true }
    const existing = get().setups.some((s) => s.id === setupId)
    const setups = existing
      ? get().setups.map((s) => (s.id === setupId ? saved : s))
      : [...get().setups, saved]
    persist(setups)
    set({ setups })
    return saved
  },

  deleteSetup: (id) => {
    const setups = get().setups.filter((s) => s.id !== id)
    persist(setups)
    set({ setups })
  },

  renameSetup: (id, newName) => {
    const setups = get().setups.map((s) =>
      // Renaming the auto-captured slot is how you promote it to one you keep: it stops being
      // overwritten by the next game only once it has a name of its own, so it gets a fresh id.
      s.id === id
        ? { ...s, id: id === LAST_SETUP_ID ? generateId() : s.id, name: newName, pinned: true, updatedAt: Date.now() }
        : s,
    )
    persist(setups)
    set({ setups })
  },
}))

/**
 * The rail's order: pinned setups by most recently touched, with the auto-captured one leading.
 *
 * "Last played" first because it is the answer to the most common question by a wide margin — the
 * game you just played is the game you are most likely to want again.
 */
export function orderedSetups(setups: readonly SavedSetup[]): SavedSetup[] {
  const last = setups.filter((s) => s.id === LAST_SETUP_ID)
  const rest = setups.filter((s) => s.id !== LAST_SETUP_ID)
  rest.sort((a, b) => b.updatedAt - a.updatedAt)
  return [...last, ...rest]
}

/**
 * Re-check every stored setup against the server this browser is talking to now, dropping the ones
 * that no longer describe a reachable game.
 *
 * Validation happens on *read*, not on write, for the same reason `pathToDraft` re-checks a URL: a
 * setup outlives the build that made it, and the AI can be switched off between sessions.
 */
export function usableSetups(
  setups: readonly SavedSetup[],
  ctx: { aiEnabled: boolean; availableSets: readonly AvailableSet[] },
): Array<SavedSetup & { notes: readonly string[] }> {
  return orderedSetups(setups).flatMap((setup) => {
    const checked = validateRecipe(setup.recipe, ctx)
    return checked ? [{ ...setup, recipe: checked.recipe, notes: checked.notes }] : []
  })
}
