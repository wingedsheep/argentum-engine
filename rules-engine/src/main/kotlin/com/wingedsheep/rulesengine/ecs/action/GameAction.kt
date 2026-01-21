package com.wingedsheep.rulesengine.ecs.action

import com.wingedsheep.rulesengine.ability.AdditionalCostPayment
import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.decision.DecisionResponse
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.zone.ZoneType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic
import kotlinx.serialization.modules.subclass

/**
 * Sealed hierarchy of ECS-compatible game actions.
 *
 * These actions operate on EntityIds rather than CardId/PlayerId,
 * allowing direct integration with the GameState.
 *
 * Actions are immutable descriptions of state changes - they don't
 * execute themselves but are interpreted by GameActionHandler.
 */
@Serializable
sealed interface GameAction {
    val description: String
}

// =============================================================================
// Life Actions
// =============================================================================

@Serializable
@SerialName("GainLife")
data class GainLife(
    val playerId: EntityId,
    val amount: Int
) : GameAction {
    override val description: String = "Player gains $amount life"
}

@Serializable
@SerialName("LoseLife")
data class LoseLife(
    val playerId: EntityId,
    val amount: Int
) : GameAction {
    override val description: String = "Player loses $amount life"
}

@Serializable
@SerialName("SetLife")
data class SetLife(
    val playerId: EntityId,
    val amount: Int
) : GameAction {
    override val description: String = "Player's life total becomes $amount"
}

@Serializable
@SerialName("DealDamageToPlayer")
data class DealDamageToPlayer(
    val targetPlayerId: EntityId,
    val amount: Int,
    val sourceEntityId: EntityId? = null
) : GameAction {
    override val description: String = "Deal $amount damage to player"
}

@Serializable
@SerialName("DealDamageToCreature")
data class DealDamageToCreature(
    val targetEntityId: EntityId,
    val amount: Int,
    val sourceEntityId: EntityId? = null
) : GameAction {
    override val description: String = "Deal $amount damage to creature"
}

// =============================================================================
// Mana Actions
// =============================================================================

@Serializable
@SerialName("AddMana")
data class AddMana(
    val playerId: EntityId,
    val color: Color,
    val amount: Int = 1
) : GameAction {
    override val description: String = "Add $amount ${color.displayName} mana"
}

@Serializable
@SerialName("AddColorlessMana")
data class AddColorlessMana(
    val playerId: EntityId,
    val amount: Int = 1
) : GameAction {
    override val description: String = "Add $amount colorless mana"
}

@Serializable
@SerialName("EmptyManaPool")
data class EmptyManaPool(
    val playerId: EntityId
) : GameAction {
    override val description: String = "Empty mana pool"
}

/**
 * Activate a mana ability on a permanent.
 *
 * Mana abilities resolve immediately without using the stack (Rule 605).
 * This action handles:
 * 1. Paying the cost (typically tapping)
 * 2. Adding mana to the player's mana pool
 *
 * @param sourceEntityId The permanent with the mana ability
 * @param abilityIndex Index of the ability in the entity's mana abilities list
 * @param playerId The player activating the ability
 */
@Serializable
@SerialName("ActivateManaAbility")
data class ActivateManaAbility(
    val sourceEntityId: EntityId,
    val abilityIndex: Int,
    val playerId: EntityId
) : GameAction {
    override val description: String = "Activate mana ability"
}

// =============================================================================
// Card Drawing Actions
// =============================================================================

@Serializable
@SerialName("DrawCard")
data class DrawCard(
    val playerId: EntityId,
    val count: Int = 1
) : GameAction {
    override val description: String = "Draw $count card(s)"
}

@Serializable
@SerialName("DiscardCard")
data class DiscardCard(
    val playerId: EntityId,
    val cardId: EntityId
) : GameAction {
    override val description: String = "Discard a card"
}

// =============================================================================
// Zone Movement Actions
// =============================================================================

@Serializable
@SerialName("MoveEntity")
data class MoveEntity(
    val entityId: EntityId,
    val fromZone: ZoneId,
    val toZone: ZoneId,
    val toTop: Boolean = true
) : GameAction {
    override val description: String = "Move entity from $fromZone to $toZone"
}

@Serializable
@SerialName("PutOntoBattlefield")
data class PutOntoBattlefield(
    val entityId: EntityId,
    val controllerId: EntityId,
    val tapped: Boolean = false
) : GameAction {
    override val description: String = "Put entity onto battlefield${if (tapped) " tapped" else ""}"
}

@Serializable
@SerialName("DestroyPermanent")
data class DestroyPermanent(
    val entityId: EntityId
) : GameAction {
    override val description: String = "Destroy permanent"
}

