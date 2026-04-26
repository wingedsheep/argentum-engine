/**
 * Pure functions for the action pipeline coordinator.
 *
 * - computePhases: determines the ordered phase list from action info flags
 * - mergeResult: applies a phase result to the accumulated action
 * - enterPhase: calls the appropriate start* method for a phase
 */
import type { EntityId, LegalActionInfo, GameAction, ClientGameState } from '@/types'
import type {
  PipelinePhase,
  PhaseResult,
  TargetingState,
  XSelectionState,
  ConvokeSelectionState,
  DelveSelectionState,
  CounterDistributionState,
  ManaColorSelectionState,
  DamageDistributionState,
} from '../types'

// ---------------------------------------------------------------------------
// Store method interface (decouples pure logic from Zustand)
// ---------------------------------------------------------------------------

export interface PipelineStoreMethods {
  startXSelection: (state: XSelectionState) => void
  startConvokeSelection: (state: ConvokeSelectionState) => void
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
  if (
    actionInfo.action.type === 'CastSpell' &&
    actionInfo.hasDelve &&
    actionInfo.validDelveCards &&
    actionInfo.validDelveCards.length > 0
  ) {
    const manaCostStr = actionInfo.manaCostString ?? ''
    const genericMatch = manaCostStr.match(/\{(\d+)\}/)
    const genericAmount = genericMatch ? parseInt(genericMatch[1]!, 10) : 0
    const maxDelve = Math.min(genericAmount, actionInfo.validDelveCards.length)
    if (maxDelve > 0) {
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

  // 4. Mana source selection (skipped when auto-tap is enabled, except for delve/convoke
  //    spells where the player should always confirm land selection after alternative payment)
  const hasAlternativePaymentPhase = phases.some((p) => p.type === 'delve' || p.type === 'convoke')
  if (
    actionInfo.availableManaSources && actionInfo.availableManaSources.length > 0 &&
    (hasAlternativePaymentPhase || !options?.autoTapEnabled)
  ) {
    phases.push({ type: 'manaSource' })
  }

  // 5. Cost payment (sacrifice/discard/tap/bounce/exile)
  if (actionInfo.additionalCostInfo?.costType) {
    const costType = actionInfo.additionalCostInfo.costType
    const costTypesNeedingSelection = [
      'SacrificePermanent',
      'SacrificeSelf',
      'SacrificeForCostReduction',
      'TapPermanents',
      'BouncePermanent',
      'DiscardCard',
      'ExileFromGraveyard',
      'ExileFromZone',
      'RevealCard',
      'Behold',
      'Blight',
      'Conspire',
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
    }
  }

  // 6. Targeting
  if (actionInfo.requiresTargets && actionInfo.validTargets && actionInfo.validTargets.length > 0) {
    phases.push({ type: 'targeting' })
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
        action.type === 'TurnFaceUp'
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
        const additionalCostPayment =
          costType === 'DiscardCard'
            ? { discardedCards: selectedTargets }
            : costType === 'ExileFromGraveyard'
              ? { exiledCards: selectedTargets }
              : costType === 'Behold'
                ? { beheldCards: selectedTargets }
                : costType === 'Blight'
                  ? { blightTargets: selectedTargets }
                  : { sacrificedPermanents: selectedTargets }
        return { ...action, additionalCostPayment }
      }
      if (action.type === 'ActivateAbility') {
        const costPayment =
          costType === 'TapPermanents'
            ? { tappedPermanents: selectedTargets }
            : costType === 'DiscardCard'
              ? { discardedCards: selectedTargets }
              : costType === 'BouncePermanent'
                ? { bouncedPermanents: selectedTargets }
                : costType === 'ExileFromGraveyard'
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
        if (card && card.zone?.zoneType === 'Graveyard') {
          return {
            type: 'Card' as const,
            cardId: targetId,
            ownerId: card.zone.ownerId,
            zone: 'Graveyard' as const,
          }
        }
        if (card && card.zone?.zoneType === 'Stack') {
          return { type: 'Spell' as const, spellEntityId: targetId }
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
      if (action.type === 'CastSpell') {
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
): void {
  switch (phase.type) {
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
      const genericAmount = genericMatch ? parseInt(genericMatch[1]!, 10) : 0
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

    case 'manaSource': {
      // Pass the accumulated action (may include xValue, delve, etc.)
      const modifiedActionInfo = { ...actionInfo, action }
      store.startManaSelection(modifiedActionInfo)
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
          break
        case 'SacrificeForCostReduction':
          validTargets = [...(costInfo.validSacrificeTargets ?? [])]
          minTargets = 0
          maxTargets = validTargets.length
          flags.isSacrificeSelection = true
          break
        case 'TapPermanents':
          validTargets = [...(costInfo.validTapTargets ?? [])]
          minTargets = costInfo.tapCount ?? 1
          maxTargets = costInfo.tapCount ?? 1
          flags.isSacrificeSelection = true
          flags.isTapPermanentSelection = true
          break
        case 'Conspire':
          validTargets = [...(costInfo.validTapTargets ?? [])]
          minTargets = costInfo.tapCount ?? 2
          maxTargets = costInfo.tapCount ?? 2
          flags.isSacrificeSelection = true
          flags.isTapPermanentSelection = true
          flags.targetDescription = costInfo.description
          break
        case 'BouncePermanent':
          validTargets = [...(costInfo.validBounceTargets ?? [])]
          minTargets = costInfo.bounceCount ?? 1
          maxTargets = costInfo.bounceCount ?? 1
          flags.isSacrificeSelection = true
          flags.isBounceSelection = true
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
          validTargets = [...(costInfo.validBeholdTargets ?? [])]
          minTargets = costInfo.beholdCount ?? 1
          maxTargets = costInfo.beholdCount ?? 1
          flags.isSacrificeSelection = true
          flags.isBeholdSelection = true
          flags.targetDescription = costInfo.description
          break
        case 'Blight':
          validTargets = [...(costInfo.validBlightTargets ?? [])]
          minTargets = 1
          maxTargets = 1
          flags.targetDescription = costInfo.description
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
      if (actionInfo.targetRequirements && actionInfo.targetRequirements.length > 1) {
        const firstReq = actionInfo.targetRequirements[0]!
        store.startTargeting({
          action,
          validTargets: [...firstReq.validTargets],
          selectedTargets: [],
          minTargets: firstReq.minTargets,
          maxTargets: firstReq.maxTargets,
          currentRequirementIndex: 0,
          allSelectedTargets: [],
          targetRequirements: actionInfo.targetRequirements,
          ...(firstReq.targetZone ? { targetZone: firstReq.targetZone } : {}),
          targetDescription: firstReq.description,
          totalRequirements: actionInfo.targetRequirements.length,
          ...(actionInfo.requiresDamageDistribution ? { requiresDamageDistribution: true } : {}),
        })
      } else {
        store.startTargeting({
          action,
          validTargets: [...(actionInfo.validTargets ?? [])],
          selectedTargets: [],
          minTargets: actionInfo.minTargets ?? actionInfo.targetCount ?? 1,
          maxTargets: actionInfo.targetCount ?? 1,
          ...(actionInfo.requiresDamageDistribution ? { requiresDamageDistribution: true } : {}),
        })
      }
      break
    }

    case 'manaColorChoice': {
      store.startManaColorSelection({ action })
      break
    }

    case 'damageDistribution': {
      // This phase is entered directly by advancePipeline, not via enterPhase.
      // See advancePipeline for damage distribution setup.
      break
    }
  }
}
