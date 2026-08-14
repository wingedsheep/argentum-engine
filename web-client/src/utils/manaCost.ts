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
 * Build the remaining mana cost symbols after applying N delve exiles.
 * Reduces the generic portion only.
 */
export function getRemainingCostSymbols(originalSymbols: string[], delveCount: number): string[] {
  const remaining = [...originalSymbols]
  let reductionsLeft = delveCount
  for (let i = 0; i < remaining.length && reductionsLeft > 0; i++) {
    const symbol = remaining[i]!
    if (/^\d+$/.test(symbol)) {
      const genericValue = parseInt(symbol, 10)
      if (genericValue > reductionsLeft) {
        remaining[i] = String(genericValue - reductionsLeft)
        reductionsLeft = 0
      } else {
        reductionsLeft -= genericValue
        remaining.splice(i, 1)
        i--
      }
    }
  }
  return remaining
}

/**
 * Apply a Harmonize creature-tap to a mana cost. The tap reduces the *generic*
 * mana the player must still pay by the chosen creature's power — printed {N}
 * first, then the generic {X} (the {X} is generic per the TDM release notes,
 * mirrored server-side by `CastSpellHandler.harmonizePaymentXValue`). Colored
 * pips are untouched, and the effect's chosen X (sent as `action.xValue`) is
 * unchanged — only the mana *paid* shrinks.
 *
 * Expands {X} to [xValue] first, then removes [reduction] generic in array order
 * (which, for land pre-selection, has the same total as the printed-first engine
 * rule — lands don't distinguish printed generic from the generic {X}).
 */
