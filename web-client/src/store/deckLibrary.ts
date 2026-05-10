/**
 * Deck library — localStorage-backed list of saved custom decks.
 *
 * Decoupled from the main game store: it has no WebSocket integration and is only consumed
 * by the deck picker. Kept as its own tiny Zustand store so subscribers (DeckPicker, future
 * tournament-deck-submission, etc.) can react to library changes without prop drilling.
 *
 * Storage format is a versioned envelope so we can migrate the shape later without losing
 * users' decks.
 *
 * ## Storage versions
 * - **v1** — `cards: Record<string, number>` only. No per-printing pinning.
 * - **v2** — adds optional `entries: SavedDeckEntry[]` and `commanderPrinting?: PrintingRef`
 *   so users can pick specific printings of cards (e.g. M10 vs. 2X2 Lightning Bolt). When
 *   `entries` is undefined the deck behaves identically to v1 (name-only).
 *
 * v1 envelopes load forward into v2 with `entries === undefined` and `commanderPrinting`
 * absent — no data loss, no shape coercion. The next save persists as v2.
 */
import { create } from 'zustand'
import type { PrintingRef } from '@/types'

export interface SavedDeckEntry {
  /** Card name (oracle identity). Same as the key in [SavedDeck.cards]. */
  name: string
  /** How many copies. Sum of `count` across entries with the same `name` MUST equal `cards[name]`. */
  count: number
  /** Optional pinned printing — when absent, server uses the card's default printing. */
  printing?: PrintingRef
}

export interface SavedDeck {
  id: string
  name: string
  cards: Record<string, number>
  /** Optional — populated by the tournament Custom-Decks flow in a later phase. */
  format?: string
  /** Optional — the set code the deck was built against, if any. */
  setCode?: string
  /**
   * Optional — designated commander for Commander/Brawl/Standard Brawl decks. Stored
   * separately from `cards` (the commander begins in the command zone, not the library).
   * Populated by the deckbuilder when the user marks a row with the crown toggle.
   */
  commander?: string
  /**
   * Optional v2 — pinned printing for the commander. Honoured when `commander` is set
   * and the lobby format is commander-shaped. Independent of `entries`: the commander
   * itself is stored separately, not inside `entries`.
   */
  commanderPrinting?: PrintingRef
  /**
   * Optional v2 — per-entry rows that pin specific printings. When present, this is
   * authoritative over `cards` for the deckbuilder picker (which renders by entry, not
   * by name) and gets sent over the wire as `cardEntries`. The flat `cards` map is kept
   * in sync so legacy code paths (counts, summaries) keep working unchanged.
   */
  entries?: readonly SavedDeckEntry[]
  updatedAt: number
}

interface DeckLibraryStorageV1 {
  version: 1
  decks: Array<Omit<SavedDeck, 'entries' | 'commanderPrinting'>>
}

interface DeckLibraryStorageV2 {
  version: 2
  decks: SavedDeck[]
}

type DeckLibraryStorage = DeckLibraryStorageV1 | DeckLibraryStorageV2

const STORAGE_KEY = 'argentum.decks'
const STORAGE_VERSION = 2

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
    if (!parsed || !Array.isArray(parsed.decks)) return []
    if (parsed.version === STORAGE_VERSION) {
      return parsed.decks
    }
    if (parsed.version === 1) {
      // v1 → v2: leave the new optional fields undefined; v1 decks are name-only and
      // continue to round-trip through `cards` exactly as before.
      return parsed.decks.map((d) => ({ ...d } as SavedDeck))
    }
    return []
  } catch {
    return []
  }
}

function persist(decks: SavedDeck[]) {
  if (typeof window === 'undefined') return
  const envelope: DeckLibraryStorageV2 = { version: STORAGE_VERSION, decks }
  window.localStorage.setItem(STORAGE_KEY, JSON.stringify(envelope))
}

function generateId(): string {
  return `deck-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

/**
 * Merge a designated commander into a card map. Used when reading a `SavedDeck`
 * for gameplay or rendering: storage keeps the commander out of `cards` (per the
 * `SavedDeck.commander` contract — and matching the server's `Deck.cards`
 * convention, CR 903.6a), so any consumer that just wants "the full deck list"
 * needs to put it back.
 *
 * Idempotent — won't double-count if the commander is already in `cards` (e.g.
 * legacy decks saved before the strip-on-save convention landed).
 */
export function mergeCommanderIntoCards(
  cards: Record<string, number>,
  commander: string | null | undefined,
): Record<string, number> {
  if (!commander) return cards
  if (cards[commander]) return cards
  return { ...cards, [commander]: 1 }
}

/**
 * Inverse of [mergeCommanderIntoCards]: subtract the designated commander from
 * a card map. Used at storage / server boundaries where `cards` must exclude the
 * commander. Idempotent — does nothing when the commander isn't present.
 */
export function stripCommanderFromCards(
  cards: Record<string, number>,
  commander: string | null | undefined,
): Record<string, number> {
  if (!commander || !(commander in cards)) return cards
  const next = { ...cards }
  const remaining = (next[commander] ?? 0) - 1
  if (remaining > 0) next[commander] = remaining
  else delete next[commander]
  return next
}

/**
 * Convert v2 [SavedDeckEntry] rows to the wire-format `cardEntries` list. Returns
 * undefined when `entries` is absent or empty so the message factory falls back to
 * the legacy name-only path.
 *
 * Each entry expands to its `count` rows so server-side counting (4-of, singleton,
 * total deck size) collapses identically across printings.
 */
export function toCardEntries(
  entries: readonly SavedDeckEntry[] | undefined,
): import('@/types').DeckEntry[] | undefined {
  if (!entries || entries.length === 0) return undefined
  const out: import('@/types').DeckEntry[] = []
  for (const entry of entries) {
    for (let i = 0; i < entry.count; i++) {
      out.push(entry.printing ? { name: entry.name, printing: entry.printing } : { name: entry.name })
    }
  }
  return out
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
      ...(input.commander !== undefined ? { commander: input.commander } : {}),
      ...(input.commanderPrinting !== undefined ? { commanderPrinting: input.commanderPrinting } : {}),
      ...(input.entries !== undefined ? { entries: input.entries } : {}),
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
