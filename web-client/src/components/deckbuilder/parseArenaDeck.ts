/**
 * Multi-format deck-list parser. Accepts what the common deck sites export — MTG Arena, MTGO,
 * Moxfield, Archidekt, TappedOut, deckstats and plain text — interchangeably, so the user can paste
 * whatever their other tool spits out. Recognised line shapes:
 *
 *   `4 Lightning Bolt`                          plain
 *   `4x Lightning Bolt`                         plain (Moxfield / Archidekt "x")
 *   `4 Lightning Bolt (LEA) 161`                Arena
 *   `1 Cardname (SET) *F* *A* 42 #tag`          Moxfield bulk-edit (foil/alter/tags)
 *   `SB: 2 Counterspell`                        MTGO sideboard prefix (Moxfield)
 *   `1x Sol Ring (CMM) 1 [Ramp]`                Archidekt, with its category tags
 *   `1x Atraxa (ONE) 196 [Commander{top}]`      Archidekt: a `Commander` category designates it
 *   `1x Foo [Maybeboard{noDeck}{noPrice},Ramp]` Archidekt: a `{noDeck}` category keeps it out
 *   `1x Atraxa, Praetors' Voice *CMDR*`         TappedOut commander marker
 *   `1 [ONE#196] Atraxa # !Commander`           deckstats: set prefix + commander marker
 *
 * Section headers (case-insensitive, an optional trailing `:` or `(count)`, and optionally behind
 * `//` as deckstats writes them) include `Deck` / `Mainboard` / `Main Deck` / `Maindeck`,
 * `Sideboard` / `Side` / `SB`, `Commander` / `Commanders` / `EDH`, `Companion`, `Maybeboard` /
 * `Maybe` / `Considering`, and `About`. Main deck, sideboard, commander, companion and the excluded
 * maybeboard are returned as separate lists; `About` only donates its `Name <…>` line. Blank lines
 * and other lines starting with `//` or `#` are comments. A line that is none of these lands in
 * `errors`, so the import can show it instead of losing it.
 *
 * Exports that leave the commander *implicit* — MTGO and Moxfield's MTGO/plain-text exports put it
 * under `SIDEBOARD:` or in a trailing block after a blank line — are resolved by
 * [inferCommanders], which needs the card catalogue to tell a commander from a sideboard card.
 */
import type { CardSummary } from './cardFilter'
import { canCardBeCommander, canPairAsCommanders, type CommanderCard } from './commanderPairing'

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
  /** Sideboard entries — the constructed "outside the game" board (CR 400.11a). */
  sideboard: ParsedEntry[]
  /**
   * Commander entries — cards listed under a `Commander` / `Commanders` / `EDH` header, or marked
   * as the commander on their own line (Archidekt's `Commander` category, TappedOut's `*CMDR*`,
   * deckstats' `# !Commander`). Most decks have exactly one; partner pairs etc. produce two.
   */
  commander: ParsedEntry[]
  /**
   * Cards under a `Companion` header. A companion starts the game outside it (CR 702.139a), so the
   * import keeps it on the sideboard, and the deck builder makes the first one the deck's companion.
   */
  companion: ParsedEntry[]
  /**
   * Cards the source list keeps out of the deck: a `Maybeboard` / `Considering` section, or an
   * Archidekt category flagged `{noDeck}`. Reported, never imported.
   */
  excluded: ParsedEntry[]
  /**
   * The main-deck entries of the list's *last* block — the run of lines after the final blank
   * line — when that block has no section header of its own and earlier blocks held main-deck
   * cards. MTGO-style plain-text exports put the commander (or the sideboard) there.
   * [inferCommanders] reads it; nothing else treats it specially, so the cards stay in [entries].
   */
  trailingBlock: ParsedEntry[]
  /**
   * Lines that looked like card entries but couldn't be parsed.
   *
   * `raw` is the verbatim line, for showing the user what it couldn't read. `cleaned` is the
   * same line with the `SB:` prefix and the site decorations (`*F*`, `#tag`, `[Category]`)
   * stripped — a caller that *rescues* an unparseable line (the lobby's paste box tolerates a
   * bare card name as one copy) must read `cleaned`, or it will manufacture a card named
   * `"SB: Counterspell"`. `section` records which board the line sat under, so the rescue
   * lands on the right one.
   */
  errors: Array<{
    line: number
    raw: string
    cleaned: string
    reason: string
    section: CardSection
  }>
  /**
   * Deck name pulled from an Arena-style `Name <…>` line in the leading
   * `About` block, if present.
   */
  deckName?: string
}

