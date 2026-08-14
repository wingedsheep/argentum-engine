import type { ChooseTargetsDecision, ClientCard, ClientGameState, EntityId } from '@/types'
import { ZoneType } from '@/types'

/**
 * A ChooseTargetsDecision that the lone-player click-to-submit path can satisfy: exactly one
 * target requirement, and that requirement wants at most one target.
 *
 * Only such a decision may be answered by clicking a single player's life orb and submitting
 * `{ 0: [playerId] }` immediately (see PlayerTargetingUI, LifeDisplay, OpponentRail). A
 * multi-target player slot — e.g. Parker Luck's "two target players" (maxTargets = 2) — is NOT
 * lone: it must accumulate orb picks through decisionSelectionState with a Confirm step
 * (BattlefieldTargetingUI), because immediately submitting one player would fail the server's
 * minimum-targets check and strand the player with no way to pick the second.
 */
export function isLoneTargetRequirement(decision: ChooseTargetsDecision): boolean {
  if (decision.targetRequirements.length !== 1) return false
  return (decision.targetRequirements[0]?.maxTargets ?? 1) <= 1
}

/** Zones whose cards are only reachable through a pile picker — never clickable on the board. */
const PILE_ZONES: ReadonlySet<ZoneType> = new Set([ZoneType.GRAVEYARD, ZoneType.EXILE])

/** How a requirement's valid targets split across the two ways a player can reach them. */
export interface TargetZonePartition {
  /** Valid targets sitting in a pile zone, in `validTargets` order — reachable only via a picker. */
  readonly pileCards: ClientCard[]
  /**
   * True when at least one valid target is *not* a pile card: a battlefield permanent, a player, a
   * stack object, or a target the client can't resolve to a card. Those are all picked by clicking
   * the board, so the board path must stay live.
   */
  readonly hasBoardTargets: boolean
}

/**
 * Split one requirement's valid targets into "reachable by clicking the board" and "reachable only
 * through a pile picker", instead of asking the all-or-nothing question [getPileTargetCards] asks.
 *
 * A union filter can span both sides — Taskmaster, Mercenary Mimic copies "target creature on the
 * battlefield **or** creature card in a graveyard" — and such a requirement needs *both* routes at
 * once: the board stays clickable and the graveyard cards get a picker. Collapsing that to a single
 * boolean is what made a mixed union's graveyard half unselectable.
 *
 * Zones come from `card.zone`, which is server-sent state; nothing here decides legality — every
 * entity considered is already in the server's `validTargets`. [targetZoneHint] is the server's
 * single-zone hint (`LegalActionTargetInfo.targetZone`), used only for a card whose client-side
 * zone is missing; a target the client can't resolve at all counts as a board target so the
 * requirement still reaches a UI that can show something.
 */
export function partitionTargetsByZone(
  validTargets: readonly EntityId[],
  cards: ClientGameState['cards'] | undefined,
  targetZoneHint?: string,
): TargetZonePartition {
  const pileCards: ClientCard[] = []
  let hasBoardTargets = false
  const hintIsPile = targetZoneHint === ZoneType.GRAVEYARD || targetZoneHint === ZoneType.EXILE

  for (const targetId of validTargets) {
    const card = cards?.[targetId]
    const zoneType = card?.zone?.zoneType
    if (card && (zoneType ? PILE_ZONES.has(zoneType) : hintIsPile)) {
      pileCards.push(card)
    } else {
      hasBoardTargets = true
    }
  }
  return { pileCards, hasBoardTargets }
}

/**
 * Which UI (or UIs) can collect one target requirement.
 *
 * - `board` — everything is clicked on the battlefield / player orbs / stack.
 * - `pile` — everything lives in a graveyard or exile pile, so a picker owns the screen.
 * - `mixed` — both, and both must stay reachable at the same time.
 */
export type TargetZoneRouteMode = 'board' | 'pile' | 'mixed'

/** [partitionTargetsByZone] plus the routing verdict and the wording for the open-the-pile button. */
export interface TargetZoneRoute extends TargetZonePartition {
  readonly mode: TargetZoneRouteMode
  /** Names the piles that actually hold valid targets: "Graveyard", "Exile", "Graveyard / Exile". */
  readonly pileZoneLabel: string
}

/**
 * The single routing decision shared by **both** targeting paths — the action pipeline
 * ([TargetingOverlay], casting a spell or activating an ability) and the decision pipeline
 * ([ChooseTargetsUI], a triggered ability's PendingDecision).
 *
 * They drifted once: the action path learned about mixed battlefield ∪ pile requirements while the
 * decision path kept an all-or-nothing test, so Taskmaster, Mercenary Mimic's trigger ("becomes a
 * copy of up to one target creature on the battlefield **or** creature card in a graveyard") fell
 * through to board-only clicking and its graveyard half was unselectable. Routing lives here so a
 * third path can't repeat that.
 */