export function reduceCostByHarmonizeTap(
  manaCost: string,
  xValue: number,
  reduction: number,
): string[] {
  const expanded = parseManaCost(manaCost).map((s) => (s === 'X' ? String(xValue) : s))
  return getRemainingCostSymbols(expanded, reduction)
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
 * A unit of floating mana available for a payment: a pip letter ("W".."G", "C" for colorless).
 * Mirrors the numeric pool fields plus the server's per-action eligible restricted entries.
 */
interface AvailableMana {
  readonly white: number
  readonly blue: number
  readonly black: number
  readonly red: number
  readonly green: number
  readonly colorless: number
}

/** A single unit of restricted mana the server flagged eligible for this payment. */
interface EligibleRestrictedMana {
  /** Pip letter ("W".."G") or null/undefined for colorless. */
  readonly color?: string | null
}

/**
 * Subtract the player's floating mana from [symbols] and return the symbols still owed.
 *
 * Pays exact-color pips first, then hybrid pips (CR 107.4e — a hybrid symbol is a colored symbol
 * of both halves), then generic — the same order the server's `ManaPool.payPartial` uses, so the
 * client's affordability check matches the actual payment.
 *
 * [eligibleRestricted] is the conditional mana ("spend this mana only to …") the *server* has
 * already judged eligible for this specific action (`LegalActionInfo.eligibleRestrictedMana`).
 * It spends exactly like unrestricted mana here; ignoring it greys out casts the server would
 * accept — e.g. convoking a big creature while Ashling, Rimebound's MV4+ mana is floating.
 */
export function applyManaPoolToCost(
  symbols: string[],
  pool: AvailableMana | undefined,
  eligibleRestricted: readonly EligibleRestrictedMana[] | undefined = undefined,
): string[] {
  const remaining = [...symbols]
  const available: Record<string, number> = {
    W: pool?.white ?? 0,
    U: pool?.blue ?? 0,
    B: pool?.black ?? 0,
    R: pool?.red ?? 0,
    G: pool?.green ?? 0,
    C: pool?.colorless ?? 0,
  }
  for (const entry of eligibleRestricted ?? []) {
    const pip = entry.color ?? 'C'
    if (pip in available) available[pip]!++
  }

  const pips = ['W', 'U', 'B', 'R', 'G', 'C']
  for (const pip of pips) {
    while (available[pip]! > 0) {
      const idx = remaining.indexOf(pip)
      if (idx < 0) break
      remaining.splice(idx, 1)
      available[pip]!--
    }
  }

  for (const pip of pips) {
    if (pip === 'C') continue
    while (available[pip]! > 0) {
      const idx = remaining.findIndex((s) => s.includes('/') && s.split('/').includes(pip))
      if (idx < 0) break
      remaining.splice(idx, 1)
      available[pip]!--
    }
  }

  let generic = pips.reduce((sum, pip) => sum + available[pip]!, 0)
  while (generic > 0) {
    const idx = remaining.findIndex((s) => /^\d+$/.test(s))
    if (idx < 0) break
    const value = parseInt(remaining[idx]!, 10)
    if (value > 1) {
      remaining[idx] = String(value - 1)
    } else {
      remaining.splice(idx, 1)
    }
    generic--
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
 * Minimal description of a mana source needed for preview trimming.
 * Mirrors the server-provided `ManaSourceInfo` shape. Generic over the
 * entityId type so callers using a branded `EntityId` don't lose that type.
 */
export interface TrimmableManaSource<Id> {
  readonly entityId: Id
  readonly producesColors?: readonly string[]
  readonly producesColorless?: boolean
  readonly manaAmount?: number
}

/**
 * Trim the server's auto-tap preview down to the subset needed for the given
 * reduced cost symbols.
 *
 * The engine is authoritative: on submit, `CastPaymentProcessor.explicitPay` /
 * `ActivateAbilityHandler.execute` re-solve against the selected sources and
 * tap only the minimum subset needed after convoke/delve has reduced the cost.
 * This function is purely a UI hint so the highlighted pre-selection reflects
 * what the engine will actually do — it walks the priority-ordered source list
 * the server already picked (basics before duals, etc.) and keeps only the
 * prefix that covers the reduced cost.
 */
export function trimAutoTapPreview<Id>(
  fullPreview: readonly Id[],
  availableSources: readonly TrimmableManaSource<Id>[],
  remainingCostSymbols: string[],
): Id[] {
  const coloredReqs: Record<string, number> = {}
  let genericReq = 0
  for (const s of remainingCostSymbols) {
    if (s === 'X') continue
    const num = parseInt(s, 10)
    if (!isNaN(num)) {
      genericReq += num
    } else {
      coloredReqs[s] = (coloredReqs[s] ?? 0) + 1
    }
  }

  const sourceById = new Map(availableSources.map((s) => [s.entityId, s]))
  const kept: Id[] = []

  const hasUnmetRequirements = () => {
    if (genericReq > 0) return true
    for (const v of Object.values(coloredReqs)) if (v > 0) return true
    return false
  }

  for (const sourceId of fullPreview) {
    if (!hasUnmetRequirements()) break

    const source = sourceById.get(sourceId)
    if (!source) {
      // Preserve unknown sources to avoid accidental drops
      kept.push(sourceId)
      continue
    }

    const amount = source.manaAmount ?? 1
    const colors = source.producesColors ?? []

    // Prefer assigning to a colored requirement this source can pay
    let consumedColor = false
    for (const color of colors) {
      if ((coloredReqs[color] ?? 0) > 0) {
        coloredReqs[color]!--
        consumedColor = true
        // Extra mana beyond the one colored pip can go to generic
        if (amount > 1) {
          genericReq = Math.max(0, genericReq - (amount - 1))
        }
        break
      }
    }

    if (consumedColor) {
      kept.push(sourceId)
      continue
    }

    // Otherwise assign to generic (any source can pay generic)
    if (genericReq > 0) {
      genericReq = Math.max(0, genericReq - amount)
      kept.push(sourceId)
      continue
    }
    // This source would contribute nothing — drop it.
  }

  return kept
}

/**
 * Build an auto-tap pre-selection from scratch when the server didn't provide
 * one. The server computes its preview by solving the **printed** cost; if a
 * spell can only be afforded once an alternative payment (convoke/delve) has
 * trimmed the cost, the server's preview is null and post-convoke pre-selection
 * would be empty. After convoke applies, this picks lands greedily to cover the
 * remaining cost so the player isn't left to hand-pick lands.
 *
 * Uses the same priority-ordered source list the server provided (basics before
 * duals, etc.) and the same colored-first-then-generic logic as
 * [trimAutoTapPreview]. The engine re-solves on submit, so over-selection is
 * safe — the preview is purely a UI hint.
 */
export function computeAutoTapPreview<Id>(
  availableSources: readonly TrimmableManaSource<Id>[],
  remainingCostSymbols: string[],
): Id[] {
  const fullPreview = availableSources.map((s) => s.entityId)
  return trimAutoTapPreview(fullPreview, availableSources, remainingCostSymbols)
}
