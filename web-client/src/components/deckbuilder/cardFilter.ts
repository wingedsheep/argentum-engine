/**
 * cardFilter — query language + predicate builder for the standalone deckbuilder.
 *
 * Pure functions, no React. The deckbuilder grid runs `parseQuery(text)` once
 * per text change and then filters the catalog with the resulting predicate.
 *
 * The same predicate is also fed by the menu-style `FilterPanel`, which works
 * by editing the query string (toggles append/remove tokens), so menu and
 * free-text stay in lockstep.
 *
 * Grammar (Scryfall-flavoured but intentionally narrower):
 *
 *   query    := term (WS+ term)*
 *   term     := '-'? atom              # leading '-' negates
 *   atom     := KEY OP value | bareword
 *   value    := '"' chars '"' | non-whitespace
 *   bareword := value                  # any term without "KEY OP" matches by name substring
 *
 * Supported keys / operators:
 *
 *   name|n       :              substring match (default for bareword)
 *   t|type       :              substring match against cardTypes ∪ supertypes ∪ subtypes
 *   o|oracle     :              substring match against oracle text
 *   c|color      : = <= >=      colour set comparison (letters wubrg, or 'colorless'/'c', 'multicolor'/'m')
 *   cmc|mv       : = != <= >= < >   numeric comparison
 *   pow          : = != <= >= < >   numeric (non-numeric power fails the comparator)
 *   tou          : = != <= >= < >
 *   r|rarity     : =            common/uncommon/rare/mythic (also c/u/r/m)
 *   s|set        : =            set code (case-insensitive)
 *   is           :              special flags: land/creature/instant/sorcery/enchantment/artifact/
 *                               planeswalker/permanent/spell/legendary/basic/token, or any keyword
 *   kw|keyword   :              keyword name (e.g. flying, trample)
 *
 * Unrecognised keys fall back to a name-substring match on the whole term.
 * An empty query matches everything.
 */

// ---------------------------------------------------------------------------
// Card shape — mirrors `DecksController.CardSummaryDTO` on the backend.
// ---------------------------------------------------------------------------

export interface CardSummary {
  name: string
  manaCost: string
  cmc: number
  colors: string[]
  cardTypes: string[]
  supertypes: string[]
  subtypes: string[]
  basicLand: boolean
  rarity: string
  setCode: string | null
  collectorNumber: string | null
  oracleText?: string | null
  power?: string | null
  toughness?: string | null
  imageUri?: string | null
  keywords?: string[]
}

export type CardPredicate = (card: CardSummary) => boolean

// ---------------------------------------------------------------------------
// Tokenisation
// ---------------------------------------------------------------------------

interface Token {
  negate: boolean
  key: string | null
  op: Op
  value: string
}

type Op = ':' | '=' | '!=' | '<=' | '>=' | '<' | '>'

const OPS: Op[] = ['<=', '>=', '!=', ':', '=', '<', '>']

/** Splits a query into terms, respecting double-quoted spans. */
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
      if (buf) {
        out.push(buf)
        buf = ''
      }
      continue
    }
    buf += ch
  }
  if (buf) out.push(buf)
  return out
}

function parseTerm(raw: string): Token | null {
  if (!raw) return null
  let term = raw
  let negate = false
  if (term.startsWith('-') && term.length > 1) {
    negate = true
    term = term.slice(1)
  }

  // Find the earliest operator that is preceded by an identifier-like key.
  // We scan for the longest matching op at each position.
  for (let i = 0; i < term.length; i++) {
    for (const op of OPS) {
      if (term.startsWith(op, i)) {
        const key = term.slice(0, i)
        const value = term.slice(i + op.length)
        if (isIdentifier(key) && value.length > 0) {
          return { negate, key: key.toLowerCase(), op, value: unquote(value) }
        }
      }
    }
  }
  return { negate, key: null, op: ':', value: unquote(term) }
}

function isIdentifier(s: string): boolean {
  return /^[a-zA-Z][a-zA-Z]*$/.test(s)
}

function unquote(s: string): string {
  if (s.length >= 2 && s.startsWith('"') && s.endsWith('"')) {
    return s.slice(1, -1)
  }
  return s
}

// ---------------------------------------------------------------------------
// Predicate construction
// ---------------------------------------------------------------------------

const ALWAYS: CardPredicate = () => true

