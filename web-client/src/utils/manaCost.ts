/**
 * Parse a mana cost string into individual symbols.
 * e.g., "{4}{U}{B}" -> ["4", "U", "B"]
 */
export function parseManaCost(manaCost: string): string[] {
  const symbols: string[] = []
  const regex = /\{([^}]+)\}/g
  let match
  while ((match = regex.exec(manaCost)) !== null) {
    symbols.push(match[1]!)
  }
  return symbols
}

const COLOR_PIP = new Set(['W', 'U', 'B', 'R', 'G'])

/**
 * Can this printed mana cost be paid using only colored mana of [allowedColors]
 * (single-letter WUBRG)? This drives the "at most" deckbuilder filter, which
 * asks whether a card is castable in a deck limited to a colour set.
 *
 * A hybrid pip like `{R/W}` is payable with EITHER half (CR 107.4e), so a
 * `{R/W}` card is castable in a mono-white *or* mono-red deck and must survive
 * "at most W". Phyrexian (`{W/P}`) and monocolor-hybrid / "twobrid" (`{2/W}`)
 * pips always have a non-colored payment (life / generic), so they never force
 * a colour. Generic / X / colorless / snow symbols never force a colour either.
 */
export function manaCostCastableWith(manaCost: string, allowedColors: ReadonlySet<string>): boolean {
  for (const raw of parseManaCost(manaCost)) {
    const sym = raw.toUpperCase()
    if (sym.includes('/')) {
      const halves = sym.split('/')
      if (halves.includes('P')) continue // Phyrexian — pay 2 life
      if (halves.some((h) => /^\d+$/.test(h))) continue // twobrid — pay generic
      if (halves.some((h) => allowedColors.has(h))) continue // hybrid — either colour
      return false
    }
    if (COLOR_PIP.has(sym) && !allowedColors.has(sym)) return false
  }
  return true
}

/** Every colour letter appearing anywhere in the printed cost (including both halves of a hybrid). */
export function manaCostColors(manaCost: string): Set<string> {
  const colors = new Set<string>()
  for (const raw of parseManaCost(manaCost)) {
    for (const half of raw.toUpperCase().split('/')) {
      if (COLOR_PIP.has(half)) colors.add(half)
    }
  }
  return colors
}

/**
 * "At most" deckbuilder semantics: can this card be *played* in a deck limited
 * to [allowedColors] (single-letter WUBRG)? The card must be castable from its
 * cost (hybrid pips give a choice — see [manaCostCastableWith]), and any colour
 * in its identity that the cost doesn't account for — off-color activation
 * costs, dual-land subtypes — must itself be within the allowed set.
 */
export function playableWithinColors(
  manaCost: string,
  colorIdentity: ReadonlySet<string>,
  allowedColors: ReadonlySet<string>,
): boolean {
  const costColors = manaCostColors(manaCost)
  for (const c of colorIdentity) {
    if (!costColors.has(c) && !allowedColors.has(c)) return false
  }
  return manaCostCastableWith(manaCost, allowedColors)
}

/**
 * Build the remaining mana cost symbols after applying convoke creatures.
 * Each creature pays for one colored symbol (exact or matching half of a hybrid,
 * per CR 107.4e / 702.51a) or one generic mana.
 *
 * Convoke payment colors arrive as backend `Color` enum names ("WHITE", "BLUE"...)
 * while cost symbols parse as pip letters ("W", "U"...), so both letter and
 * enum-name inputs are accepted and normalised to a pip letter for matching.
 */
const COLOR_NAME_TO_PIP: Record<string, string> = {
  WHITE: 'W', BLUE: 'U', BLACK: 'B', RED: 'R', GREEN: 'G',
}

export function getRemainingCostAfterConvoke(
  originalSymbols: string[],
  convokedCreatures: Record<string, { color: string | null }>
): string[] {
  const remaining = [...originalSymbols]

  for (const { color } of Object.values(convokedCreatures)) {
    if (color) {
      const pip = COLOR_NAME_TO_PIP[color] ?? color
      const exactIdx = remaining.indexOf(pip)
      if (exactIdx >= 0) {
        remaining.splice(exactIdx, 1)
        continue
      }
      const hybridIdx = remaining.findIndex(
        s => s.includes('/') && s.split('/').includes(pip)
      )
      if (hybridIdx >= 0) remaining.splice(hybridIdx, 1)
    } else {
      // Creature pays for generic mana
      const gIdx = remaining.findIndex(s => /^\d+$/.test(s))
      if (gIdx >= 0) {
        const val = parseInt(remaining[gIdx]!, 10)
        if (val > 1) {
          remaining[gIdx] = String(val - 1)
        } else {
          remaining.splice(gIdx, 1)
        }
      }
    }
  }

  return remaining
}

/**
 * Total mana still needed for [symbols]: generic symbols count as their value, every other
 * symbol as one.
 */
export function totalManaNeeded(symbols: string[]): number {
  let total = 0
  for (const s of symbols) {
    const num = parseInt(s, 10)
    total += isNaN(num) ? 1 : num
  }
  return total
}

/**
 * The cheapest of several alternative renderings of the same cost — the one whose total mana is
 * lowest — or undefined when there are none.
 *
 * Emerge (CR 702.119a) sends one resulting cost per creature the player could sacrifice, since the
 * sacrifice's mana value comes off the emerge cost. The cast button shows the best case so the
 * number it displays and the fact that it is enabled can't contradict each other.
 */
export function cheapestCost(costs: readonly string[]): string | undefined {
  let cheapest: string | undefined
  let cheapestTotal = Number.POSITIVE_INFINITY
  for (const cost of costs) {
    const total = totalManaNeeded(parseManaCost(cost))
    if (total < cheapestTotal) {
      cheapest = cost
      cheapestTotal = total
    }
  }
  return cheapest
}

/**
 * Which of a creature's colours (backend `Color` names) it should pay for convoke given the pips
 * still open: an exact coloured pip first, then a hybrid pip one of its colours covers
 * (CR 107.4e), else null — pay generic. Only a *preference*; the server checks the creature
 * actually is that colour (`AlternativePaymentHandler.validateForSpell`).
 */
export function pickConvokeColor(remainingSymbols: readonly string[], creatureColors: readonly string[]): string | null {
  const pips = creatureColors.map((c) => COLOR_NAME_TO_PIP[c] ?? c)
  for (let i = 0; i < pips.length; i++) {
    if (remainingSymbols.includes(pips[i]!)) return creatureColors[i]!
  }
  for (let i = 0; i < pips.length; i++) {
    const pip = pips[i]!
    if (remainingSymbols.some((s) => s.includes('/') && s.split('/').includes(pip))) return creatureColors[i]!
  }
  return null
}
