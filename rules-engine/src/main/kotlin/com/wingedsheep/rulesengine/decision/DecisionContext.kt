package com.wingedsheep.rulesengine.decision

import com.wingedsheep.rulesengine.ability.SearchDestination
import com.wingedsheep.rulesengine.ecs.EntityId
import kotlinx.serialization.Serializable

/**
 * Sealed interface for serializable decision context data.
 *
 * DecisionContext captures all the state needed to resume an effect's execution
 * after receiving player input. This enables stateless decision management where
 * [GameState] is the **only** thing required to resume the game.
 *
 * Each context type stores:
 * - The source and controller of the effect
 * - All parameters needed to complete the effect
 * - Validation constraints (valid targets, counts, etc.)
 *
 * When a player submits a decision response, [DecisionResumer] pattern-matches
 * on the context type to reconstruct and execute the completion logic.
 */
@Serializable
sealed interface DecisionContext {
    /** The entity that is the source of the effect (spell or ability) */
    val sourceId: EntityId

    /** The player who controls the effect and must make the decision */
    val controllerId: EntityId
}

/**
 * Context for SearchLibraryEffect decisions.
 *
 * Stores all state needed to complete a library search after the player
 * chooses which cards to take.
 */
@Serializable
data class SearchLibraryContext(
    override val sourceId: EntityId,
    override val controllerId: EntityId,
    /** The player whose library is being searched */
    val searchedPlayerId: EntityId,
    /** Entity IDs of cards that match the search filter */
    val validTargets: List<EntityId>,
    /** Maximum number of cards that can be selected */
    val maxCount: Int,
    /** Where to put the selected cards */
    val destination: SearchDestination,
    /** Whether to shuffle the library after searching */
    val shuffleAfter: Boolean,
    /** Whether cards entering battlefield enter tapped */
    val entersTapped: Boolean,
    /** Human-readable description of the card filter */
    val filterDescription: String
) : DecisionContext

/**
 * Context for DiscardCardsEffect decisions.
 *
 * Stores state needed to complete a discard effect after the player
 * chooses which cards to discard.
 */
@Serializable
data class DiscardCardsContext(
    override val sourceId: EntityId,
    override val controllerId: EntityId,
    /** The player who must discard */
    val discardingPlayerId: EntityId,
    /** Cards in hand that can be discarded */
    val validTargets: List<EntityId>,
    /** Number of cards that must be discarded */
    val requiredCount: Int,
    /** Whether the player can choose fewer cards than required */
    val mayChooseFewer: Boolean = false
) : DecisionContext

/**
 * Context for SacrificeUnlessEffect decisions.
 *
 * Stores state needed to complete a "sacrifice unless you pay X" effect.
 */
@Serializable
data class SacrificeUnlessContext(
    override val sourceId: EntityId,
    override val controllerId: EntityId,
    /** The permanent that will be sacrificed if cost not paid */
    val permanentToSacrifice: EntityId,
    /** Name of the permanent for display purposes */
    val permanentName: String,
    /** Human-readable description of the alternative cost */
    val costDescription: String,
    /** Permanents that can be sacrificed to pay the cost */
    val validCostTargets: List<EntityId>,
    /** How many permanents must be sacrificed to pay the cost */
    val requiredCount: Int
) : DecisionContext

/**
 * Context for choosing targets for spells/abilities.
 *
 * Stores state needed to complete target selection.
 */
@Serializable
data class ChooseTargetsContext(
    override val sourceId: EntityId,
    override val controllerId: EntityId,
    /** Name of the spell or ability source */
    val sourceName: String,
    /** Map of requirement index -> valid target entity IDs */
    val validTargetsByIndex: Map<Int, List<EntityId>>,
    /** Number of targets required for each requirement */
    val requiredCountsByIndex: Map<Int, Int>
) : DecisionContext

/**
 * Context for returning cards from graveyard decisions.
 */
@Serializable
data class ReturnFromGraveyardContext(
    override val sourceId: EntityId,
    override val controllerId: EntityId,
    /** The player whose graveyard is being searched */
    val graveyardOwnerId: EntityId,
    /** Cards in graveyard that match the filter */
    val validTargets: List<EntityId>,
    /** Maximum number of cards that can be returned */
    val maxCount: Int,
    /** Where to put the returned cards */
    val destination: SearchDestination,
    /** Human-readable description of the card filter */
    val filterDescription: String
) : DecisionContext

/**
 * Context for looking at top cards of library decisions.
 *
 * Used for Scry-like effects where player must decide how to order cards.
 */
@Serializable
data class LookAtTopCardsContext(
    override val sourceId: EntityId,
    override val controllerId: EntityId,
    /** The player whose library is being looked at */
    val libraryOwnerId: EntityId,
    /** The cards being looked at (in order from top to bottom) */
    val cardIds: List<EntityId>,
    /** How many cards can be put into hand (0 for pure scry) */
    val keepCount: Int,
    /** Where non-kept cards go (graveyard for surveil, library for scry) */
    val restDestination: RestDestination
) : DecisionContext

/**
 * Destination for cards not kept during look-at-top effects.
 */
@Serializable
enum class RestDestination {
    /** Put on top of library in any order (scry "top") */
    TOP_OF_LIBRARY,
    /** Put on bottom of library in any order (scry "bottom") */
    BOTTOM_OF_LIBRARY,
    /** Put into graveyard (surveil, mill-like) */
    GRAVEYARD
}

/**
 * Context for modal spell/ability choices.
 */
@Serializable
data class ModeChoiceContext(
    override val sourceId: EntityId,
    override val controllerId: EntityId,
    /** Name of the spell or ability */
    val sourceName: String,
    /** Indices of available modes */
    val availableModes: List<Int>,
    /** How many modes must be chosen */
    val modesRequired: Int,
    /** Whether the same mode can be chosen multiple times */
    val canRepeatModes: Boolean
) : DecisionContext

/**
 * Context for cleanup step discard decisions.
 *
 * Used during cleanup when a player must discard down to hand size.
 */
@Serializable
data class CleanupDiscardContext(
    override val sourceId: EntityId,
    override val controllerId: EntityId,
    /** The player who must discard */
    val discardingPlayerId: EntityId,
    /** Cards in hand that can be discarded */
    val cardsInHand: List<EntityId>,
    /** Number of cards that must be discarded to reach hand size limit */
    val discardCount: Int
) : DecisionContext