export function parseQuery(query: string): CardPredicate {
  const trimmed = query.trim()
  if (!trimmed) return ALWAYS

  const preds: CardPredicate[] = []
  for (const raw of splitTerms(trimmed)) {
    const tok = parseTerm(raw)
    if (!tok) continue
    const inner = predicateForToken(tok)
    preds.push(tok.negate ? (c) => !inner(c) : inner)
  }
  if (preds.length === 0) return ALWAYS
  return (c) => preds.every((p) => p(c))
}

function predicateForToken(tok: Token): CardPredicate {
  const { key, op, value } = tok
  switch (key) {
    case null:
    case 'name':
    case 'n':
      return matchName(value)
    case 't':
    case 'type':
      return matchType(value)
    case 'o':
    case 'oracle':
      return matchOracle(value)
    case 'c':
    case 'color':
      return matchColor(op, value)
    case 'cmc':
    case 'mv':
      return matchNumeric(op, value, (c) => c.cmc)
    case 'pow':
      return matchNumeric(op, value, (c) => parseLeadingInt(c.power))
    case 'tou':
      return matchNumeric(op, value, (c) => parseLeadingInt(c.toughness))
    case 'r':
    case 'rarity':
      return matchRarity(value)
    case 's':
    case 'set':
      return matchSet(value)
    case 'is':
      return matchIs(value)
    case 'kw':
    case 'keyword':
      return matchKeyword(value)
    default:
      // Unknown key — fall back to name match on the original key+value pair.
      return matchName(`${key}${op}${value}`)
  }
}

// ---------------------------------------------------------------------------
// Per-key matchers
// ---------------------------------------------------------------------------

const ci = (s: string) => s.toLowerCase()

function matchName(value: string): CardPredicate {
  const needle = ci(value)
  if (!needle) return ALWAYS
  return (c) => ci(c.name).includes(needle)
}

function matchType(value: string): CardPredicate {
  const needle = ci(value)
  if (!needle) return ALWAYS
  return (c) => {
    for (const t of c.cardTypes) if (ci(t).includes(needle)) return true
    for (const t of c.supertypes) if (ci(t).includes(needle)) return true
    for (const t of c.subtypes) if (ci(t).includes(needle)) return true
    return false
  }
}

function matchOracle(value: string): CardPredicate {
  const needle = ci(value)
  if (!needle) return ALWAYS
  return (c) => !!c.oracleText && ci(c.oracleText).includes(needle)
}

const COLOR_LETTER: Record<string, string> = {
  w: 'WHITE',
  u: 'BLUE',
  b: 'BLACK',
  r: 'RED',
  g: 'GREEN',
}

function parseColorSet(value: string): { kind: 'colors'; set: Set<string> } | { kind: 'colorless' } | { kind: 'multi' } | null {
  const v = ci(value)
  if (v === 'c' || v === 'colorless') return { kind: 'colorless' }
  if (v === 'm' || v === 'multi' || v === 'multicolor' || v === 'multicolour') return { kind: 'multi' }
  const set = new Set<string>()
  for (const ch of v) {
    const colour = COLOR_LETTER[ch]
    if (!colour) return null
    set.add(colour)
  }
  return { kind: 'colors', set }
}

function matchColor(op: Op, value: string): CardPredicate {
  const parsed = parseColorSet(value)
  if (!parsed) return () => false

  if (parsed.kind === 'colorless') {
    if (op === '=' || op === ':') return (c) => c.colors.length === 0
    return () => false
  }
  if (parsed.kind === 'multi') {
    if (op === '=' || op === ':') return (c) => c.colors.length >= 2
    return () => false
  }

  const wanted = parsed.set
  switch (op) {
    case ':':
    case '>=':
      // card contains every requested colour
      return (c) => {
        for (const w of wanted) if (!c.colors.includes(w)) return false
        return true
      }
    case '=':
      return (c) => {
        if (c.colors.length !== wanted.size) return false
        for (const w of wanted) if (!c.colors.includes(w)) return false
        return true
      }
    case '<=':
      // every card colour is in the requested set
      return (c) => c.colors.every((col) => wanted.has(col))
    default:
      return () => false
  }
}

function matchNumeric(op: Op, value: string, get: (c: CardSummary) => number | null): CardPredicate {
  const n = Number(value)
  if (!Number.isFinite(n)) return () => false
  return (c) => {
    const v = get(c)
    if (v === null) return false
    switch (op) {
      case ':':
      case '=':
        return v === n
      case '!=':
        return v !== n
      case '<':
        return v < n
      case '<=':
        return v <= n
      case '>':
        return v > n
      case '>=':
        return v >= n
      default:
        return false
    }
  }
}

