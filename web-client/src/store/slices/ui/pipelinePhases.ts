/**
 * Pure functions for the action pipeline coordinator.
 *
 * - computePhases: determines the ordered phase list from action info flags
 * - mergeResult: applies a phase result to the accumulated action
 * - enterPhase: calls the appropriate start* method for a phase
 */
import type { ChosenTarget, EntityId, LegalActionInfo, GameAction, ClientGameState } from '@/types'
import { TAP_FOR_GENERIC_LABEL_IMPROVISE, TAP_FOR_GENERIC_LABEL_WATERBEND } from '@/types'
import type {
  PipelinePhase,
  PhaseResult,
  TargetingState,
  ModalModeSelectionState,
  XSelectionState,
  BlightVariableSelectionState,
  PayXLifeSelectionState,
  ConvokeSelectionState,
  TapForGenericSelectionState,
  HarmonizeSelectionState,
  DelveSelectionState,
  CounterDistributionState,
  ManaColorSelectionState,
  DamageDistributionState,
} from '../types'

/**
 * Non-battlefield, non-stack zones a targeted card can live in. A target whose card is in one of
 * these is sent to the server as a `Card` target tagged with its zone, so a cross-zone union
 * requirement (Sorceress's Schemes: graveyard ∪ exile) matches it to the correct clause.
 */
const CARD_TARGET_ZONES = new Set(['Graveyard', 'Exile', 'Hand', 'Library', 'Command'])

/** The entity a chosen target points at, whichever arm of the union it is. */
function targetEntityId(target: ChosenTarget): EntityId {
  switch (target.type) {
    case 'Player':
      return target.playerId
    case 'Permanent':
      return target.entityId
    case 'Spell':
      return target.spellEntityId
    case 'Card':
      return target.cardId
  }
}

// ---------------------------------------------------------------------------
// Store method interface (decouples pure logic from Zustand)
// ---------------------------------------------------------------------------

export interface PipelineStoreMethods {
  startModalModeSelection: (state: ModalModeSelectionState) => void
  startXSelection: (state: XSelectionState) => void
  startBlightVariableSelection: (state: BlightVariableSelectionState) => void
  startPayXLifeSelection: (state: PayXLifeSelectionState) => void
  startConvokeSelection: (state: ConvokeSelectionState) => void
  startTapForGenericSelection: (state: TapForGenericSelectionState) => void
  startHarmonizeSelection: (state: HarmonizeSelectionState) => void
  startDelveSelection: (state: DelveSelectionState) => void
  startCounterDistribution: (state: CounterDistributionState) => void
  startManaSelection: (actionInfo: LegalActionInfo) => void
  startManaColorSelection: (state: ManaColorSelectionState) => void
  startTargeting: (state: TargetingState) => void
  startDamageDistribution: (state: DamageDistributionState) => void
}

// ---------------------------------------------------------------------------
// computePhases — determines the ordered phase list
// ---------------------------------------------------------------------------

export interface ComputePhasesOptions {
  /** When true, skip the manaSource phase (server will auto-tap). */
  autoTapEnabled?: boolean
}

