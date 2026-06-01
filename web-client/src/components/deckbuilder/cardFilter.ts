/**
 * cardFilter — public facade for the deckbuilder's query language and filter chip helpers.
 *
 * The actual grammar / lexer / parser / evaluator live in `./query/`. This file:
 *   - re-exports the user-facing entry points,
 *   - declares the `CardSummary` shape (mirrors `DecksController.CardSummaryDTO`),
 *   - keeps the legacy chip helpers (`addToken`/`removeToken`/`hasToken`/`toggleToken`)
 *     that the menu-style filter panel uses to round-trip top-level atoms with the
 *     free-text bar.
 *
 * The chip helpers operate on whitespace-separated terms in the raw string. They are
 * not aware of `or` / parens — when the query becomes "advanced" (uses any feature
 * the chips can't represent), the filter panel switches to read-only and stops
 * calling these helpers. See `isAdvancedQuery` in `./query`.
 */
import { parseQuery as parseQueryFull } from './query'
import type { CardPredicate, ParseResult } from './query'

// ---------------------------------------------------------------------------
// Card shape — mirrors `DecksController.CardSummaryDTO` on the backend.
// ---------------------------------------------------------------------------

export interface CardSummary {
  name: string
  manaCost: string
  cmc: number
  /** Printed mana-cost colors. Used for mana curve, pip stats, and the `cost:` operator. */
  colors: string[]
  /**
   * Color identity (CR 903.4) — drives the `c:` / `color:` filter. Includes oracle-text
   * colored symbols and basic-land-subtype colors, so a card with off-color activation costs
   * correctly surfaces under its full identity. The server stamps this from the authoritative
   * Scryfall override when one exists.
   */
  colorIdentity: string[]
  cardTypes: string[]
  supertypes: string[]
  subtypes: string[]
  basicLand: boolean
  rarity: string
  setCode: string | null
  collectorNumber: string | null
  /**
   * All sets this card has a printing in (canonical + reprints). Used by the `s:`/`set:`
   * query so a reprint surfaces under its new set code even when [setCode] still points
   * at the original printing's set. Defaults to empty for legacy fixtures.
   */
  printingSetCodes?: string[]
  oracleText?: string | null
  power?: string | null
  toughness?: string | null
  imageUri?: string | null
  keywords?: string[]
  /** Server-stamped legal formats (uppercase: STANDARD, MODERN, COMMANDER, …). */
  legalFormats?: string[]
  isDoubleFaced?: boolean
  backFaceName?: string | null
  backFaceImageUri?: string | null
  /**
   * Printed layout (`NORMAL`, `SPLIT`, `ADVENTURE`, …). `SPLIT` cards (Pain // Suffering, Rooms
   * like Unholy Annex // Ritual Chamber) have a single sideways-printed image, so the hover
   * preview rotates them 90° to read landscape. Defaults to `NORMAL` for legacy fixtures.
   */
  layout?: string
}

export type { CardPredicate, ParseResult, ParseError } from './query'
export { isAdvancedQuery, ALL_KEYS } from './query'

/**
 * Parse a query string into a card predicate plus diagnostics.
 *
 * Backwards-compatible overload: many call-sites (and the chip-detection regexes)
 * just want the predicate. Pass `{ withErrors: true }` to get the full
 * `ParseResult` including parse errors with character spans.
 */
export function parseQuery(query: string): CardPredicate
export function parseQuery(query: string, opts: { withErrors: true }): ParseResult
export function parseQuery(query: string, opts?: { withErrors: boolean }): CardPredicate | ParseResult {
  const result = parseQueryFull(query)
  if (opts?.withErrors) return result
  return result.predicate
}

// ---------------------------------------------------------------------------
// Menu ↔ query helpers
//
// The FilterPanel toggles call these to keep the text query in sync with the
// chips. They operate on the raw string so a user typing "t:creature -t:goblin"
// can still see the menu reflect the dominant choices.
//
// Token comparison is whole-term, whitespace-separated. These helpers do not
// understand `or` / parens — when the query is advanced (per `isAdvancedQuery`),
// the FilterPanel switches to read-only and stops calling them.
// ---------------------------------------------------------------------------

function splitTerms(query: string): string[] {
  const out: string[] = []
  let buf = ''
  let inQuote = false
  for (let i = 0; i < query.length; i++) {
    const ch = query[i]!
    if (ch === '"') {
      inQuote = !inQuote
      buf += ch
      continue
    }
    if (!inQuote && /\s/.test(ch)) {
      if (buf) { out.push(buf); buf = '' }
      continue
    }
    buf += ch
  }
  if (buf) out.push(buf)
  return out
}

export function addToken(query: string, token: string): string {
  const terms = splitTerms(query)
  if (terms.includes(token)) return query
  return terms.length === 0 ? token : `${terms.join(' ')} ${token}`
}

export function removeToken(query: string, token: string): string {
  const terms = splitTerms(query).filter((t) => t !== token)
  return terms.join(' ')
}

export function hasToken(query: string, token: string): boolean {
  return splitTerms(query).includes(token)
}

export function toggleToken(query: string, token: string): string {
  return hasToken(query, token) ? removeToken(query, token) : addToken(query, token)
}
