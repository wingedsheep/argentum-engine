package com.wingedsheep.engine.core

import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlinx.serialization.Serializable

/**
 * Resume after player chooses a color for a [com.wingedsheep.sdk.scripting.effects.ChooseColorThenEffect].
 *
 * The resumer stamps the chosen color onto [baseContext] and dispatches [then]
 * through the standard effect runner, so any composition of atomic effects can
 * read the chosen color from `EffectContext.chosenColor`.
 */
@Serializable
data class ChooseColorThenContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val then: Effect,
    val baseContext: EffectContext
) : ContinuationFrame

/**
 * Resume after the controller chooses a number for a
 * [com.wingedsheep.sdk.scripting.effects.ChooseNumberThenEffect].
 *
 * The resumer stamps the chosen number onto [baseContext] as the X value and dispatches
 * [then] through the standard effect runner, so any composition of atomic effects and
 * filters can read it via `EffectContext.xValue` /
 * [com.wingedsheep.sdk.scripting.predicates.CardPredicate.ManaValueEqualsX] (Void).
 */
@Serializable
data class ChooseNumberThenContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val then: Effect,
    val baseContext: EffectContext
) : ContinuationFrame

/**
 * Resume after the controller of an [com.wingedsheep.sdk.scripting.effects.AddManaOfChoiceEffect]
 * picks the mana color. The resumer re-runs the effect via the executor registry with
 * `manaColorChoice` populated on [baseContext], so dynamic amounts evaluate consistently.
 */
@Serializable
data class ChooseManaColorContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val effect: Effect,
    val baseContext: EffectContext
) : ContinuationFrame

/**
 * One pending "additional one mana of any color" bonus from a [com.wingedsheep.sdk.scripting.AdditionalManaOnTap]
 * aura with `anyColor = true` (Fertile Ground). [amount] is the resolved bonus size (already
 * evaluated from the aura's dynamic amount at tap time), [controllerId] is the land controller who
 * gets the mana and chooses the color, and [auraId] is the aura source (for events/messages).
 */
@Serializable
data class AnyColorTapBonus(
    val auraId: EntityId,
    val controllerId: EntityId,
    val amount: Int
)

/**
 * Resume after the controller picks the color for an "additional one mana of any color" bonus that
 * fires when a land they control is tapped for mana (Fertile Ground). These bonuses resolve as
 * triggered mana abilities (off-stack) but still require a per-tap color choice; this continuation
 * adds the chosen-color mana for [current], then drives the [remaining] bonuses (which may pause
 * again for their own color choice).
 */
@Serializable
data class ChooseAnyColorTapBonusContinuation(
    override val decisionId: String,
    val current: AnyColorTapBonus,
    val remaining: List<AnyColorTapBonus>
) : ContinuationFrame

/**
 * Resume after a player chooses a color to store on a permanent.
 */
@Serializable
data class ChooseColorForTargetContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val targetEntityId: EntityId
) : ContinuationFrame

/**
 * Which text-changing family a [ChooseReplacementContinuation] belongs to, so the resumer knows the
 * replacement category. [WORD] (Crystal Spray) infers color-word vs basic-land-type per chosen word;
 * [CREATURE_TYPE] (Artificial Evolution) is always a creature-type replacement.
 */
@Serializable
enum class ReplacementMode { WORD, CREATURE_TYPE }

/**
 * Resume after the player picks both halves of a text change in one [ChooseReplacementDecision]
 * (Crystal Spray, Artificial Evolution). The resumer maps the chosen indices back to words and
 * attaches the [com.wingedsheep.engine.state.components.identity.TextReplacementComponent].
 *
 * @property targetId The spell or permanent whose text is being changed
 * @property fromOptions / toOptions the option lists, so the response indices resolve to words
 * @property mode whether this is a color/land word change or a creature-type change
 * @property duration how long the resulting text change lasts
 */
