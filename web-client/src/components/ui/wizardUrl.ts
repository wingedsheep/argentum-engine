/**
 * The landing wizard's URL scheme — Phase 6, scoped to the wizard.
 *
 * Phase 7 gave the landing screen a numbered stepper whose answers are revisitable, which left the
 * screen with **two** back affordances that disagreed: the stepper stepped back a question, the
 * browser's Back left Argentum entirely. This module is what reconciles them.
 *
 * **The path carries the answers, not the step number.** The step is derived from which answers are
 * present, exactly as `PlayWizard` already derives it from its draft — so there is still one source
 * of truth, and `history.back()` is by construction "drop the last answer".
 *
 *     /                             who with?
 *     /play/group                   what with?
 *     /play/group/draft             how?
 *     /play/group/draft/bracket     ready
 *
 * Two consequences worth naming:
 *
 * - **A selection is shareable.** "Here's the thing we're playing" is now a link, which is the same
 *   reason the lobby has an invite code. It is not a lobby — nothing exists server-side until Create.
 * - **Decoding validates.** A pasted or bookmarked URL is re-checked against `modeMatrix` the same
 *   way `loadLastSelection` re-checks `localStorage`: a combination that this server or this build no
 *   longer offers degrades to the last answer that still holds, rather than rendering a dead screen.
 *   `aiEnabled` is part of that, so `/play/solo/...` on an AI-less server lands on step 1.
 *
 * Decoding also **auto-resolves a one-answer step**, so `/play/solo/bring-a-deck` is a complete
 * selection even though nobody wrote the shape down. `PlayWizard` normalises the address bar onto the
 * canonical path afterwards, which is what keeps hand-typed and generated URLs converging — but only
 * ever by *completing* it. A path this module truncates stays in the bar while the wizard shows the
 * step with its reason, because truncation is a reachability verdict and reachability needs server
 * config that may not have arrived yet. See the normalise effect in `PlayWizard` for why that matters.
 */
import {
  ROSTERS,
  SHAPE_IDS,
  shapeChoices,
  cardsChoices,
  defaultCardsAxis,
  type Roster,
  type ShapeId,
} from '../lobby/modeMatrix'
import { CARDS_KINDS, type CardsAxis, type CardsKind } from '../lobby/axes'

/** Where the wizard's URLs live. Everything under it is answers. */
export const WIZARD_PREFIX = '/play'

/** A selection in progress: each answer is null until given. */
export interface WizardDraft {
  roster: Roster | null
  cards: CardsAxis | null
  shape: ShapeId | null
}

export const EMPTY_DRAFT: WizardDraft = { roster: null, cards: null, shape: null }

/* ── Slugs ──────────────────────────────────────────────────────────────────
 * Hand-written rather than derived from the enum names, because these are URLs: `two-headed-giant`
 * reads better than `two_headed_giant`, and a rename of an internal constant must not silently break
 * every link anyone has saved. The exhaustive switch is what keeps them in step with the domains.
 * ─────────────────────────────────────────────────────────────────────────── */

function rosterSlug(roster: Roster): string {
  switch (roster) {
    case 'SOLO': return 'solo'
    case 'FRIEND': return 'friend'
    case 'GROUP': return 'group'
  }
}

function shapeSlug(shape: ShapeId): string {
  switch (shape) {
    case 'ONE_GAME': return 'one-game'
    case 'BRACKET': return 'bracket'
    case 'FREE_FOR_ALL': return 'free-for-all'
    case 'TWO_HEADED_GIANT': return 'two-headed-giant'
    case 'TEAM_VS_TEAM': return 'team-vs-team'
  }
}

/**
 * A Cards *kind*. Sub-options are deliberately not encoded — neither `BRING_A_DECK`'s `legality` nor
 * the sealed/draft shape, because the wizard sets neither: both hang off the Cards axis and are picked
 * in the lobby. Putting them in the path would invent an answer nobody gave, and the whole scheme
 * rests on one segment per answer.
 */
function cardsSlug(kind: CardsKind): string {
  switch (kind) {
    case 'BRING_A_DECK': return 'bring-a-deck'
    case 'RANDOM': return 'random'
    case 'MOMIR': return 'momir'
    case 'SEALED': return 'sealed'
    case 'DRAFT': return 'draft'
  }
}

/** The canonical path for a draft. `/` when nothing is answered yet. */
export function draftToPath(draft: WizardDraft): string {
  if (draft.roster === null) return '/'
  const parts = [WIZARD_PREFIX, rosterSlug(draft.roster)]
  if (draft.cards !== null) parts.push(cardsSlug(draft.cards.kind))
  if (draft.cards !== null && draft.shape !== null) parts.push(shapeSlug(draft.shape))
  return parts.join('/')
}

/**
 * Parse a path into a draft, keeping only the prefix of answers that still holds.
 *
 * Each answer is validated against the one before it, so an unreachable combination truncates rather
 * than throwing: `/play/group/momir` (Momir has no group implementation) yields just the roster, and
 * the player lands on step 2 with Momir visibly disabled and its reason attached.
 *
 * The query string carries no answers — a saved `?seats=4` link from when the wizard asked for a
 * seat count decodes to the same selection as one without it, and normalising drops the query.
 */
export function pathToDraft(pathname: string, aiEnabled: boolean): WizardDraft {
  const segments = pathname.replace(/\/+$/, '').split('/').filter(Boolean)
  if (segments[0] !== WIZARD_PREFIX.slice(1)) return EMPTY_DRAFT

  const roster = ROSTERS.find((r) => rosterSlug(r) === segments[1]) ?? null
  if (roster === null) return EMPTY_DRAFT
  // Same rule `loadLastSelection` applies: a solo selection is meaningless with the AI switched off.
  if (roster === 'SOLO' && !aiEnabled) return EMPTY_DRAFT

  const cards = decodeCards(roster, segments[2])
  if (cards === null) return { ...EMPTY_DRAFT, roster }

  const open = shapeChoices(roster, cards).filter((c) => !c.disabledReason)
  // An explicit shape must be one this roster and Cards value actually offer; with exactly one open
  // shape the step is skipped, so fill it in — that is what makes `/play/solo/bring-a-deck` complete.
  const named = SHAPE_IDS.find((s) => shapeSlug(s) === segments[3]) ?? null
  const shape =
    named !== null && open.some((c) => c.value === named) ? named
      : open.length === 1 ? open[0]!.value
        : null
  if (shape === null) return { ...EMPTY_DRAFT, roster, cards }

  return { roster, cards, shape }
}

/** A Cards slug, checked against what this roster can actually pick, at its default sub-shape. */
function decodeCards(roster: Roster, slug: string | undefined): CardsAxis | null {
  if (!slug) return null
  const kind = CARDS_KINDS.find((k) => cardsSlug(k) === slug)
  if (kind === undefined) return null
  if (cardsChoices(roster).some((c) => c.value === kind && c.disabledReason)) return null
  return defaultCardsAxis(kind)
}
