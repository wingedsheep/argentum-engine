/**
 * Deck share-link codec — encode a deck into a compact, URL-safe string and back.
 *
 * Sharing is entirely client-side: the whole deck travels inside the link, so there's no
 * server storage, no token to expire, and a link works as long as the cards still exist in
 * the catalog. This mirrors how the deckbuilder already keeps filter/view state in the URL —
 * the deck just rides along in a `?d=` param.
 *
 * ## Identifying cards by printing, not name
 * A card's `(setCode, collectorNumber)` is its canonical Magic identity and is far shorter than
 * its name — so the v2 wire shape stores printings, not names, and reconstructs the name on
 * decode by looking the printing up in the catalog. Real card names are long and varied (they
 * don't DEFLATE away), so this roughly halves the link versus carrying names; combined with the
 * structural packing below a typical Commander deck drops ~70% (e.g. ~1250 → ~380 chars).
 *
 * Because the codec itself has no catalog, both directions take a resolver callback:
 * [encodeSharedDeck] is handed `name → PrintingRef` (the card's catalog-default printing) and
 * [decodeSharedDeck] is handed `PrintingRef → name`. The caller (the deckbuilder) builds these
 * from `/api/cards`.
 *
 * ## Wire shape (v2)
 * A control-char-delimited text payload — the data has exactly one structure, so a fixed layout
 * beats JSON's per-field labelling. Records are split by RS (`\x1e`), fields within a record by
 * US (`\x1f`), and groups within the compact section by GS (`\x1d`). Card and deck names never
 * contain ASCII control chars, so nothing needs escaping (the deck name is sanitised on encode
 * to guarantee this).
 *
 * ```
 *  rec 0  meta       :  "2"  US  deckName  US  format
 *  rec 1  commander  :  cmd  [ GS cmd ]                                 (a partner pair adds a second, GS-split)
 *                       cmd = name  |  name US setCode US collector         (name kept verbatim — see below)
 *                       Empty when the deck has no commander. A single commander never contains GS,
 *                       so links made before partner pairs decode unchanged.
 *  rec 2  compact    :  group  GS  group  GS …
 *                       group = setCode  US  token  token …
 *                       token = <collector>[:count]   (":count" only when count > 1)
 *                       Within a group, integer collectors are delta-encoded against the previous
 *                       integer (sorted ascending, first delta from 0); non-integer collectors
 *                       (e.g. "12a", "★7") are written verbatim and don't touch the delta chain.
 *  rec 3+ overflow   :  name US count US setCode US collector   (a pinned *reprint* — non-default printing)
 *                    |  name US count                            (a card the catalog can't resolve)
 * ```
 *
 * The compact section holds the common case: a card sitting at its catalog-default printing, so
 * the name is recoverable from `(set, collector)` and need not be stored. The overflow records
 * carry the name for the two cases `(set, collector)` can't round-trip: an explicit pin to a
 * *different* printing than the default (a reprint the catalog's reverse index doesn't point at),
 * and a card missing from the catalog entirely (e.g. catalog still loading at encode time). The
 * commander likewise keeps its name verbatim — it's a single card and the deck's identity, so we
 * never want it to silently drop if its printing can't be resolved.
 *
 * The text is then UTF-8 encoded, DEFLATE-compressed, and base64url-encoded for the URL — the
 * same transport v1 used. Native deflate-raw beats brotli/zstd on payloads this small (their
 * framing overhead dominates), and a preset dictionary buys too little to justify a JS deflate
 * dependency, so the browser-native `CompressionStream('deflate-raw')` stays. The codec is
 * therefore **async**.
 *
 * [decodeSharedDeck] treats its input as fully untrusted (it came from a URL someone pasted):
 * it never throws, validates every field, drops malformed rows, and returns `null` rather than a
 * half-built deck when the payload isn't usable. It also still reads legacy v1 (JSON, name-keyed)
 * codes so old links keep working.
 */
import type { PrintingRef } from '@/types'

