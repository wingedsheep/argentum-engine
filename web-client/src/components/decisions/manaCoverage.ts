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
 * anything), then the selected sources. Alternate assignments keep a flexible source from blocking a
 * strict pip; the server still validates the payment on submit. Skips X (variable). `extraGeneric` folds in non-mana
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
  availableSources: readonly Pick<ManaSourceOption, 'entityId' | 'producesColors' | 'manaAmount'>[],
  extraGeneric = 0,
  acceptedColors: Readonly<Record<string, readonly string[]>> = {},
): PipCoverage[] {
  const pips: PipCoverage[] = costSymbols.map((symbol) => ({ symbol, floating: false, pending: false }))
  const optionsFor = (symbol: string) =>
    [...new Set(pipColorOptions(symbol).flatMap((color) => acceptedColors[color] ?? [color]))]
  const coloredIndices = pips.map((_, index) => index)
    .filter((index) => optionsFor(pips[index]!.symbol).length > 0)
    .sort((a, b) => optionsFor(pips[a]!.symbol).length - optionsFor(pips[b]!.symbol).length)

  // A multi-mana source chooses one actual color for its whole activation. Keep its units
  // together so the readout cannot spend one Lotus as both blue and white.
  interface Resource {
    colors: readonly string[]
    remaining: number
    floating: boolean
    chosenColor?: string | undefined
  }
  const resources: Resource[] = Object.entries({
    W: pool?.white ?? 0, U: pool?.blue ?? 0, B: pool?.black ?? 0,
    R: pool?.red ?? 0, G: pool?.green ?? 0, C: pool?.colorless ?? 0,
  }).filter(([, amount]) => amount > 0)
    .map(([color, amount]) => ({ colors: [color], remaining: amount, floating: true }))
  const sourceById = new Map(availableSources.map((source) => [source.entityId, source]))
  for (const id of selectedIds) {
    const source = sourceById.get(id)
    if (!source) continue
    resources.push({
      colors: source.producesColors.map(toPip),
      remaining: source.manaAmount ?? 1,
      floating: false,
    })
  }

  let best = pips.map((pip) => ({ ...pip }))
  let bestScore = -1
  const payableCount = pips.filter((pip) => pip.symbol !== 'X').length
  const seen = new Set<string>()
  // This is a display estimate, not the server's payment solver. Bound unusual source/color
  // combinations so a payment readout cannot stall rendering; keep the best partial assignment.
  const searchBudget = 10_000
  let searchedStates = 0
  const resourceKey = (resource: Resource) =>
    `${resource.floating}:${resource.chosenColor ?? resource.colors.join('/')}:${resource.remaining}`

  function finishGeneric() {
    const candidate = pips.map((pip) => ({ ...pip }))
    let floating = resources.filter((resource) => resource.floating)
      .reduce((sum, resource) => sum + resource.remaining, 0)
    let pending = extraGeneric + resources.filter((resource) => !resource.floating)
      .reduce((sum, resource) => sum + resource.remaining, 0)
    const genericIndices = candidate.map((_, index) => index)
      .filter((index) => !candidate[index]!.floating && !candidate[index]!.pending &&
        pipGenericAmount(candidate[index]!.symbol) !== null)
      .sort((a, b) => pipGenericAmount(candidate[a]!.symbol)! - pipGenericAmount(candidate[b]!.symbol)!)
    for (const index of genericIndices) {
      const amount = pipGenericAmount(candidate[index]!.symbol)!
      if (floating >= amount) {
        floating -= amount
        candidate[index]!.floating = true
      } else if (floating + pending >= amount) {
        pending -= amount - floating
        floating = 0
        candidate[index]!.pending = true
      }
    }
    const score = candidate.filter((pip) => pip.floating || pip.pending).length
    if (score > bestScore) {
      bestScore = score
      best = candidate
    }
  }

  function assign(position: number): boolean {
    if (++searchedStates > searchBudget) return false
    if (position === coloredIndices.length) {
      finishGeneric()
      return bestScore === payableCount
    }
    const coverageKey = pips.map((pip) => pip.floating ? 'f' : pip.pending ? 'p' : '-').join('')
    const stateKey = `${position}|${coverageKey}|${resources.map(resourceKey).sort().join('|')}`
    if (seen.has(stateKey)) return false
    seen.add(stateKey)
    const pip = pips[coloredIndices[position]!]!
    const options = optionsFor(pip.symbol)
    const tried = new Set<string>()
    for (const resource of resources) {
      if (resource.remaining === 0) continue
      const key = resourceKey(resource)
      if (tried.has(key)) continue
      tried.add(key)
      const colors = resource.chosenColor ? [resource.chosenColor] : resource.colors
      for (const color of colors) {
        if (!options.includes(color)) continue
        const previousColor = resource.chosenColor
        resource.chosenColor = color
        resource.remaining--
        pip.floating = resource.floating
        pip.pending = !resource.floating
        const complete = assign(position + 1)
        resource.remaining++
        resource.chosenColor = previousColor
        pip.floating = false
        pip.pending = false
        if (complete) return true
      }
    }
    // Leave a pip uncovered for the partial readout, or pay a hybrid's generic half later.
    return assign(position + 1)
  }

  assign(0)
  return best
}

/** X is chosen elsewhere, so an X pip never blocks the Pay button. */
export const isCovered = (pip: PipCoverage) => pip.floating || pip.pending || pip.symbol === 'X'
