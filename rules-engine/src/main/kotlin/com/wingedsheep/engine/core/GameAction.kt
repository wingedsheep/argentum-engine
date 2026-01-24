package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import kotlinx.serialization.Serializable

/**
 * Sealed hierarchy of all game actions.
 * Actions are the inputs to the game engine.
 */
@Serializable
sealed interface GameAction {
    /** The player performing the action */
    val playerId: EntityId
}

// =============================================================================
// Priority Actions
// =============================================================================

/**
 * Player passes priority.
 */
@Serializable
data class PassPriority(
    override val playerId: EntityId
) : GameAction

// =============================================================================
// Spell Actions
// =============================================================================

/**
 * Player casts a spell.
 *
 * @property playerId The player casting the spell
 * @property cardId The card being cast
 * @property targets Chosen targets for the spell
 * @property xValue The value of X for X-cost spells
 * @property paymentStrategy How the player intends to pay the mana cost
 */
@Serializable
data class CastSpell(
    override val playerId: EntityId,
    val cardId: EntityId,
    val targets: List<ChosenTarget> = emptyList(),
    val xValue: Int? = null,
    val paymentStrategy: PaymentStrategy = PaymentStrategy.AutoPay
) : GameAction

/**
 * Defines how a player intends to pay the mana cost of a spell or ability.
 *
 * This allows the engine to support both:
 * - **Auto-tap**: The engine automatically selects lands to tap (smooth UX for casual play)
 * - **Manual payment**: The player has already floated mana via ActivateAbility actions (hardcore mode)
 */
@Serializable
sealed interface PaymentStrategy {
    /**
     * Engine automatically calculates the best lands/mana sources to tap.
     * The engine will emit TappedEvents for each source used.
     */
    @Serializable
    data object AutoPay : PaymentStrategy

    /**
     * Player has already activated mana abilities manually.
     * Engine verifies the ManaPool has sufficient mana and deducts it.
     */
    @Serializable
    data object FromPool : PaymentStrategy

    /**
     * Advanced: Player specifies exactly which mana abilities to activate.
     * The engine activates these abilities and uses the resulting mana.
     *
     * @property manaAbilitiesToActivate Entity IDs of permanents whose mana abilities to activate
     */
    @Serializable
    data class Explicit(val manaAbilitiesToActivate: List<EntityId>) : PaymentStrategy
}

// =============================================================================
// Ability Actions
// =============================================================================

/**
 * Player activates an ability.
 */
@Serializable
data class ActivateAbility(
    override val playerId: EntityId,
    val sourceId: EntityId,
    val abilityId: AbilityId,
    val targets: List<ChosenTarget> = emptyList()
) : GameAction

// =============================================================================
// Land Actions
// =============================================================================

/**
 * Player plays a land.
 */
@Serializable
data class PlayLand(
    override val playerId: EntityId,
    val cardId: EntityId
) : GameAction

// =============================================================================
// Combat Actions
// =============================================================================

/**
 * Player declares attackers.
 */
@Serializable
data class DeclareAttackers(
    override val playerId: EntityId,
    val attackers: Map<EntityId, EntityId>  // attacker -> defending player
) : GameAction

/**
 * Player declares blockers.
 */
@Serializable
data class DeclareBlockers(
    override val playerId: EntityId,
    val blockers: Map<EntityId, List<EntityId>>  // blocker -> list of attackers
) : GameAction

/**
 * Player orders blockers for an attacker.
 */
@Serializable
data class OrderBlockers(
    override val playerId: EntityId,
    val attackerId: EntityId,
    val orderedBlockers: List<EntityId>
) : GameAction

// =============================================================================
// Decision Actions
// =============================================================================

/**
 * Player makes a choice (modal spells, may effects, etc.).
 */
@Serializable
data class MakeChoice(
    override val playerId: EntityId,
    val decisionId: String,
    val choiceIndex: Int
) : GameAction

/**
 * Player selects targets for a triggered ability.
 */
@Serializable
data class SelectTargets(
    override val playerId: EntityId,
    val abilityEntityId: EntityId,
    val targets: List<ChosenTarget>
) : GameAction

// =============================================================================
// Mana Actions
// =============================================================================

/**
 * Player chooses a color for "add one mana of any color" effects.
 */
@Serializable
data class ChooseManaColor(
    override val playerId: EntityId,
    val color: Color
) : GameAction

// =============================================================================
// Decision Response
// =============================================================================

/**
 * Player submits a response to a pending decision.
 *
 * This is used to resume the engine after it has paused for player input.
 * The response must match the type expected by the pending decision.
 */
@Serializable
data class SubmitDecision(
    override val playerId: EntityId,
    val response: DecisionResponse
) : GameAction

// =============================================================================
// Concession
// =============================================================================

/**
 * Player concedes the game.
 */
@Serializable
data class Concede(
    override val playerId: EntityId
) : GameAction
