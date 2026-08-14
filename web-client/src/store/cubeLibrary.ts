/**
 * Cube library — localStorage-backed list of saved cubes (hand-curated singleton card pools used as
 * the pack source for a limited lobby).
 *
 * Mirrors {@link ./banListLibrary banListLibrary}: a tiny standalone Zustand store with no WebSocket
 * integration, so the cube editor and the lobby's cube panel can save/load named cubes the same way
 * decks and ban lists are saved. This is the guest/offline path — signed-in users' cubes also live in
 * the account (`/api/account/cubes`), and {@link ./useUnifiedCubes useUnifiedCubes} merges the two.
 *
 * A cube is stored as card *names* plus counts, not resolved cards: names outlive both the catalog
 * (a cube can name a card that isn't implemented yet) and any particular printing.
 *
 * Storage is a versioned envelope so the shape can migrate later without losing users' cubes.
 *
 * ## Storage versions
 * - **v1** — `{ id, name, cards: CubeEntry[], basicLandSetCode, packSize, updatedAt }`.
 */
import { create } from 'zustand'

/** One line of a cube: a card name and how many physical copies the cube runs. */
export interface CubeEntry {
  readonly name: string
  readonly count: number
}

export interface SavedCube {
  id: string
  name: string
  cards: readonly CubeEntry[]
  /**
   * Set code the basic-land art comes from. A cube has no basics of its own, but the deckbuilder
   * still needs to offer them, so the cube names an art source (mirrors the server's
   * `CubeList.basicLandSetCode`).
   */
  basicLandSetCode: string
  /** Cards per pack when this cube is drafted or dealt as sealed. */
  packSize: number
  updatedAt: number
}

interface CubeStorageV1 {
  version: 1
  cubes: SavedCube[]
}

type CubeStorage = CubeStorageV1

const STORAGE_KEY = 'argentum.cubes'
const STORAGE_VERSION = 1

/** Community-standard cube pack size — 360 cards seats 8 players at 3×15 exactly. */
export const DEFAULT_CUBE_PACK_SIZE = 15

/** Total physical cards in a cube (sum of counts), which is what capacity is measured against. */
export function cubeCardCount(cards: readonly CubeEntry[]): number {
  return cards.reduce((sum, entry) => sum + entry.count, 0)
}

/** Expand a cube's entries into one name per physical card — the wire shape the lobby wants. */
export function cubeCardNames(cards: readonly CubeEntry[]): string[] {
  return cards.flatMap((entry) => Array<string>(entry.count).fill(entry.name))
}

interface CubeLibraryState {
  cubes: SavedCube[]
  hydrated: boolean

  hydrate: () => void
  saveCube: (input: Omit<SavedCube, 'id' | 'updatedAt'> & { id?: string }) => SavedCube
  deleteCube: (id: string) => void
  renameCube: (id: string, newName: string) => void
  getCube: (id: string) => SavedCube | undefined
}

function loadFromStorage(): SavedCube[] {
  if (typeof window === 'undefined') return []
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw) as CubeStorage
    if (!parsed || !Array.isArray(parsed.cubes)) return []
    if (parsed.version === STORAGE_VERSION) return parsed.cubes
    return []
  } catch {
    return []
  }
}

function persist(cubes: SavedCube[]) {
  if (typeof window === 'undefined') return
  const envelope: CubeStorageV1 = { version: STORAGE_VERSION, cubes }
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(envelope))
}

function generateId(): string {
  return `cube-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

export const useCubeLibrary = create<CubeLibraryState>((set, get) => ({
  cubes: [],
  hydrated: false,

  hydrate: () => {
    if (get().hydrated) return
    set({ cubes: loadFromStorage(), hydrated: true })
  },

  saveCube: (input) => {
    const id = input.id ?? generateId()
    const existing = input.id ? get().cubes.find((c) => c.id === input.id) : undefined
    const saved: SavedCube = {
      id,
      name: input.name,
      cards: [...input.cards],
      basicLandSetCode: input.basicLandSetCode,
      packSize: input.packSize,
      updatedAt: Date.now(),
    }
    const cubes = existing
      ? get().cubes.map((c) => (c.id === id ? saved : c))
      : [...get().cubes, saved]
    persist(cubes)
    set({ cubes })
    return saved
  },

  deleteCube: (id) => {
    const cubes = get().cubes.filter((c) => c.id !== id)
    persist(cubes)
    set({ cubes })
  },

  renameCube: (id, newName) => {
    const cubes = get().cubes.map((c) =>
      c.id === id ? { ...c, name: newName, updatedAt: Date.now() } : c
    )
    persist(cubes)
    set({ cubes })
  },

  getCube: (id) => get().cubes.find((c) => c.id === id),
}))