// Count + optional 'x' suffix (e.g. `4` or `4x`), name (lazy), optional
// `(SET) collector` suffix. The line is pre-stripped of site decorations
// (`*F*`, `*A*`, `#tag`, `[Category]`, deckstats `[SET#num]`) before this regex runs.
const ENTRY_RE = /^(\d+)x?\s+(.+?)(?:\s+\(([A-Za-z0-9]{2,5})\)(?:\s+(\S+))?)?\s*$/

/** A board a parsed entry can land on. */
export type CardSection = 'main' | 'side' | 'commander' | 'companion' | 'excluded'

type Section = CardSection | 'ignore'

const SECTION_HEADERS: Record<string, Section> = {
  deck: 'main',
  maindeck: 'main',
  'main deck': 'main',
  main: 'main',
  mainboard: 'main',
  sideboard: 'side',
  'side board': 'side',
  side: 'side',
  sb: 'side',
  commander: 'commander',
  commanders: 'commander',
  edh: 'commander',
  companion: 'companion',
  companions: 'companion',
  maybeboard: 'excluded',
  maybe: 'excluded',
  considering: 'excluded',
  about: 'ignore',
}

/**
 * The section a header line opens, or null when the line isn't a header. Accepts a trailing `:`
 * (`SIDEBOARD:`), a trailing card count (`Commander (1)`), and a leading `//` (`// Commander`,
 * `//Sideboard` — deckstats); a `//` line naming no known section stays a comment.
 */
function sectionOfHeader(trimmed: string): Section | null {
  const key = trimmed
    .toLowerCase()
    .replace(/^\/\/\s*/, '')
    .replace(/[:\s]+$/, '')
    .replace(/\s*\(\d+\)$/, '')
    .replace(/[:\s]+$/, '')
  return SECTION_HEADERS[key] ?? null
}

/**
 * Strip site decorations from a card line so the simpler entry regex can match across formats.
 * Returns the cleaned line and the section the line's own markup puts it on, if any — the MTGO
 * `SB:` prefix, an Archidekt category, a TappedOut `*CMDR*` or a deckstats `# !Commander`.
 */
