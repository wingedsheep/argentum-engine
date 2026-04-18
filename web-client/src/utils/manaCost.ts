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
