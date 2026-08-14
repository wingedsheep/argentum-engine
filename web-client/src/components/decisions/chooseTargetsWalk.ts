import type { ChooseTargetsDecision, ClientCard, ClientGameState, EntityId } from '@/types'
import { routeTargetsByZone } from '@/utils/targeting.ts'

/**
 * The state machine behind [ChooseTargetsUI]: which target requirement the player is on, what they
 * have confirmed so far, and — within a requirement whose legal targets span the battlefield *and*
 * a graveyard/exile pile — which of the two collectors currently owns the screen.
 *
 * Kept as a plain reducer so the whole walk is testable without a DOM: the component is a renderer
 * over [chooseTargetsView] and dispatches these events.
 */
export interface ChooseTargetsWalkState {
  /** Index of the requirement being collected. */
  readonly requirementIndex: number
  /**
   * Confirmed picks for the *other* requirements, keyed by index. Never holds `requirementIndex`
   * itself — stepping Back deletes an entry before stepping into it, so the current requirement's
   * pool is always recomputed against the picks that are still standing.
   */
  readonly collected: Readonly<Record<number, readonly EntityId[]>>
  /**
   * Picks to pre-select in whichever collector is showing. Non-empty when the player stepped Back
   * into a requirement they had already answered, or when they crossed between the board and the
   * pile picker inside one mixed requirement — the crossing carries the picks so both halves of a
   * `battlefield ∪ pile` union count toward the same requirement.
   */
  readonly pending: readonly EntityId[]
  /** True while the pile picker owns the screen on a mixed requirement. */
  readonly pilePickerOpen: boolean
  /** Set once the final requirement is confirmed: the payload to submit, keyed by requirement. */
  readonly submission: Readonly<Record<number, readonly EntityId[]>> | null
}

export const initialChooseTargetsWalk: ChooseTargetsWalkState = {
  requirementIndex: 0,
  collected: {},
  pending: [],
  pilePickerOpen: false,
  submission: null,
}

export type ChooseTargetsWalkEvent =
  /** The showing collector confirmed this requirement's picks. */
  | { readonly type: 'confirm'; readonly targets: readonly EntityId[]; readonly totalRequirements: number }
  /** Revise the previous requirement. */
  | { readonly type: 'back' }
  /** Mixed requirement: open the pile picker, carrying the picks made on the board. */
  | { readonly type: 'openPile'; readonly carried: readonly EntityId[] }
  /** Mixed requirement: hand control back to the board banner, carrying the picks made in the pile. */
  | { readonly type: 'closePile'; readonly carried: readonly EntityId[] }

export function chooseTargetsWalkReducer(
  state: ChooseTargetsWalkState,
  event: ChooseTargetsWalkEvent,
): ChooseTargetsWalkState {
  switch (event.type) {
    case 'confirm': {
      const collected = { ...state.collected, [state.requirementIndex]: event.targets }
      if (state.requirementIndex + 1 < event.totalRequirements) {
        return {
          requirementIndex: state.requirementIndex + 1,
          collected,
          pending: [],
          // A picker left open would otherwise reopen over the next requirement, which may have
          // nothing in a pile at all.
          pilePickerOpen: false,
          submission: null,
        }
      }
      return { ...state, collected, pending: [], pilePickerOpen: false, submission: collected }
    }
    case 'back': {
      if (state.requirementIndex === 0) return state
      const prevIndex = state.requirementIndex - 1
      const collected = { ...state.collected }
      delete collected[prevIndex]
      return {
        requirementIndex: prevIndex,
        collected,
        // Restore the previous requirement's confirmed picks so they can be revised. The current
        // requirement's in-progress picks are discarded.
        pending: state.collected[prevIndex] ?? [],
        pilePickerOpen: false,
        submission: null,
      }
    }
    case 'openPile':
      return { ...state, pending: event.carried, pilePickerOpen: true }
    case 'closePile':
      return { ...state, pending: event.carried, pilePickerOpen: false }
  }
}

/** What [ChooseTargetsUI] renders for the requirement the walk is currently on. */
export interface ChooseTargetsView {
  /** The collector that owns the screen right now. */
  readonly collector: 'board' | 'pile'
  /**
   * True when this requirement spans both routes: the board banner stays up so permanents remain
   * clickable *and* it offers a button that opens the picker for the pile half (and, with the
   * picker open, the picker offers the way back). Selections cross with the player, so a pick made
   * on either side counts toward this one requirement.
   */
  readonly isMixed: boolean
  /** This requirement's legal targets, minus picks confirmed for the other requirements. */
  readonly legalTargets: readonly EntityId[]
  /** The pile half of [legalTargets] — the picker's contents and the count on the open button. */
  readonly pileCards: ClientCard[]
  /** "Graveyard" / "Exile" / "Graveyard / Exile" — wording for the open-the-picker button. */
  readonly pileZoneLabel: string
}

export function chooseTargetsView(
  decision: ChooseTargetsDecision,
  walk: ChooseTargetsWalkState,
  cards: ClientGameState['cards'] | undefined,
): ChooseTargetsView {
  // Targets confirmed for other requirements can't be picked again.
  const alreadySelected = Object.values(walk.collected).flat()
  const legalTargets = (decision.legalTargets[walk.requirementIndex] ?? []).filter(
    (id) => !alreadySelected.includes(id),
  )

  const { mode, pileCards, pileZoneLabel } = routeTargetsByZone(legalTargets, cards)
  const isMixed = mode === 'mixed'
  const collector = mode === 'pile' || (isMixed && walk.pilePickerOpen) ? 'pile' : 'board'

  return { collector, isMixed, legalTargets, pileCards, pileZoneLabel }
}
