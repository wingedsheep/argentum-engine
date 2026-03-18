/**
 * Action resolver pipeline for useInteraction.
 *
 * Each resolver declares when it matches a LegalActionInfo and how to resolve it
 * (enter the appropriate selection UI). The pipeline replaces the monolithic if/else
 * chain in executeAction and the duplicated checks in canAutoExecute.
 */
import type { EntityId, GameAction, LegalActionInfo } from '../../types'
import type {
  TargetingState,
  XSelectionState,
  ConvokeSelectionState,
  CrewSelectionState,
  DelveSelectionState,
  ManaColorSelectionState,
  CounterDistributionState,
} from '../../store/slices/types'

export interface ActionContext {
  submitAction: (action: GameAction) => void
  selectCard: (id: EntityId | null) => void
  startTargeting: (state: TargetingState) => void
  startXSelection: (state: XSelectionState) => void
  startConvokeSelection: (state: ConvokeSelectionState) => void
  startCrewSelection: (state: CrewSelectionState) => void
  startDelveSelection: (state: DelveSelectionState) => void
  startManaColorSelection: (state: ManaColorSelectionState) => void
  startCounterDistribution: (state: CounterDistributionState) => void
  startPipeline: (actionInfo: LegalActionInfo) => void
}

export interface ActionResolver {
  matches: (actionInfo: LegalActionInfo) => boolean
  resolve: (actionInfo: LegalActionInfo, ctx: ActionContext) => void
}

// ---------------------------------------------------------------------------
// Resolvers (in priority order)
// ---------------------------------------------------------------------------

const repeatableAbilityResolver: ActionResolver = {
  matches: (info) =>
    info.action.type === 'ActivateAbility' &&
    !!info.maxRepeatableActivations &&
    info.maxRepeatableActivations > 1,

  resolve: (info, ctx) => {
    ctx.startXSelection({
      actionInfo: info,
      cardName: info.description,
      minX: 1,
      maxX: info.maxRepeatableActivations!,
      selectedX: 1,
      isRepeatCount: true,
    })
    ctx.selectCard(null)
  },
}

const counterRemovalXResolver: ActionResolver = {
  matches: (info) =>
    info.action.type === 'ActivateAbility' &&
    !!info.hasXCost &&
    !!info.additionalCostInfo?.counterRemovalCreatures &&
    info.additionalCostInfo.counterRemovalCreatures.length > 0,

  resolve: (info, ctx) => {
    const counterCreatures = info.additionalCostInfo!.counterRemovalCreatures!
    const distribution: Record<string, number> = {}
    for (const creature of counterCreatures) {
      distribution[creature.entityId] = 0
    }
    ctx.startCounterDistribution({
      actionInfo: info,
      cardName: info.description,
      xValue: 0,
      creatures: counterCreatures,
      distribution,
    })
    ctx.selectCard(null)
  },
}

const xCostResolver: ActionResolver = {
  matches: (info) =>
    (info.action.type === 'CastSpell' ||
      info.action.type === 'ActivateAbility' ||
      info.action.type === 'TurnFaceUp') &&
    !!info.hasXCost,

  resolve: (info, ctx) => {
    const action = info.action
    ctx.startXSelection({
      actionInfo: info,
      cardName:
        action.type === 'CastSpell'
          ? info.description.replace('Cast ', '')
          : info.description,
      minX: info.minX ?? 0,
      maxX: info.maxAffordableX ?? 0,
      selectedX: info.maxAffordableX ?? 0,
    })
    ctx.selectCard(null)
  },
}

const convokeResolver: ActionResolver = {
  matches: (info) =>
    info.action.type === 'CastSpell' &&
    !!info.hasConvoke &&
    !!info.validConvokeCreatures &&
    info.validConvokeCreatures.length > 0,

  resolve: (info, ctx) => {
    ctx.startConvokeSelection({
      actionInfo: info,
      cardName: info.description.replace('Cast ', ''),
      manaCost: info.manaCostString ?? '',
      selectedCreatures: [],
      validCreatures: info.validConvokeCreatures!,
    })
    ctx.selectCard(null)
  },
}

