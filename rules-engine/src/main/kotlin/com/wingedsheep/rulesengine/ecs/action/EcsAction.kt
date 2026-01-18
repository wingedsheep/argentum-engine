package com.wingedsheep.rulesengine.ecs.action

import com.wingedsheep.rulesengine.core.Color
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.zone.ZoneType
import kotlinx.serialization.Serializable

/**
 * Sealed hierarchy of ECS-compatible game actions.
 *
 * These actions operate on EntityIds rather than CardId/PlayerId,
 * allowing direct integration with the EcsGameState.
 *
 * Actions are immutable descriptions of state changes - they don't
 * execute themselves but are interpreted by EcsActionHandler.
 */
@Serializable
sealed interface EcsAction {
    val description: String
}

// =============================================================================
// Life Actions
// =============================================================================

@Serializable
data class EcsGainLife(
    val playerId: EntityId,
    val amount: Int
) : EcsAction {
    override val description: String = "Player gains $amount life"
}

@Serializable
data class EcsLoseLife(
    val playerId: EntityId,
    val amount: Int
) : EcsAction {
    override val description: String = "Player loses $amount life"
}

@Serializable
data class EcsSetLife(
    val playerId: EntityId,
    val amount: Int
) : EcsAction {
    override val description: String = "Player's life total becomes $amount"
}

@Serializable
data class EcsDealDamageToPlayer(
    val targetPlayerId: EntityId,
    val amount: Int,
    val sourceEntityId: EntityId? = null
) : EcsAction {
    override val description: String = "Deal $amount damage to player"
}

@Serializable
data class EcsDealDamageToCreature(
    val targetEntityId: EntityId,
    val amount: Int,
    val sourceEntityId: EntityId? = null
) : EcsAction {
    override val description: String = "Deal $amount damage to creature"
}

// =============================================================================
// Mana Actions
// =============================================================================

@Serializable
data class EcsAddMana(
    val playerId: EntityId,
    val color: Color,
    val amount: Int = 1
) : EcsAction {
    override val description: String = "Add $amount ${color.displayName} mana"
}

@Serializable
data class EcsAddColorlessMana(
    val playerId: EntityId,
    val amount: Int = 1
) : EcsAction {
    override val description: String = "Add $amount colorless mana"
}

@Serializable
data class EcsEmptyManaPool(
    val playerId: EntityId
) : EcsAction {
    override val description: String = "Empty mana pool"
}

// =============================================================================
// Card Drawing Actions
// =============================================================================

@Serializable
data class EcsDrawCard(
    val playerId: EntityId,
    val count: Int = 1
) : EcsAction {
    override val description: String = "Draw $count card(s)"
}

@Serializable
data class EcsDiscardCard(
    val playerId: EntityId,
    val cardId: EntityId
) : EcsAction {
    override val description: String = "Discard a card"
}

// =============================================================================
// Zone Movement Actions
// =============================================================================

@Serializable
data class EcsMoveEntity(
    val entityId: EntityId,
    val fromZone: ZoneId,
    val toZone: ZoneId,
    val toTop: Boolean = true
) : EcsAction {
    override val description: String = "Move entity from $fromZone to $toZone"
}

@Serializable
data class EcsPutOntoBattlefield(
    val entityId: EntityId,
    val controllerId: EntityId,
    val tapped: Boolean = false
) : EcsAction {
    override val description: String = "Put entity onto battlefield${if (tapped) " tapped" else ""}"
}

@Serializable
data class EcsDestroyPermanent(
    val entityId: EntityId
) : EcsAction {
    override val description: String = "Destroy permanent"
}

@Serializable
data class EcsSacrificePermanent(
    val entityId: EntityId,
    val controllerId: EntityId
) : EcsAction {
    override val description: String = "Sacrifice permanent"
}

@Serializable
data class EcsExilePermanent(
    val entityId: EntityId
) : EcsAction {
    override val description: String = "Exile permanent"
}

@Serializable
data class EcsReturnToHand(
    val entityId: EntityId
) : EcsAction {
    override val description: String = "Return to owner's hand"
}

// =============================================================================
// Tap/Untap Actions
// =============================================================================

@Serializable
data class EcsTap(
    val entityId: EntityId
) : EcsAction {
    override val description: String = "Tap permanent"
}

@Serializable
data class EcsUntap(
    val entityId: EntityId
) : EcsAction {
    override val description: String = "Untap permanent"
}

@Serializable
data class EcsUntapAll(
    val controllerId: EntityId
) : EcsAction {
    override val description: String = "Untap all permanents"
}

// =============================================================================
// Counter Actions
// =============================================================================

