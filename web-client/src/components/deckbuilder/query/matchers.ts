/**
 * Matcher registry — the swappable part of the query language.
 *
 * Every supported key (`name`, `c`, `t`, `mana`, `pow`, `is`, …) is one entry
 * in `MATCHERS`. A matcher receives the parsed atom and returns either a
 * predicate or a typed error. Adding an operator is a single registry entry,
 * never a parser edit.
 *
 * Conventions:
 *   - Matchers are pure: same atom in → same predicate out.
 *   - Errors describe what was wrong with the value, not where (the
 *     evaluator attaches the atom span).
 *   - Unknown keys are NOT handled here; the evaluator dispatches them with
 *     a "did you mean" suggestion against the known-key set.
 */
import type { AtomNode, CardPredicate, Op } from './types'
import type { CardSummary } from '../cardFilter'
import { COLOR_LETTER, parseColorValue } from './colorSugar'
import { manaCostPredicate } from './manaCost'
import { playableWithinColors } from '@/utils/manaCost'

/** Internal-representation colour names → single-letter pips used by the mana-cost helpers. */
const LETTER_OF_COLOR: Record<string, string> = {
  WHITE: 'W', BLUE: 'U', BLACK: 'B', RED: 'R', GREEN: 'G',
}

export type MatcherResult =
  | { kind: 'ok'; predicate: CardPredicate }
  | { kind: 'error'; message: string; suggestion?: string }

export interface Matcher {
  /** Aliases registered for the same matcher (e.g. `c`, `color`, `id`, `identity`). */
  aliases: string[]
  /** Operators this matcher supports. The evaluator rejects unsupported ops up front. */
  ops: Op[]
  /** Compile the atom into a runtime predicate. */
  build: (atom: AtomNode) => MatcherResult
  /** Short human-readable description for the help panel and "did you mean" output. */
  description: string
}

const ok = (p: CardPredicate): MatcherResult => ({ kind: 'ok', predicate: p })
const err = (message: string, suggestion?: string): MatcherResult =>
  suggestion ? { kind: 'error', message, suggestion } : { kind: 'error', message }

const ALWAYS_FALSE: CardPredicate = () => false

const lc = (s: string) => s.toLowerCase()

// ---------------------------------------------------------------------------
// Numeric helpers (cmc / pow / tou / loyalty + cross-field comparisons)
// ---------------------------------------------------------------------------

type NumericFieldGetter = (c: CardSummary) => number | null

const FIELDS: Record<string, NumericFieldGetter> = {
  cmc: (c) => c.cmc,
  mv: (c) => c.cmc,
  pow: (c) => parseLeadingInt(c.power),
  tou: (c) => parseLeadingInt(c.toughness),
  // We don't yet expose loyalty as a separate field on CardSummary, so `loy`
  // is a no-op for now. Listed here so cross-field compares still parse.
  loy: () => null,
}

function parseLeadingInt(s: string | null | undefined): number | null {
  if (!s) return null
  const m = s.match(/^-?\d+/)
  if (!m) return null
  const n = parseInt(m[0], 10)
  return Number.isFinite(n) ? n : null
}

function numericCompare(op: Op, a: number, b: number): boolean {
  switch (op) {
    case ':':
    case '=':
      return a === b
    case '!=':
      return a !== b
    case '<':
      return a < b
    case '<=':
      return a <= b
    case '>':
      return a > b
    case '>=':
      return a >= b
    default:
      return false
  }
}

/**
 * Build a numeric matcher. The value may be either a literal integer or the
 * name of another numeric field (`pow>tou`, `pow=cmc`). Cross-field compares
 * are common enough on Scryfall that supporting them is table-stakes.
 */
function buildNumeric(field: NumericFieldGetter): Matcher['build'] {
  return (atom) => {
    const v = lc(atom.value).trim()
    const fieldRef = FIELDS[v]
    if (fieldRef) {
      return ok((c) => {
        const left = field(c)
        const right = fieldRef(c)
        if (left === null || right === null) return false
        return numericCompare(atom.op, left, right)
      })
    }
    const n = Number(v)
    if (!Number.isFinite(n)) {
      return err(`Expected a number or field reference (cmc/pow/tou/loy), got "${atom.value}".`)
    }
    return ok((c) => {
      const left = field(c)
      if (left === null) return false
      return numericCompare(atom.op, left, n)
    })
  }
}

