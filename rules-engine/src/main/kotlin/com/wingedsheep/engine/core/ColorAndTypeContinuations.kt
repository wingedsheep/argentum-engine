package com.wingedsheep.engine.core

import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlinx.serialization.Serializable

/**
 * Resume after player chooses a color for protection granting effects.
 *
 * Used for effects like Akroma's Blessing: "Choose a color. Creatures you control
 * gain protection from the chosen color until end of turn."
 *
 * @property controllerId The player who controls the effect
 * @property sourceId The spell/ability that created this effect
 * @property sourceName Name of the source for event messages
 * @property filter Which creatures are affected
 * @property duration How long the effect lasts
 */
@Serializable
data class ChooseColorProtectionContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val filter: GroupFilter,
    val duration: Duration
) : ContinuationFrame

/**
 * Resume after player chooses a color for single-target protection granting effects.
 *
 * Used for effects like Jareth, Leonine Titan: "{W}: Jareth gains protection
 * from the color of your choice until end of turn."
 *
 * @property controllerId The player who controls the effect
 * @property sourceId The ability source that created this effect
 * @property sourceName Name of the source for event messages
 * @property targetEntityId The specific entity that gains protection
 * @property duration How long the effect lasts
 */
@Serializable
data class ChooseColorProtectionTargetContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val targetEntityId: EntityId,
    val duration: Duration
) : ContinuationFrame

/**
 * Resume after player chooses the FROM creature type for text replacement.
 *
 * Used for Artificial Evolution: "Change the text of target spell or permanent
 * by replacing all instances of one creature type with another."
 *
 * Step 1: Player chooses the creature type to replace.
 * The continuation handler then presents a second choice for the TO type.
 *
 * @property controllerId The player who controls the effect
 * @property sourceId The spell that created this effect
 * @property sourceName Name of the source for event messages
 * @property targetId The entity whose text is being changed
 * @property creatureTypes The creature type options presented (indexed by OptionChosenResponse.optionIndex)
 * @property excludedTypes Creature types that cannot be chosen as the TO type (e.g., "Wall" for Artificial Evolution)
 */
@Serializable
data class ChooseFromCreatureTypeContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val targetId: EntityId,
    val creatureTypes: List<String>,
    val excludedTypes: List<String> = emptyList()
) : ContinuationFrame

/**
 * Resume after player chooses the TO creature type for text replacement.
 *
 * Step 2: Player chooses the replacement creature type.
 * The continuation handler then applies the TextReplacementComponent.
 *
 * @property controllerId The player who controls the effect
 * @property sourceId The spell that created this effect
 * @property sourceName Name of the source for event messages
 * @property targetId The entity whose text is being changed
 * @property fromType The creature type being replaced (chosen in step 1)
 * @property creatureTypes The creature type options presented (indexed by OptionChosenResponse.optionIndex)
 */
@Serializable
data class ChooseToCreatureTypeContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val targetId: EntityId,
    val fromType: String,
    val creatureTypes: List<String>
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