function preprocessLine(raw: string): { line: string; section: CardSection | null } {
  let line = raw
  let section: CardSection | null = null

  // Per-line MTGO/Moxfield sideboard prefix (e.g. `SB: 2 Counterspell`).
  const sbMatch = line.match(/^SB:\s*/i)
  if (sbMatch) {
    section = 'side'
    line = line.slice(sbMatch[0].length)
  }

  // Archidekt label tags (`^Have,#37d67a^`) — colour labels, never meaningful here.
  line = line.replace(/\s*\^[^^]*\^\s*$/, '')

  // Archidekt categories: a trailing `[Cat{flag}…,Cat2]`. A `Commander` category designates the
  // commander; a category flagged `{noDeck}` (Maybeboard, Sideboard) keeps the card out of the
  // deck — onto the sideboard for a sideboard category, out entirely otherwise.
  const categories = line.match(/\s*\[([^\]]*)\]\s*$/)
  if (categories) {
    line = line.slice(0, categories.index).trimEnd()
    const cats = categories[1]!.split(',').map((c) => c.trim()).filter(Boolean)
    const names = cats.map((c) => c.replace(/\{[^}]*\}/g, '').trim().toLowerCase())
    if (names.includes('commander')) section = 'commander'
    else if (names.includes('companion')) section = 'companion'
    else if (cats.some((c) => /\{nodeck\}/i.test(c))) {
      section = names.some((n) => n === 'sideboard' || n === 'side') ? 'side' : 'excluded'
    }
  }

  // deckstats: `1 [ONE#196] Atraxa` / `1 [ONE] Atraxa` — a set prefix right after the count.
  line = line.replace(/^(\d+x?\s+)\[[A-Za-z0-9]{2,6}(?:#[^\]]*)?\]\s+/, '$1')

  // deckstats commander marker `# !Commander` (and Moxfield-style `#!Commander`).
  const deckstatsCommander = line.match(/\s+#\s*!commander\b.*$/i)
  if (deckstatsCommander) {
    section = 'commander'
    line = line.slice(0, deckstatsCommander.index).trimEnd()
  }

  // TappedOut commander marker `*CMDR*`.
  if (/\s+\*CMDR\*/i.test(line)) {
    section = 'commander'
    line = line.replace(/\s+\*CMDR\*/gi, '')
  }

  // Moxfield trailing tags: ` #tag`, ` #!globaltag`. Strip from the end as
  // long as the trailing token still looks like a tag, so a card whose name
  // contains `#` somewhere internally isn't accidentally truncated.
  while (true) {
    const m = line.match(/\s+#!?\S+\s*$/)
    if (!m) break
    line = line.slice(0, m.index).trimEnd()
  }

  // Moxfield foil / alter / etched flags can appear anywhere between name and number.
  // Format is `*F*` / `*A*` / `*E*` (case-insensitive).
  line = line.replace(/\s+\*[A-Za-z]\*/g, '').trim()

  return { line, section }
}

export function parseArenaDeckList(text: string): ParseResult {
  const entries: ParsedEntry[] = []
  const sideboard: ParsedEntry[] = []
  const commander: ParsedEntry[] = []
  const companion: ParsedEntry[] = []
  const excluded: ParsedEntry[] = []
  const errors: ParseResult['errors'] = []
  let deckName: string | undefined

  let section: Section = 'main'
  // Blank-line-separated blocks, for [ParseResult.trailingBlock]: the block each main entry sits
  // in, whether the current block opened with a header, and whether any earlier block held main
  // cards.
  let block = 0
  let blockHasHeader = false
  let previousLineBlank = false
  const mainBlockOf = new Map<ParsedEntry, number>()
  let lastBlockHasHeader = false

  const rawLines = text.split(/\r?\n/)
  for (let i = 0; i < rawLines.length; i++) {
    const raw = rawLines[i] ?? ''
    const trimmed = raw.trim()
    if (trimmed === '') {
      previousLineBlank = true
      continue
    }
    if (previousLineBlank) {
      block++
      blockHasHeader = false
      previousLineBlank = false
    }
    lastBlockHasHeader = blockHasHeader

    const header = sectionOfHeader(trimmed)
    if (header !== null) {
      section = header
      blockHasHeader = true
      lastBlockHasHeader = true
      continue
    }
    if (trimmed.startsWith('//') || trimmed.startsWith('#')) continue

    const { line: cleaned, section: lineSection } = preprocessLine(trimmed)
    const targetSection: Section = lineSection ?? section

    if (targetSection === 'ignore') {
      // Arena's About block carries a `Name <deck-name>` metadata line. Grab
      // the first one so the import flow can suggest a deck name.
      if (deckName === undefined) {
        const nameMatch = cleaned.match(/^name\s*:?\s+(.+)$/i)
        if (nameMatch) {
          const value = nameMatch[1]!.trim().replace(/^"(.*)"$/, '$1').trim()
          if (value) deckName = value
        }
      }
      continue
    }

    const match = ENTRY_RE.exec(cleaned)
    if (!match) {
      errors.push({
        line: i + 1,
        raw: trimmed,
        cleaned,
        reason: 'unrecognised line format',
        section: targetSection,
      })
      continue
    }
    const count = parseInt(match[1]!, 10)
    if (!Number.isFinite(count) || count <= 0) {
      errors.push({
        line: i + 1,
        raw: trimmed,
        cleaned,
        reason: 'invalid card count',
        section: targetSection,
      })
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
    switch (targetSection) {
      case 'side': sideboard.push(entry); break
      case 'commander': commander.push(entry); break
      case 'companion': companion.push(entry); break
      case 'excluded': excluded.push(entry); break
      case 'main':
        entries.push(entry)
        mainBlockOf.set(entry, block)
        break
    }
  }

  // The last block's main entries, when it's a headerless block after earlier main-deck blocks.
  const lastMainBlock = entries.length > 0 ? mainBlockOf.get(entries[entries.length - 1]!)! : -1
  const trailingBlock =
    lastMainBlock === block && lastMainBlock > 0 && !lastBlockHasHeader &&
    entries.some((e) => mainBlockOf.get(e)! < lastMainBlock)
      ? entries.filter((e) => mainBlockOf.get(e) === lastMainBlock)
      : []

  const result: ParseResult = { entries, sideboard, commander, companion, excluded, trailingBlock, errors }
  return deckName !== undefined ? { ...result, deckName } : result
}

/** Where [inferCommanders] found commanders an export left implicit. */
export type InferredCommanderSource = 'sideboard' | 'trailing'

/**
 * Resolve a commander an export left implicit. MTGO's text export (and Moxfield's MTGO and plain
 * text exports) carry no `Commander` header: the commander sits under `SIDEBOARD:` or in a block
 * after a final blank line. When [parsed] designates no commander, the deck is commander-shaped,
 * and that sideboard — or failing it, that trailing block — is one or two single copies of cards
 * that can lead the deck (a pair must pair legally, CR 702.124), those cards become the
 * commanders. Otherwise [parsed] comes back unchanged.
 *
 * "Commander-shaped" is [commanderFormat] (the deckbuilder's format is Commander / Brawl) or, when
 * the format is unknown, a list of exactly 100 cards counting the candidates. A 60-card deck with
 * a real 15-card sideboard therefore never qualifies, and neither does a lone legend on a small
 * sideboard of a 60-card deck.
 */
export function inferCommanders(
  parsed: ParseResult,
  options: {
    readonly commanderFormat: boolean
    readonly lookup: (name: string) => CommanderCard | undefined
  },
): { parsed: ParseResult; inferredFrom: InferredCommanderSource | null } {
  const unchanged = { parsed, inferredFrom: null }
  if (parsed.commander.length > 0) return unchanged

  const total = (list: readonly ParsedEntry[]) => list.reduce((sum, e) => sum + e.count, 0)
  const mainTotal = total(parsed.entries)

  const leaders = (candidates: readonly ParsedEntry[]): ParsedEntry[] | null => {
    if (candidates.length < 1 || candidates.length > 2 || candidates.some((e) => e.count !== 1)) return null
    const cards = candidates.map((e) => options.lookup(e.name))
    if (cards.some((c) => c === undefined)) return null
    const [a, b] = cards as CommanderCard[]
    if (b === undefined) return canCardBeCommander(a!) ? [...candidates] : null
    if (!canPairAsCommanders(a!, b)) return null
    // The one that can lead alone goes first (a "Choose a Background" commander before its Background).
    return canCardBeCommander(a!) ? [...candidates] : [candidates[1]!, candidates[0]!]
  }

  // MTGO / Moxfield: commander(s) as the whole sideboard.
  if (options.commanderFormat || mainTotal + total(parsed.sideboard) === 100) {
    const fromSideboard = leaders(parsed.sideboard)
    if (fromSideboard) {
      return {
        parsed: { ...parsed, commander: fromSideboard, sideboard: [] },
        inferredFrom: 'sideboard',
      }
    }
  }

  // Plain text: commander(s) as the trailing block, already counted in the main deck.
  if (options.commanderFormat || mainTotal === 100) {
    const fromTrailing = leaders(parsed.trailingBlock)
    if (fromTrailing) {
      const moved = new Set(parsed.trailingBlock)
      return {
        parsed: {
          ...parsed,
          entries: parsed.entries.filter((e) => !moved.has(e)),
          commander: fromTrailing,
          trailingBlock: [],
        },
        inferredFrom: 'trailing',
      }
    }
  }

  return unchanged
}

/** Most commanders a deck designates: one, or a partner pair (CR 702.124). */
export const MAX_COMMANDERS = 2

/**
 * The commanders an imported list designates: the first two distinct names under its
 * `Commander` section (a partner pair), canonicalised by [canonicalName]. Any further entries
 * aren't designated — they stay ordinary deck cards, which is what the import already merges
 * them into. Server validation decides whether the pair is legal.
 */
export function designatedCommanders(
  commanderEntries: readonly ParsedEntry[],
  canonicalName: (name: string) => string = (name) => name,
): string[] {
  const out: string[] = []
  for (const entry of commanderEntries) {
    const name = canonicalName(entry.name)
    if (!out.includes(name)) out.push(name)
    if (out.length === MAX_COMMANDERS) break
  }
  return out
}

/**
 * A [designatedCommanders] canonicaliser over a card catalogue: the catalogue's own casing for a
 * name it knows (case-insensitively), the name as written otherwise.
 */
export function canonicalNameIn(catalog: readonly { readonly name: string }[]): (name: string) => string {
  return (name) => catalog.find((c) => c.name.toLowerCase() === name.toLowerCase())?.name ?? name
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
  /**
   * Aggregated {name → count} of unmatched entries (cards not in the catalogue).
   * Kept separate so callers can choose whether to include unimplemented cards
   * as placeholder rows in the imported deck.
   */
  unmatchedCards: Record<string, number>
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
  const unmatchedCards: Record<string, number> = {}
  const truncated: ResolveResult['truncated'] = []
  let matchedCards = 0
  let totalCards = 0

  for (const entry of entries) {
    totalCards += entry.count
    const card = byName.get(entry.name.toLowerCase()) ?? null
    if (!card) {
      resolved.push({ entry, match: null })
      // Aggregate unmatched copies under the as-typed name, capped at 4 since
      // we can't tell whether the card is a basic land without a catalogue hit.
      const previous = unmatchedCards[entry.name] ?? 0
      unmatchedCards[entry.name] = Math.min(previous + entry.count, 4)
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
  return { resolved, deckCards, unmatchedCards, matchedCards, totalCards, unmatched, truncated }
}