@Serializable
data class EcsAddCounters(
    val entityId: EntityId,
    val counterType: String,
    val amount: Int = 1
) : EcsAction {
    override val description: String = "Add $amount $counterType counter(s)"
}

@Serializable
data class EcsRemoveCounters(
    val entityId: EntityId,
    val counterType: String,
    val amount: Int = 1
) : EcsAction {
    override val description: String = "Remove $amount $counterType counter(s)"
}

@Serializable
data class EcsAddPoisonCounters(
    val playerId: EntityId,
    val amount: Int
) : EcsAction {
    override val description: String = "Add $amount poison counter(s)"
}

// =============================================================================
// Summoning Sickness Actions
// =============================================================================

@Serializable
data class EcsRemoveSummoningSickness(
    val entityId: EntityId
) : EcsAction {
    override val description: String = "Remove summoning sickness"
}

@Serializable
data class EcsRemoveAllSummoningSickness(
    val controllerId: EntityId
) : EcsAction {
    override val description: String = "Remove summoning sickness from all creatures"
}

// =============================================================================
// Land Actions
// =============================================================================

@Serializable
data class EcsPlayLand(
    val cardId: EntityId,
    val playerId: EntityId
) : EcsAction {
    override val description: String = "Play land"
}

@Serializable
data class EcsResetLandsPlayed(
    val playerId: EntityId
) : EcsAction {
    override val description: String = "Reset lands played this turn"
}

// =============================================================================
// Library Actions
// =============================================================================

@Serializable
data class EcsShuffleLibrary(
    val playerId: EntityId
) : EcsAction {
    override val description: String = "Shuffle library"
}

// =============================================================================
// Combat Actions
// =============================================================================

@Serializable
data class EcsBeginCombat(
    val attackingPlayerId: EntityId,
    val defendingPlayerId: EntityId
) : EcsAction {
    override val description: String = "Begin combat"
}

@Serializable
data class EcsDeclareAttacker(
    val creatureId: EntityId,
    val controllerId: EntityId
) : EcsAction {
    override val description: String = "Declare attacker"
}

@Serializable
data class EcsDeclareBlocker(
    val blockerId: EntityId,
    val attackerId: EntityId,
    val controllerId: EntityId
) : EcsAction {
    override val description: String = "Declare blocker"
}

@Serializable
data class EcsEndCombat(
    val playerId: EntityId
) : EcsAction {
    override val description: String = "End combat"
}

/**
 * Order blockers for damage assignment.
 *
 * The attacking player chooses the order in which damage will be assigned
 * to creatures blocking a single attacker.
 */
@Serializable
data class EcsOrderBlockers(
    val attackerId: EntityId,
    val orderedBlockerIds: List<EntityId>,
    val playerId: EntityId
) : EcsAction {
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
data class EcsResolveCombatDamage(
    val step: CombatDamageStep,
    val preventionEffectIds: List<EntityId> = emptyList()
) : EcsAction {
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
data class EcsPassPriority(
    val playerId: EntityId
) : EcsAction {
    override val description: String = "Pass priority"
}

@Serializable
data class EcsEndGame(
    val winnerId: EntityId?
) : EcsAction {
    override val description: String = winnerId?.let { "Game ends. Winner declared!" } ?: "Game ends in a draw"
}

@Serializable
data class EcsPlayerLoses(
    val playerId: EntityId,
    val reason: String
) : EcsAction {
    override val description: String = "Player loses: $reason"
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
data class EcsResolveTopOfStack(
    val placeholder: Unit = Unit
) : EcsAction {
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
data class EcsCastSpell(
    val cardId: EntityId,
    val casterId: EntityId,
    val fromZone: com.wingedsheep.rulesengine.ecs.ZoneId,
    val targets: List<com.wingedsheep.rulesengine.ecs.event.EcsChosenTarget> = emptyList(),
    val xValue: Int? = null
) : EcsAction {
    override val description: String = "Cast spell"
}

// =============================================================================
// Attachment Actions
// =============================================================================

@Serializable
data class EcsAttach(
    val attachmentId: EntityId,
    val targetId: EntityId
) : EcsAction {
    override val description: String = "Attach to permanent"
}

@Serializable
data class EcsDetach(
    val attachmentId: EntityId
) : EcsAction {
    override val description: String = "Detach from permanent"
}

// =============================================================================
// State-Based Actions
// =============================================================================

@Serializable
data class EcsCheckStateBasedActions(
    val placeholder: Unit = Unit
) : EcsAction {
    override val description: String = "Check state-based actions"
}

@Serializable
data class EcsClearDamage(
    val entityId: EntityId? = null  // null means all creatures
) : EcsAction {
    override val description: String = "Clear damage"
}