// ---------------------------------------------------------------------------
// Name / oracle / type
// ---------------------------------------------------------------------------

/** The oracle name plus every printed name — what a name atom is matched against. */
export function cardNames(c: CardSummary): string[] {
  const printed = c.printedNamePrintings
  return printed && printed.length > 0 ? [c.name, ...printed.map((p) => p.name)] : [c.name]
}

/**
 * The string test a name atom applies (regex, `!exact`, or substring), or null when the atom is
 * malformed. Shared by the name matcher and the printed-name art selection.
 */
export function nameTest(atom: AtomNode): ((s: string) => boolean) | null {
  if (atom.regex) {
    try {
      const re = new RegExp(atom.value, atom.regexFlags || 'i')
      return (s) => re.test(s)
    } catch {
      return null
    }
  }
  const needle = lc(atom.value)
  if (atom.exact) return (s) => lc(s) === needle
  return (s) => lc(s).includes(needle)
}

function buildName(target: 'name' | 'oracle'): Matcher['build'] {
  return (atom) => {
    const get = (c: CardSummary): string[] => {
      if (target === 'name') return cardNames(c)
      return [c.oracleText ?? '']
    }
    if (atom.regex) {
      let re: RegExp
      try {
        // Scryfall's `i` is implicit by default for free-text searches; we
        // mirror that — `name:/.../` is case-insensitive unless the user
        // explicitly passes flags.
        const flags = atom.regexFlags || 'i'
        re = new RegExp(atom.value, flags)
      } catch (e) {
        return err(`Invalid regex: ${(e as Error).message}.`)
      }
      return ok((c) => get(c).some((s) => re.test(s)))
    }
    if (atom.exact && target === 'name') {
      const want = lc(atom.value)
      return ok((c) => get(c).some((s) => lc(s) === want))
    }
    const needle = lc(atom.value)
    if (!needle) return ok(() => true)
    // Oracle and name use simple substring — Scryfall does the same. Word-
    // boundary matching gets surprising fast (`o:fly` should still find
    // "flying").
    return ok((c) => get(c).some((s) => lc(s).includes(needle)))
  }
}

const TYPE_MATCHER: Matcher['build'] = (atom) => {
  // Scryfall's `t:` AND-matches space-separated words against the full type
  // line (`t:legendary creature elf` finds Llanowar Elves). We approximate
  // by AND-ing each word against the union of card types / supertypes /
  // subtypes — close enough for our catalog where the type line is just
  // the union joined by spaces.
  const words = lc(atom.value).split(/\s+/).filter((w) => w.length > 0)
  if (words.length === 0) return ok(() => true)
  return ok((c) => {
    const all = [...c.cardTypes, ...c.supertypes, ...c.subtypes].map(lc)
    for (const w of words) {
      if (!all.some((t) => t.includes(w))) return false
    }
    return true
  })
}

// ---------------------------------------------------------------------------
// Color (identity / cost) + numeric color count
// ---------------------------------------------------------------------------

/**
 * @param atMostByCastability  When true, the `<=` operator means "playable in a
 *   deck limited to these colours" rather than a plain subset test, so hybrid
 *   pips (`{R/W}`) survive a single-colour "at most". Only the colour-identity
 *   matcher opts in; `cost:` keeps literal printed-colour subset semantics.
 */
