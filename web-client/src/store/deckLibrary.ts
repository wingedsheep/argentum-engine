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
   * Optional — the second commander of a partner pair (Partner, "Partner with", Choose a
   * Background, Doctor's companion; CR 702.124). Only meaningful alongside `commander`, and
   * stored outside `cards` just like it. Absent on every deck saved before partner support,
   * which load unchanged.
   */
  secondCommander?: string
  /** Optional — pinned printing for [secondCommander]. */
  secondCommanderPrinting?: PrintingRef
  /**
   * Optional v2 — per-entry rows that pin specific printings. When present, this is
   * authoritative over `cards` for the deckbuilder picker (which renders by entry, not
   * by name) and gets sent over the wire as `cardEntries`. The flat `cards` map is kept
   * in sync so legacy code paths (counts, summaries) keep working unchanged.
   */
  entries?: readonly SavedDeckEntry[]
  /**
   * Optional — the constructed sideboard ("outside the game", CR 100.4a), card name → count.
   * Reachable in-game only by wish effects (Burning Wish, …). Sent over the wire as `sideboard`
   * and seeded into the engine's SIDEBOARD zone. Absent/empty for almost every deck. (Limited
   * decks don't store this — their sideboard is the pool − maindeck complement, derived
   * server-side, CR 100.4b.)
   */
  sideboard?: Record<string, number>
  /**
   * Optional — the deck's companion (CR 702.139): the name of a `sideboard` card with a companion
   * ability that the deck reveals before the game. Sent over the wire as `companion`; the server
   * validates the condition and the engine puts it outside the game, revealed.
   */
  companion?: string
  /** Optional — a short line the owner writes about the deck ("aggro, weak to fliers"). */
  note?: string
  /**
   * Optional — the card whose art represents the deck in every deck gallery and picker. When
   * absent (or no longer in the deck) the tile falls back to the deck's rarest card.
   */
  coverCard?: string
  /**
   * Optional — the cover card's image URL as chosen (the pinned printing's art, when one is
   * pinned). Lets a picker paint the cover without loading the card catalogue.
   */
  coverImageUri?: string
  updatedAt: number
}

/** The deck fields a "details" edit touches — none of them change what gets played. */
export interface DeckDetailsPatch {
  name?: string
  note?: string | null
  coverCard?: string | null
  coverImageUri?: string | null
}

/** Longest note a deck can carry: one line in a tile caption, two at most. */
export const DECK_NOTE_MAX_LENGTH = 120

/** Apply a details patch to a deck, dropping fields cleared to null / blank. */
export function applyDeckDetails<T extends SavedDeck>(deck: T, patch: DeckDetailsPatch): T {
  const next: T = { ...deck }
  if (patch.name !== undefined) next.name = patch.name.trim() || deck.name
  const assign = (key: 'note' | 'coverCard' | 'coverImageUri', value: string | null | undefined) => {
    if (value === undefined) return
    const trimmed = value?.trim()
    if (trimmed) next[key] = trimmed
    else delete next[key]
  }
  assign('note', patch.note === undefined ? undefined : (patch.note ?? '').slice(0, DECK_NOTE_MAX_LENGTH))
  assign('coverCard', patch.coverCard)
  assign('coverImageUri', patch.coverImageUri)
  if (patch.coverCard === null) delete next.coverImageUri
  return next
}

