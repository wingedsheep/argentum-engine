import { EntityId } from './entities'

/**
 * Game actions that can be submitted to the server.
 * Matches backend GameAction.kt sealed hierarchy.
 *
 * Note: The client typically receives pre-built actions from the server
 * in the legalActions list and submits them back unchanged.
 */
export type GameAction =
  | PassPriorityAction
  | CastSpellAction
  | ActivateAbilityAction
  | CycleCardAction
  | TypecycleCardAction
  | CrewVehicleAction
  | PlayLandAction
  | TurnFaceUpAction
  | DeclareAttackersAction
  | DeclareBlockersAction
  | OrderBlockersAction
  | ChooseManaColorAction
  | SubmitDecisionAction
  | TakeMulliganAction
  | KeepHandAction
  | BottomCardsAction
  | ConcedeAction
  | UnlockRoomDoorAction

// =============================================================================
// Priority Actions
// =============================================================================

export interface PassPriorityAction {
  readonly type: 'PassPriority'
  readonly playerId: EntityId
}

// =============================================================================
// Spell Actions
// =============================================================================

/**
 * Polymorphic target types matching server's sealed interface.
 * The 'type' field is the class discriminator for kotlinx.serialization.
 */
export type ChosenTarget =
  | { readonly type: 'Player'; readonly playerId: EntityId }
  | { readonly type: 'Permanent'; readonly entityId: EntityId }
  | { readonly type: 'Card'; readonly cardId: EntityId; readonly ownerId: EntityId; readonly zone: string }
  | { readonly type: 'Spell'; readonly spellEntityId: EntityId }

export interface AdditionalCostPayment {
  readonly sacrificedPermanents?: readonly EntityId[]
  readonly discardedCards?: readonly EntityId[]
  readonly lifePaid?: number
  readonly exiledCards?: readonly EntityId[]
  readonly beheldCards?: readonly EntityId[]
  readonly tappedPermanents?: readonly EntityId[]
  readonly bouncedPermanents?: readonly EntityId[]
  readonly counterRemovals?: Readonly<Record<EntityId, number>>
  /**
   * Typed counter-removal entries — each entry removes `count` counters of
   * `counterType` from `entityId`. Preferred over the legacy `counterRemovals`
   * map (engine still accepts that as a fallback for older clients).
   */
  readonly distributedCounterRemovals?: ReadonlyArray<{
    entityId: EntityId
    counterType: string
    count: number
  }>
  readonly blightTargets?: readonly EntityId[]
  /** X chosen for `AdditionalCost.BlightVariable` (e.g., Soul Immolation). */
  readonly blightAmount?: number
}

export interface AlternativePaymentChoice {
  readonly delvedCards: readonly EntityId[]
  readonly convokedCreatures: Record<EntityId, ConvokePayment>
}

export interface ConvokePayment {
  readonly color?: string | null
}

export interface CastSpellAction {
  readonly type: 'CastSpell'
  readonly playerId: EntityId
  readonly cardId: EntityId
  readonly targets?: readonly ChosenTarget[]
  readonly xValue?: number | null
  readonly paymentStrategy?: PaymentStrategy
  readonly additionalCostPayment?: AdditionalCostPayment
  /** Alternative payment choices (Delve, Convoke) */
  readonly alternativePayment?: AlternativePaymentChoice
  /** Whether to cast this card face-down (for Morph creatures) */
  readonly castFaceDown?: boolean
  /** Whether to cast this spell with kicker */
  readonly wasKicked?: boolean
  /** Pre-chosen damage distribution for DividedDamageEffect spells (target ID -> damage amount) */
  readonly damageDistribution?: Record<EntityId, number>
  /**
   * Chosen modal mode indices (rule 700.2). Ordered; the same index may repeat when the
   * spell's ModalEffect has `allowRepeat = true` (Escalate/Spree).
   */
  readonly chosenModes?: readonly number[]
  /** Per-mode target bindings, aligned 1:1 with `chosenModes`. */
  readonly modeTargetsOrdered?: readonly (readonly ChosenTarget[])[]
  /** Per-mode DividedDamageEffect allocations (future). */
  readonly modeDamageDistribution?: Record<number, Record<EntityId, number>>
  /** Creatures tapped to pay Conspire's optional additional cost (two distinct IDs) */
  readonly conspiredCreatures?: readonly EntityId[]
  /**
   * For split-layout cards (Rooms, etc.): index into the card's `cardFaces` of the face being
   * cast. Required for SPLIT cards; null/omitted for normal single-face cards.
   */
  readonly faceIndex?: number | null
}

export type PaymentStrategy =
  | { readonly type: 'AutoPay' }
  | { readonly type: 'FromPool' }
  | { readonly type: 'Explicit'; readonly manaAbilitiesToActivate: readonly EntityId[] }

// =============================================================================
// Ability Actions
// =============================================================================