@Serializable
@SerialName("SacrificePermanent")
data class SacrificePermanent(
    val entityId: EntityId,
    val controllerId: EntityId
) : GameAction {
    override val description: String = "Sacrifice permanent"
}

@Serializable
@SerialName("ExilePermanent")
data class ExilePermanent(
    val entityId: EntityId
) : GameAction {
    override val description: String = "Exile permanent"
}

@Serializable
@SerialName("ReturnToHand")
data class ReturnToHand(
    val entityId: EntityId
) : GameAction {
    override val description: String = "Return to owner's hand"
}

// =============================================================================
// Tap/Untap Actions
// =============================================================================

@Serializable
@SerialName("Tap")
data class Tap(
    val entityId: EntityId
) : GameAction {
    override val description: String = "Tap permanent"
}

@Serializable
@SerialName("Untap")
data class Untap(
    val entityId: EntityId
) : GameAction {
    override val description: String = "Untap permanent"
}

@Serializable
@SerialName("UntapAll")
data class UntapAll(
    val controllerId: EntityId
) : GameAction {
    override val description: String = "Untap all permanents"
}

// =============================================================================
// Counter Actions
// =============================================================================

@Serializable
@SerialName("AddCounters")
data class AddCounters(
    val entityId: EntityId,
    val counterType: String,
    val amount: Int = 1
) : GameAction {
    override val description: String = "Add $amount $counterType counter(s)"
}

@Serializable
@SerialName("RemoveCounters")
data class RemoveCounters(
    val entityId: EntityId,
    val counterType: String,
    val amount: Int = 1
) : GameAction {
    override val description: String = "Remove $amount $counterType counter(s)"
}

@Serializable
@SerialName("AddPoisonCounters")
data class AddPoisonCounters(
    val playerId: EntityId,
    val amount: Int
) : GameAction {
    override val description: String = "Add $amount poison counter(s)"
}

// =============================================================================
// Summoning Sickness Actions
// =============================================================================

@Serializable
@SerialName("RemoveSummoningSickness")
data class RemoveSummoningSickness(
    val entityId: EntityId
) : GameAction {
    override val description: String = "Remove summoning sickness"
}

@Serializable
@SerialName("RemoveAllSummoningSickness")
data class RemoveAllSummoningSickness(
    val controllerId: EntityId
) : GameAction {
    override val description: String = "Remove summoning sickness from all creatures"
}

// =============================================================================
// Land Actions
// =============================================================================

@Serializable
@SerialName("PlayLand")
data class PlayLand(
    val cardId: EntityId,
    val playerId: EntityId
) : GameAction {
    override val description: String = "Play land"
}

@Serializable
@SerialName("ResetLandsPlayed")
data class ResetLandsPlayed(
    val playerId: EntityId
) : GameAction {
    override val description: String = "Reset lands played this turn"
}

// =============================================================================
// Library Actions
// =============================================================================

@Serializable
@SerialName("ShuffleLibrary")
data class ShuffleLibrary(
    val playerId: EntityId
) : GameAction {
    override val description: String = "Shuffle library"
}

// =============================================================================
// Combat Actions
// =============================================================================

@Serializable
@SerialName("BeginCombat")
data class BeginCombat(
    val attackingPlayerId: EntityId,
    val defendingPlayerId: EntityId
) : GameAction {
    override val description: String = "Begin combat"
}

@Serializable
@SerialName("DeclareAttacker")
data class DeclareAttacker(
    val creatureId: EntityId,
    val controllerId: EntityId
) : GameAction {
    override val description: String = "Declare attacker"
}

@Serializable
@SerialName("DeclareBlocker")
data class DeclareBlocker(
    val blockerId: EntityId,
    val attackerId: EntityId,
    val controllerId: EntityId
) : GameAction {
    override val description: String = "Declare blocker"
}

@Serializable
@SerialName("EndCombat")
data class EndCombat(
    val playerId: EntityId
) : GameAction {
    override val description: String = "End combat"
}

/**
 * Order blockers for damage assignment.
 *
 * The attacking player chooses the order in which damage will be assigned
 * to creatures blocking a single attacker.
 */
@Serializable
@SerialName("OrderBlockers")
data class OrderBlockers(
    val attackerId: EntityId,
    val orderedBlockerIds: List<EntityId>,
    val playerId: EntityId
) : GameAction {
    override val description: String = "Order blockers for damage assignment"
}

/**
 * Resolve combat damage for a damage step.
 *
 * This calculates and applies all combat damage for either:
 * - The first strike damage step (first strike + double strike creatures)
 * - The regular damage step (all other creatures + double strike again)
 *
 * @param step The damage step to resolve (FIRST_STRIKE or REGULAR)
 * @param preventionEffectIds Optional list of entity IDs that are sources of
 *        active damage prevention effects (like Fog)
 */
