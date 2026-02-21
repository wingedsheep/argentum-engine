package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.sdk.scripting.AlternativePaymentChoice
import kotlinx.serialization.SerialName
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
@SerialName("PassPriority")
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
 * @property alternativePayment Optional alternative payment choices (Delve, Convoke)
 * @property castFaceDown If true, cast as a face-down 2/2 creature for {3} (morph)
 * @property damageDistribution Pre-chosen damage distribution for DividedDamageEffect spells (target ID -> damage amount)
 */
@Serializable
@SerialName("CastSpell")
data class CastSpell(
    override val playerId: EntityId,
    val cardId: EntityId,
    val targets: List<ChosenTarget> = emptyList(),
    val xValue: Int? = null,
    val paymentStrategy: PaymentStrategy = PaymentStrategy.AutoPay,
    val alternativePayment: AlternativePaymentChoice? = null,
    val additionalCostPayment: AdditionalCostPayment? = null,
    val castFaceDown: Boolean = false,
    val damageDistribution: Map<EntityId, Int>? = null
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
    @SerialName("AutoPay")
    data object AutoPay : PaymentStrategy

    /**
     * Player has already activated mana abilities manually.
     * Engine verifies the ManaPool has sufficient mana and deducts it.
     */
    @Serializable
    @SerialName("FromPool")
    data object FromPool : PaymentStrategy

    /**
     * Advanced: Player specifies exactly which mana abilities to activate.
     * The engine activates these abilities and uses the resulting mana.
     *
     * @property manaAbilitiesToActivate Entity IDs of permanents whose mana abilities to activate
     */
    @Serializable
    @SerialName("Explicit")
    data class Explicit(val manaAbilitiesToActivate: List<EntityId>) : PaymentStrategy
}

// =============================================================================
// Ability Actions
// =============================================================================

/**
 * Player activates an ability.
 *
 * @property playerId The player activating the ability
 * @property sourceId The permanent whose ability is being activated
 * @property abilityId The ID of the ability being activated
 * @property targets Chosen targets for the ability's effect
 * @property costPayment Payment choices for costs (sacrifice, etc.)
 * @property manaColorChoice Color chosen for "add one mana of any color" abilities
 */
@Serializable
@SerialName("ActivateAbility")
data class ActivateAbility(
    override val playerId: EntityId,
    val sourceId: EntityId,
    val abilityId: AbilityId,
    val targets: List<ChosenTarget> = emptyList(),
    val costPayment: AdditionalCostPayment? = null,
    val manaColorChoice: Color? = null,
    val xValue: Int? = null
) : GameAction

// =============================================================================
// Cycling Actions
// =============================================================================

/**
 * Player cycles a card from their hand.
 *
 * Cycling is an activated ability that can be activated from hand.
 * The player pays the cycling cost, discards the card, and draws a card.
 *
 * @property playerId The player cycling the card
 * @property cardId The card being cycled
 */
@Serializable
@SerialName("CycleCard")
data class CycleCard(
    override val playerId: EntityId,
    val cardId: EntityId
) : GameAction

/**
 * Player typecycles a card from their hand.
 *
 * Typecycling (e.g., Swampcycling, Wizardcycling) is an activated ability from hand.
 * The player pays the typecycling cost, discards the card, then searches their
 * library for a card of the specified type, reveals it, puts it into their hand,
 * then shuffles.
 *
 * @property playerId The player typecycling the card
 * @property cardId The card being typecycled
 */
@Serializable
@SerialName("TypecycleCard")
data class TypecycleCard(
    override val playerId: EntityId,
    val cardId: EntityId
) : GameAction

// =============================================================================
// Land Actions
// =============================================================================

/**
 * Player plays a land.
 */
@Serializable
@SerialName("PlayLand")
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
@SerialName("DeclareAttackers")
data class DeclareAttackers(
    override val playerId: EntityId,
    val attackers: Map<EntityId, EntityId>  // attacker -> defending player
) : GameAction

/**
 * Player declares blockers.
 */
@Serializable
@SerialName("DeclareBlockers")
data class DeclareBlockers(
    override val playerId: EntityId,
    val blockers: Map<EntityId, List<EntityId>>  // blocker -> list of attackers
) : GameAction

/**
 * Player orders blockers for an attacker.
 */
@Serializable
@SerialName("OrderBlockers")
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
@SerialName("MakeChoice")
data class MakeChoice(
    override val playerId: EntityId,
    val decisionId: String,
    val choiceIndex: Int
) : GameAction

/**
 * Player selects targets for a triggered ability.
 */
@Serializable
@SerialName("SelectTargets")
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
@SerialName("ChooseManaColor")
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
@SerialName("SubmitDecision")
data class SubmitDecision(
    override val playerId: EntityId,
    val response: DecisionResponse
) : GameAction

// =============================================================================
// Mulligan Actions
// =============================================================================

/**
 * Player takes a mulligan, shuffling their hand into their library
 * and drawing a new hand of (7 - mulligans taken) cards.
 */
@Serializable
@SerialName("TakeMulligan")
data class TakeMulligan(
    override val playerId: EntityId
) : GameAction

/**
 * Player keeps their current hand.
 *
 * After all players have kept, each player who took mulligans
 * puts that many cards from their hand on the bottom of their library.
 */
@Serializable
@SerialName("KeepHand")
data class KeepHand(
    override val playerId: EntityId
) : GameAction

/**
 * Player puts cards on the bottom of their library after keeping a mulligan hand.
 * This is the "London mulligan" rule where you put cards back equal to mulligans taken.
 *
 * @property cardIds The cards to put on bottom (in the order they should be placed)
 */
@Serializable
@SerialName("BottomCards")
data class BottomCards(
    override val playerId: EntityId,
    val cardIds: List<EntityId>
) : GameAction

// =============================================================================
// Concession
// =============================================================================

/**
 * Player concedes the game.
 */
@Serializable
@SerialName("Concede")
data class Concede(
    override val playerId: EntityId
) : GameAction

// =============================================================================
// Morph Actions
// =============================================================================

/**
 * Player turns a face-down creature face up by paying its morph cost.
 * This is a special action that doesn't use the stack and can be done any time
 * the player has priority.
 *
 * @property playerId The player turning the creature face up
 * @property sourceId The face-down creature to turn face up (named sourceId for frontend consistency with ActivateAbility)
 * @property paymentStrategy How the player intends to pay the morph cost
 * @property costTargetIds Permanents chosen for non-mana morph costs (e.g., return a Bird to hand)
 */
@Serializable
@SerialName("TurnFaceUp")
data class TurnFaceUp(
    override val playerId: EntityId,
    val sourceId: EntityId,
    val paymentStrategy: PaymentStrategy = PaymentStrategy.AutoPay,
    val costTargetIds: List<EntityId> = emptyList()
) : GameAction
