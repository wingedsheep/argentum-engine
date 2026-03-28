package com.wingedsheep.engine.core

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.BudgetMode
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlinx.serialization.Serializable

/**
 * Resume after player chooses a mode for a modal spell/ability.
 *
 * When a modal effect (e.g., "Choose one —") is executed, the player is presented
 * with a list of modes. After they choose, we need to execute the chosen mode's
 * effect, potentially after target selection.
 *
 * @property controllerId The player who controls the spell/ability
 * @property sourceId The spell/ability that has the modal effect
 * @property sourceName Name of the source for event messages
 * @property modes The serialized modes (effects + target requirements)
 * @property xValue The X value if applicable
 * @property opponentId The opponent player ID
 */
@Serializable
data class ModalContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val modes: List<@Serializable Mode>,
    val xValue: Int? = null,
    val opponentId: EntityId? = null,
    val triggeringEntityId: EntityId? = null
) : ContinuationFrame

/**
 * Resume after player selects targets for a chosen mode of a modal spell.
 *
 * After mode selection, if the chosen mode requires targets, this continuation
 * is pushed while the player selects targets.
 *
 * @property controllerId The player who controls the spell/ability
 * @property sourceId The spell/ability that has the modal effect
 * @property sourceName Name of the source for event messages
 * @property effect The chosen mode's effect to execute
 * @property xValue The X value if applicable
 * @property opponentId The opponent player ID
 */
@Serializable
data class ModalTargetContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val effect: Effect,
    val xValue: Int? = null,
    val opponentId: EntityId? = null,
    val targetRequirements: List<TargetRequirement> = emptyList(),
    /** Original modes list for cancelling back to mode selection */
    val modes: List<@Serializable Mode>? = null,
    val triggeringEntityId: EntityId? = null
) : ContinuationFrame

/**
 * Resume after player selects a creature to copy for Clone-style effects.
 *
 * When a permanent with EntersAsCopy resolves, the player is asked to choose
 * a creature on the battlefield. This continuation handles the response
 * and completes the permanent's entry to the battlefield.
 *
 * @property spellId The spell entity being resolved
 * @property controllerId The player who cast the spell
 * @property ownerId The owner of the card
 * @property castFaceDown Whether the spell was cast face-down
 * @property optional Whether the copy is optional (Clone is optional)
 */
@Serializable
data class CloneEntersContinuation(
    override val decisionId: String,
    val spellId: EntityId,
    val controllerId: EntityId,
    val ownerId: EntityId,
    val castFaceDown: Boolean
) : ContinuationFrame

/**
 * Resume after player chooses a color for an "as enters, choose a color" effect.
 *
 * When a permanent with EntersWithColorChoice resolves, the player is asked to choose
 * a color. This continuation handles the response and stores the chosen color.
 * If the permanent also has EntersWithCreatureTypeChoice, it chains to that decision next.
 *
 * @property spellId The spell entity being resolved
 * @property controllerId The player who cast the spell
 * @property ownerId The owner of the card
 */
@Serializable
data class ChooseColorEntersContinuation(
    override val decisionId: String,
    val spellId: EntityId,
    val controllerId: EntityId,
    val ownerId: EntityId
) : ContinuationFrame

/**
 * Resume after player chooses a creature type for an "as enters, choose a creature type" effect.
 *
 * When a permanent with EntersWithCreatureTypeChoice resolves, the player is asked to choose
 * a creature type. This continuation handles the response and completes the permanent's
 * entry to the battlefield with the chosen type stored as a component.
 *
 * @property spellId The spell entity being resolved
 * @property controllerId The player who cast the spell
 * @property ownerId The owner of the card
 * @property creatureTypes The list of creature types presented to the player
 */
@Serializable
data class ChooseCreatureTypeEntersContinuation(
    override val decisionId: String,
    val spellId: EntityId,
    val controllerId: EntityId,
    val ownerId: EntityId,
    val creatureTypes: List<String>
) : ContinuationFrame

/**
 * Resume after player chooses a creature for an "as enters, choose another creature you control" effect.
 *
 * When a permanent with EntersWithCreatureChoice resolves, the player is asked to choose
 * another creature they control. This continuation handles the response and stores the
 * chosen creature's EntityId as a component.
 *
 * @property spellId The spell entity being resolved
 * @property controllerId The player who cast the spell
 * @property ownerId The owner of the card
 */
@Serializable
data class ChooseCreatureEntersContinuation(
    override val decisionId: String,
    val spellId: EntityId,
    val controllerId: EntityId,
    val ownerId: EntityId
) : ContinuationFrame

/**
 * Resume after player reveals cards from hand for Amplify.
 *
 * When a creature with Amplify enters, the controller may reveal cards from hand
 * that share a creature type. For each revealed card, N +1/+1 counters are placed
 * on the creature as it enters.
 *
 * @property spellId The spell entity being resolved
 * @property controllerId The player who cast the spell
 * @property ownerId The card's owner
 * @property countersPerReveal Number of +1/+1 counters per revealed card (the N in "Amplify N")
 */
@Serializable
data class AmplifyEntersContinuation(
    override val decisionId: String,
    val spellId: EntityId,
    val controllerId: EntityId,
    val ownerId: EntityId,
    val countersPerReveal: Int
) : ContinuationFrame

/**
 * Resume after player chooses a budget modal combination (e.g., Season cycle pawprint modes).
 *
 * The executor pre-computes all valid combinations of modes that fit within the budget
 * and presents them as options. After the player chooses, this continuation maps the
 * chosen option back to a list of mode effects and executes them in order.
 *
 * @property controllerId The player who controls the spell
 * @property sourceId The spell entity
 * @property sourceName Name of the source for event messages
 * @property modes The budget modes (cost + effect)
 * @property combinations Pre-computed valid combinations, each a list of mode indices
 * @property opponentId The opponent player ID
 */
@Serializable
data class BudgetModalContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val modes: List<@Serializable BudgetMode>,
    val combinations: List<List<Int>>,
    val opponentId: EntityId? = null
) : ContinuationFrame
