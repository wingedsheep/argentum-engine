/**
 * Render a sealed/draft deck-building state into an MTG Arena deck list — the same
 * text shape `parseArenaDeck.ts` reads back, so an exported deck round-trips through
 * the deckbuilder's import flow (and pastes straight into Arena / Moxfield).
 *
 * Line shape per card is `count Name (SET) Collector`, falling back to `count Name`
 * when the pool card carries no printing metadata (test cards lack it). Sections use
 * Arena's headers — `Commander`, `Deck`, `Sideboard` — recognised by the parser's
 * `SECTION_HEADERS` table.
 *
 * The leftover pool (cards opened/drafted but not in the maindeck) is emitted as the
 * `Sideboard`, mirroring how Arena exports a limited pool — the player keeps a record
 * of their whole pool, not just the 40 they sleeved up.
 */
import type { SealedCardInfo } from '@/types'

/** The slice of deck-building state needed to render an Arena list. */
export interface ArenaExportInput {
  /** Card names in the maindeck (one entry per copy). */
  readonly deck: readonly string[]
  /** The opened/drafted pool — source of printing metadata and the sideboard. */
  readonly cardPool: readonly SealedCardInfo[]
  /** Basic lands available to add, carrying their own printing metadata. */
  readonly basicLands: readonly SealedCardInfo[]
  /** Basic-land copies added to the deck, by land name. */
  readonly landCounts: Readonly<Record<string, number>>
  /** Designated commander name (commander formats only); it lives in [deck] too. */
  readonly commander: string | null
}

/** Format one Arena line: `count Name (SET) Collector`, or `count Name` without a printing. */
function formatLine(count: number, name: string, printing: SealedCardInfo | undefined): string {
  if (printing?.setCode && printing.collectorNumber) {
    return `${count} ${name} (${printing.setCode.toUpperCase()}) ${printing.collectorNumber}`
  }
  return `${count} ${name}`
}

/** Tally a list of names into a name → count map. */
function tally(names: readonly string[]): Map<string, number> {
  const counts = new Map<string, number>()
  for (const name of names) counts.set(name, (counts.get(name) ?? 0) + 1)
  return counts
}

/**
 * Build the MTG Arena deck-list text for the given deck-building state. Returns the
 * empty string when there is nothing to export (no maindeck cards or basic lands).
 */
export function buildArenaDeckList(input: ArenaExportInput): string {
  const { deck, cardPool, basicLands, landCounts, commander } = input

  // First printing seen per name wins — pool cards of the same name share a printing.
  const printingByName = new Map<string, SealedCardInfo>()
  for (const card of [...cardPool, ...basicLands]) {
    if (!printingByName.has(card.name)) printingByName.set(card.name, card)
  }

  // How many copies of each name the pool actually opened — bounds the sideboard.
  const poolCounts = tally(cardPool.map((c) => c.name))

  // Raw maindeck usage (commander copy included) — used to subtract from the pool
  // when computing the sideboard.
  const rawDeckCounts = tally(deck)

  const deckCounts = tally(deck)
  // The commander lives in the command zone, not the maindeck (CR 903.6) — pull one
  // copy out of the maindeck tally so it isn't double-listed.
  if (commander) {
    const remaining = (deckCounts.get(commander) ?? 0) - 1
    if (remaining > 0) deckCounts.set(commander, remaining)
    else deckCounts.delete(commander)
  }

  const sections: string[] = []

  if (commander) {
    sections.push(['Commander', formatLine(1, commander, printingByName.get(commander))].join('\n'))
  }

  const mainLines: string[] = []
  for (const name of [...deckCounts.keys()].sort((a, b) => a.localeCompare(b))) {
    mainLines.push(formatLine(deckCounts.get(name)!, name, printingByName.get(name)))
  }
  for (const name of Object.keys(landCounts).sort((a, b) => a.localeCompare(b))) {
    const count = landCounts[name] ?? 0
    if (count > 0) mainLines.push(formatLine(count, name, printingByName.get(name)))
  }
  if (mainLines.length > 0) sections.push(['Deck', ...mainLines].join('\n'))

  // Sideboard = pool copies not used in the maindeck (commander excluded — it's not
  // a sideboard card). Names absent from the deck list keep their full pool count.
  const sideLines: string[] = []
  for (const name of [...poolCounts.keys()].sort((a, b) => a.localeCompare(b))) {
    const used = name === commander ? poolCounts.get(name)! : rawDeckCounts.get(name) ?? 0
    const leftover = poolCounts.get(name)! - used
    if (leftover > 0) sideLines.push(formatLine(leftover, name, printingByName.get(name)))
  }
  if (sideLines.length > 0) sections.push(['Sideboard', ...sideLines].join('\n'))

  return sections.join('\n\n')
}