export function computePhases(actionInfo: LegalActionInfo, options?: ComputePhasesOptions): PipelinePhase[] {
  const phases: PipelinePhase[] = []

  // 0. Choose-N modal (Spree / "choose one or more"): the player picks the mode subset in a
  //    single panel. Usually this is the ONLY client phase — the additional cost depends on
  //    which modes are chosen, and per-mode targeting happens on the battlefield afterward, so
  //    the server drives targeting and mana payment once `chosenModes` is submitted. Return
  //    early so no manaSource/targeting phase runs against the (incomplete) base cost.
  //
  //    Exception: a "choose both if you blight" modal (Pyrrhic Strike) surfaces its blight path
  //    as a distinct cast variant whose `modalEnumeration` forces every mode. The engine only
  //    unlocks those extra modes once the submitted action carries `blightTargets` (and it reads
  //    the same field to apply the −1/−1 counters) — so we must collect the blight target via a
  //    `costPayment` phase here rather than dropping it. Without this, the action submits with
  //    no blight and the engine rejects it ("Too many modes chosen"). Per-mode targeting + mana
  //    still run server-side after submit.
  //
  //    Teamwork N (CR 702.194) takes the same exception for the same reason: "Choose one. If this
  //    spell was cast using teamwork, choose both instead" surfaces the teamwork cast as its own
  //    modal variant, and the second mode only unlocks once the submitted action carries the
  //    tapped creatures in `variableCostPermanents`.
  if (
    actionInfo.action.type === 'CastSpell' &&
    actionInfo.modalEnumeration &&
    actionInfo.modalEnumeration.chooseCount > 1
  ) {
    const modalPhases: PipelinePhase[] = [{ type: 'modalModes' }]
    const modalCostType = actionInfo.additionalCostInfo?.costType
    if (modalCostType === 'Blight' || modalCostType === 'TapForTotalPower') {
      modalPhases.push({ type: 'costPayment' })
    }
    return modalPhases
  }

  // 1. Counter distribution
  //    - X cost with counter removal creatures (Remove X +1/+1 counters), OR
  //    - Fixed distributed cost (RemoveCountersFromYourCreatures, e.g. Dawnhand Dissident)
  const hasCounterCreatures =
    (actionInfo.additionalCostInfo?.counterRemovalCreatures?.length ?? 0) > 0
  const hasFixedCounterCost =
    (actionInfo.additionalCostInfo?.distributedCounterRemovalTotal ?? 0) > 0
  if (actionInfo.hasXCost && hasCounterCreatures) {
    phases.push({ type: 'counterDistribution' })
  } else if (hasFixedCounterCost && hasCounterCreatures) {
    phases.push({ type: 'counterDistribution' })
  } else if (actionInfo.hasXCost) {
    phases.push({ type: 'xSelection' })
  } else if (
    actionInfo.action.type === 'ActivateAbility' &&
    actionInfo.maxRepeatableActivations != null &&
    actionInfo.maxRepeatableActivations > 1
  ) {
    phases.push({ type: 'xSelection' })
  }

  // 2. Delve
  //    Push when there's any generic mana that delve could pay for — either
  //    printed generic (Murderous Cut's {4}{B}) or generic that appears once an X
  //    cost has been resolved by xSelection (Empty the Pits' {X}{X}{B}{B}{B}{B}
  //    becomes {6}{B}{B}{B}{B} for X=3). `maxDelve` is recomputed against the
  //    merged action's xValue in enterPhase('delve').
  if (
    actionInfo.action.type === 'CastSpell' &&
    actionInfo.hasDelve &&
    actionInfo.validDelveCards &&
    actionInfo.validDelveCards.length > 0
  ) {
    const manaCostStr = actionInfo.manaCostString ?? ''
    const genericMatch = manaCostStr.match(/\{(\d+)\}/)
    const printedGeneric = genericMatch ? parseInt(genericMatch[1]!, 10) : 0
    const hasXGeneric = !!actionInfo.hasXCost && (actionInfo.maxAffordableX ?? 0) > 0
    if (printedGeneric > 0 || hasXGeneric) {
      phases.push({ type: 'delve' })
    }
  }

  // 3. Convoke (spells with Convoke keyword, or activated abilities with hasConvoke like Heirloom Epic)
  if (
    (actionInfo.action.type === 'CastSpell' || actionInfo.action.type === 'ActivateAbility') &&
    actionInfo.hasConvoke &&
    actionInfo.validConvokeCreatures &&
    actionInfo.validConvokeCreatures.length > 0
  ) {
    phases.push({ type: 'convoke' })
  }

  // 3a. Tap-for-generic: improvise (CR 702.126) on a spell, or a waterbend cost on a spell or
  //     activated ability. Optional either way — the player may tap eligible permanents to help
  //     pay the generic cost, or confirm with none selected and pay it all with mana.
  if (
    (actionInfo.action.type === 'CastSpell' || actionInfo.action.type === 'ActivateAbility') &&
    actionInfo.hasTapForGeneric &&
    actionInfo.validTapForGenericPermanents &&
    actionInfo.validTapForGenericPermanents.length > 0
  ) {
    phases.push({ type: 'tapForGeneric' })
  }

  // 3b. Harmonize creature-tap (cast from graveyard via Harmonize). Optional: the player
  //     may tap one creature to reduce the generic cost by its power. Runs after xSelection
  //     so the displayed cost reflects the chosen X (which {X} the tap can reduce).
  if (
    actionInfo.action.type === 'CastSpell' &&
    actionInfo.hasHarmonize &&
    actionInfo.validHarmonizeCreatures &&
    actionInfo.validHarmonizeCreatures.length > 0
  ) {
    phases.push({ type: 'harmonize' })
  }

  // 3c. Emerge sacrifice (CR 702.119). The sacrificed creature's mana value reduces the emerge
  //     cost, so — like the harmonize creature-tap above — the pick has to happen before any
  //     manual mana-source selection, which prices what's left to pay. Pushed here instead of in
  //     the generic cost-payment step below, which runs after mana selection.
  const isEmergeCast =
    actionInfo.action.type === 'CastSpell' &&
    actionInfo.action.alternativeCostType === 'EMERGE' &&
    (actionInfo.additionalCostInfo?.validSacrificeTargets?.length ?? 0) > 0
  if (isEmergeCast) {
    phases.push({ type: 'costPayment' })
  }

  // 4. Mana source selection (skipped when auto-tap is enabled, except for delve/convoke
  //    spells where the player should always confirm land selection after alternative payment)
  //
  //    A `tapForGeneric` phase deliberately does NOT force it. Improvise is *grantable over a
  //    whole card type* — Ironheart, Clever Champion gives every noncreature spell you cast
  //    improvise — so treating it like delve/convoke would silently turn auto-tap off for the
  //    rest of the game, two confirmation clicks per spell, on a board the player didn't opt into
  //    per-card. The server applies the taps and then auto-solves the remainder (exactly what the
  //    harmonize note below describes), so the manaSource step buys nothing under auto-tap.
  const hasAlternativePaymentPhase = phases.some(
    (p) => p.type === 'delve' || p.type === 'convoke',
  )
  const hasPhyrexianMana = (actionInfo.manaCostString ?? '').includes('/P}')
  //    A spell that taxes itself per target ("costs {W}{U} more for each target beyond the first")
  //    advertises only its one-target minimum, so picking sources before targeting would always
  //    under-tap and the server would reject the cast. Defer the manaSource step past targeting,
  //    exactly as an X cost puts `xSelection` before it. Under auto-tap neither phase runs and the
  //    server prices the submitted targets itself, so this only bites manual tappers.
  const manaAfterTargeting = actionInfo.manaCostPerExtraTarget != null
  const needsManaSource =
    ((actionInfo.availableManaSources?.length ?? 0) > 0 || hasPhyrexianMana) &&
    (hasAlternativePaymentPhase || hasPhyrexianMana || !options?.autoTapEnabled)
  if (needsManaSource && !manaAfterTargeting) {
    phases.push({ type: 'manaSource' })
  }

  // A cost priced off the spell's targets can't be paid before they're chosen — the engine
  // determines it at CR 601.2f, after targets are announced at 601.2c. `exileWeightPerTarget`
  // carries the per-target prices *and* is the signal to defer, exactly as `manaCostPerExtraTarget`
  // defers the manaSource step above. Urgent Necropsy: "collect evidence X, where X is the total
  // mana value of the permanents this spell targets."
  const costAfterTargeting =
    Object.keys(actionInfo.additionalCostInfo?.exileWeightPerTarget ?? {}).length > 0

  // 5. Cost payment (sacrifice/discard/tap/bounce/exile) — emerge already pushed its own above.
  if (actionInfo.additionalCostInfo?.costType && !isEmergeCast && !costAfterTargeting) {
    const costType = actionInfo.additionalCostInfo.costType
    const costTypesNeedingSelection = [
      'SacrificePermanent',
      'SacrificeSelf',
      'SacrificeForCostReduction',
      'TapPermanents',
      'BouncePermanent',
      'DiscardCard',
      'ExileFromGraveyard',
      'ExileFromHand',
      'CollectEvidence',
      'ExileForTotal',
      'ExileFromZone',
      'RevealCard',
      'Behold',
      'ChooseEntity',
      'Blight',
      'Conspire',
      'Casualty',
      'Craft',
      'TapForTotalPower',
    ]

    if (costTypesNeedingSelection.includes(costType)) {
      // SacrificeSelf with exact count is auto-selected (no UI needed)
      const isAutoSelectable =
        costType === 'SacrificeSelf' &&
        (actionInfo.additionalCostInfo.validSacrificeTargets?.length ?? 0) ===
          (actionInfo.additionalCostInfo.sacrificeCount ?? 1)

      if (!isAutoSelectable) {
        phases.push({ type: 'costPayment' })
      }
    } else if (costType === 'BlightVariable') {
      phases.push({ type: 'blightVariable' })
    } else if (costType === 'PayXLife') {
      phases.push({ type: 'payXLife' })
    }
  }

  // 6. Targeting
  if (actionInfo.requiresTargets && actionInfo.validTargets && actionInfo.validTargets.length > 0) {
    phases.push({ type: 'targeting' })
  }

  // 6a. The deferred cost-payment step for a target-priced cost (see phase 5): the targets are
  //     chosen now, so the threshold the picker gates on is the real one.
  if (costAfterTargeting) {
    phases.push({ type: 'costPayment' })
  }

  // 6b. The deferred manaSource step for a per-target-taxed spell (see phase 4): now that the
  //     targets are chosen, the price the player taps for is the real one.
  if (needsManaSource && manaAfterTargeting) {
    phases.push({ type: 'manaSource' })
  }

  // 7. Mana color choice (abilities only, after cost)
  if (actionInfo.requiresManaColorChoice) {
    phases.push({ type: 'manaColorChoice' })
  }

  // Note: damageDistribution is added dynamically by advancePipeline
  // when targeting completes with >1 targets and requiresDamageDistribution

  return phases
}