/**
 * The shareable subset of a deck. A near-identity slice of the persisted `SavedDeck`
 * (minus library bookkeeping like `id` / `updatedAt`), so it round-trips to/from both the
 * deckbuilder's working state and a saved deck with no lossy mapping.
 */
export interface SharedDeck {
  name: string
  /** Card name → copies. Excludes the commander (which lives in the command zone, CR 903.6a). */
  cards: Record<string, number>
  /** Optional deck-construction format (e.g. `STANDARD`, `COMMANDER`). */
  format?: string
  /** Optional designated commander name. */
  commander?: string
  /** Optional pinned printing for the commander. */
  commanderPrinting?: PrintingRef
  /**
   * Optional second commander — the partner of [commander] (Partner, Partner with, Choose a
   * Background, Doctor's companion; CR 702.124). Only meaningful when `commander` is set.
   */
  secondCommander?: string
  /** Optional pinned printing for the second commander. */
  secondCommanderPrinting?: PrintingRef
  /** Optional sparse per-card printing pins, keyed by card name. */
  printings?: Record<string, PrintingRef>
  /**
   * Optional constructed sideboard ("outside the game", CR 400.11a), card name → copies.
   *
   * Carried by the account (cloud) path only, which stores this object as JSON verbatim. The v2
   * *share code* below does not encode it — its compact framing has no field for one — so a deck
   * shared by link arrives without its sideboard.
   */
  sideboard?: Record<string, number>
  /**
   * Optional companion (CR 702.139) — the sideboard card the deck reveals. Unlike the rest of the
   * sideboard the share code does carry it (a fourth field on the header record, which older
   * decoders ignore); a decoded companion comes back with one copy on `sideboard`.
   */
  companion?: string
  /**
   * Optional owner's note and chosen cover art. Carried by the account save (stored verbatim);
   * the share-link codec has no field for them and drops them — a shared deck is the list.
   */
  note?: string
  coverCard?: string
  coverImageUri?: string
}

/**
 * Resolve a card name to its catalog-default printing, or `null` when the catalog doesn't know
 * the card. Supplied by the caller (the deckbuilder reads it off `/api/cards`).
 */
export type ResolvePrinting = (name: string) => PrintingRef | null

/**
 * Resolve a `(setCode, collectorNumber)` back to a card name, or `null` when the catalog has no
 * such printing. Inverse of [ResolvePrinting]; supplied by the caller.
 */
export type ResolveName = (printing: PrintingRef) => string | null

/** Query-param key the deckbuilder reads/writes a share code under. */
export const SHARE_PARAM = 'd'

// Record / field / group separators. ASCII control chars never appear in card or (sanitised)
// deck names, so the delimited payload needs no escaping.
const RS = '\x1e'
const US = '\x1f'
const GS = '\x1d'

/** v2 payloads begin with this char; legacy v1 (JSON) payloads begin with `{`. */
const V2_TAG = '2'

// --- base64url <-> bytes (Unicode-safe via TextEncoder/TextDecoder) ------------------------

function bytesToBase64Url(bytes: Uint8Array): string {
  let binary = ''
  const CHUNK = 0x8000 // chunk so we don't blow the call-stack on String.fromCharCode(...spread)
  for (let i = 0; i < bytes.length; i += CHUNK) {
    binary += String.fromCharCode(...bytes.subarray(i, i + CHUNK))
  }
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '')
}

function base64UrlToBytes(code: string): Uint8Array {
  const b64 = code.replace(/-/g, '+').replace(/_/g, '/')
  const pad = b64.length % 4 === 0 ? '' : '='.repeat(4 - (b64.length % 4))
  const binary = atob(b64 + pad)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) bytes[i] = binary.charCodeAt(i)
  return bytes
}

// --- DEFLATE via the native (Web) compression streams --------------------------------------