@Serializable
@SerialName("ResolveCombatDamage")
data class ResolveCombatDamage(
    val step: CombatDamageStep,
    val preventionEffectIds: List<EntityId> = emptyList()
) : GameAction {
    override val description: String = "Resolve ${step.displayName} combat damage"
}

/**
 * Identifies which combat damage step is being resolved.
 */
@Serializable
enum class CombatDamageStep(val displayName: String) {
    FIRST_STRIKE("first strike"),
    REGULAR("regular")
}

// =============================================================================
// Game Flow Actions
// =============================================================================

@Serializable
@SerialName("PassPriority")
data class PassPriority(
    val playerId: EntityId
) : GameAction {
    override val description: String = "Pass priority"
}

@Serializable
@SerialName("EndGame")
data class EndGame(
    val winnerId: EntityId?
) : GameAction {
    override val description: String = winnerId?.let { "Game ends. Winner declared!" } ?: "Game ends in a draw"
}

@Serializable
@SerialName("PlayerLoses")
data class PlayerLoses(
    val playerId: EntityId,
    val reason: String
) : GameAction {
    override val description: String = "Player loses: $reason"
}

/**
 * Resolve a Legend Rule choice.
 *
 * When a player controls multiple legendary permanents with the same name,
 * they must choose which one to keep. This action resolves that choice.
 *
 * @param controllerId The player making the choice
 * @param legendaryName The name of the legendary permanent
 * @param keepEntityId The legendary permanent to keep (others are sacrificed)
 */
@Serializable
@SerialName("ResolveLegendRule")
data class ResolveLegendRule(
    val controllerId: EntityId,
    val legendaryName: String,
    val keepEntityId: EntityId
) : GameAction {
    override val description: String = "Choose legendary to keep: $legendaryName"
}

// =============================================================================
// Stack Resolution Actions
// =============================================================================

/**
 * Resolve the top item on the stack.
 *
 * This handles:
 * - Permanent spells: Move to battlefield
 * - Non-permanent spells: Execute effects, move to graveyard
 * - Triggered/activated abilities: Execute effects
 *
 * Also validates targets on resolution and fizzles if all targets are invalid.
 */
@Serializable
@SerialName("ResolveTopOfStack")
data class ResolveTopOfStack(
    val placeholder: Unit = Unit
) : GameAction {
    override val description: String = "Resolve top of stack"
}

/**
 * Cast a spell from hand (or another zone).
 *
 * This moves the card to the stack and adds SpellOnStackComponent.
 * Mana payment and target selection should be handled before this action.
 *
 * @param cardId The card being cast
 * @param casterId The player casting the spell
 * @param targets The chosen targets for the spell
 * @param xValue The value of X if applicable
 */
@Serializable
@SerialName("CastSpell")
data class CastSpell(
    val cardId: EntityId,
    val casterId: EntityId,
    val fromZone: com.wingedsheep.rulesengine.ecs.ZoneId,
    val targets: List<com.wingedsheep.rulesengine.ecs.event.ChosenTarget> = emptyList(),
    val xValue: Int? = null,
    /** Additional costs that were paid (sacrifice, discard, etc.) */
    val additionalCostPayment: AdditionalCostPayment = AdditionalCostPayment.NONE
) : GameAction {
    override val description: String = "Cast spell"
}

// =============================================================================
// Attachment Actions
// =============================================================================

@Serializable
@SerialName("Attach")
data class Attach(
    val attachmentId: EntityId,
    val targetId: EntityId
) : GameAction {
    override val description: String = "Attach to permanent"
}

@Serializable
@SerialName("Detach")
data class Detach(
    val attachmentId: EntityId
) : GameAction {
    override val description: String = "Detach from permanent"
}

// =============================================================================
// State-Based Actions
// =============================================================================

@Serializable
@SerialName("CheckStateBasedActions")
data class CheckStateBasedActions(
    val placeholder: Unit = Unit
) : GameAction {
    override val description: String = "Check state-based actions"
}

@Serializable
@SerialName("ClearDamage")
data class ClearDamage(
    val entityId: EntityId? = null  // null means all creatures
) : GameAction {
    override val description: String = "Clear damage"
}

// =============================================================================
// Turn/Step Actions
// =============================================================================

/**
 * Perform the cleanup step at the end of turn.
 *
 * This handles:
 * - Expiring "until end of turn" continuous effects
 * - Clearing damage from all creatures
 * - Discarding down to maximum hand size (triggers decision if needed)
 *
 * Note: The actual step advancement happens via TurnState.advanceStep()
 * after this action completes.
 */