function parseLeadingInt(s: string | null | undefined): number | null {
  if (!s) return null
  const m = s.match(/^-?\d+/)
  if (!m) return null
  const n = parseInt(m[0], 10)
  return Number.isFinite(n) ? n : null
}

const RARITY_ALIAS: Record<string, string> = {
  c: 'COMMON',
  common: 'COMMON',
  u: 'UNCOMMON',
  uncommon: 'UNCOMMON',
  r: 'RARE',
  rare: 'RARE',
  m: 'MYTHIC',
  mythic: 'MYTHIC',
  s: 'SPECIAL',
  special: 'SPECIAL',
  bonus: 'BONUS',
}

function matchRarity(value: string): CardPredicate {
  const target = RARITY_ALIAS[ci(value)] ?? ci(value).toUpperCase()
  return (c) => c.rarity === target
}

function matchSet(value: string): CardPredicate {
  const target = ci(value)
  if (!target) return ALWAYS
  return (c) => !!c.setCode && ci(c.setCode) === target
}

function matchKeyword(value: string): CardPredicate {
  const target = ci(value)
  if (!target) return ALWAYS
  return (c) => !!c.keywords && c.keywords.some((k) => ci(k) === target)
}

/**
 * `is:` is a kitchen-sink modifier covering common boolean predicates that
 * don't belong to a single field. Falls through to keyword-presence so that
 * `is:flying` works the same as `kw:flying`.
 */
function matchIs(value: string): CardPredicate {
  const v = ci(value)
  switch (v) {
    case 'land':
      return (c) => c.cardTypes.includes('LAND')
    case 'creature':
      return (c) => c.cardTypes.includes('CREATURE')
    case 'instant':
      return (c) => c.cardTypes.includes('INSTANT')
    case 'sorcery':
      return (c) => c.cardTypes.includes('SORCERY')
    case 'enchantment':
      return (c) => c.cardTypes.includes('ENCHANTMENT')
    case 'artifact':
      return (c) => c.cardTypes.includes('ARTIFACT')
    case 'planeswalker':
      return (c) => c.cardTypes.includes('PLANESWALKER')
    case 'kindred':
      return (c) => c.cardTypes.includes('KINDRED')
    case 'permanent':
      return (c) =>
        c.cardTypes.includes('CREATURE') ||
        c.cardTypes.includes('LAND') ||
        c.cardTypes.includes('ARTIFACT') ||
        c.cardTypes.includes('ENCHANTMENT') ||
        c.cardTypes.includes('PLANESWALKER')
    case 'spell':
      return (c) => !c.cardTypes.includes('LAND')
    case 'legendary':
      return (c) => c.supertypes.includes('LEGENDARY')
    case 'basic':
      return (c) => c.basicLand
    case 'colorless':
      return (c) => c.colors.length === 0
    case 'multicolor':
    case 'multicolour':
      return (c) => c.colors.length >= 2
    case 'monocolor':
    case 'monocolour':
      return (c) => c.colors.length === 1
    default:
      return matchKeyword(v)
  }
}

// ---------------------------------------------------------------------------
// Menu ↔ query helpers
//
// FilterPanel toggles call these to keep the text query in sync with the menu
// state. They operate on the raw string so a user typing "t:creature -t:goblin"
// can still see the menu reflect the dominant choices.
// ---------------------------------------------------------------------------

/**
 * Adds a token if absent, otherwise returns the query unchanged. Tokens are
 * compared as whole, whitespace-separated terms.
 */
export function addToken(query: string, token: string): string {
  const terms = splitTerms(query)
  if (terms.includes(token)) return query
  return terms.length === 0 ? token : `${terms.join(' ')} ${token}`
}

/** Removes a token if present. */
export function removeToken(query: string, token: string): string {
  const terms = splitTerms(query).filter((t) => t !== token)
  return terms.join(' ')
}

/** True iff the query contains the token as a whole term. */
export function hasToken(query: string, token: string): boolean {
  return splitTerms(query).includes(token)
}

/**
 * Adds the token if absent, removes it if present. Convenience for menu
 * checkboxes.
 */
export function toggleToken(query: string, token: string): string {
  return hasToken(query, token) ? removeToken(query, token) : addToken(query, token)
}