const delveResolver: ActionResolver = {
  matches: (info) => {
    if (
      info.action.type !== 'CastSpell' ||
      !info.hasDelve ||
      !info.validDelveCards ||
      info.validDelveCards.length === 0
    )
      return false
    const manaCostStr = info.manaCostString ?? ''
    const genericMatch = manaCostStr.match(/\{(\d+)\}/)
    const genericAmount = genericMatch ? parseInt(genericMatch[1]!, 10) : 0
    const maxDelve = Math.min(genericAmount, info.validDelveCards.length)
    return maxDelve > 0
  },

  resolve: (info, ctx) => {
    const manaCostStr = info.manaCostString ?? ''
    const genericMatch = manaCostStr.match(/\{(\d+)\}/)
    const genericAmount = genericMatch ? parseInt(genericMatch[1]!, 10) : 0
    const maxDelve = Math.min(genericAmount, info.validDelveCards!.length)

    ctx.startDelveSelection({
      actionInfo: info,
      cardName: info.description.replace('Cast ', ''),
      manaCost: manaCostStr,
      selectedCards: [],
      validCards: info.validDelveCards!,
      maxDelve,
      minDelveNeeded: info.minDelveNeeded ?? 0,
    })
    ctx.selectCard(null)
  },
}

const crewResolver: ActionResolver = {
  matches: (info) =>
    info.action.type === 'CrewVehicle' &&
    !!info.hasCrew &&
    !!info.validCrewCreatures &&
    info.validCrewCreatures.length > 0,

  resolve: (info, ctx) => {
    ctx.startCrewSelection({
      actionInfo: info,
      vehicleName: info.description.replace('Crew ', ''),
      crewPower: info.crewPower ?? 0,
      selectedCreatures: [],
      validCreatures: info.validCrewCreatures!,
    })
    ctx.selectCard(null)
  },
}

const turnFaceUpSacrificeResolver: ActionResolver = {
  matches: (info) =>
    info.action.type === 'TurnFaceUp' &&
    info.additionalCostInfo?.costType === 'Sacrifice',

  resolve: (info, ctx) => {
    const costInfo = info.additionalCostInfo!
    const returnCount = costInfo.sacrificeCount ?? 1
    const validTargets = costInfo.validSacrificeTargets ?? []

    ctx.startTargeting({
      action: info.action,
      validTargets: [...validTargets],
      selectedTargets: [],
      minTargets: returnCount,
      maxTargets: returnCount,
      isSacrificeSelection: true,
      pendingActionInfo: info,
    })
    ctx.selectCard(null)
  },
}

const turnFaceUpExileResolver: ActionResolver = {
  matches: (info) =>
    info.action.type === 'TurnFaceUp' &&
    info.additionalCostInfo?.costType === 'ExileFromZone',

  resolve: (info, ctx) => {
    const costInfo = info.additionalCostInfo!
    const exileCount = costInfo.exileMaxCount ?? 1
    const validTargets = costInfo.validExileTargets ?? []

    ctx.startTargeting({
      action: info.action,
      validTargets: [...validTargets],
      selectedTargets: [],
      minTargets: exileCount,
      maxTargets: exileCount,
      isSacrificeSelection: true,
      pendingActionInfo: info,
    })
    ctx.selectCard(null)
  },
}

const turnFaceUpRevealResolver: ActionResolver = {
  matches: (info) =>
    info.action.type === 'TurnFaceUp' &&
    info.additionalCostInfo?.costType === 'RevealCard',

  resolve: (info, ctx) => {
    const costInfo = info.additionalCostInfo!
    const revealCount = costInfo.discardCount ?? 1
    const validTargets = costInfo.validDiscardTargets ?? []

    ctx.startTargeting({
      action: info.action,
      validTargets: [...validTargets],
      selectedTargets: [],
      minTargets: revealCount,
      maxTargets: revealCount,
      isSacrificeSelection: true,
      isRevealSelection: true,
      pendingActionInfo: info,
    })
    ctx.selectCard(null)
  },
}

const sacrificeForCostReductionResolver: ActionResolver = {
  matches: (info) =>
    info.action.type === 'CastSpell' &&
    info.additionalCostInfo?.costType === 'SacrificeForCostReduction',

  resolve: (info, ctx) => {
    const validSacTargets = info.additionalCostInfo!.validSacrificeTargets ?? []

    ctx.startTargeting({
      action: info.action,
      validTargets: [...validSacTargets],
      selectedTargets: [],
      minTargets: 0,
      maxTargets: validSacTargets.length,
      isSacrificeSelection: true,
      pendingActionInfo: info,
    })
    ctx.selectCard(null)
  },
}

