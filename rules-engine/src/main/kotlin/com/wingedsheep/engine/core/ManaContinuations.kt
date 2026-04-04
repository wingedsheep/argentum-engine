package com.wingedsheep.engine.core

import com.wingedsheep.engine.event.PendingTrigger
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ManaRestriction
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlinx.serialization.Serializable

/**
 * Resume after the spell's controller decides whether to pay a mana cost
 * to prevent their spell from being countered.
 *
 * "Counter target spell unless its controller pays {cost}."
 *
 * @property payingPlayerId The spell's controller who must decide whether to pay
 * @property spellEntityId The spell that will be countered if they don't pay
 * @property manaCost The mana cost to pay
 * @property sourceId The source of the counter-unless-pays effect
 * @property sourceName Name of the source for event messages
 */
@Serializable
data class CounterUnlessPaysContinuation(
    override val decisionId: String,
    val payingPlayerId: EntityId,
    val spellEntityId: EntityId,
    val manaCost: ManaCost,
    val sourceId: EntityId?,
    val sourceName: String?,
    val exileOnCounter: Boolean = false,
    val controllerId: EntityId? = null
) : ContinuationFrame

/**
 * Resume after the controller decides whether to pay a mana cost for an optional
 * mana payment effect (e.g., Lightning Rift's "you may pay {1}").
 *
 * If the player pays, the inner effect is executed. If not, nothing happens.
 *
 * @property playerId The player who may pay
 * @property sourceId The source of the effect
 * @property sourceName Name of the source for display
 * @property manaCost The mana cost to pay
 * @property effect The effect to execute if the player pays
 * @property controllerId The controller for effect context
 * @property opponentId The opponent for effect context
 * @property xValue The X value if applicable
 * @property targets The chosen targets for effect context
 */
@Serializable
data class MayPayManaContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceName: String?,
    val manaCost: ManaCost,
    val effect: Effect,
    val effectContext: EffectContext
) : ContinuationFrame

/**
 * Resume after the controller selects mana sources to pay a "you may pay" cost
 * for a non-targeted triggered ability. After payment, the inner effect is executed.
 *
 * @property playerId The player paying
 * @property sourceName Name of the source for display
 * @property manaCost The mana cost to pay
 * @property effect The effect to execute after payment
 * @property effectContext The context for effect execution
 * @property availableSources Available mana sources the player can choose from
 * @property autoPaySuggestion Pre-computed auto-tap suggestion
 */
@Serializable
data class MayPayManaSelectionContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceName: String?,
    val manaCost: ManaCost,
    val effect: Effect,
    val effectContext: EffectContext,
    val availableSources: List<ManaSourceOption>,
    val autoPaySuggestion: List<EntityId>
) : ContinuationFrame

/**
 * Resume after the controller chooses an X value for "you may pay {X}" effects.
 * The player selects a number (0 to max affordable), and if > 0, we pay that mana
 * and execute the inner effect with the chosen X value.
 *
 * Example: Decree of Justice cycling trigger - "you may pay {X}. If you do,
 * create X 1/1 white Soldier creature tokens."
 */
@Serializable
data class MayPayXContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceName: String?,
    val effect: Effect,
    val maxX: Int,
    val effectContext: EffectContext
) : ContinuationFrame

/**
 * Resume after the controller decides whether to pay a mana cost for a triggered
 * ability that also requires targets (e.g., Lightning Rift).
 *
 * The flow is: ask "pay {cost}?" → if yes, show mana source selection → then target selection.
 * This is different from MayPayManaContinuation which asks about targets first.
 *
 * @property trigger The full pending trigger to process if the player pays
 * @property targetRequirement The target requirement for the ability
 * @property manaCost The mana cost to pay
 */
@Serializable
data class MayPayManaTriggerContinuation(
    override val decisionId: String,
    val trigger: PendingTrigger,
    val targetRequirement: TargetRequirement,
    val manaCost: ManaCost
) : ContinuationFrame

/**
 * Resume after the controller selects mana sources to pay a "counter unless pays" cost.
 * After payment, the spell resolves normally. If the player selected invalid sources,
 * the spell is countered.
 *
 * @property payingPlayerId The spell's controller who is paying
 * @property spellEntityId The spell that will be countered if payment fails
 * @property manaCost The mana cost to pay
 * @property availableSources Available mana sources the player can choose from
 * @property autoPaySuggestion Pre-computed auto-tap suggestion
 * @property exileOnCounter Whether to exile the spell if countered
 * @property controllerId The controller of the counter effect
 */
@Serializable
data class CounterUnlessPaysManaSelectionContinuation(
    override val decisionId: String,
    val payingPlayerId: EntityId,
    val spellEntityId: EntityId,
    val manaCost: ManaCost,
    val availableSources: List<ManaSourceOption>,
    val autoPaySuggestion: List<EntityId>,
    val exileOnCounter: Boolean = false,
    val controllerId: EntityId? = null
) : ContinuationFrame

/**
 * Resume after the controller selects mana sources to pay a cost for a triggered
 * ability that also requires targets.
 *
 * After mana sources are selected and cost is paid, proceeds to target selection.
 *
 * @property trigger The full pending trigger to process after payment
 * @property targetRequirement The target requirement for the ability
 * @property manaCost The mana cost to pay
 * @property availableSources Available mana sources the player can choose from
 * @property autoPaySuggestion Pre-computed auto-tap suggestion
 */
@Serializable
data class ManaSourceSelectionContinuation(
    override val decisionId: String,
    val trigger: PendingTrigger,
    val targetRequirement: TargetRequirement,
    val manaCost: ManaCost,
    val availableSources: List<ManaSourceOption>,
    val autoPaySuggestion: List<EntityId>
) : ContinuationFrame

/**
 * Information about a mana source available for manual selection.
 */
@Serializable
data class ManaSourceOption(
    val entityId: EntityId,
    val name: String,
    val producesColors: Set<Color>,
    val producesColorless: Boolean
)

/**
 * Continuation for AddDynamicManaEffect.
 *
 * Resume after a player chooses how to distribute mana among allowed colors.
 * The player picks how much of [firstColor] to add; the remainder goes to [secondColor].
 *
 * @property playerId The player receiving the mana
 * @property sourceId The spell/ability that caused the effect
 * @property sourceName Name of the source for display
 * @property totalAmount Total mana to add
 * @property firstColor First color option
 * @property secondColor Second color option (gets remainder)
 */
@Serializable
data class AddDynamicManaContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val totalAmount: Int,
    val firstColor: Color,
    val secondColor: Color,
    val restriction: ManaRestriction? = null
) : ContinuationFrame