function buildColor(
  get: (c: CardSummary) => string[],
  atMostByCastability = false,
): Matcher['build'] {
  return (atom) => {
    const parsed = parseColorValue(atom.value)
    if (parsed.kind === 'error') return err(parsed.message)
    if (parsed.kind === 'count') {
      const n = parsed.value
      return ok((c) => numericCompare(atom.op, get(c).length, n))
    }
    if (parsed.kind === 'colorless') {
      if (atom.op === '=' || atom.op === ':') return ok((c) => get(c).length === 0)
      return ok(ALWAYS_FALSE)
    }
    if (parsed.kind === 'multi') {
      if (atom.op === '=' || atom.op === ':') return ok((c) => get(c).length >= 2)
      return ok(ALWAYS_FALSE)
    }
    if (parsed.kind === 'mono') {
      if (atom.op === '=' || atom.op === ':') return ok((c) => get(c).length === 1)
      return ok(ALWAYS_FALSE)
    }
    const wanted = parsed.set
    switch (atom.op) {
      case ':':
      case '>=':
        return ok((c) => {
          const cs = get(c)
          for (const w of wanted) if (!cs.includes(w)) return false
          return true
        })
      case '=':
        return ok((c) => {
          const cs = get(c)
          if (cs.length !== wanted.size) return false
          for (const w of wanted) if (!cs.includes(w)) return false
          return true
        })
      case '<=':
        if (atMostByCastability) {
          const allowed = new Set([...wanted].map((n) => LETTER_OF_COLOR[n] ?? n))
          return ok((c) => {
            const identity = new Set(get(c).map((n) => LETTER_OF_COLOR[n] ?? n))
            return playableWithinColors(c.manaCost, identity, allowed)
          })
        }
        return ok((c) => get(c).every((col) => wanted.has(col)))
      case '>':
        return ok((c) => {
          const cs = get(c)
          for (const w of wanted) if (!cs.includes(w)) return false
          return cs.length > wanted.size
        })
      case '<':
        return ok((c) => {
          const cs = get(c)
          if (cs.length >= wanted.size) return false
          return cs.every((col) => wanted.has(col))
        })
      default:
        return err(`Unsupported operator "${atom.op}" on color.`)
    }
  }
}

// ---------------------------------------------------------------------------
// `is:` / layout / format / set / rarity / keyword
// ---------------------------------------------------------------------------

const RARITY_ALIAS: Record<string, string> = {
  c: 'COMMON', common: 'COMMON',
  u: 'UNCOMMON', uncommon: 'UNCOMMON',
  r: 'RARE', rare: 'RARE',
  m: 'MYTHIC', mythic: 'MYTHIC',
  s: 'SPECIAL', special: 'SPECIAL',
  bonus: 'BONUS',
}

/**
 * Ordinal rank for `r>=...` / `r<=...` comparators (mirrors Scryfall's
 * common < uncommon < rare < mythic ordering). Non-standard rarities sit
 * outside the normal range so they don't accidentally sort into the middle.
 */
const RARITY_RANK: Record<string, number> = {
  COMMON: 1, UNCOMMON: 2, RARE: 3, MYTHIC: 4, SPECIAL: 5, BONUS: 6,
}

const RARITY: Matcher['build'] = (atom) => {
  const target = RARITY_ALIAS[lc(atom.value)] ?? lc(atom.value).toUpperCase()
  switch (atom.op) {
    case ':':
    case '=':
      return ok((c) => c.rarity === target)
    case '!=':
      return ok((c) => c.rarity !== target)
    case '<':
    case '<=':
    case '>':
    case '>=': {
      const wantedRank = RARITY_RANK[target]
      if (wantedRank === undefined) {
        return err(`Cannot order "${atom.value}" — known rarities are common/uncommon/rare/mythic.`)
      }
      return ok((c) => {
        const rank = RARITY_RANK[c.rarity]
        if (rank === undefined) return false
        return numericCompare(atom.op, rank, wantedRank)
      })
    }
    default:
      return err(`Unsupported operator "${atom.op}" on rarity.`)
  }
}

const SET: Matcher['build'] = (atom) => {
  const target = lc(atom.value).trim()
  if (!target) return ok(() => true)
  // Match the canonical printing's set OR any reprint set the card has a printing in,
  // so `s:EOE Banishing Light` works for cards reprinted in EOE even though their
  // canonical setCode points at the original printing (BLB).
  return ok((c) => {
    if (c.setCode && lc(c.setCode) === target) return true
    if (c.printingSetCodes) {
      for (const code of c.printingSetCodes) {
        if (lc(code) === target) return true
      }
    }
    return false
  })
}

const FORMAT: Matcher['build'] = (atom) => {
  const target = atom.value.toUpperCase().trim()
  if (!target) return ok(() => true)
  return ok((c) => !!c.legalFormats && c.legalFormats.includes(target))
}

const KEYWORD: Matcher['build'] = (atom) => {
  const target = lc(atom.value).replace(/\s+/g, '_')
  if (!target) return ok(() => true)
  return ok((c) => !!c.keywords && c.keywords.some((k) => lc(k) === target))
}

