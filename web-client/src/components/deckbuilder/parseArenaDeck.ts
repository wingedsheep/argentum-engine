/**
 * Parser for MTG Arena deck-list format.
 *
 * Each entry looks like:
 *   `4 Lightning Bolt (LEA) 161`
 *   `2 Counterspell`
 *
 * Section headers (`Deck`, `Sideboard`, `Companion`, `Commander`) are
 * recognised so we can ignore everything after the sideboard. Blank lines and
 * `//` / `#` comments are skipped. The number of copies and the card name are
 * required; set code and collector number are optional and used as tiebreakers.
 */
import type { CardSummary } from './cardFilter'

export interface ParsedEntry {
  count: number
  name: string
  setCode?: string
  collectorNumber?: string
  /** Original line — kept for error messages. */
  raw: string
  /** Line number in the source text (1-based) — kept for error messages. */
  line: number
}

export interface ParseResult {
  /** Main-deck entries, in source order. */
  entries: ParsedEntry[]
  /** Sideboard entries (recognised but not imported into the working deck). */
  sideboard: ParsedEntry[]
  /** Lines that looked like card entries but couldn't be parsed. */
  errors: Array<{ line: number; raw: string; reason: string }>
}

const ENTRY_RE = /^(\d+)\s+(.+?)(?:\s+\(([A-Za-z0-9]{2,5})\)(?:\s+(\S+))?)?\s*$/

const SECTION_HEADERS: Record<string, 'main' | 'side' | 'ignore'> = {
  deck: 'main',
  maindeck: 'main',
  main: 'main',
  sideboard: 'side',
  companion: 'ignore',
  commander: 'ignore',
}

export function parseArenaDeckList(text: string): ParseResult {
  const entries: ParsedEntry[] = []
  const sideboard: ParsedEntry[] = []
  const errors: ParseResult['errors'] = []

  let section: 'main' | 'side' | 'ignore' = 'main'

  const rawLines = text.split(/\r?\n/)
  for (let i = 0; i < rawLines.length; i++) {
    const raw = rawLines[i] ?? ''
    const trimmed = raw.trim()
    if (trimmed === '') continue
    if (trimmed.startsWith('//') || trimmed.startsWith('#')) continue

    const headerKey = trimmed.toLowerCase().replace(/[:\s]+$/, '')
    if (headerKey in SECTION_HEADERS) {
      section = SECTION_HEADERS[headerKey]!
      continue
    }

    if (section === 'ignore') continue

    const match = ENTRY_RE.exec(trimmed)
    if (!match) {
      errors.push({ line: i + 1, raw: trimmed, reason: 'unrecognised line format' })
      continue
    }
    const count = parseInt(match[1]!, 10)
    if (!Number.isFinite(count) || count <= 0) {
      errors.push({ line: i + 1, raw: trimmed, reason: 'invalid card count' })
      continue
    }
    const entry: ParsedEntry = {
      count,
      name: match[2]!.trim(),
      raw: trimmed,
      line: i + 1,
      ...(match[3] ? { setCode: match[3].toUpperCase() } : {}),
      ...(match[4] ? { collectorNumber: match[4] } : {}),
    }
    if (section === 'side') sideboard.push(entry)
    else entries.push(entry)
  }

  return { entries, sideboard, errors }
}

export interface ResolvedEntry {
  entry: ParsedEntry
  match: CardSummary | null
  /** True if the name was matched but the requested set code didn't match. */
  setMismatch?: boolean
}

export interface ResolveResult {
  resolved: ResolvedEntry[]
  /** Aggregated {name → count} of successfully matched entries. */
  deckCards: Record<string, number>
  /** Total cards across resolved entries (matched copies only). */
  matchedCards: number
  /** Total cards in the parsed list, matched or not. */
  totalCards: number
  /** Entries we couldn't match by name. */
  unmatched: ResolvedEntry[]
  /** Entries that exceeded the four-of limit (or basic-land exemption). */
  truncated: Array<{ name: string; requested: number; capped: number }>
}

/**
 * Match parsed entries against the catalogue by name (case-insensitive). The
 * set code, if present, is used only to flag a soft mismatch — the name still
 * resolves so users aren't blocked by a stale set tag. Copy counts above the
 * four-of limit (basic lands exempted) are silently capped, with the original
 * request reported back so the UI can surface a warning.
 */
export function resolveAgainstCatalog(
  entries: ParsedEntry[],
  catalog: CardSummary[]
): ResolveResult {
  const byName = new Map<string, CardSummary>()
  for (const c of catalog) byName.set(c.name.toLowerCase(), c)

  const resolved: ResolvedEntry[] = []
  const deckCards: Record<string, number> = {}
  const truncated: ResolveResult['truncated'] = []
  let matchedCards = 0
  let totalCards = 0

  for (const entry of entries) {
    totalCards += entry.count
    const card = byName.get(entry.name.toLowerCase()) ?? null
    if (!card) {
      resolved.push({ entry, match: null })
      continue
    }
    const setMismatch =
      !!entry.setCode && !!card.setCode && entry.setCode.toUpperCase() !== card.setCode.toUpperCase()
    resolved.push({ entry, match: card, ...(setMismatch ? { setMismatch: true } : {}) })

    const max = card.basicLand ? Infinity : 4
    const previous = deckCards[card.name] ?? 0
    const requested = previous + entry.count
    const capped = Math.min(requested, max)
    deckCards[card.name] = capped
    matchedCards += capped - previous
    if (capped < requested) {
      const existing = truncated.find((t) => t.name === card.name)
      if (existing) {
        existing.requested = requested
        existing.capped = capped
      } else {
        truncated.push({ name: card.name, requested, capped })
      }
    }
  }

  const unmatched = resolved.filter((r) => r.match === null)
  return { resolved, deckCards, matchedCards, totalCards, unmatched, truncated }
}