async function pumpThroughStream(
  bytes: Uint8Array,
  stream: GenericTransformStream,
): Promise<Uint8Array> {
  const writer = stream.writable.getWriter()
  // Swallow the writer-side promises: when the input is invalid (e.g. inflating a
  // non-deflate legacy code), both the writable and readable ends reject. We surface the
  // failure through the readable end below; without these `.catch` guards the writer-side
  // rejections would float as unhandled.
  void writer.write(bytes).catch(() => {})
  void writer.close().catch(() => {})
  return new Uint8Array(await new Response(stream.readable).arrayBuffer())
}

// 'deflate-raw' drops the 2-byte zlib header + checksum the plain 'deflate' format carries —
// every byte counts when the result rides in a URL. Cast because some TS lib versions still
// type CompressionFormat without 'deflate-raw'; it's supported by every engine that ships the
// streams (Chrome 103+, Firefox 113+, Safari 16.4+, Node 18+).
const RAW_DEFLATE = 'deflate-raw' as CompressionFormat

const deflate = (bytes: Uint8Array): Promise<Uint8Array> =>
  pumpThroughStream(bytes, new CompressionStream(RAW_DEFLATE))
const inflate = (bytes: Uint8Array): Promise<Uint8Array> =>
  pumpThroughStream(bytes, new DecompressionStream(RAW_DEFLATE))

// --- collector-number helpers --------------------------------------------------------------

// A collector number is "integer-like" only when it round-trips through parseInt unchanged —
// so "12" qualifies but "012" (leading zero) and "12a" don't. Only integer-like collectors are
// delta-encoded; everything else is stored verbatim, which keeps reverse-lookup byte-exact.
function asInteger(collector: string): number | null {
  if (!/^\d+$/.test(collector)) return null
  const n = Number.parseInt(collector, 10)
  return String(n) === collector ? n : null
}

// Strip the control chars we use as delimiters out of free text (the user-entered deck name),
// so a pathological name can't corrupt the payload framing. Card names never contain them.
function sanitize(text: string): string {
  // eslint-disable-next-line no-control-regex
  return text.replace(/[\x1d\x1e\x1f]/g, ' ')
}

// --- encode --------------------------------------------------------------------------------

/**
 * Encode a deck into a URL-safe share code. Inverse of [decodeSharedDeck]. `resolvePrinting`
 * maps a card name to its catalog-default printing; cards it can't resolve fall back to carrying
 * their name in the payload, so a deck still shares (just larger) when the catalog is incomplete.
 */
export async function encodeSharedDeck(
  deck: SharedDeck,
  resolvePrinting: ResolvePrinting,
): Promise<string> {
  const pins = deck.printings ?? {}

  // Cards whose stored printing is the catalog default → compact section, grouped by set.
  // Cards with a reprint pin or no resolvable printing → overflow rows that carry the name.
  const bySet = new Map<string, Array<{ collector: string; count: number }>>()
  const overflow: string[] = []

  for (const [name, count] of Object.entries(deck.cards)) {
    if (count <= 0) continue
    const pin = pins[name]
    const fallback = resolvePrinting(name)
    if (pin && !(fallback && samePrinting(pin, fallback))) {
      // Explicit pin to a non-default printing: carry the name so decode can re-pin it.
      overflow.push([name, String(count), pin.setCode, pin.collectorNumber].join(US))
    } else if (fallback) {
      // Sitting at the catalog default — the printing alone recovers the name on decode.
      const set = fallback.setCode
      const list = bySet.get(set) ?? []
      list.push({ collector: fallback.collectorNumber, count })
      bySet.set(set, list)
    } else {
      // Catalog can't resolve the card at all: carry just the name (recipient resolves by name).
      overflow.push([name, String(count)].join(US))
    }
  }

  const groups: string[] = []
  for (const [set, cards] of bySet) {
    const ints: Array<{ n: number; count: number }> = []
    const verbatim: Array<{ collector: string; count: number }> = []
    for (const c of cards) {
      const n = asInteger(c.collector)
      if (n === null) verbatim.push(c)
      else ints.push({ n, count: c.count })
    }
    ints.sort((a, b) => a.n - b.n)
    const tokens: string[] = []
    let prev = 0
    for (const { n, count } of ints) {
      tokens.push(withCount(String(n - prev), count))
      prev = n
    }
    for (const { collector, count } of verbatim) tokens.push(withCount(collector, count))
    groups.push(set + US + tokens.join(' '))
  }

  const commanderField = (name: string, printing: PrintingRef | undefined): string =>
    printing
      ? [sanitize(name), printing.setCode, printing.collectorNumber].join(US)
      : sanitize(name)
  const commanderRec = deck.commander
    ? [
        commanderField(deck.commander, deck.commanderPrinting),
        ...(deck.secondCommander ? [commanderField(deck.secondCommander, deck.secondCommanderPrinting)] : []),
      ].join(GS)
    : ''

  const records = [
    [
      V2_TAG,
      sanitize(deck.name),
      sanitize(deck.format ?? ''),
      ...(deck.companion ? [sanitize(deck.companion)] : []),
    ].join(US),
    commanderRec,
    groups.join(GS),
    ...overflow,
  ]
  const payload = new TextEncoder().encode(records.join(RS))
  return bytesToBase64Url(await deflate(payload))
}