const LAYOUT: Matcher['build'] = (atom) => {
  const v = lc(atom.value)
  switch (v) {
    case 'transform':
    case 'mdfc':
    case 'modal_dfc':
    case 'dfc':
    case 'doublefaced':
    case 'double_faced':
      return ok((c) => !!c.isDoubleFaced)
    case 'normal':
      return ok((c) => !c.isDoubleFaced)
    default:
      return err(`Unknown layout "${atom.value}". Try transform / mdfc / normal.`)
  }
}

/**
 * Scryfall `is:` flags we recognise but can't evaluate yet because the
 * underlying data isn't on `CardSummary`. We emit a clear error rather than
 * silently falling through to keyword lookup — silent fallthrough is the
 * antipattern we're rewriting the language to fix.
 */
const IS_KNOWN_UNSUPPORTED: Record<string, string> = {
  reprint: 'print metadata not stamped on the catalog',
  firstprint: 'print metadata not stamped on the catalog',
  unique: 'print metadata not stamped on the catalog',
  hybrid: 'mana-symbol metadata not parsed',
  phyrexian: 'mana-symbol metadata not parsed',
  split: 'split-card layout not modelled',
  flip: 'flip-card layout not modelled',
  meld: 'meld-card layout not modelled',
  meldpart: 'meld-card layout not modelled',
  meldresult: 'meld-card layout not modelled',
  leveler: 'leveler-card layout not modelled',
  modal: 'modal-spell metadata not surfaced',
  party: 'party-tribe metadata not surfaced',
  outlaw: 'outlaw-supertypes metadata not surfaced',
  manland: 'land-cycle metadata not surfaced',
  bikeland: 'land-cycle metadata not surfaced',
  cycleland: 'land-cycle metadata not surfaced',
  checkland: 'land-cycle metadata not surfaced',
  shockland: 'land-cycle metadata not surfaced',
  fetchland: 'land-cycle metadata not surfaced',
  painland: 'land-cycle metadata not surfaced',
  scryland: 'land-cycle metadata not surfaced',
  fastland: 'land-cycle metadata not surfaced',
  slowland: 'land-cycle metadata not surfaced',
  filterland: 'land-cycle metadata not surfaced',
  gainland: 'land-cycle metadata not surfaced',
  triland: 'land-cycle metadata not surfaced',
  triome: 'land-cycle metadata not surfaced',
  creatureland: 'land-cycle metadata not surfaced',
  dual: 'land-cycle metadata not surfaced',
  funny: 'set-style metadata not surfaced',
  digital: 'print game-mode not surfaced',
  alchemy: 'print game-mode not surfaced',
  rebalanced: 'print game-mode not surfaced',
  promo: 'print metadata not surfaced',
  spotlight: 'print metadata not surfaced',
  scryfallpreview: 'print metadata not surfaced',
  foil: 'physical printing not surfaced',
  nonfoil: 'physical printing not surfaced',
  etched: 'physical printing not surfaced',
  glossy: 'physical printing not surfaced',
  hires: 'image metadata not surfaced',
  universesbeyond: 'set-style metadata not surfaced',
  default: 'print-prefer metadata not surfaced',
  atypical: 'print-prefer metadata not surfaced',
  old: 'border metadata not surfaced',
  new: 'border metadata not surfaced',
  datestamped: 'print metadata not surfaced',
  booster: 'set metadata not surfaced',
  league: 'set metadata not surfaced',
  buyabox: 'set metadata not surfaced',
  giftbox: 'set metadata not surfaced',
  intro_pack: 'set metadata not surfaced',
  gameday: 'set metadata not surfaced',
  prerelease: 'set metadata not surfaced',
  release: 'set metadata not surfaced',
  fnm: 'set metadata not surfaced',
  judge_gift: 'set metadata not surfaced',
  arena_league: 'set metadata not surfaced',
  player_rewards: 'set metadata not surfaced',
  media_insert: 'set metadata not surfaced',
  instore: 'set metadata not surfaced',
  convention: 'set metadata not surfaced',
  set_promo: 'set metadata not surfaced',
  planeswalker_deck: 'set metadata not surfaced',
  commander: 'ambiguous (use t:legendary t:creature instead)',
  brawler: 'format-eligibility metadata not surfaced',
  companion: 'format-eligibility metadata not surfaced',
  duelcommander: 'format-eligibility metadata not surfaced',
  oathbreaker: 'format-eligibility metadata not surfaced',
  partner: 'format-eligibility metadata not surfaced',
  gamechanger: 'format-eligibility metadata not surfaced',
  reserved: 'reserved-list metadata not surfaced',
  tdfc: 'DFC layout subtype not surfaced (use is:dfc)',
  newinpauper: 'format-eligibility delta not surfaced',
}

