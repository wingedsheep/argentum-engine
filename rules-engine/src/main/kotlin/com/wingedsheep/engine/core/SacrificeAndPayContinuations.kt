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
    val zone: Zone? = null,
    /**
     * Trigger context from the original PayOrSufferEffect execution, preserved so the
     * suffer effect can still resolve [com.wingedsheep.sdk.scripting.references.Player.TriggeringPlayer]
     * after the player explicitly declined to pay (Nafs Asp's "that player loses 1 life
     * unless they pay {1}"). The auto-suffer path runs synchronously with the original
     * context; the via-decision path goes through this continuation, so we have to thread
     * the triggering player through too — otherwise the suffer's target resolves to null
     * and the effect silently fizzles.
     */
    val triggeringEntityId: EntityId? = null,
    val triggeringPlayerId: EntityId? = null
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
    EXILE,
    CHOICE,
    TAP
}

/**
 * Resume after player picks which cost option to pay for a multi-option "pay or suffer" effect.
 *
 * Used when PayCost.Choice gives the player multiple avoidance options.
 * The player chooses via ChooseOptionDecision, then we delegate to the chosen sub-cost.
 *
 * @property options The available cost options (same order as the ChooseOptionDecision)
 * @property sufferEffect The effect to execute if the player declines all options
 */
@Serializable
data class PayOrSufferChoiceContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId,
    val sourceName: String,
    val options: List<PayCost>,
    val sufferEffect: Effect,
    val targets: List<ChosenTarget> = emptyList(),
    val namedTargets: Map<String, ChosenTarget> = emptyMap(),
    /** Mirror of [PayOrSufferContinuation.triggeringEntityId] for the multi-option path. */
    val triggeringEntityId: EntityId? = null,
    /** Mirror of [PayOrSufferContinuation.triggeringPlayerId] for the multi-option path. */
    val triggeringPlayerId: EntityId? = null
) : ContinuationFrame

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
 * @property consequence The effect to execute if any player pays (null = nothing)
 * @property consequenceIfNonePaid The effect to execute if no player pays (null = nothing)
 * @property requiredCount Number of items required (for sacrifice costs)
 * @property filter The filter for valid selections (for sacrifice costs)
 * @property storedCollections Pipeline collections carried into whichever consequence fires, so a
 *   consequence can reference cards gathered earlier in the same resolution ("…this way").
 * @property triggeringEntityId Trigger context from the original effect, preserved so a consequence
 *   referencing [com.wingedsheep.sdk.scripting.references.Player.TriggeringPlayer] still resolves
 *   after the async pay-or-decline round-trip (mirrors [PayOrSufferContinuation]).
 * @property triggeringPlayerId See [triggeringEntityId].
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
    val consequence: Effect? = null,
    val consequenceIfNonePaid: Effect? = null,
    val requiredCount: Int,
    val filter: GameObjectFilter,
    val storedCollections: Map<String, List<EntityId>> = emptyMap(),
    val triggeringEntityId: EntityId? = null,
    val triggeringPlayerId: EntityId? = null
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