export function routeTargetsByZone(
  validTargets: readonly EntityId[],
  cards: ClientGameState['cards'] | undefined,
  targetZoneHint?: string,
): TargetZoneRoute {
  const partition = partitionTargetsByZone(validTargets, cards, targetZoneHint)
  const mode: TargetZoneRouteMode =
    partition.pileCards.length === 0 ? 'board' : partition.hasBoardTargets ? 'mixed' : 'pile'
  return { ...partition, mode, pileZoneLabel: describePileZones(partition.pileCards) }
}

/**
 * Label for the pile the player is being sent to, derived from the cards actually there rather
 * than from the effect's prose — "Exile" for Blade of the Swarm, "Graveyard / Exile" for a union
 * like Sorceress's Schemes. Empty input reads as "Graveyard" (the label is unused in that case).
 */
export function describePileZones(pileCards: readonly ClientCard[]): string {
  const zones = new Set(pileCards.map((card) => card.zone?.zoneType ?? ZoneType.GRAVEYARD))
  if (zones.size > 1) return 'Graveyard / Exile'
  return zones.has(ZoneType.EXILE) ? 'Exile' : 'Graveyard'
}

/**
 * Boilerplate that hangs off the *exile* verb in O-Ring style effects — `ExileUntilLeavesEffect`
 * renders "Exile … until this permanent leaves the battlefield". It names the battlefield without
 * the effect ever putting anything there, so it must not read as reanimation (The Spot, Living
 * Portal composes two of them and would otherwise offer "Put onto Battlefield" for a card it exiles).
 */
const LEAVES_BATTLEFIELD_BOILERPLATE = 'leaves the battlefield'

/**
 * Wordings where the picked card is the *copy source* and never moves — Taskmaster, Mercenary Mimic
 * ("becomes a copy of up to one target creature on the battlefield or creature card in a
 * graveyard") and the token-copy shape ("create a token that's a copy of …"). Both name the
 * battlefield while leaving the picked card exactly where it is, so this must be tested **before**
 * the battlefield branch or the card's own zone change gets announced instead of the copy.
 */
const COPY_SOURCE_PATTERN = /becomes? a copy of|token that(?:'s| is) a copy of/

/** "…to its owner's hand", "…to your hand" — word-bounded so "beforehand" can't trip it. */
const TO_HAND_PATTERN = /\bhands?\b/

/** What a pile-targeting requirement will do to the picked cards, in button and sentence form. */
export interface PileAction {
  /** Label for the confirm button on an optional pile target. */
  confirmText: string
  /** Verb phrase for the helper sentence: "Choose a card to <verb>." */
  verb: string
}

/**
 * Derive the action wording for a pile-targeting requirement from the decision's effect hint:
 * "Exile card in a graveyard" → Exile; "Shuffle … into its owner's library" → Shuffle into Library;
 * "Put … onto the battlefield" → Put onto Battlefield; "becomes a copy of …" → Copy.
 *
 * Effects can be wrapped (ForEachTargetEffect, CompositeEffect, …) so the keyword may not be at the
 * start — match anywhere in the hint.
 *
 * **The fallback is deliberately verb-less.** It used to be "Return to Hand", which meant every
 * unrecognised effect made a *false statement* about what the game was about to do: Taskmaster's
 * copy trigger offered "Optional: choose up to one card to return to your hand" for a card that
 * stays in the graveyard. A vague prompt is recoverable; a confidently wrong one is not. An
 * unmatched hint now falls through to the same neutral "Confirm Target" wording the mandatory path
 * already uses, and reanimation has to be named explicitly to be claimed.
 *
 * Order is load-bearing — first match wins, and several real hints contain more than one keyword:
 *  - **copy before battlefield** — Taskmaster's hint names the battlefield as a place to *target*,
 *    not a destination.
 *  - **battlefield before exile** — a blink ("exile target creature, then return it to the
 *    battlefield") ends with the card on the battlefield, so the destination wins over the means.
 *    [LEAVES_BATTLEFIELD_BOILERPLATE] is discounted first so `ExileUntilLeavesEffect`'s "…until this
 *    permanent leaves the battlefield" (The Spot, Living Portal) isn't mistaken for that.
 *  - **exile before hand** — "Exile … permanents you control or cards from your hand or graveyard"
 *    mentions the hand as a *source*.
 */
export function derivePileAction(effectHint: string | null | undefined): PileAction {
  const hint = (effectHint?.toLowerCase() ?? '').replaceAll(LEAVES_BATTLEFIELD_BOILERPLATE, '')

  if (COPY_SOURCE_PATTERN.test(hint)) {
    return { confirmText: 'Copy', verb: 'copy' }
  }
  if (hint.includes('battlefield')) {
    return { confirmText: 'Put onto Battlefield', verb: 'put onto the battlefield' }
  }
  if (hint.includes('shuffle') && hint.includes('library')) {
    return { confirmText: 'Shuffle into Library', verb: 'shuffle into your library' }
  }
  if (hint.includes('exile')) {
    return { confirmText: 'Exile', verb: 'exile' }
  }
  if (TO_HAND_PATTERN.test(hint)) {
    return { confirmText: 'Return to Hand', verb: 'return to your hand' }
  }
  return { confirmText: 'Confirm Target', verb: 'target' }
}