const IS: Matcher['build'] = (atom) => {
  const v = lc(atom.value)
  if (IS_KNOWN_UNSUPPORTED[v]) {
    return err(`\`is:${v}\` is not yet supported (${IS_KNOWN_UNSUPPORTED[v]}).`)
  }
  switch (v) {
    case 'land':         return ok((c) => c.cardTypes.includes('LAND'))
    case 'creature':     return ok((c) => c.cardTypes.includes('CREATURE'))
    case 'instant':      return ok((c) => c.cardTypes.includes('INSTANT'))
    case 'sorcery':      return ok((c) => c.cardTypes.includes('SORCERY'))
    case 'enchantment':  return ok((c) => c.cardTypes.includes('ENCHANTMENT'))
    case 'artifact':     return ok((c) => c.cardTypes.includes('ARTIFACT'))
    case 'planeswalker': return ok((c) => c.cardTypes.includes('PLANESWALKER'))
    case 'kindred':      return ok((c) => c.cardTypes.includes('KINDRED'))
    case 'permanent':    return ok((c) =>
      c.cardTypes.includes('CREATURE') ||
      c.cardTypes.includes('LAND') ||
      c.cardTypes.includes('ARTIFACT') ||
      c.cardTypes.includes('ENCHANTMENT') ||
      c.cardTypes.includes('PLANESWALKER'))
    case 'spell':        return ok((c) => !c.cardTypes.includes('LAND'))
    case 'legendary':    return ok((c) => c.supertypes.includes('LEGENDARY'))
    case 'basic':        return ok((c) => c.basicLand)
    case 'colorless':    return ok((c) => c.colorIdentity.length === 0)
    case 'multicolor':
    case 'multicolour':  return ok((c) => c.colorIdentity.length >= 2)
    case 'monocolor':
    case 'monocolour':
    case 'mono':         return ok((c) => c.colorIdentity.length === 1)
    case 'dfc':
    case 'mdfc':
    case 'transform':
    case 'doublefaced':
    case 'double_faced': return ok((c) => !!c.isDoubleFaced)
    // "Vanilla" creatures: no oracle text. Useful for combo searches and
    // archetype identification.
    case 'vanilla':      return ok((c) => c.cardTypes.includes('CREATURE') && !(c.oracleText && c.oracleText.trim()))
    // "French vanilla": only keyword abilities. We approximate by "has at
    // least one keyword and oracle text is short" — exact detection would
    // need parsed oracle structure we don't ship to the client.
    case 'frenchvanilla':
    case 'french_vanilla': return ok((c) => c.cardTypes.includes('CREATURE') && !!c.keywords && c.keywords.length > 0)
    // Bears: Scryfall folklore for 2/2 creatures with CMC 2.
    case 'bear':         return ok((c) =>
      c.cardTypes.includes('CREATURE') &&
      c.cmc === 2 &&
      parseLeadingInt(c.power) === 2 &&
      parseLeadingInt(c.toughness) === 2)
    case 'historic':     return ok((c) =>
      c.supertypes.includes('LEGENDARY') ||
      c.cardTypes.includes('ARTIFACT') ||
      c.subtypes.includes('SAGA'))
    default:
      // Any unrecognised `is:X` falls back to keyword presence — `is:flying`
      // ≡ `kw:flying`. This mirrors the Scryfall fallthrough.
      return KEYWORD({ ...atom, value: v })
  }
}

// ---------------------------------------------------------------------------
// Mana cost
// ---------------------------------------------------------------------------

const MANA: Matcher['build'] = (atom) => {
  return ok(manaCostPredicate(atom.op, atom.value))
}

// ---------------------------------------------------------------------------
// Registry
// ---------------------------------------------------------------------------

const NUMERIC_OPS: Op[] = [':', '=', '!=', '<=', '>=', '<', '>']
const COLOR_OPS: Op[] = [':', '=', '<=', '>=', '<', '>']
const STRING_OPS: Op[] = [':']

