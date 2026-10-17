import { useShallow } from 'zustand/react/shallow'
import { useGameStore, type GameStore } from '@/store/gameStore.ts'
import type { EntityId } from '@/types'
import { bandIsLegal, mergedBand } from '@/utils/combatBands'
import type { CounterRemovalCreatureInfo } from '@/types/messages.ts'

/**
 * Everything a `GameCard` needs to know about the local interaction in progress — targeting,
 * combat, decisions, and the payment/selection modes — reduced to what it means for *one* card.
 *
 * `GameCard` used to subscribe to each interaction object whole (`targetingState`, `combatState`,
 * `pendingDecision`, …). Those objects are replaced on every click — toggling an attacker, picking a
 * target, a mana-source toggle, a distribute +/- — so every card on the board re-rendered for every
 * click, even though only one or two of them looked any different afterwards. This hook selects the
 * per-card answers instead (all primitives, or references that only change when this card's slice of
 * the state does) under `useShallow`, so a click re-renders exactly the cards whose answers flipped.
 *
 * Mode flags such as `isInTargetingMode` are still global — they flip once when a mode starts or ends,
 * which legitimately changes every card. What they avoid is re-rendering on every step *within* a mode.
 *
 * Anything only a handler needs (the pending decision's id, the convoke selection to price a colour,
 * the attack targets a drop resolves against) is read from `useGameStore.getState()` at event time
 * rather than subscribed to here.
 */
export interface CardInteractionState {
  // Targeting
  readonly isInTargetingMode: boolean
  readonly isValidTarget: boolean
  readonly isSelectedTarget: boolean
  readonly isBeingCast: boolean
  readonly costIfSacrificed: string | undefined
  // Pending decision
  readonly hasPendingDecision: boolean
  readonly isChooseTargetsDecision: boolean
  readonly isValidDecisionTarget: boolean
  readonly isManaPaymentWindow: boolean
  readonly isTriggerYesNo: boolean
  readonly isDecisionSubject: boolean
  // Decision selection (SelectCardsDecision with useTargetingUI)
  readonly hasDecisionSelection: boolean
  readonly isValidDecisionSelection: boolean
  readonly isSelectedDecisionOption: boolean
  // Inline damage distribution — the remaining/min figures are only filled in for a target.
  readonly isDistributeTarget: boolean
  readonly distributeAllocated: number
  readonly distributeRemaining: number
  readonly distributeMinPerTarget: number
  readonly distributeMaxForCard: number | undefined
  // Inline counter removal — the server's row for this creature and its allocation record, both
  // references into the store that stay put while other creatures' allocations change.
  readonly counterCreature: CounterRemovalCreatureInfo | undefined
  readonly counterDistInner: Readonly<Record<string, number>> | undefined
  // Selection-based payments
  readonly isInManaSelectionMode: boolean
  readonly isManaValidSource: boolean
  readonly isManaSelected: boolean
  readonly isInTapForPowerMode: boolean
  readonly isValidTapForPowerCreature: boolean
  readonly isSelectedTapForPowerCreature: boolean
  readonly isInConvokeMode: boolean
  readonly isValidConvokeCreature: boolean
  readonly isSelectedConvokeCreature: boolean
  readonly isInTapForGenericMode: boolean
  readonly isValidTapForGenericPermanent: boolean
  readonly isSelectedTapForGenericPermanent: boolean
  readonly isInHarmonizeMode: boolean
  readonly isValidHarmonizeCreature: boolean
  readonly isSelectedHarmonizeCreature: boolean
  // Combat
  readonly isInAttackerMode: boolean
  readonly isInBlockerMode: boolean
  readonly isValidAttacker: boolean
  readonly isSelectedAsAttacker: boolean
  readonly bandIndex: number
  readonly isBandDropTarget: boolean
  readonly isValidBlocker: boolean
  readonly isSelectedAsBlocker: boolean
  readonly isAttackingInBlockerMode: boolean
  readonly isMustBeBlocked: boolean
  readonly isBystanderAttacker: boolean
  readonly isValidAttackTargetCard: boolean
  /** This attackable permanent is an attack target and at least one attacker is selected. */
  readonly isAttackTargetWithAttackers: boolean
  /** This attackable permanent is already assigned as some selected attacker's target. */
  readonly isTargetedByAttacker: boolean
  // Drag state
  readonly isBlockerDragActive: boolean
  readonly isDraggingThisBlocker: boolean
  readonly isDraggingThisAttacker: boolean
}

