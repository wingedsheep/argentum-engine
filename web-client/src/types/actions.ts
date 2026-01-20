import { Color, CombatDamageStep } from './enums'
import { EntityId, ZoneId } from './entities'

/**
 * Game actions that can be submitted to the server.
 * Matches backend GameAction.kt sealed hierarchy.
 *
 * Note: The client typically receives pre-built actions from the server
 * in the legalActions list and submits them back unchanged.
 */
export type GameAction =
  // Life Actions
  | GainLifeAction
  | LoseLifeAction
  | SetLifeAction
  | DealDamageToPlayerAction
  | DealDamageToCreatureAction
  // Mana Actions
  | AddManaAction
  | AddColorlessManaAction
  | EmptyManaPoolAction
  | ActivateManaAbilityAction
  // Card Drawing Actions
  | DrawCardAction
  | DiscardCardAction
  // Zone Movement Actions
  | MoveEntityAction
  | PutOntoBattlefieldAction
  | DestroyPermanentAction
  | SacrificePermanentAction
  | ExilePermanentAction
  | ReturnToHandAction
  // Tap/Untap Actions
  | TapAction
  | UntapAction
  | UntapAllAction
  // Counter Actions
  | AddCountersAction
  | RemoveCountersAction
  | AddPoisonCountersAction
  // Summoning Sickness Actions
  | RemoveSummoningSicknessAction
  | RemoveAllSummoningSicknessAction
  // Land Actions
  | PlayLandAction
  | ResetLandsPlayedAction
  // Library Actions
  | ShuffleLibraryAction
  // Combat Actions
  | BeginCombatAction
  | DeclareAttackerAction
  | DeclareBlockerAction
  | EndCombatAction
  | OrderBlockersAction
  | ResolveCombatDamageAction
  // Game Flow Actions
  | PassPriorityAction
  | EndGameAction
  | PlayerLosesAction
  | ResolveLegendRuleAction
  // Stack Resolution Actions
  | ResolveTopOfStackAction
  | CastSpellAction
  // Attachment Actions
  | AttachAction
  | DetachAction
  // State-Based Actions
  | CheckStateBasedActionsAction
  | ClearDamageAction
  // Turn/Step Actions
  | PerformCleanupStepAction
  | ExpireEndOfCombatEffectsAction
  | ExpireEffectsForPermanentAction
  | ResolveCleanupDiscardAction
  // Decision Actions
  | SubmitDecisionAction

// ============================================================================
// Life Actions
// ============================================================================

export interface GainLifeAction {
  readonly type: 'GainLife'
  readonly playerId: EntityId
  readonly amount: number
  readonly description: string
}

export interface LoseLifeAction {
  readonly type: 'LoseLife'
  readonly playerId: EntityId
  readonly amount: number
  readonly description: string
}

export interface SetLifeAction {
  readonly type: 'SetLife'
  readonly playerId: EntityId
  readonly amount: number
  readonly description: string
}

export interface DealDamageToPlayerAction {
  readonly type: 'DealDamageToPlayer'
  readonly targetPlayerId: EntityId
  readonly amount: number
  readonly sourceEntityId: EntityId | null
  readonly description: string
}

export interface DealDamageToCreatureAction {
  readonly type: 'DealDamageToCreature'
  readonly targetEntityId: EntityId
  readonly amount: number
  readonly sourceEntityId: EntityId | null
  readonly description: string
}

// ============================================================================
// Mana Actions
// ============================================================================

export interface AddManaAction {
  readonly type: 'AddMana'
  readonly playerId: EntityId
  readonly color: Color
  readonly amount: number
  readonly description: string
}

export interface AddColorlessManaAction {
  readonly type: 'AddColorlessMana'
  readonly playerId: EntityId
  readonly amount: number
  readonly description: string
}

export interface EmptyManaPoolAction {
  readonly type: 'EmptyManaPool'
  readonly playerId: EntityId
  readonly description: string
}

export interface ActivateManaAbilityAction {
  readonly type: 'ActivateManaAbility'
  readonly sourceEntityId: EntityId
  readonly abilityIndex: number
  readonly playerId: EntityId
  readonly description: string
}

// ============================================================================
// Card Drawing Actions
// ============================================================================

export interface DrawCardAction {
  readonly type: 'DrawCard'
  readonly playerId: EntityId
  readonly count: number
  readonly description: string
}