function withCount(head: string, count: number): string {
  return count > 1 ? `${head}:${count}` : head
}

function samePrinting(a: PrintingRef, b: PrintingRef): boolean {
  return a.setCode === b.setCode && a.collectorNumber === b.collectorNumber
}

/** Build the full shareable deckbuilder URL for a code. */
export function buildShareUrl(origin: string, code: string): string {
  return `${origin}/deckbuilder?${SHARE_PARAM}=${code}`
}

// --- decode --------------------------------------------------------------------------------

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

/**
 * Decode a share code back into a [SharedDeck]. `resolveName` maps a `(set, collector)` printing
 * back to a card name (built from the catalog by the caller). Returns `null` for anything that
 * isn't a usable payload (bad base64, no resolvable cards). Never throws — the input is untrusted
 * URL content. Reads both the current v2 (printing-keyed) and legacy v1 (JSON, name-keyed) shapes.
 */
export async function decodeSharedDeck(
  code: string,
  resolveName: ResolveName,
): Promise<SharedDeck | null> {
  let text: string
  try {
    const bytes = base64UrlToBytes(code)
    // Current format is DEFLATE-compressed; the very earliest links carried the bytes
    // uncompressed. Try to inflate, falling back to reading the raw bytes as UTF-8.
    try {
      text = new TextDecoder().decode(await inflate(bytes))
    } catch {
      text = new TextDecoder().decode(bytes)
    }
  } catch {
    return null
  }

  const decoded = text.startsWith(V2_TAG + US)
    ? decodeV2(text, resolveName)
    : decodeV1(text)
  if (!decoded) return null
  if (Object.keys(decoded.cards).length === 0) return null
  return decoded
}