// ---------------------------------------------------------------------------------------------
// Membership caches. Each selector below runs for every card on every store write — hover writes
// included, at up to 60 Hz — so an O(n) `.includes` / `.some` per list per card adds up on a full
// board. The lists are immutable and replaced wholesale, so a Set built once per list reference
// (and dropped with it) turns each check into a lookup. Short lists skip the cache: a scan is
// cheaper than the WeakMap round-trip. Same idea as `getLegalActionsIndex` in store/selectors.ts.
// ---------------------------------------------------------------------------------------------

const SCAN_LIMIT = 16

const idListSets = new WeakMap<readonly EntityId[], ReadonlySet<EntityId>>()
function listHas(list: readonly EntityId[] | undefined | null, id: EntityId): boolean {
  if (!list || list.length === 0) return false
  if (list.length <= SCAN_LIMIT) return list.includes(id)
  let set = idListSets.get(list)
  if (!set) {
    set = new Set(list)
    idListSets.set(list, set)
  }
  return set.has(id)
}

const entityListSets = new WeakMap<readonly { readonly entityId: EntityId }[], ReadonlySet<EntityId>>()
function entityListHas(list: readonly { readonly entityId: EntityId }[] | undefined | null, id: EntityId): boolean {
  if (!list || list.length === 0) return false
  if (list.length <= SCAN_LIMIT) return list.some((e) => e.entityId === id)
  let set = entityListSets.get(list)
  if (!set) {
    set = new Set(list.map((e) => e.entityId))
    entityListSets.set(list, set)
  }
  return set.has(id)
}

type ServerAttacker = { readonly creatureId: EntityId; readonly bandId?: string | null | undefined }

/**
 * For the server's declared attackers: which creatures are attacking, and the band index of each
 * banded one — bands ordered by the first appearance of each unique `bandId` in attacker order, so
 * the colour assignment is stable and matches what the attacker submitted.
 */
const serverAttackerIndex = new WeakMap<readonly ServerAttacker[], { attacking: ReadonlySet<EntityId>; band: ReadonlyMap<EntityId, number> }>()
function indexServerAttackers(attackers: readonly ServerAttacker[]) {
  let index = serverAttackerIndex.get(attackers)
  if (!index) {
    const attacking = new Set<EntityId>()
    const band = new Map<EntityId, number>()
    const seen: string[] = []
    for (const att of attackers) {
      // A creature's band is read off its *first* entry, as `attackers.find(...)` would.
      const firstEntry = !attacking.has(att.creatureId)
      attacking.add(att.creatureId)
      if (!att.bandId) continue
      let i = seen.indexOf(att.bandId)
      if (i === -1) {
        seen.push(att.bandId)
        i = seen.length - 1
      }
      if (firstEntry) band.set(att.creatureId, i)
    }
    index = { attacking, band }
    serverAttackerIndex.set(attackers, index)
  }
  return index
}

/** Client-side bands being assembled during declare-attackers: creature → first band holding it. */
const localBandIndex = new WeakMap<readonly (readonly EntityId[])[], ReadonlyMap<EntityId, number>>()
function indexLocalBands(bands: readonly (readonly EntityId[])[]): ReadonlyMap<EntityId, number> {
  let index = localBandIndex.get(bands)
  if (!index) {
    const map = new Map<EntityId, number>()
    bands.forEach((band, i) => {
      for (const id of band) if (!map.has(id)) map.set(id, i)
    })
    index = map
    localBandIndex.set(bands, index)
  }
  return index
}

export interface CardInteractionInput {
  readonly cardId: EntityId
  readonly isCreature: boolean
  readonly isOpponentCard: boolean
}

