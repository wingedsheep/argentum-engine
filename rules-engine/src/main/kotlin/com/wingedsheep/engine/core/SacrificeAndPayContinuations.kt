package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.ChooseOnePerCategoryEffect
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
 * Resume after a player picked the one permanent they keep for a single category of a
 * [ChooseOnePerCategoryEffect] — "chooses a permanent they control of each permanent type".
 *
 * The step publishes nothing until every chooser has answered every category (CR 101.4), so the
 * frame carries the whole in-progress tally.
 *
 * @property storedCollections The resolving pipeline's collections, re-published on completion so
 *   the downstream "…the rest" steps see both the pool and the picks.
 * @property pendingPlayers The choosers that still have picks to make, the current one first.
 * @property categoryIndex Index into `effect.categories` that the pending decision answers.
 * @property picks Every pick made so far, across all choosers.
 */
@Serializable
data class ChooseOnePerCategoryContinuation(
    override val decisionId: String,
    val effect: ChooseOnePerCategoryEffect,
    val sourceId: EntityId?,
    val sourceName: String?,
    val storedCollections: Map<String, List<EntityId>>,
    val pendingPlayers: List<EntityId>,
    val categoryIndex: Int,
    val picks: List<EntityId>
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
    val counterType: String? = null,
    /**
     * How many counters of [counterType] the payment places, for
     * [PayOrSufferCostType.PUT_COUNTERS]. Distinct from [requiredCount], which is how many
     * *permanents* the player must select — one, for every printed use.
     */
    val requiredCounters: Int = 1,
    val self: Boolean = false,
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
    val triggeringPlayerId: EntityId? = null,
    /**
     * The triggered/activated ability's controller — distinct from [playerId], which is the player
     * who must *pay* the cost (the two diverge when the cost is routed to a non-controller via
     * `PayOrSufferEffect.player`, e.g. Meathook Massacre II's "a creature an opponent controls dies,
     * they may pay 3 life"). The suffer effect is part of that ability, so it resolves under the
     * ability's controller: `EffectTarget.Controller` inside the consequence means the ability's
     * controller (you steal the card), not the player who declined to pay. Falls back to [playerId]
     * for the common case where the payer *is* the controller.
     */
    val abilityControllerId: EntityId? = null,
    /**
     * The resolving pipeline's collections, carried across the pay-or-decline pause so a suffer
     * effect can still name them. Wand of Ith's suffer is "discard the card revealed this way" — a
     * `MoveCollection` over a collection built earlier in the same resolution — and without this it
     * resumes against an empty pipeline and silently discards nothing.
     *
     * Mirrors the same field on [AnyPlayerMayPayContinuation], for the same reason.
     */
    val storedCollections: Map<String, List<EntityId>> = emptyMap(),
    /**
     * The enclosing `ForEachInGroup` loop's current entity, when the pay-or-suffer sits inside one
     * (Tidal Flats: "for each attacking creature ... its controller may pay {1}"). The consequence
     * refers back to it — "creatures you control blocking *that creature*" — so the resumed context
     * has to rebind `PipelineState.iterationTarget`. Without it the auto-suffer path (nothing to
     * pay with, no prompt) worked while the far more common declined-a-prompt path silently
     * matched nothing.
     */
    val iterationEntityId: EntityId? = null
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
    TAP,
    REMOVE_COUNTERS,
    PUT_COUNTERS,
    MILL
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
    val triggeringPlayerId: EntityId? = null,
    /** Mirror of [PayOrSufferContinuation.abilityControllerId] for the multi-option path. */
    val abilityControllerId: EntityId? = null,
    /**
     * The effect's authored consequence clause, carried so the second prompt — the one for the
     * cost option the player picked — asks in the same words as the first. Rebuilding a
     * single-cost effect without it would silently fall back to the generated description.
     */
    val consequenceDescription: String? = null,
    /** Mirror of [PayOrSufferContinuation.storedCollections] for the multi-option path. */
    val storedCollections: Map<String, List<EntityId>> = emptyMap(),
    /** Mirror of [PayOrSufferContinuation.iterationEntityId] for the multi-option path. */
    val iterationEntityId: EntityId? = null
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
 * @property iterationTarget The permanent the enclosing `ForEachInGroup` / `ForEachInCollection`
 *   loop is currently on, preserved across the pay-or-decline round-trip so a consequence written
 *   as `EffectTarget.Self` still means *that* permanent. Cleansing ("for each land, destroy that
 *   land unless any player pays 1 life") is the shape that needs it: without this the consequence
 *   resolves `Self` to the resolving spell and destroys nothing.
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
    val triggeringPlayerId: EntityId? = null,
    val iterationTarget: EntityId? = null
) : ContinuationFrame

/**
 * Resume after player selects which permanents to keep tapped during untap step.
 *
 * Used for permanents with "You may choose not to untap" keyword (MAY_NOT_UNTAP) and for
 * untap-count restrictions such as Damping Field ("can't untap more than one artifact"). The
 * player selects which permanents to keep tapped; everything else untaps normally.
 *
 * @property playerId The active player making the choice
 * @property allPermanentsToUntap All permanents that would normally untap
 * @property untapLimits Active untap-count caps (Damping Field). Each pair is the set of would-untap
 *   permanents matching the restriction's filter and the maximum of them allowed to untap. The
 *   resumer enforces that no more than `max` of each set untaps (defence in depth — the raised
 *   decision's `minSelections` already prevents an under-keep when the matching pool is homogeneous).
 */
@Serializable
data class UntapChoiceContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val allPermanentsToUntap: List<EntityId>,
    val untapLimits: List<UntapLimitChoice> = emptyList()
) : ContinuationFrame

/**
 * One active untap-count cap during a player's untap step: at most [max] of [matchingPermanents]
 * (the would-untap permanents matching the restriction's filter) may untap.
 */
@Serializable
data class UntapLimitChoice(
    val matchingPermanents: List<EntityId>,
    val max: Int
)

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

/**
 * Resume after the payer picks mana sources for a "pay {N} or suffer" cost they already agreed to.
 *
 * Same second step [CostPaymentManaSelectionContinuation] adds, for the [PayOrSufferEffect] path:
 * answering "yes" used to hand the cost straight to the auto-tap solver, so the payer couldn't
 * choose their sources and couldn't activate a mana ability to cover it (CR 605.3a). Declining at
 * this step is the same outcome as answering "no" — the suffer effect runs.
 */
@Serializable
data class PayOrSufferManaSelectionContinuation(
    override val decisionId: String,
    val inner: PayOrSufferContinuation,
    val manaCost: ManaCost,
    val availableSources: List<ManaSourceOption>
) : ContinuationFrame