interface DeckLibraryStorageV1 {
  version: 1
  decks: Array<Omit<SavedDeck, 'entries' | 'commanderPrinting' | 'secondCommander' | 'secondCommanderPrinting'>>
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
  updateDetails: (id: string, patch: DeckDetailsPatch) => void
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
 * The deck's designated commanders, first then (for a partner pair) second. Empty when the deck
 * has none. A `secondCommander` without a `commander` is ignored — it's never written that way.
 */
export function commandersOf(deck: {
  readonly commander?: string | null | undefined
  readonly secondCommander?: string | null | undefined
}): string[] {
  if (!deck.commander) return []
  return deck.secondCommander && deck.secondCommander !== deck.commander
    ? [deck.commander, deck.secondCommander]
    : [deck.commander]
}

/**
 * Merge the designated commander(s) into a card map. Used when reading a `SavedDeck`
 * for gameplay or rendering: storage keeps the commander out of `cards` (per the
 * `SavedDeck.commander` contract — and matching the server's `Deck.cards`
 * convention, CR 903.6a), so any consumer that just wants "the full deck list"
 * needs to put it back.
 *
 * Idempotent — won't double-count if the commander is already in `cards` (e.g.
 * legacy decks saved before the strip-on-save convention landed). Pass both commanders of a
 * partner pair; null/undefined entries are skipped.
 */
export function mergeCommanderIntoCards(
  cards: Record<string, number>,
  ...commanders: ReadonlyArray<string | null | undefined>
): Record<string, number> {
  let out = cards
  for (const commander of commanders) {
    if (!commander || out[commander]) continue
    out = { ...out, [commander]: 1 }
  }
  return out
}

/**
 * Inverse of [mergeCommanderIntoCards]: subtract the designated commander from
 * a card map. Used at storage / server boundaries where `cards` must exclude the
 * commander(s). Idempotent — does nothing for a commander that isn't present. Removes one
 * copy per commander passed (both halves of a partner pair).
 */
export function stripCommanderFromCards(
  cards: Record<string, number>,
  ...commanders: ReadonlyArray<string | null | undefined>
): Record<string, number> {
  let out = cards
  for (const commander of commanders) {
    if (!commander || !(commander in out)) continue
    const next = { ...out }
    const remaining = (next[commander] ?? 0) - 1
    if (remaining > 0) next[commander] = remaining
    else delete next[commander]
    out = next
  }
  return out
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

/** Minimal pool-card shape needed to recover a card's drafted printing. */
export interface PoolCardPrinting {
  readonly name: string
  readonly setCode?: string | null
  readonly collectorNumber?: string | null
}

/**
 * Build the `{ cards, entries }` pair for saving a drafted/sealed deck to the library,
 * pinning each card to the exact printing it was opened/drafted as.
 *
 * A drafted/sealed deck is tracked only by card name (the spell list plus basic-land
 * counts), so the printing has to be recovered from the original pool — each pool card
 * carries the `(setCode, collectorNumber)` it was opened as. We look each deck card up by
 * name and attach that printing; without this the save falls back to the card's default
 * printing and the player loses (e.g.) the specific art they drafted.
 *
 * `entries` is returned only when at least one card resolved to a printing — otherwise the
 * save stays name-only (identical to the legacy path), avoiding noise for pools whose cards
 * lack printing metadata (e.g. test cards).
 */
export function buildDraftedDeckSave(
  deckCardNames: readonly string[],
  landCounts: Record<string, number>,
  pool: readonly PoolCardPrinting[],
): { cards: Record<string, number>; entries: SavedDeckEntry[] | undefined } {
  const printingByName = new Map<string, PrintingRef>()
  for (const card of pool) {
    if (card.setCode && card.collectorNumber && !printingByName.has(card.name)) {
      printingByName.set(card.name, { setCode: card.setCode, collectorNumber: card.collectorNumber })
    }
  }

  const counts = new Map<string, number>()
  for (const name of deckCardNames) counts.set(name, (counts.get(name) ?? 0) + 1)
  for (const [name, count] of Object.entries(landCounts)) {
    if (count > 0) counts.set(name, (counts.get(name) ?? 0) + count)
  }

  const cards: Record<string, number> = {}
  const entries: SavedDeckEntry[] = []
  let anyPrinting = false
  for (const [name, count] of counts) {
    cards[name] = count
    const printing = printingByName.get(name)
    if (printing) anyPrinting = true
    entries.push(printing ? { name, count, printing } : { name, count })
  }

  return { cards, entries: anyPrinting ? entries : undefined }
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
      ...(input.secondCommander !== undefined ? { secondCommander: input.secondCommander } : {}),
      ...(input.secondCommanderPrinting !== undefined
        ? { secondCommanderPrinting: input.secondCommanderPrinting }
        : {}),
      ...(input.entries !== undefined ? { entries: input.entries } : {}),
      ...(input.sideboard !== undefined && Object.keys(input.sideboard).length > 0
        ? { sideboard: input.sideboard }
        : {}),
      ...(input.companion ? { companion: input.companion } : {}),
      ...(input.note?.trim() ? { note: input.note.trim().slice(0, DECK_NOTE_MAX_LENGTH) } : {}),
      ...(input.coverCard ? { coverCard: input.coverCard } : {}),
      ...(input.coverCard && input.coverImageUri ? { coverImageUri: input.coverImageUri } : {}),
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

  updateDetails: (id, patch) => {
    const decks = get().decks.map((d) =>
      d.id === id ? { ...applyDeckDetails(d, patch), updatedAt: Date.now() } : d
    )
    persist(decks)
    set({ decks })
  },

  getDeck: (id) => get().decks.find((d) => d.id === id),
}))