@Serializable
@SerialName("PerformCleanupStep")
data class PerformCleanupStep(
    val playerId: EntityId
) : GameAction {
    override val description: String = "Perform cleanup step"
}

/**
 * Expire continuous effects at the end of combat.
 *
 * Called when combat ends to clean up "until end of combat" effects.
 */
@Serializable
@SerialName("ExpireEndOfCombatEffects")
data class ExpireEndOfCombatEffects(
    val placeholder: Unit = Unit
) : GameAction {
    override val description: String = "Expire end of combat effects"
}

/**
 * Expire effects when a permanent leaves the battlefield.
 *
 * Called to clean up effects with WhileOnBattlefield or WhileAttached duration
 * when the associated permanent leaves.
 */
@Serializable
@SerialName("ExpireEffectsForPermanent")
data class ExpireEffectsForPermanent(
    val permanentId: EntityId
) : GameAction {
    override val description: String = "Expire effects for leaving permanent"
}

/**
 * Resolve a cleanup discard decision.
 *
 * During cleanup, if a player has more cards than their maximum hand size,
 * they must discard down to that limit. This action resolves that choice.
 *
 * @param playerId The player who is discarding
 * @param cardsToDiscard The cards chosen to be discarded
 */
@Serializable
@SerialName("ResolveCleanupDiscard")
data class ResolveCleanupDiscard(
    val playerId: EntityId,
    val cardsToDiscard: List<EntityId>
) : GameAction {
    override val description: String = "Discard to hand size"
}

// =============================================================================
// Decision Submission Actions
// =============================================================================

/**
 * Submit a player's response to a pending decision.
 *
 * This is the primary mechanism for players to respond to effect-driven choices
 * like library searches, discard selections, sacrifice decisions, etc.
 *
 * When processed:
 * 1. Validates the response matches the pending decision
 * 2. Uses [DecisionResumer] to complete the effect
 * 3. Clears the pending decision from game state
 *
 * @param response The player's response to the pending decision
 * @see com.wingedsheep.rulesengine.decision.DecisionResumer
 */
@Serializable
@SerialName("SubmitDecision")
data class SubmitDecision(
    val response: DecisionResponse
) : GameAction {
    override val description: String = "Submit decision response"
}

// =============================================================================
// Serialization Module
// =============================================================================

/**
 * SerializersModule for polymorphic GameAction serialization.
 * Required for JSON serialization/deserialization of GameAction subtypes.
 */
val gameActionSerializersModule = SerializersModule {
    polymorphic(GameAction::class) {
        subclass(GainLife::class)
        subclass(LoseLife::class)
        subclass(SetLife::class)
        subclass(DealDamageToPlayer::class)
        subclass(DealDamageToCreature::class)
        subclass(AddMana::class)
        subclass(AddColorlessMana::class)
        subclass(EmptyManaPool::class)
        subclass(ActivateManaAbility::class)
        subclass(DrawCard::class)
        subclass(DiscardCard::class)
        subclass(MoveEntity::class)
        subclass(PutOntoBattlefield::class)
        subclass(DestroyPermanent::class)
        subclass(SacrificePermanent::class)
        subclass(ExilePermanent::class)
        subclass(ReturnToHand::class)
        subclass(Tap::class)
        subclass(Untap::class)
        subclass(UntapAll::class)
        subclass(AddCounters::class)
        subclass(RemoveCounters::class)
        subclass(AddPoisonCounters::class)
        subclass(RemoveSummoningSickness::class)
        subclass(RemoveAllSummoningSickness::class)
        subclass(PlayLand::class)
        subclass(ResetLandsPlayed::class)
        subclass(ShuffleLibrary::class)
        subclass(BeginCombat::class)
        subclass(DeclareAttacker::class)
        subclass(DeclareBlocker::class)
        subclass(EndCombat::class)
        subclass(OrderBlockers::class)
        subclass(ResolveCombatDamage::class)
        subclass(PassPriority::class)
        subclass(EndGame::class)
        subclass(PlayerLoses::class)
        subclass(ResolveLegendRule::class)
        subclass(ResolveTopOfStack::class)
        subclass(CastSpell::class)
        subclass(Attach::class)
        subclass(Detach::class)
        subclass(CheckStateBasedActions::class)
        subclass(ClearDamage::class)
        subclass(PerformCleanupStep::class)
        subclass(ExpireEndOfCombatEffects::class)
        subclass(ExpireEffectsForPermanent::class)
        subclass(ResolveCleanupDiscard::class)
        subclass(SubmitDecision::class)
    }
}