@Serializable
data class ChooseReplacementContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val targetId: EntityId,
    val fromOptions: List<String>,
    val toOptions: List<String>,
    val mode: ReplacementMode,
    val duration: Duration
) : ContinuationFrame

/**
 * Resume after player chooses a creature type for a "becomes the creature type
 * of your choice" effect.
 *
 * @property controllerId The player who controls the spell/ability
 * @property sourceId The spell/ability that caused this effect
 * @property sourceName Name of the source for event messages
 * @property targetId The creature whose type will change
 * @property creatureTypes The creature type options (indexed by OptionChosenResponse.optionIndex)
 * @property duration How long the type change lasts
 */
@Serializable
data class BecomeCreatureTypeContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val targetId: EntityId,
    val creatureTypes: List<String>,
    val duration: Duration
) : ContinuationFrame

/**
 * Resume after the controller chooses a card type for
 * [com.wingedsheep.sdk.scripting.effects.GrantProtectionFromChosenCardTypeEffect]
 * (Pippin, Guard of the Citadel). The resumer grants the target a floating
 * `PROTECTION_FROM_CARDTYPE_<TYPE>` keyword for [duration].
 *
 * @property controllerId The player who controls the ability
 * @property sourceId The ability source (for event messages)
 * @property sourceName Name of the source for event messages
 * @property targetId The creature gaining protection
 * @property cardTypes The card-type options (indexed by OptionChosenResponse.optionIndex)
 * @property duration How long the protection lasts
 */
@Serializable
data class ChooseCardTypeForProtectionContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val targetId: EntityId,
    val cardTypes: List<String>,
    val duration: Duration
) : ContinuationFrame

/**
 * Resume after a player chose a creature type for "each player chooses a creature type" effects.
 *
 * Each player (in APNAP order) chooses a creature type. After all players have chosen,
 * the accumulated chosen types are stored in the EffectContinuation below via storedStringLists[storeAs].
 *
 * @property sourceId The spell that created this effect
 * @property sourceName Name of the source for display
 * @property controllerId The controller of the spell
 * @property currentPlayerId The player whose choice we are waiting for
 * @property remainingPlayers Players who still need to choose (APNAP order)
 * @property chosenTypes Creature types chosen so far by each player
 * @property creatureTypes The creature type options list
 * @property storeAs Key under which the chosen types are stored in storedStringLists
 */
@Serializable
data class EachPlayerChoosesCreatureTypeContinuation(
    override val decisionId: String,
    val sourceId: EntityId?,
    val sourceName: String?,
    val controllerId: EntityId,
    val currentPlayerId: EntityId,
    val remainingPlayers: List<EntityId>,
    val chosenTypes: List<String>,
    val creatureTypes: List<String>,
    val storeAs: String
) : ContinuationFrame

/**
 * Resume casting a spell after the player chooses a creature type during casting.
 *
 * Used for spells like Aphetto Dredging where the creature type choice is part of
 * casting (not resolution), so the opponent can see the chosen type on the stack.
 *
 * @property cardId The card being cast
 * @property casterId The player casting the spell
 * @property targets The chosen targets
 * @property xValue The X value if applicable
 * @property sacrificedPermanents Snapshots of permanents sacrificed as additional costs
 * @property targetRequirements The target requirements for resolution-time re-validation
 * @property count Legacy field, unused by pipeline effects
 * @property creatureTypes The creature type options (indexed by OptionChosenResponse.optionIndex)
 */
@Serializable
data class CastWithCreatureTypeContinuation(
    override val decisionId: String,
    val cardId: EntityId,
    val casterId: EntityId,
    val targets: List<ChosenTarget> = emptyList(),
    val xValue: Int? = null,
    val sacrificedPermanents: List<com.wingedsheep.engine.state.components.stack.PermanentSnapshot> = emptyList(),
    val targetRequirements: List<TargetRequirement> = emptyList(),
    val count: Int,
    val creatureTypes: List<String>
) : ContinuationFrame
