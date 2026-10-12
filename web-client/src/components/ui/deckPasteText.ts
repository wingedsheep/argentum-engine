/**
 * The paste box's text ↔ deck conversion, kept out of the component so it can be tested on its own.
 *
 * The lobby picker accepts the same Arena / Moxfield / plain-text shapes as the deckbuilder's
 * import — it delegates the line parsing to {@link parseArenaDeckList} — and adds two things the
 * textarea has always tolerated: a bare card name meaning one copy, and rendering a deck back out
 * for editing.
 */
import { parseArenaDeckList } from '@/components/deckbuilder/parseArenaDeck'

export interface ParsedPaste {
  cards: Record<string, number>
  /**
   * The `Sideboard` / `SB:` section, card name → copies. The constructed sideboard lives
   * "outside the game" (CR 400.11a) and is only reachable through wish effects, so it is kept
   * apart from `cards` rather than merged in — merging it would deal the sideboard into the
   * library and inflate the deck past its legal size.
   */
  sideboard: Record<string, number>
  /** The first card under a `Companion` header — the deck's companion (CR 702.139), also on `sideboard`. */
  companion?: string
  deckName?: string
}

export function parseDeckText(text: string): ParsedPaste {
  // Delegate to the shared Arena/Moxfield/plain-text parser so section headers
  // (`About`, `Deck`, `Sideboard`, …) and Arena's `Name <deck-name>` metadata
  // line don't end up as bogus card entries.
  const parsed = parseArenaDeckList(text)
  const cards: Record<string, number> = {}
  const sideboard: Record<string, number> = {}
  // The commander is a card of the deck (the picker has no designation of its own); a companion
  // starts outside the game (CR 702.139a), so it rides on the sideboard. A maybeboard is dropped.
  for (const entry of [...parsed.commander, ...parsed.entries]) {
    cards[entry.name] = (cards[entry.name] ?? 0) + entry.count
  }
  for (const entry of [...parsed.sideboard, ...parsed.companion]) {
    sideboard[entry.name] = (sideboard[entry.name] ?? 0) + entry.count
  }
  // Tolerate the "bare card name = 1 copy" shorthand the old parser supported, since the
  // picker's textarea never required a leading count. Read `cleaned`, not `raw`: `raw` still
  // carries the `SB:` prefix and any Moxfield decorations, which would become part of the card
  // name and then fail to resolve. The rescued line stays on whichever board it was written under.
  for (const err of parsed.errors) {
    if (err.reason !== 'unrecognised line format') continue
    const name = err.cleaned.trim()
    if (!name) continue
    if (err.section === 'excluded') continue
    const board = err.section === 'side' || err.section === 'companion' ? sideboard : cards
    board[name] = (board[name] ?? 0) + 1
  }
  const companion = parsed.companion[0]?.name
  return {
    cards,
    sideboard,
    ...(companion ? { companion } : {}),
    ...(parsed.deckName !== undefined ? { deckName: parsed.deckName } : {}),
  }
}

function formatDeckLines(cards: Record<string, number>): string {
  return Object.entries(cards)
    .filter(([, n]) => n > 0)
    .map(([name, n]) => `${n} ${name}`)
    .join('\n')
}

/**
 * Render a deck back into the paste textarea. A companion is emitted under a `Companion` header
 * and the rest of the sideboard under a `Sideboard`
 * header — the same shape {@link parseDeckText} reads — so editing a saved deck that has one
 * round-trips instead of silently dropping it.
 */
export function formatDeckText(
  cards: Record<string, number>,
  sideboard?: Record<string, number>,
  companion?: string | null,
): string {
  const main = formatDeckLines(cards)
  // A companion is written under its own `Companion` header (one copy taken off the sideboard),
  // so the paste reads it back as the deck's companion.
  const board = { ...(sideboard ?? {}) }
  const hasCompanion = !!companion && (board[companion] ?? 0) > 0
  if (hasCompanion) board[companion!] = board[companion!]! - 1
  const side = formatDeckLines(board)
  const parts = [main]
  if (hasCompanion) parts.push(`Companion\n1 ${companion}`)
  if (side) parts.push(`Sideboard\n${side}`)
  return parts.join('\n\n')
}