export interface DiscardCardAction {
  readonly type: 'DiscardCard'
  readonly playerId: EntityId
  readonly cardId: EntityId
  readonly description: string
}

// ============================================================================
// Zone Movement Actions
// ============================================================================

export interface MoveEntityAction {
  readonly type: 'MoveEntity'
  readonly entityId: EntityId
  readonly fromZone: ZoneId
  readonly toZone: ZoneId
  readonly toTop: boolean
  readonly description: string
}

export interface PutOntoBattlefieldAction {
  readonly type: 'PutOntoBattlefield'
  readonly entityId: EntityId
  readonly controllerId: EntityId
  readonly tapped: boolean
  readonly description: string
}

export interface DestroyPermanentAction {
  readonly type: 'DestroyPermanent'
  readonly entityId: EntityId
  readonly description: string
}

export interface SacrificePermanentAction {
  readonly type: 'SacrificePermanent'
  readonly entityId: EntityId
  readonly controllerId: EntityId
  readonly description: string
}

export interface ExilePermanentAction {
  readonly type: 'ExilePermanent'
  readonly entityId: EntityId
  readonly description: string
}

export interface ReturnToHandAction {
  readonly type: 'ReturnToHand'
  readonly entityId: EntityId
  readonly description: string
}

// ============================================================================
// Tap/Untap Actions
// ============================================================================

export interface TapAction {
  readonly type: 'Tap'
  readonly entityId: EntityId
  readonly description: string
}

export interface UntapAction {
  readonly type: 'Untap'
  readonly entityId: EntityId
  readonly description: string
}

export interface UntapAllAction {
  readonly type: 'UntapAll'
  readonly controllerId: EntityId
  readonly description: string
}

// ============================================================================
// Counter Actions
// ============================================================================

export interface AddCountersAction {
  readonly type: 'AddCounters'
  readonly entityId: EntityId
  readonly counterType: string
  readonly amount: number
  readonly description: string
}

export interface RemoveCountersAction {
  readonly type: 'RemoveCounters'
  readonly entityId: EntityId
  readonly counterType: string
  readonly amount: number
  readonly description: string
}

export interface AddPoisonCountersAction {
  readonly type: 'AddPoisonCounters'
  readonly playerId: EntityId
  readonly amount: number
  readonly description: string
}

// ============================================================================
// Summoning Sickness Actions
// ============================================================================

export interface RemoveSummoningSicknessAction {
  readonly type: 'RemoveSummoningSickness'
  readonly entityId: EntityId
  readonly description: string
}

export interface RemoveAllSummoningSicknessAction {
  readonly type: 'RemoveAllSummoningSickness'
  readonly controllerId: EntityId
  readonly description: string
}

// ============================================================================
// Land Actions
// ============================================================================

export interface PlayLandAction {
  readonly type: 'PlayLand'
  readonly cardId: EntityId
  readonly playerId: EntityId
  readonly description: string
}

export interface ResetLandsPlayedAction {
  readonly type: 'ResetLandsPlayed'
  readonly playerId: EntityId
  readonly description: string
}

// ============================================================================
// Library Actions
// ============================================================================

export interface ShuffleLibraryAction {
  readonly type: 'ShuffleLibrary'
  readonly playerId: EntityId
  readonly description: string
}

// ============================================================================
// Combat Actions
// ============================================================================

export interface BeginCombatAction {
  readonly type: 'BeginCombat'
  readonly attackingPlayerId: EntityId
  readonly defendingPlayerId: EntityId
  readonly description: string
}

export interface DeclareAttackerAction {
  readonly type: 'DeclareAttacker'
  readonly creatureId: EntityId
  readonly controllerId: EntityId
  readonly description: string
}

export interface DeclareBlockerAction {
  readonly type: 'DeclareBlocker'
  readonly blockerId: EntityId
  readonly attackerId: EntityId
  readonly controllerId: EntityId
  readonly description: string
}

export interface EndCombatAction {
  readonly type: 'EndCombat'
  readonly playerId: EntityId
  readonly description: string
}

export interface OrderBlockersAction {
  readonly type: 'OrderBlockers'
  readonly attackerId: EntityId
  readonly orderedBlockerIds: readonly EntityId[]
  readonly playerId: EntityId
  readonly description: string
}