// ---------------------------------------------------------------------------
// mergeResult — applies a phase result to the accumulated action
// ---------------------------------------------------------------------------

export function mergeResult(
  action: GameAction,
  _actionInfo: LegalActionInfo,
  result: PhaseResult,
  gameState: ClientGameState,
): GameAction {
  switch (result.type) {
    case 'modalModes': {
      if (action.type === 'CastSpell') {
        // Targets are deferred to the engine's per-mode target pause, so submit modes only.
        return { ...action, chosenModes: [...result.chosenModes] }
      }
      return action
    }

    case 'counterDistribution': {
      if (action.type === 'ActivateAbility') {
        // Activated abilities only use this for `RemoveXPlusOnePlusOneCounters`,
        // which is single-type. Sum the typed entries per creature back into the
        // legacy `counterRemovals: Map<EntityId, Int>` shape the engine still
        // consumes for that path.
        const counterRemovals: Record<string, number> = {}
        for (const r of result.distributedCounterRemovals) {
          counterRemovals[r.entityId] = (counterRemovals[r.entityId] ?? 0) + r.count
        }
        return {
          ...action,
          xValue: result.xValue,
          costPayment: {
            ...action.costPayment,
            counterRemovals,
          },
        }
      }
      if (action.type === 'CastSpell') {
        // Fixed distributed counter cost (Dawnhand Dissident's linked-exile cost) —
        // send the typed payload so the engine knows exactly which counter type
        // came off each creature.
        return {
          ...action,
          additionalCostPayment: {
            ...action.additionalCostPayment,
            distributedCounterRemovals: [...result.distributedCounterRemovals],
          },
        }
      }
      return action
    }

    case 'xSelection': {
      if (result.isRepeatCount && action.type === 'ActivateAbility') {
        return { ...action, repeatCount: result.xValue }
      }
      if (
        action.type === 'CastSpell' ||
        action.type === 'ActivateAbility' ||
        action.type === 'TurnFaceUp' ||
        action.type === 'CycleCard'
      ) {
        return { ...action, xValue: result.xValue }
      }
      return action
    }

    case 'delve': {
      if (action.type === 'CastSpell') {
        return {
          ...action,
          alternativePayment: {
            delvedCards: result.delvedCards,
            convokedCreatures: action.alternativePayment?.convokedCreatures ?? {},
          },
        }
      }
      return action
    }

    case 'convoke': {
      if (action.type === 'CastSpell' || action.type === 'ActivateAbility') {
        return {
          ...action,
          alternativePayment: {
            delvedCards: action.alternativePayment?.delvedCards ?? [],
            convokedCreatures: result.convokedCreatures,
          },
        }
      }
      return action
    }

    case 'tapForGeneric': {
      if (action.type === 'CastSpell' || action.type === 'ActivateAbility') {
        return {
          ...action,
          alternativePayment: {
            delvedCards: action.alternativePayment?.delvedCards ?? [],
            convokedCreatures: action.alternativePayment?.convokedCreatures ?? {},
            tapForGenericPermanents: result.tapForGenericPermanents,
          },
        }
      }
      return action
    }

    case 'harmonize': {
      if (action.type === 'CastSpell') {
        return {
          ...action,
          alternativePayment: {
            delvedCards: action.alternativePayment?.delvedCards ?? [],
            convokedCreatures: action.alternativePayment?.convokedCreatures ?? {},
            harmonizeCreature: result.harmonizeCreature,
          },
        }
      }
      return action
    }

    case 'manaSource': {
      if (
        action.type === 'CastSpell' ||
        action.type === 'ActivateAbility' ||
        action.type === 'CycleCard' ||
        action.type === 'TypecycleCard' ||
        action.type === 'TurnFaceUp'
      ) {
        return {
          ...action,
          paymentStrategy: {
            type: 'Explicit' as const,
            manaAbilitiesToActivate: result.selectedSources,
            phyrexianLifePayments: result.phyrexianLifePayments ?? [],
          },
        }
      }
      return action
    }

    case 'blightVariable': {
      if (action.type === 'CastSpell') {
        return {
          ...action,
          additionalCostPayment: {
            ...action.additionalCostPayment,
            blightAmount: result.blightAmount,
          },
        }
      }
      return action
    }

    case 'payXLife': {
      if (action.type === 'CastSpell') {
        return {
          ...action,
          additionalCostPayment: {
            ...action.additionalCostPayment,
            payXLifeAmount: result.payXLifeAmount,
          },
        }
      }
      return action
    }

    case 'costPayment': {
      const { costType, selectedTargets } = result
      if (action.type === 'CastSpell') {
        // Conspire populates a dedicated field on CastSpell, not additionalCostPayment.
        if (costType === 'Conspire') {
          return { ...action, conspiredCreatures: selectedTargets }
        }
        // Teamwork (CR 702.194a) pays through the shared variable-count permanent channel.
        if (costType === 'TapForTotalPower') {
          return {
            ...action,
            additionalCostPayment: {
              ...action.additionalCostPayment,
              variableCostPermanents: selectedTargets,
            },
          }
        }
        // Casualty sacrifices a single chosen creature into its own dedicated field.
        if (costType === 'Casualty') {
          const casualtyCreature = selectedTargets[0]
          return casualtyCreature ? { ...action, casualtyCreature } : action
        }
        const fieldUpdate =
          costType === 'TapPermanents'
            ? { tappedPermanents: selectedTargets }
            : costType === 'DiscardCard'
              ? { discardedCards: selectedTargets }
              : costType === 'BouncePermanent'
                ? { bouncedPermanents: selectedTargets }
                : costType === 'ExileFromGraveyard' || costType === 'ExileFromHand' ||
                    costType === 'CollectEvidence' ||
                    costType === 'ExileForTotal'
                  ? { exiledCards: selectedTargets }
                  : costType === 'Behold' || costType === 'ChooseEntity'
                    ? { beheldCards: selectedTargets }
                    : costType === 'Blight' || costType === 'BlightVariable'
                      ? { blightTargets: selectedTargets }
                      : { sacrificedPermanents: selectedTargets }
        // Spread the existing additionalCostPayment so prior phases' fields
        // (e.g. `blightAmount` from a preceding BlightVariable phase) survive.
        const additionalCostPayment = {
          ...action.additionalCostPayment,
          ...fieldUpdate,
        }
        return { ...action, additionalCostPayment }
      }
      if (action.type === 'ActivateAbility') {
        // Station-style multi-select shortcut: when the tap cost is batch-activatable, the number
        // of creatures chosen becomes repeatCount — one activation per creature goes on the stack,
        // each tapping its own creature (the engine slices `tappedPermanents` per activation).
        if (
          costType === 'TapPermanents' &&
          (_actionInfo.additionalCostInfo?.tapBatchMaxActivations ?? 1) > 1
        ) {
          return {
            ...action,
            costPayment: { tappedPermanents: selectedTargets },
            repeatCount: selectedTargets.length,
          }
        }
        const costPayment =
          costType === 'TapPermanents'
            ? { tappedPermanents: selectedTargets }
            : costType === 'DiscardCard'
              ? { discardedCards: selectedTargets }
              : costType === 'BouncePermanent'
                ? { bouncedPermanents: selectedTargets }
                : costType === 'ExileFromGraveyard' || costType === 'ExileFromHand' ||
                    costType === 'Craft' ||
                    costType === 'CollectEvidence' || costType === 'ExileForTotal'
                  ? { exiledCards: selectedTargets }
                  : costType === 'Blight'
                    ? { blightTargets: selectedTargets }
                    : { sacrificedPermanents: selectedTargets }
        return { ...action, costPayment }
      }
      if (action.type === 'TurnFaceUp') {
        return { ...action, costTargetIds: selectedTargets }
      }
      return action
    }

    case 'targeting': {
      const targets = result.selectedTargets.map((targetId) => {
        const isPlayer = gameState.players.some((p) => p.playerId === targetId)
        if (isPlayer) {
          return { type: 'Player' as const, playerId: targetId }
        }
        const card = gameState.cards[targetId]
        const zoneType = card?.zone?.zoneType
        if (card && zoneType === 'Stack') {
          return { type: 'Spell' as const, spellEntityId: targetId }
        }
        // Card target in a non-battlefield zone — carry its actual zone so the server matches it
        // to the right clause (a cross-zone union like Sorceress's Schemes spans graveyard ∪ exile).
        if (card && zoneType && CARD_TARGET_ZONES.has(zoneType)) {
          return {
            type: 'Card' as const,
            cardId: targetId,
            ownerId: card.zone!.ownerId,
            zone: zoneType,
          }
        }
        return { type: 'Permanent' as const, entityId: targetId }
      })
      if (action.type === 'CastSpell' || action.type === 'ActivateAbility') {
        return { ...action, targets }
      }
      return action
    }

    case 'manaColorChoice': {
      if (action.type === 'ActivateAbility') {
        return { ...action, manaColorChoice: result.color }
      }
      return action
    }

    case 'damageDistribution': {
      if (action.type === 'CastSpell' || action.type === 'ActivateAbility') {
        return { ...action, damageDistribution: result.distribution }
      }
      return action
    }
  }
}