const ENTRIES: Matcher[] = [
  { aliases: ['name', 'n'], ops: STRING_OPS, build: buildName('name'), description: 'Card name (substring or regex)' },
  { aliases: ['oracle', 'o'], ops: STRING_OPS, build: buildName('oracle'), description: 'Oracle text contains' },
  { aliases: ['type', 't'], ops: STRING_OPS, build: TYPE_MATCHER, description: 'Type line (AND of words)' },
  { aliases: ['c', 'color', 'colour', 'id', 'identity'], ops: COLOR_OPS, build: buildColor((c) => c.colorIdentity, true), description: 'Color identity (CR 903.4)' },
  { aliases: ['cost'], ops: COLOR_OPS, build: buildColor((c) => c.colors), description: 'Printed mana-cost colors' },
  { aliases: ['mana', 'm'], ops: NUMERIC_OPS, build: MANA, description: 'Mana cost symbols (multiset compare)' },
  { aliases: ['cmc', 'mv', 'manavalue'], ops: NUMERIC_OPS, build: buildNumeric((c) => c.cmc), description: 'Mana value' },
  { aliases: ['pow', 'power'], ops: NUMERIC_OPS, build: buildNumeric((c) => parseLeadingInt(c.power)), description: 'Power (numeric only)' },
  { aliases: ['tou', 'toughness'], ops: NUMERIC_OPS, build: buildNumeric((c) => parseLeadingInt(c.toughness)), description: 'Toughness (numeric only)' },
  { aliases: ['loy', 'loyalty'], ops: NUMERIC_OPS, build: buildNumeric(() => null), description: 'Loyalty (not surfaced yet)' },
  { aliases: ['r', 'rarity'], ops: NUMERIC_OPS, build: RARITY, description: 'Rarity (ordinal: common < uncommon < rare < mythic)' },
  { aliases: ['s', 'set', 'e', 'edition'], ops: STRING_OPS, build: SET, description: 'Set code' },
  { aliases: ['f', 'format', 'legal'], ops: STRING_OPS, build: FORMAT, description: 'Format legality' },
  { aliases: ['kw', 'keyword'], ops: STRING_OPS, build: KEYWORD, description: 'Keyword ability' },
  { aliases: ['layout'], ops: STRING_OPS, build: LAYOUT, description: 'Card layout (transform / mdfc / normal)' },
  { aliases: ['is'], ops: STRING_OPS, build: IS, description: 'Boolean flag (land / creature / vanilla / dfc / …)' },
]

export const MATCHERS: Map<string, Matcher> = (() => {
  const m = new Map<string, Matcher>()
  for (const entry of ENTRIES) {
    for (const a of entry.aliases) m.set(a, entry)
  }
  return m
})()

export const ALL_KEYS: string[] = (() => {
  const seen = new Set<string>()
  for (const entry of ENTRIES) for (const a of entry.aliases) seen.add(a)
  return [...seen].sort()
})()

/** Suggest the closest known key by Levenshtein distance. */
export function suggestKey(unknown: string): string | undefined {
  let best: string | undefined
  let bestDist = Infinity
  for (const k of ALL_KEYS) {
    const d = levenshtein(unknown, k)
    if (d < bestDist) { bestDist = d; best = k }
  }
  // Only suggest if it's a plausible typo, not a wildly different word.
  return bestDist <= 2 ? best : undefined
}

function levenshtein(a: string, b: string): number {
  if (a === b) return 0
  if (a.length === 0) return b.length
  if (b.length === 0) return a.length
  const prev: number[] = Array.from({ length: b.length + 1 }, (_, i) => i)
  const curr: number[] = new Array(b.length + 1).fill(0)
  for (let i = 1; i <= a.length; i++) {
    curr[0] = i
    for (let j = 1; j <= b.length; j++) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1
      curr[j] = Math.min((curr[j - 1] ?? 0) + 1, (prev[j] ?? 0) + 1, (prev[j - 1] ?? 0) + cost)
    }
    for (let j = 0; j <= b.length; j++) prev[j] = curr[j] ?? 0
  }
  return prev[b.length] ?? 0
}

// Suppress “unused” complaint when COLOR_LETTER isn't referenced after the
// colorSugar export. Keeping the import side-effect makes the registry the
// natural single entry point for tests/tooling that want a list of color
// letters in lockstep with the matcher behaviour.
void COLOR_LETTER