const sacrificePermanentResolver: ActionResolver = {
  matches: (info) =>
    (info.action.type === 'CastSpell' || info.action.type === 'ActivateAbility') &&
    (info.additionalCostInfo?.costType === 'SacrificePermanent' ||
      info.additionalCostInfo?.costType === 'SacrificeSelf'),

  resolve: (info, ctx) => {
    const action = info.action
    const costInfo = info.additionalCostInfo!
    const sacrificeCount = costInfo.sacrificeCount ?? 1
    const validSacTargets = costInfo.validSacrificeTargets ?? []

    // Auto-select for SacrificeSelf (obvious — always the source itself)
    if (costInfo.costType === 'SacrificeSelf' && validSacTargets.length === sacrificeCount) {
      if (action.type === 'CastSpell') {
        const actionWithCost = {
          ...action,
          additionalCostPayment: {
            sacrificedPermanents: [...validSacTargets],
          },
        }
        if (info.requiresTargets && info.validTargets && info.validTargets.length > 0) {
          ctx.startTargeting({
            action: actionWithCost,
            validTargets: [...info.validTargets],
            selectedTargets: [],
            minTargets: info.minTargets ?? info.targetCount ?? 1,
            maxTargets: info.targetCount ?? 1,
            ...(info.requiresDamageDistribution ? { pendingActionInfo: info } : {}),
          })
        } else {
          ctx.submitAction(actionWithCost)
        }
      } else if (action.type === 'ActivateAbility') {
        const actionWithCost = {
          ...action,
          costPayment: { sacrificedPermanents: [...validSacTargets] },
        }
        if (info.requiresTargets && info.validTargets && info.validTargets.length > 0) {
          ctx.startTargeting({
            action: actionWithCost,
            validTargets: [...info.validTargets],
            selectedTargets: [],
            minTargets: info.minTargets ?? info.targetCount ?? 1,
            maxTargets: info.targetCount ?? 1,
            ...(info.requiresDamageDistribution ? { pendingActionInfo: info } : {}),
          })
        } else if (info.requiresManaColorChoice) {
          ctx.startManaColorSelection({ action: actionWithCost })
        } else {
          ctx.submitAction(actionWithCost)
        }
      }
      ctx.selectCard(null)
      return
    }

    // SacrificePermanent always shows the sacrifice selection modal
    ctx.startTargeting({
      action: info.action,
      validTargets: [...validSacTargets],
      selectedTargets: [],
      minTargets: sacrificeCount,
      maxTargets: sacrificeCount,
      isSacrificeSelection: true,
      pendingActionInfo: info,
    })
    ctx.selectCard(null)
  },
}

const tapPermanentsResolver: ActionResolver = {
  matches: (info) =>
    info.action.type === 'ActivateAbility' &&
    info.additionalCostInfo?.costType === 'TapPermanents',

  resolve: (info, ctx) => {
    const costInfo = info.additionalCostInfo!
    ctx.startTargeting({
      action: info.action,
      validTargets: [...(costInfo.validTapTargets ?? [])],
      selectedTargets: [],
      minTargets: costInfo.tapCount ?? 1,
      maxTargets: costInfo.tapCount ?? 1,
      isSacrificeSelection: true,
      isTapPermanentSelection: true,
      pendingActionInfo: info,
    })
    ctx.selectCard(null)
  },
}

const bouncePermanentResolver: ActionResolver = {
  matches: (info) =>
    info.action.type === 'ActivateAbility' &&
    info.additionalCostInfo?.costType === 'BouncePermanent',

  resolve: (info, ctx) => {
    const costInfo = info.additionalCostInfo!
    ctx.startTargeting({
      action: info.action,
      validTargets: [...(costInfo.validBounceTargets ?? [])],
      selectedTargets: [],
      minTargets: costInfo.bounceCount ?? 1,
      maxTargets: costInfo.bounceCount ?? 1,
      isSacrificeSelection: true,
      isBounceSelection: true,
      pendingActionInfo: info,
    })
    ctx.selectCard(null)
  },
}

const discardCardResolver: ActionResolver = {
  matches: (info) =>
    (info.action.type === 'CastSpell' || info.action.type === 'ActivateAbility') &&
    info.additionalCostInfo?.costType === 'DiscardCard',

  resolve: (info, ctx) => {
    const costInfo = info.additionalCostInfo!
    ctx.startTargeting({
      action: info.action,
      validTargets: [...(costInfo.validDiscardTargets ?? [])],
      selectedTargets: [],
      minTargets: costInfo.discardCount ?? 1,
      maxTargets: costInfo.discardCount ?? 1,
      isSacrificeSelection: true,
      isDiscardSelection: true,
      pendingActionInfo: info,
    })
    ctx.selectCard(null)
  },
}

const exileFromGraveyardResolver: ActionResolver = {
  matches: (info) =>
    (info.action.type === 'CastSpell' || info.action.type === 'ActivateAbility') &&
    info.additionalCostInfo?.costType === 'ExileFromGraveyard',

  resolve: (info, ctx) => {
    const costInfo = info.additionalCostInfo!
    ctx.startTargeting({
      action: info.action,
      validTargets: [...(costInfo.validExileTargets ?? [])],
      selectedTargets: [],
      minTargets: costInfo.exileMinCount ?? 1,
      maxTargets: costInfo.exileMaxCount ?? costInfo.validExileTargets?.length ?? 1,
      isSacrificeSelection: true,
      pendingActionInfo: info,
      targetZone: 'Graveyard',
      targetDescription: costInfo.description,
      sourceCardName: info.description.replace(/^Cast /, '').replace(/^Activate /, ''),
    })
    ctx.selectCard(null)
  },
}