export function selectCardInteraction(state: GameStore, input: CardInteractionInput): CardInteractionState {
  const { cardId, isCreature, isOpponentCard } = input
  const viewed = state.spectatingState?.gameState ?? state.gameState

  // Targeting
  const targeting = state.targetingState
  const isInTargetingMode = targeting !== null
  const isValidTarget = listHas(targeting?.validTargets, cardId)
  const isSelectedTarget = listHas(targeting?.selectedTargets, cardId)
  const isBeingCast = isInTargetingMode && targeting.action != null &&
    'cardId' in targeting.action && targeting.action.cardId === cardId
  // Emerge (CR 702.119a): the server prices every sacrifice candidate; see GameCard for the badge.
  const costIfSacrificed = targeting?.costAfterSacrifice?.[cardId]

  // Pending decision
  const decision = state.pendingDecision
  const isChooseTargetsDecision = decision?.type === 'ChooseTargetsDecision'
  const isValidDecisionTarget = !state.decisionSelectionState && decision?.type === 'ChooseTargetsDecision' &&
    decision.targetRequirements.length === 1 &&
    listHas(decision.legalTargets[0], cardId)
  const isTriggerYesNo = decision?.type === 'YesNoDecision'
    && !!decision.context.inlineOnTrigger
    && decision.context.triggeringEntityId === cardId
  const isDecisionSubject = decision?.context.subjectEntityId === cardId

  // Decision selection
  const decisionSelection = state.decisionSelectionState
  const isValidDecisionSelection = listHas(decisionSelection?.validOptions, cardId)
  const isSelectedDecisionOption = listHas(decisionSelection?.selectedOptions, cardId)

  // Inline damage distribution
  const distribute = state.distributeState
  const isDistributeTarget = listHas(distribute?.targets, cardId)
  const distributeAllocated = isDistributeTarget ? (distribute?.distribution[cardId] ?? 0) : 0
  let distributeRemaining = 0
  if (isDistributeTarget && distribute) {
    let total = 0
    for (const v of Object.values(distribute.distribution)) total += v
    distributeRemaining = distribute.totalAmount - total
  }
  const distributeMinPerTarget = isDistributeTarget ? (distribute?.minPerTarget ?? 0) : 0
  const distributeMaxForCard = distribute?.maxPerTarget?.[cardId]

  // Inline counter removal
  const counterDist = state.counterDistributionState
  const counterCreature = counterDist?.creatures.find((c) => c.entityId === cardId)
  const counterDistInner = counterCreature != null ? counterDist?.distribution[cardId] : undefined

  // Selection-based payments
  const mana = state.manaSelectionState
  const tapForPower = state.tapForPowerSelectionState
  const convoke = state.convokeSelectionState
  const tapForGeneric = state.tapForGenericSelectionState
  const harmonize = state.harmonizeSelectionState

  // Combat. In single-client hotseat the seat we control may render on the opponent row, so any
  // creature counts as "ours" / "theirs" and combatState membership disambiguates.
  const combat = state.combatState
  const hotseat = viewed?.hotseat ?? false
  const ownForCombat = (!isOpponentCard && isCreature) || (hotseat && isCreature)
  const opponentForCombat = isOpponentCard || hotseat
  const isInAttackerMode = combat?.mode === 'declareAttackers'
  const isInBlockerMode = combat?.mode === 'declareBlockers'
  const isValidCombatCreature = listHas(combat?.validCreatures, cardId)
  // `validCreatures` is the server's list (tapped creatures are already out of it — the engine's
  // MustBeUntappedAttackRule); re-checking tapped state here would only let the two disagree.
  const isValidAttacker = isInAttackerMode && ownForCombat && isValidCombatCreature
  const isSelectedAsAttacker = isInAttackerMode && listHas(combat.selectedAttackers, cardId)

  // Banding (CR 702.22): local bands while this player declares, the server's bandIds after.
  const serverAttackers = viewed?.combat?.attackers ?? null
  const serverIndex = serverAttackers ? indexServerAttackers(serverAttackers) : null
  let bandIndex = -1
  if (isInAttackerMode) bandIndex = indexLocalBands(combat.bands).get(cardId) ?? -1
  if (bandIndex === -1) bandIndex = serverIndex?.band.get(cardId) ?? -1

  const draggingAttackerId = state.draggingAttackerId
  const isBandDropTarget =
    isInAttackerMode &&
    !!draggingAttackerId &&
    draggingAttackerId !== cardId &&
    isValidCombatCreature &&
    // Exactly the band the drop would make, checked as the server will (CR 702.22c — banding or
    // "bands with other"), so the highlight never promises a band the link would refuse.
    bandIsLegal(mergedBand(combat.bands, draggingAttackerId, cardId), (id) => viewed?.cards?.[id])

  const isValidBlocker = isInBlockerMode && ownForCombat && isValidCombatCreature
  const isSelectedAsBlocker = isInBlockerMode && !!(combat.blockerAssignments[cardId]?.length)
  const isAttackingInBlockerMode = isInBlockerMode && opponentForCombat && listHas(combat.attackingCreatures, cardId)
  const isMustBeBlocked = isInBlockerMode && opponentForCombat && listHas(combat.mustBeBlockedAttackers, cardId)
  // Multiplayer: an attacker in this combat that is attacking a *different* defender — you can't
  // block it (CR 509.1b). `attackingCreatures` is already scoped to attacks on the acting defender.
  const isBystanderAttacker = isInBlockerMode && opponentForCombat &&
    !listHas(combat.attackingCreatures, cardId) &&
    (serverIndex?.attacking.has(cardId) ?? false)

  // An attackable permanent (planeswalker or battle) — the server's list is the only authority.
  const isValidAttackTargetCard = isInAttackerMode && listHas(combat.validAttackTargets, cardId)
  const isAttackTargetWithAttackers = isValidAttackTargetCard && combat.selectedAttackers.length > 0
  const isTargetedByAttacker = isValidAttackTargetCard && Object.values(combat.attackerTargets).includes(cardId)

  const draggingBlockerId = state.draggingBlockerId

  return {
    isInTargetingMode,
    isValidTarget,
    isSelectedTarget,
    isBeingCast,
    costIfSacrificed,
    hasPendingDecision: decision !== null,
    isChooseTargetsDecision,
    isValidDecisionTarget,
    isManaPaymentWindow: decision?.type === 'SelectManaSourcesDecision',
    isTriggerYesNo,
    isDecisionSubject,
    hasDecisionSelection: decisionSelection !== null,
    isValidDecisionSelection,
    isSelectedDecisionOption,
    isDistributeTarget,
    distributeAllocated,
    distributeRemaining,
    distributeMinPerTarget,
    distributeMaxForCard,
    counterCreature,
    counterDistInner,
    isInManaSelectionMode: mana !== null,
    isManaValidSource: listHas(mana?.validSources, cardId),
    isManaSelected: listHas(mana?.selectedSources, cardId),
    isInTapForPowerMode: tapForPower !== null,
    isValidTapForPowerCreature: entityListHas(tapForPower?.validCreatures, cardId),
    isSelectedTapForPowerCreature: listHas(tapForPower?.selectedCreatures, cardId),
    isInConvokeMode: convoke !== null,
    isValidConvokeCreature: entityListHas(convoke?.validCreatures, cardId),
    isSelectedConvokeCreature: entityListHas(convoke?.selectedCreatures, cardId),
    isInTapForGenericMode: tapForGeneric !== null,
    isValidTapForGenericPermanent: entityListHas(tapForGeneric?.validPermanents, cardId),
    isSelectedTapForGenericPermanent: listHas(tapForGeneric?.selectedPermanents, cardId),
    isInHarmonizeMode: harmonize !== null,
    isValidHarmonizeCreature: entityListHas(harmonize?.validCreatures, cardId),
    isSelectedHarmonizeCreature: harmonize?.selectedCreature === cardId,
    isInAttackerMode,
    isInBlockerMode,
    isValidAttacker,
    isSelectedAsAttacker,
    bandIndex,
    isBandDropTarget,
    isValidBlocker,
    isSelectedAsBlocker,
    isAttackingInBlockerMode,
    isMustBeBlocked,
    isBystanderAttacker,
    isValidAttackTargetCard,
    isAttackTargetWithAttackers,
    isTargetedByAttacker,
    isBlockerDragActive: draggingBlockerId !== null,
    isDraggingThisBlocker: draggingBlockerId === cardId,
    isDraggingThisAttacker: draggingAttackerId === cardId,
  }
}

/** Subscribes a card to its own slice of the interaction state; see [CardInteractionState]. */
export function useCardInteractionState(input: CardInteractionInput): CardInteractionState {
  const { cardId, isCreature, isOpponentCard } = input
  return useGameStore(
    useShallow((state) => selectCardInteraction(state, { cardId, isCreature, isOpponentCard })),
  )
}
