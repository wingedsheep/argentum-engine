package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import kotlinx.serialization.Serializable

/**
 * Resume after player selects cards for sacrifice.
 *
 * @property playerId The player who is sacrificing
 * @property sourceId The spell/ability that caused the sacrifice
 * @property sourceName Name of the source for event messages
 * @property remainingPlayers Players still to process for "each opponent" sacrifice effects
 * @property filter Filter for valid sacrifice targets (needed to chain remaining players)
 * @property count Number of permanents each player must sacrifice
 */
@Serializable
data class SacrificeContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val remainingPlayers: List<EntityId> = emptyList(),
    val filter: GameObjectFilter? = null,
    val count: Int = 1
) : ContinuationFrame

/**
 * Resume after player selects cards for multi-zone exile.
 * Used for Lich's Mastery: "exile a permanent you control or a card from your hand or graveyard."
 *
 * @property playerId The player who must exile
 * @property sourceId The spell/ability that caused the exile
 * @property sourceName Name of the source for event messages
 */
@Serializable
data class ExileMultiZoneContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?
) : ContinuationFrame

/**
 * Resume after player selects cards/permanents for a generic "pay or suffer" effect.
 *
 * Used for unified "unless" mechanics like PayOrSufferEffect.
 *
 * @property playerId The player who must make the choice
 * @property sourceId The source that triggered this effect
 * @property sourceName Name of the source for event messages
 * @property costType The type of cost being paid (for dispatch to appropriate handler)
 * @property sufferEffect The effect to execute if the player doesn't pay
 * @property requiredCount Number of items required (cards to discard, permanents to sacrifice)
 * @property filter The filter for valid selections
 * @property random Whether the selection should be random (for Discard costs)
 */
@Serializable
data class PayOrSufferContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId,
    val sourceName: String,
    val costType: PayOrSufferCostType,
    val sufferEffect: Effect,
    val requiredCount: Int,
    val filter: GameObjectFilter,
    val random: Boolean = false,
    val targets: List<ChosenTarget> = emptyList(),
    val namedTargets: Map<String, ChosenTarget> = emptyMap(),
    val manaCost: ManaCost? = null,
    val zone: Zone? = null
) : ContinuationFrame

/**
 * Discriminator for the cost type in PayOrSufferContinuation.
 */
@Serializable
enum class PayOrSufferCostType {
    DISCARD,
    SACRIFICE,
    PAY_LIFE,
    MANA,
    EXILE
}

/**
 * Resume after a player decides whether to pay a cost for "any player may [cost]" effects.
 *
 * Each player in APNAP order gets the chance to pay. If the current player pays,
 * the consequence is executed immediately. If they decline, we move to the next player.
 *
 * @property currentPlayerId The player currently being asked
 * @property remainingPlayers Players still to be asked after the current one
 * @property sourceId The source permanent
 * @property sourceName Name of the source for display
 * @property controllerId The controller of the source permanent
 * @property cost The cost being offered
 * @property consequence The effect to execute if any player pays
 * @property requiredCount Number of items required (for sacrifice costs)
 * @property filter The filter for valid selections (for sacrifice costs)
 */
@Serializable
data class AnyPlayerMayPayContinuation(
    override val decisionId: String,
    val currentPlayerId: EntityId,
    val remainingPlayers: List<EntityId>,
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId,
    val cost: PayCost,
    val consequence: Effect,
    val requiredCount: Int,
    val filter: GameObjectFilter
) : ContinuationFrame

/**
 * Resume after player selects which permanents to keep tapped during untap step.
 *
 * Used for permanents with "You may choose not to untap" keyword (MAY_NOT_UNTAP).
 * The player selects which permanents to keep tapped; everything else untaps normally.
 *
 * @property playerId The active player making the choice
 * @property allPermanentsToUntap All permanents that would normally untap
 */
@Serializable
data class UntapChoiceContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val allPermanentsToUntap: List<EntityId>
) : ContinuationFrame

/**
 * Resume after player selects a card from their graveyard.
 *
 * Used for spells like Elven Cache and Déjà Vu that let the player
 * choose a card from their graveyard to return to hand/battlefield.
 *
 * @property playerId The player who is searching their graveyard
 * @property sourceId The spell/ability that caused the search
 * @property sourceName Name of the source for event messages
 * @property destination Where to put the selected card (HAND or BATTLEFIELD)
 */
@Serializable
data class ReturnFromGraveyardContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val destination: SearchDestination
) : ContinuationFrame
