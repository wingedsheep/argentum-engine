import type { ClientManaPool, EntityId, ManaSourceOption } from '@/types'

/**
 * Pure coverage logic for the mana payment prompt, kept out of the component so it can be unit
 * tested: importing the component pulls in the Zustand store, which reads localStorage at module
 * load and is unavailable under the node test environment.
 */
// Server serializes Color enums by name ("BLACK"), but cost symbols use pip letters ("B").
const COLOR_NAME_TO_PIP: Record<string, string> = {
  WHITE: 'W', BLUE: 'U', BLACK: 'B', RED: 'R', GREEN: 'G',
}

export const toPip = (color: string): string => COLOR_NAME_TO_PIP[color] ?? color

export interface PipCoverage {
  symbol: string
  /** Covered by mana already floating in the pool. */
  floating: boolean
  /** Covered by a source the player has selected but not yet tapped. */
  pending: boolean
}

/**
 * Works out which pips of the cost are already covered, and by what.
 *
 * Floating mana is applied first (it is real, and the resumers spend the pool before tapping
 * anything), then the selected sources. Mirrors the engine's solver well enough for a UI readout —
 * the server still re-solves on submit. Skips X (variable). `extraGeneric` folds in non-mana
 * payment: each tapped Waterbend permanent pays {1} generic.
 */
const COLOR_OR_COLORLESS = new Set(['W', 'U', 'B', 'R', 'G', 'C'])

/**
 * The colours a pip will accept. A hybrid pays with *either* half (CR 107.4e), so `{W/B}` accepts
 * white or black; a monocolour hybrid like `{2/W}` accepts white here and its generic half in the
 * generic pass. Phyrexian `{W/P}` keeps its colour half (the life option isn't paid from sources).
 */
export function pipColorOptions(symbol: string): string[] {
  return symbol.split('/').filter((part) => COLOR_OR_COLORLESS.has(part))
}

/** The generic amount a pip can be paid with, if any: `3` -> 3, `2/W` -> 2, `W` -> null. */
export function pipGenericAmount(symbol: string): number | null {
  for (const part of symbol.split('/')) {
    const parsed = parseInt(part, 10)
    if (!isNaN(parsed)) return parsed
  }
  return null
}

export function computeCoverage(
  costSymbols: readonly string[],
  pool: ClientManaPool | null,
  selectedIds: readonly EntityId[],
  availableSources: readonly ManaSourceOption[],
  extraGeneric = 0,
  acceptedColors: Readonly<Record<string, readonly string[]>> = {},
): PipCoverage[] {
  const pips: PipCoverage[] = costSymbols.map((symbol) => ({ symbol, floating: false, pending: false }))

  const optionsFor = (symbol: string) =>
    [...new Set(pipColorOptions(symbol).flatMap((color) => acceptedColors[color] ?? [color]))]
  // The readout keeps printed pip order; assign strict pips first using server-supplied options.
  const assignmentPips = [...pips].sort((a, b) => optionsFor(a.symbol).length - optionsFor(b.symbol).length)

  const floatingByColor: Record<string, number> = {
    W: pool?.white ?? 0,
    U: pool?.blue ?? 0,
    B: pool?.black ?? 0,
    R: pool?.red ?? 0,
    G: pool?.green ?? 0,
    C: pool?.colorless ?? 0,
  }

  // Pass 1 — floating mana against coloured pips it exactly matches.
  for (const pip of assignmentPips) {
    const paidWith = optionsFor(pip.symbol).find((color) => (floatingByColor[color] ?? 0) > 0)
    if (paidWith !== undefined) {
      floatingByColor[paidWith] = (floatingByColor[paidWith] ?? 0) - 1
      pip.floating = true
    }
  }

  // Pass 2 — selected sources against the coloured pips still open. A source that adds more
  // than one mana (Gilded Lotus: "three mana of any one color") covers up to that many pips of
  // one colour it produces — the colour that pays the most — and carries the rest into the
  // generic pass. `manaAmount` is the server's number, the same one the cast path's
  // `ManaSourceInfo` sends; without it a Lotus counted as one pip and Pay stayed dead on a cost
  // the server would have accepted.
  const sourceById = new Map(availableSources.map((s) => [s.entityId, s]))
  let spareFromSources = 0
  const openPipsPayableBy = (color: string) =>
    assignmentPips.filter((pip) => !pip.floating && !pip.pending && optionsFor(pip.symbol).includes(color))
  for (const id of selectedIds) {
    const source = sourceById.get(id)
    if (!source) continue
    const amount = source.manaAmount ?? 1
    const colors = (source.producesColors ?? []).map(toPip)
    let best: { color: string; count: number } | undefined
    for (const color of colors) {
      const count = Math.min(amount, openPipsPayableBy(color).length)
      if (count > 0 && (!best || count > best.count)) best = { color, count }
    }
    if (best) {
      for (const pip of openPipsPayableBy(best.color).slice(0, best.count)) pip.pending = true
      spareFromSources += amount - best.count
    } else {
      spareFromSources += amount
    }
  }

  // Pass 3 — whatever is left (leftover floating, mana from sources that matched no coloured pip
  // or had some to spare, Waterbend taps) pays generic pips, cheapest first.
  let leftoverFloating = Object.values(floatingByColor).reduce((a, b) => a + b, 0)
  let leftoverPending = spareFromSources + extraGeneric
  for (const pip of assignmentPips) {
    if (pip.floating || pip.pending) continue
    const amount = pipGenericAmount(pip.symbol)
    if (amount === null) continue
    if (leftoverFloating >= amount) {
      leftoverFloating -= amount
      pip.floating = true
    } else if (leftoverFloating + leftoverPending >= amount) {
      leftoverPending -= amount - leftoverFloating
      leftoverFloating = 0
      pip.pending = true
    }
  }

  return pips
}

/** X is chosen elsewhere, so an X pip never blocks the Pay button. */
export const isCovered = (pip: PipCoverage) => pip.floating || pip.pending || pip.symbol === 'X'
