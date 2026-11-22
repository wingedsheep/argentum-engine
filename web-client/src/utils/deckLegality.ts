/**
 * Format-legality helpers shared by the deckbuilder and lobby DeckPicker.
 *
 * The authoritative legality computation lives in the backend (`POST /api/decks/legal-formats`);
 * this module only provides the request hook + presentational constants. We never re-implement
 * the format rules client-side because a Commander deck (singleton, exact-100) cannot be
 * validated correctly with just the `legalFormats` array bundled on each card.
 */
import { useEffect, useRef, useState } from 'react'

export interface CardLike {
  legalFormats?: ReadonlyArray<string>
}

/**
 * Constructed deck-construction formats whose legality data is sourced from Scryfall. Order
 * matches the deckbuilder's right-rail picker so badges and selectors render in the same
 * sequence everywhere.
 */
export const DECK_FORMATS: ReadonlyArray<{ value: string; label: string }> = [
  { value: 'standard', label: 'Standard' },
  { value: 'pioneer', label: 'Pioneer' },
  { value: 'modern', label: 'Modern' },
  { value: 'pauper', label: 'Pauper' },
  { value: 'legacy', label: 'Legacy' },
  { value: 'vintage', label: 'Vintage' },
  { value: 'commander', label: 'Commander' },
  { value: 'brawl', label: 'Brawl' },
  { value: 'standard_brawl', label: 'Standard Brawl' },
  { value: 'premodern', label: 'Premodern' },
  { value: 'highlander', label: 'Highlander' },
  { value: 'canadian_highlander', label: 'Canadian Highlander' },
  { value: 'primordial', label: 'Primordial' },
]

/**
 * Formats that allow one copy of each non-basic card: the commander shapes and the two Highlander
 * formats. Mirrors the server's per-format copy cap so the deckbuilder's `+` stops at 1; the
 * server's validation stays the authority.
 */
const SINGLETON_FORMATS: ReadonlySet<string> = new Set([
  'COMMANDER', 'BRAWL', 'STANDARD_BRAWL', 'HIGHLANDER', 'CANADIAN_HIGHLANDER',
])

/**
 * Size of the deck the server auto-builds for a non-commander format: the Highlander formats'
 * 100-card singleton, Primordial's 40 from one set, otherwise constructed's 60.
 */
export function autoBuiltDeckSize(formatUpper: string | null): number {
  const f = formatUpper?.toUpperCase()
  if (f === 'PRIMORDIAL') return 40
  return f === 'HIGHLANDER' || f === 'CANADIAN_HIGHLANDER' ? 100 : 60
}

/**
 * Primordial: decks come from one block or standalone set with a rarity budget, so copy caps
 * follow rarity and the deckbuilder narrows its catalog to the deck's set. The server decides
 * the set and every rule; this only switches the deckbuilder's presentation.
 */
export function isPrimordialFormat(formatUpper: string | null): boolean {
  return formatUpper?.toUpperCase() === 'PRIMORDIAL'
}

export function isSingletonFormat(formatUpper: string | null): boolean {
  return formatUpper !== null && SINGLETON_FORMATS.has(formatUpper.toUpperCase())
}

/** Maps an upper-case format code (STANDARD, MODERN, …) to its display label. */
export function labelForFormat(formatUpper: string): string {
  const found = DECK_FORMATS.find((f) => f.value.toUpperCase() === formatUpper)
  return found ? found.label : formatUpper
}

export type DeckId = string
export type DeckList = Record<string, number>

/**
 * Calls `POST /api/decks/legal-formats` with the supplied decks and returns a stable map of
 * `deckId → upper-case format names the deck is fully legal in`.
 *
 * The hook keys cache by stable content hash, not by reference, so a parent re-rendering with
 * an equal-but-new `decks` object doesn't refetch. Decks whose content hasn't changed since
 * the last response are returned from cache; only new/changed deck IDs trigger a request.
 *
 * Returns the *previous* response while a refetch is in flight to avoid badge flicker.
 */