// ---------------------------------------------------------------------------
// enterPhase — calls the appropriate start* method with computed parameters
// ---------------------------------------------------------------------------

export function enterPhase(
  phase: PipelinePhase,
  actionInfo: LegalActionInfo,
  action: GameAction,
  store: PipelineStoreMethods,
  gameState?: ClientGameState,
): void {
  switch (phase.type) {
    case 'modalModes': {
      store.startModalModeSelection({
        actionInfo,
        cardName: actionInfo.description.replace('Cast ', ''),
        baseManaCost: actionInfo.manaCostString ?? '',
        enumeration: actionInfo.modalEnumeration!,
      })
      break
    }

    case 'counterDistribution': {
      const counterCreatures = actionInfo.additionalCostInfo!.counterRemovalCreatures!
      // Seed a zero allocation per (creature, counterType). When a creature
      // exposes multiple types via `availableCountersByType`, each type gets its
      // own slot; older payloads (pre-engine-fix) without the map fall back to
      // a single-type `+1/+1` slot keyed by total.
      const distribution: Record<string, Record<string, number>> = {}
      for (const creature of counterCreatures) {
        const byType = creature.availableCountersByType
        if (byType && Object.keys(byType).length > 0) {
          const inner: Record<string, number> = {}
          for (const counterType of Object.keys(byType)) inner[counterType] = 0
          distribution[creature.entityId] = inner
        } else {
          distribution[creature.entityId] = { '+1/+1': 0 }
        }
      }
      const fixedTotal = actionInfo.additionalCostInfo?.distributedCounterRemovalTotal
      store.startCounterDistribution({
        actionInfo,
        cardName: actionInfo.description,
        xValue: 0,
        creatures: counterCreatures,
        distribution,
        ...(fixedTotal && fixedTotal > 0
          ? { requiredTotal: fixedTotal, description: actionInfo.additionalCostInfo!.description }
          : {}),
      })
      break
    }

    case 'xSelection': {
      const isRepeatCount =
        actionInfo.action.type === 'ActivateAbility' &&
        !!actionInfo.maxRepeatableActivations &&
        actionInfo.maxRepeatableActivations > 1

      if (isRepeatCount) {
        store.startXSelection({
          actionInfo,
          cardName: actionInfo.description,
          minX: 1,
          maxX: actionInfo.maxRepeatableActivations!,
          selectedX: 1,
          isRepeatCount: true,
        })
      } else {
        store.startXSelection({
          actionInfo,
          cardName:
            action.type === 'CastSpell'
              ? actionInfo.description.replace('Cast ', '')
              : actionInfo.description,
          minX: actionInfo.minX ?? 0,
          maxX: actionInfo.maxAffordableX ?? 0,
          selectedX: actionInfo.maxAffordableX ?? 0,
        })
      }
      break
    }

    case 'delve': {
      const manaCostStr = actionInfo.manaCostString ?? ''
      const genericMatch = manaCostStr.match(/\{(\d+)\}/)
      const printedGeneric = genericMatch ? parseInt(genericMatch[1]!, 10) : 0
      // X mana resolves to xValue per {X} of generic, which delve can pay for like
      // any other generic. xValue is set by the preceding xSelection phase.
      const xCount = (manaCostStr.match(/\{X\}/g) ?? []).length
      const xValue = action.type === 'CastSpell' ? action.xValue ?? 0 : 0
      const genericAmount = printedGeneric + xCount * xValue
      const maxDelve = Math.min(genericAmount, actionInfo.validDelveCards!.length)
      store.startDelveSelection({
        actionInfo,
        cardName: actionInfo.description.replace('Cast ', ''),
        manaCost: manaCostStr,
        selectedCards: [],
        validCards: actionInfo.validDelveCards!,
        maxDelve,
        minDelveNeeded: actionInfo.minDelveNeeded ?? 0,
      })
      break
    }

    case 'convoke': {
      store.startConvokeSelection({
        actionInfo,
        cardName: actionInfo.description.replace('Cast ', ''),
        manaCost: actionInfo.manaCostString ?? '',
        selectedCreatures: [],
        validCreatures: actionInfo.validConvokeCreatures!,
      })
      break
    }

    case 'tapForGeneric': {
      // Fold {X} -> the chosen X so the HUD shows the real generic the taps reduce. xValue is
      // set by the preceding xSelection phase (0 if none). "waterbend {X}" spells carry an {X}.
      const xValue = action.type === 'CastSpell' ? action.xValue ?? 0 : 0
      const manaCost = (actionInfo.manaCostString ?? '').replace(/\{X\}/g, `{${xValue}}`)
      const genericIn = (cost: string): number => {
        let total = 0
        const genericRe = /\{(\d+)\}/g
        let gm: RegExpExecArray | null
        while ((gm = genericRe.exec(cost)) !== null) total += parseInt(gm[1]!, 10)
        return total
      }
      // Tap cap: an explicit spell-level waterbend {N}; else the chosen X for "waterbend {X}";
      // else the generic mana in the cost.
      //
      // Improvise counts only the *printed* generic, which is a known gap rather than the rule:
      // CR 702.126a bounds the taps at the generic in the spell's TOTAL cost, and X is locked in
      // before that total is determined (CR 601.2b/601.2f), so improvise does pay X-derived
      // generic — see the Whir of Invention ruling. Four printed cards have improvise with {X}
      // (Whir of Invention, Universal Surveillance, Saheeli's Directive, Battle at the Bridge);
      // none is implemented yet. The cap stays at the printed generic only because the *server*
      // does not credit taps against the X mana yet (see the TODO in CastSpellEnumerator's
      // maxAffordableX block) — offering more here would let the player tap artifacts the cast
      // then refuses to credit. Lift this together with that TODO.
      const isImprovise = actionInfo.tapForGenericLabel === TAP_FOR_GENERIC_LABEL_IMPROVISE
      const maxTaps = actionInfo.tapForGenericAmount ??
        (isImprovise
          ? genericIn(actionInfo.manaCostString ?? '')
          : actionInfo.hasXCost
            ? xValue
            : genericIn(manaCost))
      store.startTapForGenericSelection({
        actionInfo,
        // Strip the leading verb and the trailing " (waterbend {N})" disambiguator the enumerator
        // appends to the optional paid action's description — the HUD already says the verb and
        // shows the cost as mana pips, so the suffix would double the text and render {N} as
        // literal characters.
        cardName: actionInfo.description
          .replace('Cast ', '')
          .replace('Activate ', '')
          .replace(/\s*\(waterbend \{[^}]*\}\)\s*$/i, ''),
        manaCost,
        selectedPermanents: [],
        validPermanents: actionInfo.validTapForGenericPermanents!,
        maxTaps,
        label: actionInfo.tapForGenericLabel ?? TAP_FOR_GENERIC_LABEL_WATERBEND,
      })
      break
    }

    case 'harmonize': {
      // Expand {X} in the harmonize cost to the chosen X so the HUD shows the real generic
      // the tap will reduce. xValue is set by the preceding xSelection phase (0 if none).
      const xValue = action.type === 'CastSpell' ? action.xValue ?? 0 : 0
      const manaCost = (actionInfo.manaCostString ?? '').replace(/\{X\}/g, `{${xValue}}`)
      store.startHarmonizeSelection({
        actionInfo,
        cardName: actionInfo.description.replace('Cast ', '').replace(' (Harmonize)', ''),
        manaCost,
        selectedCreature: null,
        validCreatures: actionInfo.validHarmonizeCreatures!,
      })
      break
    }

    case 'manaSource': {
      // Pass the accumulated action (may include xValue, delve, etc.)
      const modifiedActionInfo = { ...actionInfo, action }
      store.startManaSelection(modifiedActionInfo)
      break
    }

    // Escalate (CR 702.120a) with a non-mana cost: pay it once per mode chosen beyond the first.
    // The enumeration advertises one extra mode's cost, so the counts scale by the modes picked in
    // the preceding modalModes phase; the server capped chooseCount at what the caster can pay, so
    // the candidate pool always covers them.
    case 'escalateCost': {
      const costInfo = actionInfo.modalEnumeration?.additionalCostPerExtraMode
      const extraModes =
        action.type === 'CastSpell' ? Math.max(0, (action.chosenModes?.length ?? 0) - 1) : 0
      if (!costInfo || extraModes === 0) return

      const scaled = (perMode: number | undefined) => (perMode ?? 1) * extraModes
      const flags: Partial<TargetingState> = { targetDescription: costInfo.description }
      let validTargets: EntityId[]
      let count: number

      switch (costInfo.costType) {
        case 'DiscardCard':
          validTargets = [...costInfo.validDiscardTargets!]
          count = scaled(costInfo.discardCount)
          flags.isSacrificeSelection = true
          flags.isDiscardSelection = true
          break
        case 'TapPermanents':
          validTargets = [...costInfo.validTapTargets!]
          count = scaled(costInfo.tapCount)
          flags.isSacrificeSelection = true
          flags.isTapPermanentSelection = true
          break
        case 'SacrificePermanent':
          validTargets = [...costInfo.validSacrificeTargets!]
          count = scaled(costInfo.sacrificeCount)
          flags.isSacrificeSelection = true
          break
        case 'BouncePermanent':
          validTargets = [...costInfo.validBounceTargets!]
          count = scaled(costInfo.bounceCount)
          flags.isSacrificeSelection = true
          flags.isBounceSelection = true
          break
        case 'ExileFromGraveyard':
          validTargets = [...costInfo.validExileTargets!]
          count = scaled(costInfo.exileMinCount)
          flags.isSacrificeSelection = true
          flags.targetZone = 'Graveyard'
          break
        default:
          return
      }

      store.startTargeting({
        action,
        validTargets,
        selectedTargets: [],
        minTargets: count,
        maxTargets: count,
        ...flags,
      })
      break
    }

    case 'costPayment': {
      const costInfo = actionInfo.additionalCostInfo!
      const costType = costInfo.costType!

      let validTargets: EntityId[]
      let minTargets: number
      let maxTargets: number
      const flags: Partial<TargetingState> = {}

      switch (costType) {
        case 'SacrificePermanent':
        case 'SacrificeSelf':
          validTargets = [...(costInfo.validSacrificeTargets ?? [])]
          minTargets = costInfo.sacrificeCount ?? 1
          maxTargets = costInfo.sacrificeCount ?? 1
          flags.isSacrificeSelection = true
          // The cost knows what it wants ("sacrifice an artifact"); without forwarding it the
          // overlay falls back to hardcoded "creature" wording and misdescribes every
          // non-creature sacrifice cost (Castle Doom's artifact, a land, an enchantment).
          flags.targetDescription = costInfo.description
          // Emerge (CR 702.119) is the one sacrifice cost where the choice changes the mana owed.
          // Forward the server's per-candidate costs so the overlay can price each pick.
          if (costInfo.costAfterSacrifice) {
            flags.costAfterSacrifice = costInfo.costAfterSacrifice
            if (actionInfo.manaCostString) flags.costBeforeSacrifice = actionInfo.manaCostString
          }
          break
        case 'SacrificeForCostReduction':
          validTargets = [...(costInfo.validSacrificeTargets ?? [])]
          minTargets = 0
          maxTargets = validTargets.length
          flags.isSacrificeSelection = true
          flags.targetDescription = costInfo.description
          break
        case 'TapPermanents': {
          validTargets = [...(costInfo.validTapTargets ?? [])]
          // Station-style multi-select shortcut (CR 702.184a): each chosen creature becomes its
          // own activation on the stack (one tap each). Let the player pick 1..N distinct
          // creatures; mergeResult sets repeatCount to the number chosen. tapBatchMaxActivations
          // is the server-validated cap (the count of legal tap targets). Picking one is the
          // unchanged single-station behaviour, so this is strictly additive.
          const batchMax = costInfo.tapBatchMaxActivations ?? 1
          if (batchMax > 1) {
            minTargets = 1
            maxTargets = Math.min(batchMax, validTargets.length)
          } else {
            // `tapCount = 0` from the server is the TapXPermanents sentinel for "variable,
            // equals the chosen X". The xSelection pipeline phase already ran and put the
            // chosen value on action.xValue; use it as the required tap count so the prompt
            // reads "Select 3/3" instead of "Select 0/0".
            const xTapCount =
              actionInfo.hasXCost &&
              (action.type === 'ActivateAbility' || action.type === 'CastSpell') &&
              typeof action.xValue === 'number'
                ? action.xValue
                : null
            const requiredTaps = xTapCount ?? costInfo.tapCount ?? 1
            minTargets = requiredTaps
            maxTargets = requiredTaps
          }
          flags.isSacrificeSelection = true
          flags.isTapPermanentSelection = true
          break
        }
        case 'Conspire':
          validTargets = [...(costInfo.validTapTargets ?? [])]
          minTargets = costInfo.tapCount ?? 2
          maxTargets = costInfo.tapCount ?? 2
          flags.isSacrificeSelection = true
          flags.isTapPermanentSelection = true
          flags.targetDescription = costInfo.description
          break
        // Teamwork N (CR 702.194a) — "tap any number of creatures you control with total power N
        // or more". The count is free, so the confirm gate is the power total, not minTargets.
        case 'TapForTotalPower': {
          const creatures = costInfo.tapForPowerCreatures ?? []
          validTargets = creatures.map((c) => c.entityId)
          minTargets = 0
          maxTargets = validTargets.length
          flags.isSacrificeSelection = true
          flags.isTapPermanentSelection = true
          flags.targetDescription = costInfo.description
          flags.requiredTotalPower = costInfo.tapForPowerRequired ?? 0
          const powerByEntityId: Record<EntityId, number> = {}
          for (const c of creatures) powerByEntityId[c.entityId] = c.power
          flags.powerByEntityId = powerByEntityId
          break
        }
        case 'BouncePermanent':
          validTargets = [...(costInfo.validBounceTargets ?? [])]
          minTargets = costInfo.bounceCount ?? 1
          maxTargets = costInfo.bounceCount ?? 1
          // isSacrificeSelection drives the on-battlefield "click a permanent you control"
          // selection behavior; isBounceSelection + targetDescription give the correct
          // "return to hand" wording (Sneak, CR 702.190 — it's a return, not a sacrifice).
          flags.isSacrificeSelection = true
          flags.isBounceSelection = true
          flags.targetDescription = costInfo.description
          break
        case 'DiscardCard':
          validTargets = [...(costInfo.validDiscardTargets ?? [])]
          minTargets = costInfo.discardCount ?? 1
          maxTargets = costInfo.discardCount ?? 1
          flags.isSacrificeSelection = true
          flags.isDiscardSelection = true
          break
        case 'ExileFromGraveyard':
          validTargets = [...(costInfo.validExileTargets ?? [])]
          minTargets = costInfo.exileMinCount ?? 1
          maxTargets = costInfo.exileMaxCount ?? costInfo.validExileTargets?.length ?? 1
          flags.isSacrificeSelection = true
          flags.targetZone = 'Graveyard'
          flags.targetDescription = costInfo.description
          flags.sourceCardName = actionInfo.description
            .replace(/^Cast /, '')
            .replace(/^Activate /, '')
          break
        case 'ExileFromHand':
          validTargets = [...(costInfo.validExileTargets ?? [])]
          minTargets = costInfo.exileMinCount ?? 1
          maxTargets = costInfo.exileMaxCount ?? costInfo.validExileTargets?.length ?? 1
          flags.isSacrificeSelection = true
          flags.targetZone = 'Hand'
          flags.targetDescription = costInfo.description
          break
        // The sum-gated graveyard exiles: collect evidence N (CR 701.59a) and "exile any number of
        // <filter> cards from your graveyard with N or more <measure>" (Baron Helmut Zemo). Any
        // number of cards, gated on a summed measure rather than a count — so the count bounds are
        // simply 1..whole graveyard and `minTotalWeight` carries the real constraint. Both read the
        // server's per-card weights: the client computes no measure of its own, which is what lets
        // one branch serve both costs.
        case 'CollectEvidence':
        case 'ExileForTotal': {
          validTargets = [...(costInfo.validExileTargets ?? [])]
          maxTargets = costInfo.validExileTargets?.length ?? 1
          flags.isSacrificeSelection = true
          flags.targetZone = 'Graveyard'
          flags.targetDescription = costInfo.description
          if (costInfo.exileMinTotalWeight != null) {
            // A target-priced threshold (Urgent Necropsy) reaches its real value only here, once
            // `computePhases` has run this step behind targeting: the server's floor plus the
            // per-target price of everything the caster actually chose (CR 601.2c → 601.2f). The
            // weights are still the server's — the client only adds up the ones it picked.
            const perTarget = costInfo.exileWeightPerTarget
            const chosenTargets =
              perTarget && (action.type === 'CastSpell' || action.type === 'ActivateAbility')
                ? (action.targets ?? [])
                : []
            const targetTotal = chosenTargets.reduce(
              (sum, t) => sum + (perTarget![targetEntityId(t)] ?? 0),
              0,
            )
            flags.minTotalWeight = costInfo.exileMinTotalWeight + targetTotal
            flags.cardWeights = { ...(costInfo.exileCardWeights ?? {}) }
            if (costInfo.exileWeightUnit != null) flags.weightUnit = costInfo.exileWeightUnit
          }
          // The count floor follows the sum gate rather than leading it: any threshold above 0
          // needs at least one card, but collecting evidence 0 — Urgent Necropsy cast with no
          // targets — is paid by exiling nothing, and a floor of 1 would make Confirm unreachable.
          minTargets = (flags.minTotalWeight ?? 0) > 0 ? 1 : 0
          break
        }
        case 'ExileFromZone':
          validTargets = [...(costInfo.validExileTargets ?? [])]
          minTargets = costInfo.exileMaxCount ?? 1
          maxTargets = costInfo.exileMaxCount ?? 1
          flags.isSacrificeSelection = true
          break
        case 'RevealCard':
          validTargets = [...(costInfo.validDiscardTargets ?? [])]
          minTargets = costInfo.discardCount ?? 1
          maxTargets = costInfo.discardCount ?? 1
          flags.isSacrificeSelection = true
          flags.isRevealSelection = true
          break
        case 'Behold':
        case 'ChooseEntity':
          validTargets = [...(costInfo.validBeholdTargets ?? [])]
          minTargets = costInfo.beholdCount ?? 1
          maxTargets = costInfo.beholdCount ?? 1
          flags.isSacrificeSelection = true
          flags.isBeholdSelection = true
          flags.targetDescription = costInfo.description
          break
        case 'Blight':
        case 'BlightVariable':
          validTargets = [...(costInfo.validBlightTargets ?? [])]
          minTargets = 1
          maxTargets = 1
          flags.targetDescription =
            costType === 'BlightVariable'
              ? `Choose a creature to receive ${(action as { additionalCostPayment?: { blightAmount?: number } }).additionalCostPayment?.blightAmount ?? 0} -1/-1 counter(s)`
              : costInfo.description
          break
        case 'Craft':
          // Craft materials span both battlefield and graveyard (CR 702.167a-b). Route to the
          // dedicated cross-zone overlay rather than the single-zone targeting flow.
          validTargets = [...(costInfo.validCraftMaterials ?? [])]
          minTargets = costInfo.craftMinCount ?? 1
          maxTargets = costInfo.craftMaxCount ?? validTargets.length
          flags.isCraftMaterialSelection = true
          flags.targetDescription = costInfo.description
          flags.sourceCardName = actionInfo.description
            .replace(/^Cast /, '')
            .replace(/^Activate /, '')
          break
        default:
          return
      }

      store.startTargeting({
        action,
        validTargets,
        selectedTargets: [],
        minTargets,
        maxTargets,
        ...flags,
      })
      break
    }

    case 'targeting': {
      // X-cost spells with "mana value X or less" target restrictions: the engine
      // enumerates targets permissively because X is unbound at enumeration time.
      // Once X has been chosen (cast-time or activation-time), narrow the candidate
      // list to creatures whose mana value the chosen X actually covers.
      const chosenX: number | null = (() => {
        if (gameState == null) return null
        if (action.type === 'CastSpell' || action.type === 'ActivateAbility' || action.type === 'TurnFaceUp') {
          return typeof action.xValue === 'number' ? action.xValue : null
        }
        return null
      })()
      const filterByX = (
        ids: readonly EntityId[],
        constrained: boolean | undefined,
      ): EntityId[] => {
        if (!constrained || chosenX == null || gameState == null) return [...ids]
        return ids.filter((id) => {
          const mv = gameState.cards[id]?.manaValue
          return typeof mv === 'number' && mv <= chosenX
        })
      }
      // Ent-Draught Basin: "target creature with power X" — keep only creatures whose
      // (projected) power equals the chosen X. The server enumerates permissively before X
      // is bound, so the client narrows the list once X is known.
      const filterByPowerX = (
        ids: readonly EntityId[],
        constrained: boolean | undefined,
      ): EntityId[] => {
        if (!constrained || chosenX == null || gameState == null) return [...ids]
        return ids.filter((id) => {
          const power = gameState.cards[id]?.power
          return typeof power === 'number' && power === chosenX
        })
      }
      // Likeness Looter / Rydia: "with mana value X" is an *equality* filter, not "X or less".
      const filterByExactManaValueX = (
        ids: readonly EntityId[],
        constrained: boolean | undefined,
      ): EntityId[] => {
        if (!constrained || chosenX == null || gameState == null) return [...ids]
        return ids.filter((id) => {
          const mv = gameState.cards[id]?.manaValue
          return typeof mv === 'number' && mv === chosenX
        })
      }
      const applyXFilters = (
        ids: readonly EntityId[],
        mvConstrained: boolean | undefined,
        powerConstrained: boolean | undefined,
        exactMvConstrained?: boolean | undefined,
      ): EntityId[] =>
        filterByExactManaValueX(
          filterByPowerX(filterByX(ids, mvConstrained), powerConstrained),
          exactMvConstrained,
        )

      // When a requirement's max-count is X-driven (TargetObject.dynamicMaxCount =
      // XValue server-side), the static `count` field is just a placeholder (often
      // its default of 1). After the user picks X via the cast-time xSelection
      // phase, the chosen X *replaces* the placeholder as the max — not min(static, X).
      const resolveMaxByX = (staticMax: number, constrained: boolean | undefined): number => {
        if (!constrained) return staticMax
        if (chosenX == null) return staticMax
        return chosenX
      }

      if (actionInfo.targetRequirements && actionInfo.targetRequirements.length > 1) {
        const firstReq = actionInfo.targetRequirements[0]!
        const maxTargets = resolveMaxByX(firstReq.maxTargets, firstReq.xConstrainsCount)
        store.startTargeting({
          action,
          validTargets: applyXFilters(firstReq.validTargets, firstReq.xConstrainsManaValue, firstReq.xConstrainsPower, firstReq.xConstrainsManaValueExactly),
          selectedTargets: [],
          minTargets: Math.min(firstReq.minTargets, maxTargets),
          maxTargets,
          currentRequirementIndex: 0,
          allSelectedTargets: [],
          targetRequirements: actionInfo.targetRequirements,
          ...(firstReq.targetZone ? { targetZone: firstReq.targetZone } : {}),
          targetDescription: firstReq.description,
          totalRequirements: actionInfo.targetRequirements.length,
          ...(actionInfo.requiresDamageDistribution ? { requiresDamageDistribution: true } : {}),
        })
      } else {
        const rawMax = actionInfo.targetCount ?? 1
        const maxTargets = resolveMaxByX(rawMax, actionInfo.xConstrainsTargetCount)
        const rawMin = actionInfo.minTargets ?? rawMax
        store.startTargeting({
          action,
          validTargets: applyXFilters(actionInfo.validTargets ?? [], actionInfo.xConstrainsTargetManaValue, actionInfo.xConstrainsTargetPower, actionInfo.xConstrainsTargetManaValueExactly),
          selectedTargets: [],
          minTargets: Math.min(rawMin, maxTargets),
          maxTargets,
          ...(actionInfo.requiresDamageDistribution ? { requiresDamageDistribution: true } : {}),
        })
      }
      break
    }

    case 'manaColorChoice': {
      store.startManaColorSelection({
        action,
        ...(actionInfo.availableManaColors ? { availableColors: actionInfo.availableManaColors } : {}),
      })
      break
    }

    case 'blightVariable': {
      const costInfo = actionInfo.additionalCostInfo
      if (!costInfo) return
      const cardName = actionInfo.description
        .replace(/^Cast /, '')
        .replace(/^Activate /, '')
      store.startBlightVariableSelection({
        actionInfo,
        cardName,
        maxX: costInfo.blightVariableMaxX ?? 0,
        selectedX: 0,
      })
      break
    }

    case 'payXLife': {
      const costInfo = actionInfo.additionalCostInfo
      if (!costInfo) return
      const cardName = actionInfo.description
        .replace(/^Cast /, '')
        .replace(/^Activate /, '')
      store.startPayXLifeSelection({
        actionInfo,
        cardName,
        maxX: costInfo.payXLifeMaxX ?? 0,
        selectedX: 0,
      })
      break
    }

    case 'damageDistribution': {
      // This phase is entered directly by advancePipeline, not via enterPhase.
      // See advancePipeline for damage distribution setup.
      break
    }
  }
}