export interface ResolveCombatDamageAction {
  readonly type: 'ResolveCombatDamage'
  readonly step: CombatDamageStep
  readonly preventionEffectIds: readonly EntityId[]
  readonly description: string
}

// ============================================================================
// Game Flow Actions
// ============================================================================

export interface PassPriorityAction {
  readonly type: 'PassPriority'
  readonly playerId: EntityId
  readonly description: string
}

export interface EndGameAction {
  readonly type: 'EndGame'
  readonly winnerId: EntityId | null
  readonly description: string
}

export interface PlayerLosesAction {
  readonly type: 'PlayerLoses'
  readonly playerId: EntityId
  readonly reason: string
  readonly description: string
}

export interface ResolveLegendRuleAction {
  readonly type: 'ResolveLegendRule'
  readonly controllerId: EntityId
  readonly legendaryName: string
  readonly keepEntityId: EntityId
  readonly description: string
}

// ============================================================================
// Stack Resolution Actions
// ============================================================================

export interface ResolveTopOfStackAction {
  readonly type: 'ResolveTopOfStack'
  readonly description: string
}

export interface ChosenTarget {
  readonly targetId: EntityId
  readonly targetType: 'creature' | 'player' | 'permanent' | 'spell' | 'card'
}

export interface CastSpellAction {
  readonly type: 'CastSpell'
  readonly cardId: EntityId
  readonly casterId: EntityId
  readonly fromZone: ZoneId
  readonly targets: readonly ChosenTarget[]
  readonly xValue: number | null
  readonly description: string
}

// ============================================================================
// Attachment Actions
// ============================================================================

export interface AttachAction {
  readonly type: 'Attach'
  readonly attachmentId: EntityId
  readonly targetId: EntityId
  readonly description: string
}

export interface DetachAction {
  readonly type: 'Detach'
  readonly attachmentId: EntityId
  readonly description: string
}

// ============================================================================
// State-Based Actions
// ============================================================================

export interface CheckStateBasedActionsAction {
  readonly type: 'CheckStateBasedActions'
  readonly description: string
}

export interface ClearDamageAction {
  readonly type: 'ClearDamage'
  readonly entityId: EntityId | null
  readonly description: string
}

// ============================================================================
// Turn/Step Actions
// ============================================================================

export interface PerformCleanupStepAction {
  readonly type: 'PerformCleanupStep'
  readonly playerId: EntityId
  readonly description: string
}

export interface ExpireEndOfCombatEffectsAction {
  readonly type: 'ExpireEndOfCombatEffects'
  readonly description: string
}

export interface ExpireEffectsForPermanentAction {
  readonly type: 'ExpireEffectsForPermanent'
  readonly permanentId: EntityId
  readonly description: string
}

export interface ResolveCleanupDiscardAction {
  readonly type: 'ResolveCleanupDiscard'
  readonly playerId: EntityId
  readonly cardsToDiscard: readonly EntityId[]
  readonly description: string
}

// ============================================================================
// Decision Actions
// ============================================================================

export interface DecisionResponse {
  readonly decisionId: string
  readonly selectedEntityIds?: readonly EntityId[]
  readonly selectedIndex?: number
  readonly confirmed?: boolean
}

export interface SubmitDecisionAction {
  readonly type: 'SubmitDecision'
  readonly response: DecisionResponse
  readonly description: string
}

// ============================================================================
// Helper Functions
// ============================================================================

/**
 * Get the action type for display.
 */
export function getActionDisplayName(action: GameAction): string {
  return action.description
}

/**
 * Check if an action requires target selection.
 */
export function actionRequiresTargets(action: GameAction): boolean {
  if (action.type === 'CastSpell') {
    return action.targets.length > 0
  }
  return false
}

/**
 * Get the entity ID that this action primarily affects.
 */
export function getActionSubject(action: GameAction): EntityId | null {
  switch (action.type) {
    case 'PlayLand':
      return action.cardId
    case 'CastSpell':
      return action.cardId
    case 'ActivateManaAbility':
      return action.sourceEntityId
    case 'Tap':
    case 'Untap':
      return action.entityId
    case 'DeclareAttacker':
      return action.creatureId
    case 'DeclareBlocker':
      return action.blockerId
    default:
      return null
  }
}