const manaColorResolver: ActionResolver = {
  matches: (info) =>
    info.action.type === 'ActivateAbility' && !!info.requiresManaColorChoice,

  resolve: (info, ctx) => {
    ctx.startManaColorSelection({ action: info.action })
    ctx.selectCard(null)
  },
}

const targetingResolver: ActionResolver = {
  matches: (info) =>
    !!info.requiresTargets && !!info.validTargets && info.validTargets.length > 0,

  resolve: (info, ctx) => {
    const action = info.action

    if (info.targetRequirements && info.targetRequirements.length > 1) {
      // Multi-target spell
      const firstReq = info.targetRequirements[0]!
      const targetingState: TargetingState = {
        action,
        validTargets: [...firstReq.validTargets],
        selectedTargets: [] as readonly EntityId[],
        minTargets: firstReq.minTargets,
        maxTargets: firstReq.maxTargets,
        currentRequirementIndex: 0,
        allSelectedTargets: [] as readonly (readonly EntityId[])[],
        targetRequirements: info.targetRequirements,
        ...(firstReq.targetZone ? { targetZone: firstReq.targetZone } : {}),
        targetDescription: firstReq.description,
        totalRequirements: info.targetRequirements.length,
      }
      if (info.requiresDamageDistribution) {
        ctx.startTargeting({ ...targetingState, pendingActionInfo: info })
      } else {
        ctx.startTargeting(targetingState)
      }
    } else {
      // Single-target spell
      const targetingState: TargetingState = {
        action,
        validTargets: [...info.validTargets!],
        selectedTargets: [] as readonly EntityId[],
        minTargets: info.minTargets ?? info.targetCount ?? 1,
        maxTargets: info.targetCount ?? 1,
      }
      if (info.requiresDamageDistribution) {
        ctx.startTargeting({ ...targetingState, pendingActionInfo: info })
      } else {
        ctx.startTargeting(targetingState)
      }
    }
    ctx.selectCard(null)
  },
}

const cyclingMenuResolver: ActionResolver = {
  matches: (info) =>
    info.action.type === 'CycleCard' || info.action.type === 'TypecycleCard',

  // This resolver only exists for needsInteraction — it should never actually be called
  // since cycling actions go through the action menu, not executeAction directly.
  resolve: (_info, _ctx) => {
    // No-op: cycling is handled by the action menu
  },
}

// ---------------------------------------------------------------------------
// Pipeline
// ---------------------------------------------------------------------------

/**
 * Ordered list of resolvers. The first one that matches wins.
 */
const resolvers: readonly ActionResolver[] = [
  repeatableAbilityResolver,
  counterRemovalXResolver,
  xCostResolver,
  convokeResolver,
  delveResolver,
  crewResolver,
  turnFaceUpSacrificeResolver,
  turnFaceUpExileResolver,
  turnFaceUpRevealResolver,
  sacrificeForCostReductionResolver,
  sacrificePermanentResolver,
  tapPermanentsResolver,
  bouncePermanentResolver,
  discardCardResolver,
  exileFromGraveyardResolver,
  manaColorResolver,
  targetingResolver,
  cyclingMenuResolver,
]

/**
 * Find the first matching resolver and execute it. Returns true if a resolver
 * handled the action, false if none matched (caller should submit directly).
 *
 * Crew and cycling are standalone single-phase flows. Everything else goes
 * through the pipeline coordinator which computes the full phase sequence up
 * front and advances through it.
 */
export function resolveAction(actionInfo: LegalActionInfo, ctx: ActionContext): boolean {
  // Crew and cycling are standalone — not part of the pipeline
  if (crewResolver.matches(actionInfo)) {
    crewResolver.resolve(actionInfo, ctx)
    return true
  }
  if (cyclingMenuResolver.matches(actionInfo)) {
    return true
  }

  // Everything else goes through the pipeline coordinator.
  // computePhases inside startPipeline decides what UI phases are needed;
  // if none, startPipeline submits directly.
  ctx.startPipeline(actionInfo)
  return true
}

/**
 * Returns true if the action requires interaction (selection UI) before it can
 * be submitted. This is the inverse of the old `canAutoExecute`.
 */
export function needsInteraction(actionInfo: LegalActionInfo): boolean {
  for (const resolver of resolvers) {
    if (resolver.matches(actionInfo)) {
      return true
    }
  }
  return false
}