function decodeV2(text: string, resolveName: ResolveName): SharedDeck | null {
  const records = text.split(RS)
  const meta = (records[0] ?? '').split(US)
  if (meta[0] !== V2_TAG) return null

  const cards: Record<string, number> = {}
  const printings: Record<string, PrintingRef> = {}
  const add = (name: string, count: number) => {
    if (!name || !Number.isFinite(count) || count <= 0) return
    cards[name] = (cards[name] ?? 0) + Math.floor(count)
  }

  // rec 2: compact groups (catalog-default printings, name recovered via resolveName).
  for (const group of (records[2] ?? '').split(GS)) {
    if (!group) continue
    const fields = group.split(US)
    const set = fields[0]
    if (!set) continue
    const body = fields[1] ?? ''
    let prev = 0
    for (const token of body.split(' ')) {
      if (!token) continue
      const sep = token.lastIndexOf(':')
      const head = sep === -1 ? token : token.slice(0, sep)
      const count = sep === -1 ? 1 : Number.parseInt(token.slice(sep + 1), 10)
      const delta = asInteger(head)
      const collector = delta === null ? head : String((prev += delta))
      const name = resolveName({ setCode: set, collectorNumber: collector })
      if (name) add(name, count)
    }
  }

  // rec 3+: overflow rows carrying the name (reprint pins + catalog-unresolvable cards).
  for (let i = 3; i < records.length; i++) {
    const f = records[i]?.split(US)
    if (!f) continue
    const name = f[0]
    const count = Number.parseInt(f[1] ?? '', 10)
    if (!name) continue
    add(name, count)
    if (f.length >= 4 && f[2] && f[3]) {
      printings[name] = { setCode: f[2], collectorNumber: f[3] }
    }
  }

  const out: SharedDeck = { name: meta[1] ?? '', cards }
  if (Object.keys(printings).length > 0) out.printings = printings
  if (meta[2]) out.format = meta[2]
  if (meta[3]) {
    out.companion = meta[3]
    out.sideboard = { [meta[3]]: 1 }
  }

  // rec 1: commander(s) — name verbatim, optional pinned printing; a partner pair is GS-split.
  const [first, second] = (records[1] ?? '').split(GS)
  const cmd = (first ?? '').split(US)
  if (cmd[0]) {
    out.commander = cmd[0]
    if (cmd.length >= 3 && cmd[1] && cmd[2]) {
      out.commanderPrinting = { setCode: cmd[1], collectorNumber: cmd[2] }
    }
    const cmd2 = (second ?? '').split(US)
    if (cmd2[0] && cmd2[0] !== cmd[0]) {
      out.secondCommander = cmd2[0]
      if (cmd2.length >= 3 && cmd2[1] && cmd2[2]) {
        out.secondCommanderPrinting = { setCode: cmd2[1], collectorNumber: cmd2[2] }
      }
    }
  }
  return out
}

// Legacy v1: `{ v:1, n, f?, c?, cp?, c2?, cp2?, d:[ [name,count] | [name,count,set,coll] ] }`. Name-keyed,
// so it decodes without the catalog resolver — kept so links shared before v2 still open.
function decodeV1(text: string): SharedDeck | null {
  let raw: unknown
  try {
    raw = JSON.parse(text)
  } catch {
    return null
  }
  if (!isRecord(raw) || raw.v !== 1 || !Array.isArray(raw.d)) return null

  const cards: Record<string, number> = {}
  const printings: Record<string, PrintingRef> = {}
  for (const row of raw.d) {
    if (!Array.isArray(row)) continue
    const [name, count, setCode, collectorNumber] = row
    if (typeof name !== 'string' || typeof count !== 'number') continue
    if (!Number.isFinite(count) || count <= 0) continue
    cards[name] = (cards[name] ?? 0) + Math.floor(count)
    if (typeof setCode === 'string' && typeof collectorNumber === 'string') {
      printings[name] = { setCode, collectorNumber }
    }
  }
  if (Object.keys(cards).length === 0) return null

  const out: SharedDeck = { name: typeof raw.n === 'string' ? raw.n : '', cards }
  if (Object.keys(printings).length > 0) out.printings = printings
  if (typeof raw.f === 'string') out.format = raw.f
  if (typeof raw.c === 'string') out.commander = raw.c
  if (Array.isArray(raw.cp) && typeof raw.cp[0] === 'string' && typeof raw.cp[1] === 'string') {
    out.commanderPrinting = { setCode: raw.cp[0], collectorNumber: raw.cp[1] }
  }
  if (out.commander && typeof raw.c2 === 'string' && raw.c2 && raw.c2 !== out.commander) {
    out.secondCommander = raw.c2
    if (Array.isArray(raw.cp2) && typeof raw.cp2[0] === 'string' && typeof raw.cp2[1] === 'string') {
      out.secondCommanderPrinting = { setCode: raw.cp2[0], collectorNumber: raw.cp2[1] }
    }
  }
  return out
}