export function useDeckLegalFormats(
  decks: Record<DeckId, DeckList>
): Record<DeckId, string[]> {
  const [result, setResult] = useState<Record<DeckId, string[]>>({})
  const cacheRef = useRef<Record<DeckId, { hash: string; formats: string[] }>>({})
  const inFlightRef = useRef<AbortController | null>(null)

  // Build a list of (id, hash, deckList) for everything currently passed in, then diff against
  // the cache. Only un-cached / changed entries are sent to the server. We always emit a result
  // map covering *every* id in `decks` so consumers don't have to handle "missing" specially.
  useEffect(() => {
    const ids = Object.keys(decks)
    if (ids.length === 0) {
      // Drop stale cache entries when the input goes empty so unbounded local state doesn't grow.
      if (Object.keys(result).length > 0) setResult({})
      return
    }

    const hashes: Record<DeckId, string> = {}
    const stale: Record<DeckId, DeckList> = {}
    for (const id of ids) {
      const list = decks[id] ?? {}
      const hash = hashDeck(list)
      hashes[id] = hash
      const cached = cacheRef.current[id]
      if (!cached || cached.hash !== hash) stale[id] = list
    }

    // If nothing is stale we can publish from cache and skip the network call entirely.
    const cachedView: Record<DeckId, string[]> = {}
    for (const id of ids) {
      const entry = cacheRef.current[id]
      if (entry && entry.hash === hashes[id]) cachedView[id] = entry.formats
    }
    if (Object.keys(stale).length === 0) {
      if (!shallowEqual(result, cachedView)) setResult(cachedView)
      return
    }

    // Cancel any in-flight request — its payload is now stale.
    inFlightRef.current?.abort()
    const ctrl = new AbortController()
    inFlightRef.current = ctrl

    fetch('/api/decks/legal-formats', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ decks: stale }),
      signal: ctrl.signal,
    })
      .then((r) => (r.ok ? r.json() : null))
      .then((res: Record<DeckId, string[]> | null) => {
        if (ctrl.signal.aborted) return
        if (res) {
          for (const [id, formats] of Object.entries(res)) {
            const hash = hashes[id]
            if (hash !== undefined) cacheRef.current[id] = { hash, formats }
          }
        }
        // Project the cache against the *current* ids so removed decks drop out cleanly.
        const next: Record<DeckId, string[]> = {}
        for (const id of ids) {
          const entry = cacheRef.current[id]
          if (entry && entry.hash === hashes[id]) next[id] = entry.formats
        }
        setResult(next)
      })
      .catch(() => {
        // Network failure: surface what the cache has so badges don't disappear on a flaky link.
        if (ctrl.signal.aborted) return
        const fallback: Record<DeckId, string[]> = {}
        for (const id of ids) {
          const entry = cacheRef.current[id]
          if (entry) fallback[id] = entry.formats
        }
        setResult(fallback)
      })

    return () => {
      ctrl.abort()
    }
    // We deliberately key on the deck content hashes — the `decks` object reference may
    // change every render even when contents are equal.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [hashAll(decks)])

  return result
}

function hashDeck(deck: DeckList): string {
  return Object.entries(deck)
    .filter(([, n]) => n > 0)
    .sort(([a], [b]) => (a < b ? -1 : a > b ? 1 : 0))
    .map(([n, c]) => `${n}=${c}`)
    .join('|')
}

function hashAll(decks: Record<DeckId, DeckList>): string {
  return Object.keys(decks)
    .sort()
    .map((id) => `${id}#${hashDeck(decks[id] ?? {})}`)
    .join(';')
}

function shallowEqual(
  a: Record<string, string[]>,
  b: Record<string, string[]>
): boolean {
  const ak = Object.keys(a)
  const bk = Object.keys(b)
  if (ak.length !== bk.length) return false
  for (const k of ak) {
    const av = a[k]
    const bv = b[k]
    if (!bv || av?.length !== bv.length) return false
    for (let i = 0; i < (av?.length ?? 0); i++) if (av?.[i] !== bv[i]) return false
  }
  return true
}