export interface ActivateAbilityAction {
  readonly type: 'ActivateAbility'
  readonly playerId: EntityId
  readonly sourceId: EntityId
  readonly abilityId: string
  readonly targets?: readonly ChosenTarget[]
  /** Payment choices for ability costs (sacrifice, etc.) */
  readonly costPayment?: AdditionalCostPayment
  /** Color chosen for "add one mana of any color" abilities */
  readonly manaColorChoice?: string
  /** Value of X for X-cost activated abilities */
  readonly xValue?: number | null
  /** Number of times to repeat this activation (for batch activation) */
  readonly repeatCount?: number
  readonly paymentStrategy?: PaymentStrategy
  /** Alternative payment choices (e.g., convoke for abilities like Heirloom Epic) */
  readonly alternativePayment?: AlternativePaymentChoice
}

// =============================================================================
// Cycling Actions
// =============================================================================

export interface CycleCardAction {
  readonly type: 'CycleCard'
  readonly playerId: EntityId
  readonly cardId: EntityId
  readonly paymentStrategy?: PaymentStrategy
}

export interface TypecycleCardAction {
  readonly type: 'TypecycleCard'
  readonly playerId: EntityId
  readonly cardId: EntityId
  readonly paymentStrategy?: PaymentStrategy
}

// =============================================================================
// Crew Actions
// =============================================================================

export interface CrewVehicleAction {
  readonly type: 'CrewVehicle'
  readonly playerId: EntityId
  readonly vehicleId: EntityId
  readonly crewCreatures: readonly EntityId[]
}

// =============================================================================
// Morph Actions
// =============================================================================

export interface TurnFaceUpAction {
  readonly type: 'TurnFaceUp'
  readonly playerId: EntityId
  readonly sourceId: EntityId
  readonly paymentStrategy?: PaymentStrategy
  readonly costTargetIds?: readonly EntityId[]
  readonly xValue?: number | null
}

// =============================================================================
// Land Actions
// =============================================================================

export interface PlayLandAction {
  readonly type: 'PlayLand'
  readonly playerId: EntityId
  readonly cardId: EntityId
}

// =============================================================================
// Combat Actions
// =============================================================================

export interface DeclareAttackersAction {
  readonly type: 'DeclareAttackers'
  readonly playerId: EntityId
  readonly attackers: Record<EntityId, EntityId>  // attacker -> defending player
  /**
   * Optional banding groupings (CR 702.21). Each entry is the set of attacker IDs forming
   * one band. All band members must also appear as keys in [attackers] with the same defender,
   * and at most one creature per band may lack the BANDING keyword.
   */
  readonly bands?: ReadonlyArray<ReadonlyArray<EntityId>>
}

export interface DeclareBlockersAction {
  readonly type: 'DeclareBlockers'
  readonly playerId: EntityId
  readonly blockers: Record<EntityId, readonly EntityId[]>  // blocker -> attackers
}

export interface OrderBlockersAction {
  readonly type: 'OrderBlockers'
  readonly playerId: EntityId
  readonly attackerId: EntityId
  readonly orderedBlockers: readonly EntityId[]
}

// =============================================================================
// Decision Actions
// =============================================================================

export interface ChooseManaColorAction {
  readonly type: 'ChooseManaColor'
  readonly playerId: EntityId
  readonly color: string
}

export interface DecisionResponse {
  readonly decisionId: string
  readonly selectedEntityIds?: readonly EntityId[]
  readonly selectedIndex?: number
  readonly confirmed?: boolean
}

export interface SubmitDecisionAction {
  readonly type: 'SubmitDecision'
  readonly playerId: EntityId
  readonly response: DecisionResponse
}

// =============================================================================
// Mulligan Actions
// =============================================================================

export interface TakeMulliganAction {
  readonly type: 'TakeMulligan'
  readonly playerId: EntityId
}

export interface KeepHandAction {
  readonly type: 'KeepHand'
  readonly playerId: EntityId
}

export interface BottomCardsAction {
  readonly type: 'BottomCards'
  readonly playerId: EntityId
  readonly cardIds: readonly EntityId[]
}

// =============================================================================
// Room Actions (CR 709.5e — special action, not the stack)
// =============================================================================

export interface UnlockRoomDoorAction {
  readonly type: 'UnlockRoomDoor'
  readonly playerId: EntityId
  readonly roomId: EntityId
  /**
   * Face id of the locked door being unlocked. Backend `RoomFaceId` is a `@JvmInline value class`
   * around a String, so kotlinx-serialization encodes it transparently as a JSON string.
   * Currently the face's printed name (e.g., "Ritual Chamber").
   */
  readonly faceId: string
  readonly paymentStrategy?: PaymentStrategy
}

// =============================================================================
// Concession
// =============================================================================

export interface ConcedeAction {
  readonly type: 'Concede'
  readonly playerId: EntityId
}

// =============================================================================
// Helper Functions
// =============================================================================

/**
 * Get the action type for display.
 */
export function getActionDisplayName(action: GameAction): string {
  return action.type
}

/**
 * Check if an action requires target selection.
 */
export function actionRequiresTargets(action: GameAction): boolean {
  if (action.type === 'CastSpell') {
    return (action.targets?.length ?? 0) > 0
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
    case 'CycleCard':
      return action.cardId
    case 'TypecycleCard':
      return action.cardId
    case 'ActivateAbility':
      return action.sourceId
    case 'TurnFaceUp':
      return action.sourceId
    case 'CrewVehicle':
      return action.vehicleId
    case 'UnlockRoomDoor':
      return action.roomId
    default:
      return null
  }
}
