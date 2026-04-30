/**
 * Deck library — localStorage-backed list of saved custom decks.
 *
 * Decoupled from the main game store: it has no WebSocket integration and is only consumed
 * by the deck picker. Kept as its own tiny Zustand store so subscribers (DeckPicker, future
 * tournament-deck-submission, etc.) can react to library changes without prop drilling.
 *
 * Storage format is a versioned envelope so we can migrate the shape later without losing
 * users' decks.
 */
import { create } from 'zustand'

export interface SavedDeck {
  id: string
  name: string
  cards: Record<string, number>
  /** Optional — populated by the tournament Custom-Decks flow in a later phase. */
  format?: string
  /** Optional — the set code the deck was built against, if any. */
  setCode?: string
  updatedAt: number
}

interface DeckLibraryStorage {
  version: 1
  decks: SavedDeck[]
}

const STORAGE_KEY = 'argentum.decks'
const STORAGE_VERSION = 1

interface DeckLibraryState {
  decks: SavedDeck[]
  hydrated: boolean

  hydrate: () => void
  saveDeck: (input: Omit<SavedDeck, 'id' | 'updatedAt'> & { id?: string }) => SavedDeck
  deleteDeck: (id: string) => void
  renameDeck: (id: string, newName: string) => void
  getDeck: (id: string) => SavedDeck | undefined
}

function loadFromStorage(): SavedDeck[] {
  if (typeof window === 'undefined') return []
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw) as DeckLibraryStorage
    if (parsed?.version !== STORAGE_VERSION || !Array.isArray(parsed.decks)) return []
    return parsed.decks
  } catch {
    return []
  }
}

function persist(decks: SavedDeck[]) {
  if (typeof window === 'undefined') return
  const envelope: DeckLibraryStorage = { version: STORAGE_VERSION, decks }
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(envelope))
}

function generateId(): string {
  return `deck-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

export const useDeckLibrary = create<DeckLibraryState>((set, get) => ({
  decks: [],
  hydrated: false,

  hydrate: () => {
    if (get().hydrated) return
    set({ decks: loadFromStorage(), hydrated: true })
  },

  saveDeck: (input) => {
    const now = Date.now()
    const id = input.id ?? generateId()
    const existing = input.id ? get().decks.find((d) => d.id === input.id) : undefined
    const saved: SavedDeck = {
      id,
      name: input.name,
      cards: input.cards,
      ...(input.format !== undefined ? { format: input.format } : {}),
      ...(input.setCode !== undefined ? { setCode: input.setCode } : {}),
      updatedAt: now,
    }
    const decks = existing
      ? get().decks.map((d) => (d.id === id ? saved : d))
      : [...get().decks, saved]
    persist(decks)
    set({ decks })
    return saved
  },

  deleteDeck: (id) => {
    const decks = get().decks.filter((d) => d.id !== id)
    persist(decks)
    set({ decks })
  },

  renameDeck: (id, newName) => {
    const decks = get().decks.map((d) =>
      d.id === id ? { ...d, name: newName, updatedAt: Date.now() } : d
    )
    persist(decks)
    set({ decks })
  },

  getDeck: (id) => get().decks.find((d) => d.id === id),
}))
